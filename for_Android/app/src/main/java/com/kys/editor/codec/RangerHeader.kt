package com.kys.editor.codec

data class RangerHeader(
    var inShip: Int = 0,
    var where: Int = -1,
    var my: Int = 0,
    var mx: Int = 0,
    var sy: Int = 0,
    var sx: Int = 0,
    var mface: Int = 0,
    var shipX: Int = 0,
    var shipY: Int = 0,
    var time: Int = 0,
    var timeEvent: Int = 0,
    var randomEvent: Int = 0,
    var sface: Int = 0,
    var shipFace: Int = 0,
    var gameTime: Int = 0,
    var team: MutableList<Int> = mutableListOf(-1, -1, -1, -1, -1, -1),
    var money: Int = 0,
    var inventory: MutableList<InventorySlot> = mutableListOf()
)
