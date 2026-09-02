package it.mattia.glucoseglyph.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import it.mattia.glucoseglyph.model.AppSettings
import it.mattia.glucoseglyph.model.GlucoseState
import it.mattia.glucoseglyph.model.Trend
import it.mattia.glucoseglyph.glyph.MatrixRenderer
import it.mattia.glucoseglyph.glyph.PixelFont
import it.mattia.glucoseglyph.net.ControlX2Client
import it.mattia.glucoseglyph.service.GlucosePollingService
import it.mattia.glucoseglyph.ui.theme.GlucoseGlyphTheme
import it.mattia.glucoseglyph.ui.theme.NothingGrey
import it.mattia.glucoseglyph.ui.theme.NothingRed
import it.mattia.glucoseglyph.ui.theme.NothingWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var settings: AppSettings

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* if denied, the foreground service still runs, just silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettings(this)
        setContent {
            GlucoseGlyphTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    SettingsScreen(
                        settings = settings,
                        onStartService = ::startPollingService,
                        onStopService = ::stopPollingService
                    )
                }
            }
        }
    }

    private fun startPollingService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        startForegroundService(Intent(this, GlucosePollingService::class.java))
    }

    private fun stopPollingService() {
        startService(Intent(this, GlucosePollingService::class.java).setAction(GlucosePollingService.ACTION_STOP))
    }
}

