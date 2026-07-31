package com.kys.editor

import android.app.Application
import com.kys.editor.fs.SafHelper
import com.kys.editor.ui.context.EditorContext
import com.kys.editor.util.BitmapCache

class KysEditorApp : Application() {
    lateinit var editorContext: EditorContext
        private set

    override fun onCreate() {
        super.onCreate()
        SafHelper.init(this)
        BitmapCache.init(this)
        editorContext = EditorContext(this)
    }
}
