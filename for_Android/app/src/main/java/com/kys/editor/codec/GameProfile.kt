package com.kys.editor.codec

data class WarLayout(
    val words: Int,
    val mateCount: Int,
    val enemyCount: Int,
    val mateOff: Int,
    val mateXOff: Int,
    val mateYOff: Int,
    val enemyOff: Int,
    val enemyXOff: Int,
    val enemyYOff: Int,
    val autoMateOff: Int = -1,
    val autoMateCount: Int = 0,
    val boutEventOff: Int = -1,
    val operationEventOff: Int = -1,
    val getKongfuOff: Int = -1,
    val getKongfuCount: Int = 0,
    val getItemsOff: Int = -1,
    val getItemsCount: Int = 0,
    val getMoneyOff: Int = -1
) {
    companion object {
        val PROMISE = WarLayout(
            words = 156, mateCount = 12, enemyCount = 30,
            mateOff = 9, autoMateOff = 21, autoMateCount = 12,
            mateXOff = 33, mateYOff = 45, enemyOff = 57,
            enemyXOff = 87, enemyYOff = 117, boutEventOff = 147,
            operationEventOff = 148, getKongfuOff = 149, getKongfuCount = 3,
            getItemsOff = 152, getItemsCount = 3, getMoneyOff = 155
        )
        val CLASSIC = WarLayout(
            words = 93, mateCount = 6, enemyCount = 20,
            autoMateOff = 9, autoMateCount = 6, mateOff = 15,
            mateXOff = 21, mateYOff = 27, enemyOff = 33,
            enemyXOff = 53, enemyYOff = 73
        )
    }
}

data class AssetPaths(
    val headsMode: String = "pic",
    val headsPic: String = "resource/Heads.Pic",
    val headsDir: String = "head",
    val itemsMode: String = "pic",
    val itemsPic: String = "resource/Items.Pic",
    val itemsDir: String = "item",
    val fightMode: String = "pic_tree",
    val fightPicFmt: String = "fight/%03d/%02d.pic",
    val fightIdxFmt: String = "fight/fight%03d.idx",
    val fightGrpFmt: String = "fight/fight%03d.grp",
    val eftMode: String = "pic_file",
    val eftPicFmt: String = "eft/eft%03d.pic",
    val eftIdx: String = "resource/eft.idx",
    val eftGrp: String = "resource/eft.grp"
)

data class EditorCompat(
    val magicHurtPerLevel: Boolean = false,
    val itemHatShoesEquip: Boolean = true,
    val itemBattleWineSet: Boolean = true,
    val magicGongtiBlock: Boolean = true,
    val roleGongtiFields: Boolean = true
) {
    companion object {
        val PROMISE = EditorCompat()
        val CLASSIC = EditorCompat(
            magicHurtPerLevel = true, itemHatShoesEquip = false,
            itemBattleWineSet = false, magicGongtiBlock = false, roleGongtiFields = false
        )
    }
}

data class GameProfile(
    val id: String,
    val displayName: String,
    val roleWords: Int = 91,
    val itemWords: Int = 95,
    val sceneWords: Int = 26,
    val magicWords: Int = 111,
    val shopWords: Int = 18,
    val inventorySlots: Int = 400,
    val rangerTeamOffset: Int = 30,
    val rangerTeamCount: Int = 6,
    val rangerMoneyOffset: Int = -1,
    val rangerInventoryBase: Int = 42,
    val war: WarLayout = WarLayout.PROMISE,
    val saveSubdir: String = "save",
    val resourceSubdir: String = "resource",
    val assets: AssetPaths = AssetPaths(),
    val defaultTextEncoding: String = "auto",
    val compat: EditorCompat = EditorCompat.PROMISE
) {
    companion object {
        val PROMISE = GameProfile(
            id = "promise", displayName = "金庸群侠前传 (Kys Promise)",
            magicWords = 111, shopWords = 18, inventorySlots = 400,
            rangerTeamOffset = 30, rangerTeamCount = 6,
            rangerMoneyOffset = -1, rangerInventoryBase = 42,
            war = WarLayout.PROMISE, assets = AssetPaths(), compat = EditorCompat.PROMISE
        )
        val CLASSIC = GameProfile(
            id = "classic", displayName = "经典 KYS (天龙八部)",
            magicWords = 68, shopWords = 15, inventorySlots = 200,
            rangerTeamOffset = 24, rangerTeamCount = 6,
            rangerMoneyOffset = 42, rangerInventoryBase = 44,
            war = WarLayout.CLASSIC,
            assets = AssetPaths(headsMode = "png_dir", itemsMode = "png_dir", fightMode = "idx_grp", eftMode = "idx_grp"),
            defaultTextEncoding = "big5", compat = EditorCompat.CLASSIC
        )
    }
}
