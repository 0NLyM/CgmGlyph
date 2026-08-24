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
        // IMPORTANCE_LOW (not MIN): MIN explicitly hides the icon from the status bar on Android,
        // only showing it in the pulled-down notification list. LOW keeps it silent (no sound,
        // no heads-up popup) while still showing the status-bar icon, which is the whole point of
        // rendering the glucose value into it.
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Monitoraggio glucosio",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Servizio in background che legge il glucosio da ControlX2"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        // Bumped from "glucose_polling": a channel's importance is immutable once created, so
        // installs that already had the old (IMPORTANCE_MIN, status-bar-hidden) channel need a
        // fresh ID to actually pick up IMPORTANCE_LOW.
        const val NOTIFICATION_CHANNEL_ID = "glucose_polling_v2"
    }
}