@Composable
private fun SettingsScreen(
    settings: AppSettings,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    var host by remember { mutableStateOf(settings.host) }
    var port by remember { mutableStateOf(settings.port.toString()) }
    var username by remember { mutableStateOf(settings.username) }
    var password by remember { mutableStateOf(settings.password) }
    var pollInterval by remember { mutableStateOf(settings.pollIntervalSeconds.toString()) }
    var useMmol by remember { mutableStateOf(settings.useMmol) }
    var serviceOn by remember { mutableStateOf(settings.serviceEnabled) }
    var arrowStyle by remember { mutableStateOf(settings.arrowStyle) }
    var clockDigitStyle by remember { mutableStateOf(settings.clockDigitStyle) }
    var valueDigitStyle by remember { mutableStateOf(settings.valueDigitStyle) }
    var sensorDurationDays by remember { mutableStateOf(settings.sensorDurationDays) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    var showCustomization by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Live-refresh the matrix preview and status text as new readings arrive.
    var tick by remember { mutableStateOf(0L) }
    DisposableEffect(Unit) {
        val listener = { tick = System.currentTimeMillis() }
        GlucoseState.addListener(listener)
        onDispose { GlucoseState.removeListener(listener) }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tick = System.currentTimeMillis()
        }
    }

    // Fixed (non-scrolling) layout: everything essential lives on this one screen, and the
    // style pickers open in a bottom sheet instead of extending the page.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "GLUCOSE ",
                style = MaterialTheme.typography.titleLarge,
                color = NothingWhite
            )
            Text(
                text = "GLYPH",
                style = MaterialTheme.typography.titleLarge,
                color = NothingRed
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            MatrixPreview(
                reading = GlucoseState.current,
                useMmol = useMmol,
                tick = tick,
                arrowStyle = arrowStyle,
                clockDigitStyle = clockDigitStyle,
                valueDigitStyle = valueDigitStyle
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                UnitChip("mg/dL", selected = !useMmol) { useMmol = false; settings.useMmol = false }
                Spacer(Modifier.height(6.dp))
                UnitChip("mmol/L", selected = useMmol) { useMmol = true; settings.useMmol = true }
                Spacer(Modifier.height(10.dp))
                StatusLine(tick = tick)
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionLabel("CONTROLX2")
        SettingsCard {
            Row {
                LabeledField("Host", host, modifier = Modifier.weight(2f)) { host = it; settings.host = it }
                Spacer(Modifier.width(8.dp))
                LabeledField("Porta", port, KeyboardType.Number, modifier = Modifier.weight(1f)) {
                    port = it
                    it.toIntOrNull()?.let { p -> settings.port = p }
                }
            }
            LabeledField("Utente", username) { username = it; settings.username = it }
            LabeledField("Password", password, isPassword = true) {
                password = it; settings.password = it
            }

            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        testing = true
                        testResult = null
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                ControlX2Client(host, port.toIntOrNull() ?: 18282, username, password)
                                    .testConnection()
                            }
                            testing = false
                            testResult = when (result) {
                                is ControlX2Client.FetchResult.Success -> "OK — connesso"
                                is ControlX2Client.FetchResult.Failure -> result.message
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    enabled = !testing
                ) {
                    Text(if (testing) "..." else "Testa connessione")
                }
                testResult?.let {
                    Spacer(Modifier.width(10.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = NothingGrey)
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        SettingsCard {
            LabeledField("Intervallo di aggiornamento (secondi)", pollInterval, KeyboardType.Number) {
                pollInterval = it
                it.toIntOrNull()?.let { s -> settings.pollIntervalSeconds = s.coerceIn(15, 900) }
            }
            Spacer(Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Lettura in background",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NothingWhite
                )
                Switch(
                    checked = serviceOn,
                    onCheckedChange = {
                        serviceOn = it
                        if (it) onStartService() else onStopService()
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = NothingRed)
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "ControlX2 → Impostazioni → Debug → HTTP API (stesse credenziali). " +
                "Poi Glyph Toys sul retro → \"Glucose\".",
            style = MaterialTheme.typography.labelSmall,
            color = NothingGrey
        )

        Spacer(Modifier.weight(1f))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showCustomization = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "PERSONALIZZAZIONE",
                    style = MaterialTheme.typography.labelLarge,
                    color = NothingWhite
                )
                Text("›", style = MaterialTheme.typography.titleLarge, color = NothingRed)
            }
        }
    }

    if (showCustomization) {
        CustomizationSheet(
            arrowStyle = arrowStyle,
            clockDigitStyle = clockDigitStyle,
            valueDigitStyle = valueDigitStyle,
            sensorDurationDays = sensorDurationDays,
            onArrowStyle = { arrowStyle = it; settings.arrowStyle = it },
            onClockDigitStyle = { clockDigitStyle = it; settings.clockDigitStyle = it },
            onValueDigitStyle = { valueDigitStyle = it; settings.valueDigitStyle = it },
            onSensorDurationDays = { sensorDurationDays = it; settings.sensorDurationDays = it },
            onDismiss = { showCustomization = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomizationSheet(
    arrowStyle: PixelFont.ArrowStyle,
    clockDigitStyle: PixelFont.DigitStyle,
    valueDigitStyle: PixelFont.DigitStyle,
    sensorDurationDays: Int,
    onArrowStyle: (PixelFont.ArrowStyle) -> Unit,
    onClockDigitStyle: (PixelFont.DigitStyle) -> Unit,
    onValueDigitStyle: (PixelFont.DigitStyle) -> Unit,
    onSensorDurationDays: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 28.dp)) {
            SectionLabel("PERSONALIZZAZIONE")
            Spacer(Modifier.height(6.dp))

            // Offer only styles that actually have glyph data drawn in -- an enum entry without
            // a matching glyph set (e.g. added ahead of its artwork) must not be selectable.
            StyleLabel("Frecce")
            StyleRow(
                options = PixelFont.ArrowStyle.entries.filter { it in PixelFont.arrowSets },
                selected = arrowStyle,
                labelOf = { it.label },
                onSelect = onArrowStyle
            )
            StylePreviewRow(glyphs = arrowPreviewGlyphs(arrowStyle))

            Spacer(Modifier.height(12.dp))
            StyleLabel("Caratteri orologio")
            StyleRow(
                options = PixelFont.DigitStyle.entries.filter { it in PixelFont.clockDigitSets },
                selected = clockDigitStyle,
                labelOf = { it.label },
                onSelect = onClockDigitStyle
            )
            StylePreviewRow(glyphs = digitPreviewGlyphs(PixelFont.clockDigitSets, clockDigitStyle))

            Spacer(Modifier.height(12.dp))
            StyleLabel("Caratteri valore glicemico")
            StyleRow(
                options = PixelFont.DigitStyle.entries.filter { it in PixelFont.valueDigitSets },
                selected = valueDigitStyle,
                labelOf = { it.label },
                onSelect = onValueDigitStyle
            )
            StylePreviewRow(glyphs = digitPreviewGlyphs(PixelFont.valueDigitSets, valueDigitStyle))

            Spacer(Modifier.height(12.dp))
            StyleLabel("Durata sensore (per i giorni alla scadenza)")
            StyleRow(
                options = SENSOR_DURATION_OPTIONS,
                selected = sensorDurationDays,
                labelOf = { "$it gg" },
                onSelect = onSensorDurationDays
            )
        }
    }
}

// ControlX2/the pump don't report a CGM sensor's total lifespan, only when its session started,
// so the user picks the closest match for their own sensor here -- common Dexcom/Libre lifespans.
private val SENSOR_DURATION_OPTIONS = listOf(7, 10, 14, 15, 21)

/** Digits 0-9 of the given style, for the preview strip under its picker. */
private fun digitPreviewGlyphs(
    sets: Map<PixelFont.DigitStyle, PixelFont.GlyphSet>,
    style: PixelFont.DigitStyle
): List<List<String>> {
    val set = sets[style] ?: sets.getValue(PixelFont.DigitStyle.CURRENT)
    return ('0'..'9').map { set.glyphs.getValue(it) }
}

/** The five distinct arrow symbols of the given style (up/down each cover single and double). */
private fun arrowPreviewGlyphs(style: PixelFont.ArrowStyle): List<List<String>> {
    val set = PixelFont.arrowSets[style] ?: PixelFont.arrowSets.getValue(PixelFont.ArrowStyle.CURRENT)
    return listOf(
        Trend.SINGLE_UP, Trend.FORTY_FIVE_UP, Trend.FLAT, Trend.FORTY_FIVE_DOWN, Trend.SINGLE_DOWN
    ).map { set.getValue(it) }
}

/** A one-line strip previewing the selected style's glyphs as warm-white rounded squares on
 * black, echoing the Glyph Matrix's own cell look (per the designer's request; not the round
 * dots the main matrix preview uses). */
@Composable
private fun StylePreviewRow(glyphs: List<List<String>>) {
    val rows = glyphs.maxOf { it.size }
    val widths = glyphs.map { glyph -> glyph.maxOf { it.length } }
    val totalCols = widths.sum() + (glyphs.size - 1) // 1 blank column between glyphs
    val cellColor = Color(0xFFFFF8F0)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(Color.Black, RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .height(30.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cell = minOf(size.height / rows, size.width / totalCols)
            val square = cell * 0.84f
            val inset = (cell - square) / 2f
            val corner = CornerRadius(square * 0.28f)
            val x0 = (size.width - cell * totalCols) / 2f
            val y0 = (size.height - cell * rows) / 2f
            var colCursor = 0
            glyphs.forEachIndexed { i, glyph ->
                glyph.forEachIndexed { r, line ->
                    line.forEachIndexed { c, ch ->
                        if (ch == '1') {
                            drawRoundRect(
                                color = cellColor,
                                topLeft = Offset(
                                    x = x0 + (colCursor + c) * cell + inset,
                                    y = y0 + r * cell + inset
                                ),
                                size = Size(square, square),
                                cornerRadius = corner
                            )
                        }
                    }
                }
                colCursor += widths[i] + 1
            }
        }
    }
}

@Composable
private fun StyleLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = NothingGrey,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun <T> StyleRow(
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option ->
            UnitChip(labelOf(option), selected = option == selected) { onSelect(option) }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = NothingGrey,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), content = content)
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    modifier: Modifier = Modifier,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, color = NothingGrey, style = MaterialTheme.typography.bodySmall) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = NothingWhite,
            unfocusedTextColor = NothingWhite,
            focusedBorderColor = NothingRed,
            unfocusedBorderColor = NothingGrey,
            cursorColor = NothingRed
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    )
}

