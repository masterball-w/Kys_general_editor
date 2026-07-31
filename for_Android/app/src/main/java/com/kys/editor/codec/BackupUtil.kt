package com.kys.editor.codec

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Backup utility - mirrors editor/kys_formats/backup.py
 * Creates timestamped .bak copies before overwriting files.
 */
object BackupUtil {
    private val tsFmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    fun makeBackup(file: File): File? {
        if (!file.exists()) return null
        val ts = tsFmt.format(Date())
        val bak = File(file.parentFile, "${file.name}.${ts}.bak")
        file.copyTo(bak, overwrite = true)
        // Keep only last 5 backups
        val prefix = "${file.name}."
        val suffix = ".bak"
        file.parentFile?.listFiles { f ->
            f.name.startsWith(prefix) && f.name.endsWith(suffix)
        }?.sortedByDescending { it.name }
            ?.drop(5)
            ?.forEach { it.delete() }
        return bak
    }
}
