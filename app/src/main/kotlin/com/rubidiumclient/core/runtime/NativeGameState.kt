package com.rubidiumclient.core.runtime

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.Socket
import org.json.JSONObject

/** Loopback telemetry from the Native runtime running inside Minecraft. */
data class NativeGameState(
    val connected: Boolean = false,
    val libraryLoaded: Boolean = false,
    val fingerprintMatched: Boolean = false,
    val symbolsReady: Boolean = false,
    val hookInstalled: Boolean = false,
    val playerSeen: Boolean = false,
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val yaw: Float = 0f,
    val nativeHeartbeat: Long = 0L,
    val lastNativeUpdate: Long = 0L,
    val buildId: String = "",
    val libraryPath: String = "",
    val status: String = "not connected"
)

object NativeGameBridge {
    private const val TAG = "NativeGameBridge"
    private const val HOST = "127.0.0.1"
    private const val PORT = 38170

    @Volatile var state: NativeGameState = NativeGameState()
        private set
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch(Dispatchers.IO) {
            while (isActive) {
                pollOnce()
                delay(750)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        state = NativeGameState()
    }

    private fun pollOnce() {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(HOST, PORT), 350)
                socket.soTimeout = 700
                val line = socket.getInputStream().bufferedReader().readLine() ?: return
                val o = JSONObject(line)
                state = NativeGameState(
                    connected = true,
                    libraryLoaded = o.optBoolean("library"),
                    fingerprintMatched = o.optBoolean("fingerprint"),
                    symbolsReady = o.optBoolean("symbols"),
                    hookInstalled = o.optBoolean("hook"),
                    playerSeen = o.optBoolean("player"),
                    x = o.optDouble("x", 0.0).toFloat(),
                    y = o.optDouble("y", 0.0).toFloat(),
                    z = o.optDouble("z", 0.0).toFloat(),
                    yaw = o.optDouble("yaw", 0.0).toFloat(),
                    nativeHeartbeat = o.optLong("heartbeat", 0L),
                    lastNativeUpdate = o.optLong("timestamp", 0L),
                    buildId = o.optString("buildId", ""),
                    libraryPath = o.optString("path", ""),
                    status = o.optString("status", "unknown")
                )
            }
        } catch (_: Exception) {
            if (state.connected) Log.d(TAG, "Native runtime disconnected")
            state = NativeGameState()
        }
    }
}
