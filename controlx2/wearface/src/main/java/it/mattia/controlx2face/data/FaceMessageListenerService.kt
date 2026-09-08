package it.mattia.controlx2face.data

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.jwoglom.controlx2.shared.MessagePaths
import com.jwoglom.controlx2.shared.PumpMessageSerializer

class FaceMessageListenerService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != MessagePaths.FROM_PUMP_RECEIVE_MESSAGE) return

        try {
            val payload = messageEvent.data
            val deserialized = PumpMessageSerializer.fromBytes(payload)
            Log.d(TAG, "onMessageReceived: ${deserialized.javaClass.simpleName}")

            val state = FaceStateHolder.getInstance(this)
            val bridge = FacePumpMessageBridge(FacePrefs(this))
            bridge.processPumpMessage(deserialized)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process pump message", e)
        }
    }

    companion object {
        private const val TAG = "FaceMessageListener"
    }
}