@Composable
private fun UnitChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) NothingRed else Color.Transparent,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = if (selected) NothingWhite else NothingGrey,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun StatusLine(tick: Long) {
    val reading = GlucoseState.current
    val error = GlucoseState.lastError
    val text = when {
        error != null -> "Errore: $error"
        reading == null -> "Nessuna lettura ancora"
        else -> {
            val ageSec = ((System.currentTimeMillis() - reading.receivedEpochMillis) / 1000).coerceAtLeast(0)
            "${reading.mgdl} mg/dL · aggiornato ${formatAge(ageSec)} fa"
        }
    }
    Text(text, style = MaterialTheme.typography.bodySmall, color = NothingGrey)
}

private fun formatAge(seconds: Long): String = when {
    seconds < 60 -> "${seconds}s"
    seconds < 3600 -> "${seconds / 60}m"
    else -> "${seconds / 3600}h"
}

@Composable
private fun MatrixPreview(
    reading: it.mattia.glucoseglyph.model.GlucoseReading?,
    useMmol: Boolean,
    tick: Long,
    arrowStyle: PixelFont.ArrowStyle,
    clockDigitStyle: PixelFont.DigitStyle,
    valueDigitStyle: PixelFont.DigitStyle
) {
    // Clock/battery are drawn only on the physical Glyph Matrix (GlucoseToyService); this
    // preview intentionally mirrors just the glucose reading itself.
    val grid = remember(reading, useMmol, tick, arrowStyle, clockDigitStyle, valueDigitStyle) {
        MatrixRenderer.render(
            reading, useMmol,
            valueDigitStyle = valueDigitStyle,
            clockDigitStyle = clockDigitStyle,
            arrowStyle = arrowStyle
        )
    }
    Box(
        modifier = Modifier
            .size(112.dp)
            .background(Color.Black, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSize = MatrixRenderer.SIZE
            val cell = size.minDimension / gridSize
            val gap = cell * 0.18f
            val dot = cell - gap
            for (row in 0 until gridSize) {
                for (col in 0 until gridSize) {
                    val brightness = grid[row * gridSize + col]
                    if (brightness <= 0) continue
                    val alpha = (brightness / 255f).coerceIn(0.12f, 1f)
                    drawCircle(
                        color = Color(1f, 1f, 1f, alpha),
                        radius = dot / 2f,
                        center = Offset(
                            x = col * cell + cell / 2f,
                            y = row * cell + cell / 2f
                        )
                    )
                }
            }
        }
    }
}
