"""Ranger.grp header layout probing (team / money / inventory offsets)."""

from __future__ import annotations

import struct
from dataclasses import dataclass
from typing import Sequence, Tuple

# Pascal header: team list follows gametime at byte 28.
STANDARD_TEAM_OFFSET = 30
MAX_TEAM_SLOTS = 6


@dataclass(frozen=True)
class RangerHeaderLayout:
    """Byte layout of the ranger save header before role table."""

    team_offset: int = STANDARD_TEAM_OFFSET
    team_count: int = 6
    money_offset: int = -1  # -1 if no dedicated money word
    inventory_base: int = 42

    @property
    def has_money_word(self) -> bool:
        return self.money_offset >= 0


def _score_team_block(header: bytes, team_off: int, count: int, role_count: int) -> int:
    score = 0
    for i in range(count):
        off = team_off + i * 2
        if off + 2 > len(header):
            return -999
        v = struct.unpack_from("<h", header, off)[0]
        if v == -1:
            score += 4
        elif v == 0:
            score += 1
        elif 0 < v < role_count:
            score += 5
        elif v > 0:
            score -= 3
    return score


def _team_count_before_inventory(inv_base: int, money_offset: int) -> int:
    """Team list length on disk (Pascal / cpp_reborn always uses 6 at byte 30)."""
    _ = inv_base, money_offset
    return MAX_TEAM_SLOTS


def probe_ranger_header_layout(
    role_offset: int,
    magic_words: int,
    header: bytes,
    *,
    role_count: int = 300,
) -> RangerHeaderLayout:
    """Pick money / inventory offsets; team stays at byte 30 with derived slot count."""
    header = header[: max(64, min(len(header), role_offset))]
    best_score = -10**9
    best = RangerHeaderLayout()

    inv_options: list[Tuple[int, int]] = []
    for inv_base in range(32, 52, 2):
        nbytes = role_offset - inv_base
        if nbytes < 0 or nbytes % 4 != 0:
            continue
        inv_options.append((inv_base, nbytes // 4))

    for inv_base, slots in inv_options:
        slot_score = 0
        if slots == 200 and magic_words == 68:
            slot_score += 25
        if slots in (400, 401) and magic_words in (111, 93):
            slot_score += 25
        if slots in (198, 199, 200, 201, 400, 401):
            slot_score += 8

        money_candidates: Sequence[int] = (-1,)
        if inv_base == 44:
            money_candidates = (42, -1)
        elif inv_base == 46:
            money_candidates = (44, -1)

        for money_off in money_candidates:
            if money_off >= 0 and money_off + 2 > inv_base:
                continue
            team_cnt = _team_count_before_inventory(inv_base, money_off)
            if team_cnt <= 0:
                continue
            ts = _score_team_block(
                header, STANDARD_TEAM_OFFSET, team_cnt, role_count
            )
            total = slot_score + ts
            if inv_base == 42 and slots in (400, 401) and team_cnt == 6:
                total += 12
            if inv_base == 44 and slots in (198, 199, 200) and money_off == 42 and team_cnt == 6:
                total += 20
            if inv_base == 36:
                # inv@36 overlaps team[3..5] (bytes 36-41); never use for KYS headers.
                total -= 40
            if total > best_score:
                best_score = total
                best = RangerHeaderLayout(
                    team_offset=STANDARD_TEAM_OFFSET,
                    team_count=team_cnt,
                    money_offset=money_off,
                    inventory_base=inv_base,
                )
    return best


def probe_ranger_inventory_base(role_offset: int, magic_words: int) -> int:
    layout = probe_ranger_header_layout(role_offset, magic_words, b"\xff\xff" * 32)
    return layout.inventory_base
