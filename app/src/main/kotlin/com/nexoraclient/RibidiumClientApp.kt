package com.rubidiumclient

import android.app.Application
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import java.io.File
import com.rubidiumclient.auth.AccountManager
import com.rubidiumclient.auth.MicrosoftAuthManager
import com.rubidiumclient.config.Config
import com.rubidiumclient.config.ServerConfig
import com.rubidiumclient.core.relay.Definitions
import com.rubidiumclient.core.runtime.NativeRuntimeBridge
import com.rubidiumclient.core.runtime.NativeGameBridge
import com.rubidiumclient.core.runtime.HybridDiagnostics
import com.rubidiumclient.module.ModuleManager
import com.rubidiumclient.module.social.FriendManager
import com.rubidiumclient.utils.ItemIconProvider
import com.rubidiumclient.utils.ErrorLog      // <-- ADDED
import com.rubidiumclient.utils.WorldBlockTracker   // <-- ADDED

class RubidiumClientApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        private const val TAG = "RubidiumClientApp"
        lateinit var instance: RubidiumClientApp
            private set
    }

    override fun onTerminate() {
        HybridDiagnostics.stop()
        NativeGameBridge.stop()
        applicationScope.cancel()
        super.onTerminate()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        ErrorLog.init(applicationContext)
        installCrashLogger()

        ServerConfig.init(applicationContext)
        Config.init(applicationContext)
        ModuleManager.init(applicationContext)
        AccountManager.init(applicationContext)
        MicrosoftAuthManager.init(applicationContext)
        FriendManager.init(applicationContext)
        NativeRuntimeBridge.init(applicationContext)
        NativeGameBridge.start(applicationScope)
        HybridDiagnostics.start()

        Thread({
            try {
                Definitions.init(applicationContext)
            } catch (e: Exception) {
                Log.e(TAG, "Definitions load error: ${e.message}", e)
                ErrorLog.record(message = "Definitions load error: ${e.message}", throwable = e)
            }
        }, "RubidiumDefinitionsLoader").apply {
            isDaemon = true
            start()
        }

        WorldBlockTracker.init()    // <-- now resolved
        ItemIconProvider.init(applicationContext)  // <-- now resolved

        // Module registration is now handled inside ModuleManager.init
        // No need to call registerModules() here.
    }

    private fun installCrashLogger() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            ErrorLog.record(message = "UNCAUGHT EXCEPTION on ${thread.name}", throwable = throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

}
