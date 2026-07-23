"""Big-map (.002) grid codecs — 480×480 int16 tile codes."""

from __future__ import annotations

import struct
from pathlib import Path
from typing import List, Optional

from .backup import atomic_write, backup_file

WORLD_SIZE = 480
WORLD_BYTES = WORLD_SIZE * WORLD_SIZE * 2  # 460800


class WorldLayerGrid:
    """One 480×480 int16 layer (earth / surface / building / …)."""

    def __init__(self) -> None:
        self.path: Optional[Path] = None
        self.size = WORLD_SIZE
        # [x][y]
        self.grid: List[List[int]] = []

    def load(self, path: str | Path) -> None:
        path = Path(path)
        self.path = path
        raw = path.read_bytes()
        if len(raw) < WORLD_BYTES:
            raise ValueError(f"{path.name} size {len(raw)} < {WORLD_BYTES}")
        self.grid = [[0] * WORLD_SIZE for _ in range(WORLD_SIZE)]
        for x in range(WORLD_SIZE):
            for y in range(WORLD_SIZE):
                off = (x * WORLD_SIZE + y) * 2
                self.grid[x][y] = struct.unpack_from("<h", raw, off)[0]

    def get(self, x: int, y: int) -> int:
        return self.grid[x][y]

    def set(self, x: int, y: int, value: int) -> None:
        self.grid[x][y] = int(value)

    def to_bytes(self) -> bytes:
        out = bytearray()
        for x in range(WORLD_SIZE):
            for y in range(WORLD_SIZE):
                out.extend(struct.pack("<h", self.grid[x][y]))
        return bytes(out)

    def save(self, backup: bool = True) -> None:
        if not self.path:
            raise RuntimeError("not loaded")
        if backup:
            backup_file(self.path)
        atomic_write(self.path, self.to_bytes())


class WorldMapBundle:
    """Load common big-map layers from resource/."""

    LAYER_FILES = (
        ("earth", ("earth.002", "Earth.002")),
        ("surface", ("surface.002", "Surface.002")),
        ("building", ("building.002", "Building.002")),
        ("buildx", ("buildx.002", "Buildx.002")),
        ("buildy", ("buildy.002", "Buildy.002")),
    )

    def __init__(self) -> None:
        self.layers: dict[str, WorldLayerGrid] = {}

    def load(self, resource_dir: str | Path) -> None:
        resource_dir = Path(resource_dir)
        self.layers = {}
        for key, names in self.LAYER_FILES:
            path = None
            for n in names:
                p = resource_dir / n
                if p.is_file():
                    path = p
                    break
            if path is None:
                continue
            grid = WorldLayerGrid()
            grid.load(path)
            self.layers[key] = grid

    @property
    def has_earth(self) -> bool:
        return "earth" in self.layers
