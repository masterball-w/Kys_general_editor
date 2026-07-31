package com.kys.editor.ui.context

import android.content.Context
import com.kys.editor.codec.*
import com.kys.editor.codec.meta.ItemMeta
import com.kys.editor.codec.meta.MagicMeta
import com.kys.editor.codec.meta.RoleMeta
import com.kys.editor.fs.GameRootResolver
import com.kys.editor.fs.SafHelper
import com.kys.editor.fs.VfsNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.nio.charset.Charset

enum class EditorTab { Save, Events, Battle, WorldMap, Assets, CrossRef }

class EditorContext(private val appContext: Context) {
    private val _dataRoot = MutableStateFlow<VfsNode?>(null)
    val dataRoot: StateFlow<VfsNode?> = _dataRoot.asStateFlow()

    private val _profile = MutableStateFlow(GameProfile.CLASSIC)
    val profile: StateFlow<GameProfile> = _profile.asStateFlow()

    private val _ranger = MutableStateFlow<RangerArchive?>(null)
    val ranger: StateFlow<RangerArchive?> = _ranger.asStateFlow()

    private val _kdef = MutableStateFlow<KdefArchive?>(null)
    val kdef: StateFlow<KdefArchive?> = _kdef.asStateFlow()

    private val _war = MutableStateFlow<WarArchive?>(null)
    val war: StateFlow<WarArchive?> = _war.asStateFlow()

    private val _talk = MutableStateFlow<TalkArchive?>(null)
    val talk: StateFlow<TalkArchive?> = _talk.asStateFlow()

    private val _worldMap = MutableStateFlow<WorldMapBundle?>(null)
    val worldMap: StateFlow<WorldMapBundle?> = _worldMap.asStateFlow()

    private val _currentSlot = MutableStateFlow(0)
    val currentSlot: StateFlow<Int> = _currentSlot.asStateFlow()

    private val _textEncoding = MutableStateFlow(Charset.forName("GBK"))
    val textEncoding: StateFlow<Charset> = _textEncoding.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    val itemMeta = ItemMeta
    val magicMeta = MagicMeta
    val roleMeta = RoleMeta

    init {
        SafHelper.init(appContext)
    }

    fun setDataRoot(root: VfsNode?) {
        _dataRoot.value = root
        if (root != null) {
            val detected = GameRootResolver.detectProfile(root)
            _profile.value = detected
            _textEncoding.value = when (detected.defaultTextEncoding) {
                "big5" -> Charset.forName("Big5")
                else -> Charset.forName("GBK")
            }
        }
    }

    suspend fun loadSlot(slot: Int) {
        val root = _dataRoot.value ?: return
        _isLoading.value = true
        try {
            withContext(Dispatchers.IO) {
                val saveDir = root.child("save")
                val arch = RangerArchive(RangerLayout.fromProfile(_profile.value))
                arch.textEncoding = _textEncoding.value
                arch.load(saveDir, slot)
                _ranger.value = arch
                _currentSlot.value = slot

                // Try to load kdef
                try {
                    val resDir = root.child("resource")
                    val k = KdefArchive()
                    k.load(resDir)
                    _kdef.value = k
                } catch (_: Exception) { _kdef.value = null }

                // Try to load war
                try {
                    val resDir = root.child("resource")
                    val w = WarArchive(_profile.value.war)
                    w.load(resDir)
                    _war.value = w
                } catch (_: Exception) { _war.value = null }

                // Try to load talk
                try {
                    val t = TalkArchive()
                    t.textEncoding = if (_profile.value.defaultTextEncoding == "big5") TextEnc.BIG5 else TextEnc.GBK
                    t.load(saveDir)
                    _talk.value = t
                } catch (_: Exception) { _talk.value = null }
            }
            _statusMessage.value = "存档 $slot 加载成功 - ${_profile.value.displayName}"
        } catch (e: Exception) {
            _statusMessage.value = "加载失败: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun saveCurrent() {
        val root = _dataRoot.value ?: return
        val arch = _ranger.value ?: return
        _isLoading.value = true
        try {
            withContext(Dispatchers.IO) {
                val saveDir = root.child("save")
                val grpNode = RangerArchive.resolveGrp(saveDir, _currentSlot.value)
                val data = arch.toBytes()
                grpNode.writeBytes(data)
            }
            _statusMessage.value = "保存成功"
        } catch (e: Exception) {
            _statusMessage.value = "保存失败: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun loadWorldMap() {
        val root = _dataRoot.value ?: return
        _isLoading.value = true
        try {
            withContext(Dispatchers.IO) {
                val resDir = root.child("resource")
                val wm = WorldMapBundle()
                wm.load(resDir)
                _worldMap.value = wm
            }
            _statusMessage.value = "大地图加载成功"
        } catch (e: Exception) {
            _statusMessage.value = "大地图加载失败: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun reloadAll() {
        loadSlot(_currentSlot.value)
    }
}
