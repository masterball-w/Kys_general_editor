package com.kys.editor.fs

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

object SafHelper {
    internal var appContext: Context? = null

    private const val PREFS = "kys_editor_prefs"
    private const val KEY_ROOT_URI = "data_root_uri"

    fun init(ctx: Context) {
        appContext = ctx.applicationContext
    }

    fun selectDirectoryIntent(): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }

    fun saveRootUri(uri: Uri) {
        val ctx = appContext ?: return
        val resolver = ctx.contentResolver
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        resolver.takePersistableUriPermission(uri, takeFlags)
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_ROOT_URI, uri.toString()).apply()
    }

    fun loadSavedRoot(): DocumentFile? {
        val ctx = appContext ?: return null
        val uriStr = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ROOT_URI, null) ?: return null
        val uri = Uri.parse(uriStr)
        return try {
            DocumentFile.fromTreeUri(ctx, uri)
        } catch (_: Exception) {
            null
        }
    }

    fun clearRoot() {
        val ctx = appContext ?: return
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_ROOT_URI).apply()
    }

    fun getRootVfs(): VfsNode? {
        val doc = loadSavedRoot() ?: return null
        return SafNode(doc)
    }
}
