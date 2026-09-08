package it.mattia.controlx2face.data

import android.util.Log
import kotlin.concurrent.Volatile

class FaceState(private val prefs: FacePrefs) {
    @Volatile
    private var snapshot = prefs.getSnapshot()

    private val listeners = mutableListOf<(FaceSnapshot) -> Unit>()

    fun getSnapshot(): FaceSnapshot = snapshot

    fun updateSnapshot(block: (FaceSnapshot) -> FaceSnapshot) {
        val updated = block(snapshot)
        if (updated !== snapshot) {
            snapshot = updated
            prefs.setSnapshot(updated)
            notifyListeners(updated)
        }
    }

    fun addListener(listener: (FaceSnapshot) -> Unit) {
        synchronized(listeners) {
            listeners.add(listener)
        }
        listener(snapshot)
    }

    fun removeListener(listener: (FaceSnapshot) -> Unit) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    private fun notifyListeners(snapshot: FaceSnapshot) {
        synchronized(listeners) {
            listeners.forEach { listener ->
                try {
                    listener(snapshot)
                } catch (e: Exception) {
                    Log.e(TAG, "Listener threw exception", e)
                }
            }
        }
    }

    companion object {
        private const val TAG = "FaceState"
    }
}
