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

        // A channel's importance is immutable once created, so each past importance change here
        // needed a new channel ID -- but Android never removes old channels on its own, so every
        // one of those IDs was still piling up as its own separate "category" in system Settings.
        // Delete anything left over from an earlier ID scheme; only NOTIFICATION_CHANNEL_ID
        // should ever show up there, however many more times the importance changes later.
        for (existing in manager.notificationChannels) {
            if (existing.id.startsWith(CHANNEL_ID_PREFIX) && existing.id != NOTIFICATION_CHANNEL_ID) {
                manager.deleteNotificationChannel(existing.id)
            }
        }

        // IMPORTANCE_HIGH: the highest sort position a notification can get. HIGH normally also
        // means a heads-up popup + sound on every post, but this channel's sound/vibration are
        // explicitly disabled below and the notification uses setOnlyAlertOnce(true), so updates
        // stay quiet.
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
        private const val CHANNEL_ID_PREFIX = "glucose_polling"
        const val NOTIFICATION_CHANNEL_ID = "${CHANNEL_ID_PREFIX}_v4"
    }
}
