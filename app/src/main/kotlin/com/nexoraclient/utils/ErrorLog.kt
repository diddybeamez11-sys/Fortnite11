package com.rubidiumclient.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Persistent application error log written to the public Downloads folder. */
object ErrorLog {
    private const val TAG = "EClientErrorLog"
    private const val FILE_NAME = "EClient-errors.txt"
    private val RELATIVE_PATH = Environment.DIRECTORY_DOWNLOADS + "/E-Client/"

    @Volatile private var appContext: Context? = null

    fun init(context: Context) { appContext = context.applicationContext }

    fun record(context: Context? = appContext, message: String, throwable: Throwable? = null) {
        val ctx = context ?: return
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US).format(Date())
        val stack = throwable?.let { "\n${Log.getStackTraceString(it)}" } ?: ""
        val entry = "[$time] $message$stack\n\n"
        try { append(ctx, entry) } catch (e: Throwable) { Log.e(TAG, "Unable to write persistent error log", e) }
    }

    private fun append(context: Context, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val uri = findOrCreateUri(context.contentResolver) ?: return
            context.contentResolver.openOutputStream(uri, "wa")?.use {
                it.write(text.toByteArray(Charsets.UTF_8))
            }
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).resolve("E-Client")
            if (!dir.exists()) dir.mkdirs()
            File(dir, FILE_NAME).appendText(text, Charsets.UTF_8)
        }
    }

    private fun findOrCreateUri(resolver: android.content.ContentResolver): android.net.Uri? {
        resolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Downloads._ID),
            "${MediaStore.Downloads.DISPLAY_NAME}=? AND ${MediaStore.Downloads.RELATIVE_PATH}=?",
            arrayOf(FILE_NAME, RELATIVE_PATH), null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return android.content.ContentUris.withAppendedId(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, cursor.getLong(0)
                )
            }
        }
        return resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, FILE_NAME)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, RELATIVE_PATH)
        })
    }
}
