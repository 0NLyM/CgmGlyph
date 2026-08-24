package it.mattia.glucoseglyph

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

class GlucoseGlyphApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // IMPORTANCE_DEFAULT (not LOW/MIN): notification sort position in the shade/status bar is
        // driven by channel importance, and MIN/LOW notifications get pushed below DEFAULT and
        // HIGH ones. DEFAULT is the highest level that doesn't add a heads-up popup on every
        // update (HIGH does); explicitly silencing the channel's sound below keeps it from
        // dinging on every poll despite the higher importance.
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Monitoraggio glucosio",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Servizio in background che legge il glucosio da ControlX2"
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        // Bumped from "glucose_polling_v2": a channel's importance is immutable once created, so
        // installs that already had the old (IMPORTANCE_LOW) channel need a fresh ID to actually
        // pick up IMPORTANCE_DEFAULT's higher sort position.
        const val NOTIFICATION_CHANNEL_ID = "glucose_polling_v3"
    }
}
