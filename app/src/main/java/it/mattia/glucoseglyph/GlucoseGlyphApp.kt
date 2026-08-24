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
        // IMPORTANCE_HIGH: the highest sort position a notification can get, which is what was
        // asked for after DEFAULT still wasn't landing at the top. HIGH normally also means a
        // heads-up popup + sound on every post, but this channel's sound/vibration are explicitly
        // disabled below and the notification uses setOnlyAlertOnce(true), so updates stay quiet.
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Monitoraggio glucosio",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Servizio in background che legge il glucosio da ControlX2"
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        // Bumped from "glucose_polling_v3": a channel's importance is immutable once created, so
        // installs that already had the old (IMPORTANCE_DEFAULT) channel need a fresh ID to
        // actually pick up IMPORTANCE_HIGH's higher sort position.
        const val NOTIFICATION_CHANNEL_ID = "glucose_polling_v4"
    }
}
