package it.mattia.glucoseglyph.net

import it.mattia.glucoseglyph.model.GlucoseReading
import it.mattia.glucoseglyph.model.PUMP_EPOCH_OFFSET_SECONDS
import it.mattia.glucoseglyph.model.Trend
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Talks to the HTTP Debug API that ControlX2 exposes on-device (Settings > Debug > HTTP API).
 * See ControlX2's HttpDebugApiService/openapi.json for the underlying contract.
 */
class ControlX2Client(
    private val host: String,
    private val port: Int,
    private val username: String,
    private val password: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "http://$host:$port"
    private val authHeader = Credentials.basic(username, password)

    sealed class FetchResult {
        data class Success(val reading: GlucoseReading) : FetchResult()
        data class Failure(val message: String) : FetchResult()
    }

    /** Verifies host/port/credentials by hitting the unauthenticated-shape index endpoint. */
    fun testConnection(): FetchResult {
        val request = Request.Builder()
            .url(baseUrl + "/")
            .header("Authorization", authHeader)
            .get()
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    FetchResult.Success(emptyReading())
                } else {
                    FetchResult.Failure(httpErrorMessage(response.code))
                }
            }
        } catch (e: IOException) {
            FetchResult.Failure(e.message ?: "Errore di rete")
        }
    }

    /**
     * Actively asks the pump for a fresh CGM reading and its trend arrow, plus its own battery
     * level and reservoir insulin units, in a single batch:
     *  - opCode 34 (CurrentEGVGuiDataRequest)  -> CurrentEGVGuiDataResponse: mg/dL + timestamp
     *  - opCode 56 (HomeScreenMirrorRequest)   -> HomeScreenMirrorResponse: cgmTrendIconId, the
     *    exact same trend arrow ControlX2's own Dashboard/home screen shows.
     *  - opCode 52 (CurrentBatteryV1Request)   -> CurrentBatteryV1Response: currentBatteryIbc, the
     *    pump's own displayed battery percent.
     *  - opCode 36 (InsulinStatusRequest)      -> InsulinStatusResponse: currentInsulinAmount, the
     *    reservoir/cartridge units remaining.
     * All four use the CURRENT_STATUS characteristic. The battery/reservoir messages are optional
     * extras -- a missing or malformed reply for either just leaves that field null, it never
     * fails the whole reading (only a missing EGV does that).
     */
    fun fetchLatestReading(): FetchResult {
        val body = JSONArray()
            .put(JSONObject().put("cargo", "").put("opCode", 34).put("characteristic", "CURRENT_STATUS"))
            .put(JSONObject().put("cargo", "").put("opCode", 56).put("characteristic", "CURRENT_STATUS"))
            .put(JSONObject().put("cargo", "").put("opCode", 52).put("characteristic", "CURRENT_STATUS"))
            .put(JSONObject().put("cargo", "").put("opCode", 36).put("characteristic", "CURRENT_STATUS"))
            .toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(baseUrl + "/api/pump/messages")
            .header("Authorization", authHeader)
            .post(body)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return FetchResult.Failure(httpErrorMessage(response.code))
                }
                val text = response.body?.string().orEmpty()
                parseEgvResponse(text)
                    ?: FetchResult.Failure("Nessuna lettura CGM nella risposta del pump")
            }
        } catch (e: IOException) {
            FetchResult.Failure(e.message ?: "Errore di rete")
        }
    }

    private fun parseEgvResponse(text: String): FetchResult? {
        val array = try {
            JSONArray(text)
        } catch (e: Exception) {
            return null
        }

        var egv: JSONObject? = null
        var homeScreenTrendIconId: Int? = null
        var homeScreenAlertIconId: Int? = null
        var pumpBatteryPercent: Int? = null
        var reservoirUnits: Int? = null
        for (i in 0 until array.length()) {
            val message = array.optJSONObject(i) ?: continue
            val name = message.optString("name")
            val params = message.optJSONObject("params") ?: continue
            when {
                name.endsWith("CurrentEGVGuiDataResponse") -> egv = params
                name.endsWith("HomeScreenMirrorResponse") -> {
                    homeScreenTrendIconId = params.optInt("cgmTrendIconId", -1).takeIf { it >= 0 }
                    homeScreenAlertIconId = params.optInt("cgmAlertIconId", -1).takeIf { it >= 0 }
                }
                // CurrentBatteryV1Response and CurrentBatteryV2Response both expose the pump's
                // displayed battery percent under this same field name.
                name.endsWith("CurrentBatteryV1Response") || name.endsWith("CurrentBatteryV2Response") ->
                    pumpBatteryPercent = params.optInt("currentBatteryIbc", -1).takeIf { it in 0..100 }
                name.endsWith("InsulinStatusResponse") ->
                    reservoirUnits = params.optInt("currentInsulinAmount", -1).takeIf { it >= 0 }
            }
        }

        val params = egv ?: return null
        val cgmReading = params.optInt("cgmReading", -1)
        if (cgmReading < 0) return null
        val pumpTimestampSeconds = params.optLong("bgReadingTimestampSeconds", 0L)
        val readingEpochMillis = (pumpTimestampSeconds + PUMP_EPOCH_OFFSET_SECONDS) * 1000L

        // Prefer the pump's own trend icon (matches ControlX2's UI exactly); fall back to the
        // trendRate heuristic only if HomeScreenMirrorResponse wasn't in the batch response.
        val trend = homeScreenTrendIconId?.let { Trend.fromCgmTrendIconId(it) }
            ?: Trend.fromTrendRate(params.optInt("trendRate", 0))

        return FetchResult.Success(
            GlucoseReading(
                mgdl = cgmReading,
                trend = trend,
                readingEpochMillis = readingEpochMillis,
                receivedEpochMillis = System.currentTimeMillis(),
                // ControlX2's own convention (GlucoseHeroCard): mgdl == 0 means no CGM is
                // connected to the pump, shown there as "n/a".
                valid = cgmReading > 0,
                // HomeScreenMirrorResponse.CGMAlertIcon.REPLACE_SENSOR == 11: the pump's own
                // "sensor expired, insert a new one" alert.
                sensorExpired = homeScreenAlertIconId == 11,
                pumpBatteryPercent = pumpBatteryPercent,
                reservoirUnits = reservoirUnits
            )
        )
    }

    private fun emptyReading() = GlucoseReading(
        mgdl = 0,
        trend = Trend.UNKNOWN,
        readingEpochMillis = 0,
        receivedEpochMillis = System.currentTimeMillis(),
        valid = false
    )

    private fun httpErrorMessage(code: Int): String = when (code) {
        401 -> "Credenziali non valide"
        404 -> "API non trovata (versione ControlX2 non compatibile?)"
        else -> "Errore HTTP $code"
    }
}
