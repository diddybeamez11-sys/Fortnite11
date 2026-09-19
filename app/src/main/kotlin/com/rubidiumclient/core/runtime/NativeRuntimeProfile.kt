package com.rubidiumclient.core.runtime

import android.content.Context
import org.json.JSONObject

data class NativeRuntimeProfile(
    val minecraftVersion: String,
    val protocolVersion: Int,
    val abi: String,
    val runtimeName: String,
    val nativeLibrary: String,
    val nativeBuildId: String,
    val nativeSha256: String,
    val nativeLibrarySize: Long,
    val nativeSymbolsStripped: Boolean
) {
    companion object {
        private const val ASSET = "runtime/1.21.80-arm64-profile.json"

        fun load(context: Context): NativeRuntimeProfile {
            val json = context.assets.open(ASSET).bufferedReader().use { JSONObject(it.readText()) }
            return NativeRuntimeProfile(
                minecraftVersion = json.getString("minecraft_version"),
                protocolVersion = json.getInt("protocol_version"),
                abi = json.getString("abi"),
                runtimeName = json.getString("runtime_name"),
                nativeLibrary = json.getString("native_library"),
                nativeBuildId = json.optString("native_build_id"),
                nativeSha256 = json.optString("native_sha256"),
                nativeLibrarySize = json.optLong("native_library_size", 0L),
                nativeSymbolsStripped = json.optBoolean("native_symbols_stripped", true)
            )
        }
    }

    fun isExactTarget(version: String?): Boolean = version == minecraftVersion
}
