package com.rubidiumclient.core.runtime

import android.content.Context
import android.util.Log
import com.rubidiumclient.utils.ErrorLog

/**
 * Standalone E-Client native runtime control plane.
 *
 * The native library is bundled with E-Client and runs in the E-Client Android
 * process. It validates the selected Minecraft 1.21.80.3 ARM64 target and exposes
 * diagnostics over loopback. It does not claim that Minecraft is running in the
 * same process unless a future runtime attachment mechanism explicitly proves it.
 */
object NativeRuntimeBridge {
    private const val TAG = "NativeRuntimeBridge"
    private const val LIBRARY = "eclient_runtime"

    @Volatile private var loaded = false

    init {
        try {
            System.loadLibrary(LIBRARY)
            loaded = true
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to load native runtime", t)
            ErrorLog.record(message = "Failed to load native runtime", throwable = t)
        }
    }

    @JvmStatic private external fun nativeStart(): Boolean
    @JvmStatic private external fun nativeStop()

    fun init(context: Context): NativeRuntimeProfile {
        val profile = NativeRuntimeProfile.load(context.applicationContext)
        if (loaded) {
            runCatching { nativeStart() }
                .onSuccess { ok ->
                    if (!ok) {
                        Log.e(TAG, "Native runtime reported startup failure")
                        ErrorLog.record(message = "Native runtime reported startup failure")
                    }
                }
                .onFailure {
                    Log.e(TAG, "Native runtime start failed", it)
                    ErrorLog.record(message = "Native runtime start failed", throwable = it)
                }
        }
        Log.i(TAG, "Target ${profile.minecraftVersion} / protocol ${profile.protocolVersion} / ${profile.abi}")
        return profile
    }

    fun profile(context: Context): NativeRuntimeProfile = NativeRuntimeProfile.load(context.applicationContext)

    fun status(context: Context): String {
        val p = profile(context)
        return "Standalone native runtime • ${p.minecraftVersion} • protocol ${p.protocolVersion} • ${p.abi}"
    }

    fun stop() {
        if (loaded) runCatching { nativeStop() }
    }
}
