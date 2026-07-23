"""Big-map (earth/surface/building .002) overview editor."""

from __future__ import annotations

from PySide6.QtCore import Qt
from PySide6.QtWidgets import (
    QWidget, QVBoxLayout, QHBoxLayout, QLabel, QComboBox, QPushButton, QMessageBox,
)

from ui.context import EditorContext
from ui.map_view import MapOverviewPanel


class WorldMapEditorWidget(QWidget):
    def __init__(self, ctx: EditorContext) -> None:
        super().__init__()
        self.ctx = ctx
        lay = QVBoxLayout(self)
        top = QHBoxLayout()
        top.addWidget(QLabel("大地图层"))
        self.layer_combo = QComboBox()
        self.layer_combo.currentIndexChanged.connect(self._reload)
        top.addWidget(self.layer_combo)
        save = QPushButton("保存当前层 .002")
        save.clicked.connect(self._save)
        top.addWidget(save)
        top.addStretch()
        lay.addLayout(top)
        hint = QLabel(
            "大地图 480×480；地面色来自 earth + mmap 砖库主色。"
            "默认色块较小以便浏览；进入调整模式后点击写入笔刷值。"
        )
        hint.setWordWrap(True)
        lay.addWidget(hint)
        self.overview = MapOverviewPanel("大地图俯视图")
        self.overview.sp_cell.setValue(2)
        self.overview.chk_events.setVisible(False)
        lay.addWidget(self.overview)
        ctx.dataRootChanged.connect(lambda _: self.refresh())

    def refresh(self) -> None:
        self.layer_combo.blockSignals(True)
        self.layer_combo.clear()
        wm = self.ctx.world_map
        if not wm:
            self.layer_combo.blockSignals(False)
            return
        for key in wm.layers:
            self.layer_combo.addItem(key, key)
        self.layer_combo.blockSignals(False)
        self._reload()

    def _current_layer_key(self) -> str | None:
        return self.layer_combo.currentData()

    def _reload(self) -> None:
        wm = self.ctx.world_map
        key = self._current_layer_key()
        if not wm or not key or key not in wm.layers:
            return
        grid = wm.layers[key]
        earth = wm.layers.get("earth", grid)

        def get_code(x: int, y: int) -> int:
            return grid.get(x, y)

        def set_code(x: int, y: int, v: int) -> None:
            grid.set(x, y, v)

        def ground(x: int, y: int) -> int:
            return earth.get(x, y)

        self.overview.bind(
            grid.size,
            grid.size,
            get_code,
            set_code,
            ground_code=ground,
            event_code=None,
            tile_pack=self.ctx.mmap_tiles,
            palette=self.ctx.palette,
        )

    def _save(self) -> None:
        wm = self.ctx.world_map
        key = self._current_layer_key()
        if not wm or not key or key not in wm.layers:
            return
        try:
            wm.layers[key].save(backup=True)
            QMessageBox.information(self, "保存", f"已保存 {wm.layers[key].path}")
        except Exception as e:
            QMessageBox.critical(self, "失败", str(e))
