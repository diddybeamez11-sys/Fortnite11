package com.rubidiumclient.core.runtime

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.rubidiumclient.ui.overlay.OverlayService
import com.rubidiumclient.utils.ErrorLog

/**
 * Controlled-session launcher modeled on the observable Toolbox launch contract.
 *
 * This deliberately uses Android's task/document launch APIs instead of claiming
 * that a normal package launch is an in-process native injection. The actual
 * native bridge reports attachment separately and remains truthful on modern
 * Android where third-party instrumentation of another app is restricted.
 */
object ToolboxStyleLauncher {
    private const val TAG = "EClientToolboxLaunch"
    const val MINECRAFT_PACKAGE = "com.mojang.minecraftpe"
    const val TARGET_VERSION = "1.21.80.3"
    const val EXTRA_CONTROLLED_SESSION = "com.rubidiumclient.controlled_session"

    fun launch(activity: Activity, targetPackage: String = MINECRAFT_PACKAGE): Boolean {
        val info = try {
            activity.packageManager.getPackageInfo(targetPackage, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            toast(activity, "Minecraft is not installed")
            ErrorLog.record("Toolbox-style launch failed: Minecraft package not installed")
            return false
        }

        if (targetPackage == MINECRAFT_PACKAGE && info.versionName != TARGET_VERSION) {
            toast(activity, "E-Client requires Minecraft $TARGET_VERSION (found ${info.versionName})")
            ErrorLog.record("Toolbox-style launch rejected: expected $TARGET_VERSION, found ${info.versionName}")
            return false
        }

        val launchIntent = activity.packageManager.getLaunchIntentForPackage(targetPackage)
            ?: run {
                toast(activity, "Minecraft launcher activity was not found")
                ErrorLog.record("Toolbox-style launch failed: no launch intent for $targetPackage")
                return false
            }

        // Start the overlay before Minecraft so the client UI is ready when the
        // new task appears. Login remains entirely inside Minecraft.
        runCatching { OverlayService.start(activity) }
            .onFailure { ErrorLog.record("Overlay startup before Minecraft launch failed", it) }

        launchIntent.apply {
            putExtra(EXTRA_CONTROLLED_SESSION, true)
            putExtra("eclient_target_version", TARGET_VERSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
            addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            component?.let { ComponentName(it.packageName, it.className) }
        }

        return runCatching {
            activity.startActivity(launchIntent)
            Log.i(TAG, "Started controlled Minecraft task for ${info.versionName}")
            ErrorLog.record("Started Toolbox-style Minecraft task: ${info.versionName}")
            true
        }.getOrElse {
            Log.e(TAG, "Controlled Minecraft launch failed", it)
            ErrorLog.record("Controlled Minecraft launch failed", it)
            toast(activity, "Minecraft could not be started: ${it.javaClass.simpleName}")
            false
        }
    }

    private fun toast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
