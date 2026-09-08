package it.mattia.controlx2face.data

import android.content.Context

object FaceStateHolder {
    @Volatile
    private var state: FaceState? = null

    fun getInstance(context: Context): FaceState {
        return state ?: synchronized(this) {
            state ?: FaceState(FacePrefs(context.applicationContext)).also { state = it }
        }
    }
}
