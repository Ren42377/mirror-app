package com.ren42377.mirror.data

import android.content.Context
import java.io.File

object RecordingCacheManager {
    private const val RECORDINGS_FOLDER = "recordings"

    fun getRecordingsDir(context: Context): File {
        val dir = File(context.cacheDir, RECORDINGS_FOLDER)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun createTempVideoFile(context: Context): File {
        clearAllRecordings(context)
        val dir = getRecordingsDir(context)
        return File(dir, "mirror_${System.currentTimeMillis()}.mp4")
    }

    fun deleteRecording(file: File?) {
        if (file != null && file.exists()) {
            runCatching { file.delete() }
        }
    }

    fun clearAllRecordings(context: Context) {
        val dir = File(context.cacheDir, RECORDINGS_FOLDER)
        if (dir.exists() && dir.isDirectory) {
            dir.listFiles()?.forEach { child ->
                runCatching { child.delete() }
            }
        }
    }
}
