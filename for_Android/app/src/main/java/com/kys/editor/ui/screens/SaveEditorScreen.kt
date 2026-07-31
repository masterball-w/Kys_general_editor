package com.kys.editor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kys.editor.KysEditorApp
import com.kys.editor.codec.InventorySlot
import com.kys.editor.codec.RangerArchive
import com.kys.editor.codec.RecordTable
import com.kys.editor.ui.components.IntField
import java.nio.charset.Charset

enum class SaveTab { Overview, Roles, Items, Magics, Inventory, Shops, Scenes }

/**
 * Describes a single field within a role/item/magic record.
 * [word] is the 16-bit word index, [label] is the display label,
 * [min]/[max] define the valid integer range, and for text fields
 * [isText] is true with [wordCount] specifying how many words the text spans.
 */
data class FieldDef(
    val word: Int,
    val label: String,
    val min: Int = -32768,
    val max: Int = 32767,
    val isText: Boolean = false,
    val wordCount: Int = 1
) {
    companion object {
        /** Role record fields (91 words, indices 0..90). */
        val ROLE_FIELDS: List<FieldDef> = listOf(
            FieldDef(0, "列表号 ListNum[0]"),
            FieldDef(1, "头像 HeadNum[1]"),
            FieldDef(2, "成长 IncLife[2]"),
            FieldDef(3, "保留 Reserved[3]"),
            FieldDef(4, "姓名 Name[4]", isText = true, wordCount = 5),
            FieldDef(9, "绰号 Nick[9]", isText = true, wordCount = 5),
            FieldDef(14, "性别 Sexual[14]"),
            FieldDef(15, "等级 Level[15]"),
            FieldDef(16, "经验 Exp[16]", min = 0, max = 65535),
            FieldDef(17, "当前生命 CurrentHP[17]"),
            FieldDef(18, "生命上限 MaxHP[18]"),
            FieldDef(19, "内伤 Hurt[19]"),
            FieldDef(20, "中毒 Poision[20]"),
            FieldDef(21, "体力 PhyPower[21]"),
            FieldDef(22, "装备经验 ExpForItem[22]", min = 0, max = 65535),
            FieldDef(23, "装备1 Equip0[23]"),
            FieldDef(24, "装备2 Equip1[24]"),
            FieldDef(25, "装备3 Equip2[25]"),
            FieldDef(26, "装备4 Equip3[26]"),
            FieldDef(27, "装备5 Equip4[27]"),
            FieldDef(28, "当前功体 Gongti[28]"),
            FieldDef(29, "队伍状态 TeamState[29]"),
            FieldDef(30, "怒气 Angry[30]"),
            FieldDef(31, "功体经验 GongtiExam[31]", min = 0, max = 65535),
            FieldDef(32, "可移动 Moveable[32]"),
            FieldDef(33, "技能点 AddSkillPoint[33]"),
            FieldDef(34, "宠物数 PetAmount[34]"),
            FieldDef(35, "好感 Impression[35]"),
            FieldDef(36, "Reset[36]"),
            FieldDef(37, "难度 difficulty[37]"),
            FieldDef(38, "保留 Reserved[38]"),
            FieldDef(39, "保留 Reserved[39]"),
            FieldDef(40, "内力属性 MPType[40]"),
            FieldDef(41, "当前内力 CurrentMP[41]"),
            FieldDef(42, "内力上限 MaxMP[42]"),
            FieldDef(43, "攻击 Attack[43]"),
            FieldDef(44, "轻功 Speed[44]"),
            FieldDef(45, "防御 Defence[45]"),
            FieldDef(46, "医疗 Medcine[46]"),
            FieldDef(47, "用毒 UsePoi[47]"),
            FieldDef(48, "解毒 MedPoi[48]"),
            FieldDef(49, "抗毒 DefPoi[49]"),
            FieldDef(50, "拳掌 Fist[50]"),
            FieldDef(51, "剑术 Sword[51]"),
            FieldDef(52, "刀法 Knife[52]"),
            FieldDef(53, "奇门 Unusual[53]"),
            FieldDef(54, "暗器 HidWeapon[54]"),
            FieldDef(55, "学识 Knowledge[55]"),
            FieldDef(56, "道德 Ethics[56]"),
            FieldDef(57, "攻击带毒 AttPoi[57]"),
            FieldDef(58, "连击 AttTwice[58]"),
            FieldDef(59, "声望 Repute[59]"),
            FieldDef(60, "资质 Aptitude[60]"),
            FieldDef(61, "修炼秘籍 PracticeBook[61]"),
            FieldDef(62, "修炼经验 ExpForBook[62]", min = 0, max = 65535),
            FieldDef(63, "武功1 Magic0[63]"),
            FieldDef(64, "武功2 Magic1[64]"),
            FieldDef(65, "武功3 Magic2[65]"),
            FieldDef(66, "武功4 Magic3[66]"),
            FieldDef(67, "武功5 Magic4[67]"),
            FieldDef(68, "武功6 Magic5[68]"),
            FieldDef(69, "武功7 Magic6[69]"),
            FieldDef(70, "武功8 Magic7[70]"),
            FieldDef(71, "武功9 Magic8[71]"),
            FieldDef(72, "武功10 Magic9[72]"),
            FieldDef(73, "武功等级1 MagLv0[73]"),
            FieldDef(74, "武功等级2 MagLv1[74]"),
            FieldDef(75, "武功等级3 MagLv2[75]"),
            FieldDef(76, "武功等级4 MagLv3[76]"),
            FieldDef(77, "武功等级5 MagLv4[77]"),
            FieldDef(78, "武功等级6 MagLv5[78]"),
            FieldDef(79, "武功等级7 MagLv6[79]"),
            FieldDef(80, "武功等级8 MagLv7[80]"),
            FieldDef(81, "武功等级9 MagLv8[81]"),
            FieldDef(82, "武功等级10 MagLv9[82]"),
            FieldDef(83, "随身物品1 TakeItem0[83]"),
            FieldDef(84, "随身物品2 TakeItem1[84]"),
            FieldDef(85, "随身物品3 TakeItem2[85]"),
            FieldDef(86, "随身物品4 TakeItem3[86]"),
            FieldDef(87, "物品数量1 Amount0[87]"),
            FieldDef(88, "物品数量2 Amount1[88]"),
            FieldDef(89, "物品数量3 Amount2[89]"),
            FieldDef(90, "物品数量4 Amount3[90]")
        )

        /** Item record fields (95 words, indices 0..94). */
        val ITEM_FIELDS: List<FieldDef> = buildList {
            add(FieldDef(0, "列表号 ListNum[0]"))
            add(FieldDef(1, "名称 Name[1]", isText = true, wordCount = 10))
            add(FieldDef(11, "秘籍经验 ExpOfMagic[11]"))
            add(FieldDef(12, "套装号 SetNum[12]"))
            add(FieldDef(13, "装备战斗特效 BattleEffect[13]"))
            add(FieldDef(14, "酒效应 WineEffect[14]"))
            add(FieldDef(15, "性别限制 needSex[15]"))
            for (w in 16..20) add(FieldDef(w, "保留 Reserved[$w]"))
            add(FieldDef(21, "说明 Introduction[21]", isText = true, wordCount = 15))
            add(FieldDef(36, "关联武功 Magic[36]"))
            add(FieldDef(37, "动画 AmiNum[37]"))
            add(FieldDef(38, "使用者 User[38]"))
            add(FieldDef(39, "装备部位 EquipType[39]"))
            add(FieldDef(40, "显示说明 ShowIntro[40]"))
            add(FieldDef(41, "物品类型 ItemType[41]"))
            add(FieldDef(42, "模板库存 inventory[42]"))
            add(FieldDef(43, "价格 price[43]"))
            add(FieldDef(44, "使用事件 EventNum[44]"))
            add(FieldDef(45, "加当前生命 AddHP[45]"))
            add(FieldDef(46, "加生命上限 AddMaxHP[46]"))
            add(FieldDef(47, "加毒 AddPoi[47]"))
            add(FieldDef(48, "加体力 AddPhy[48]"))
            add(FieldDef(49, "改内力属性 ChangeMP[49]"))
            add(FieldDef(50, "加当前内力 AddMP[50]"))
            add(FieldDef(51, "加内力上限 AddMaxMP[51]"))
            add(FieldDef(52, "加攻击 AddAtt[52]"))
            add(FieldDef(53, "加轻功 AddSpd[53]"))
            add(FieldDef(54, "加防御 AddDef[54]"))
            add(FieldDef(55, "加医疗 AddMed[55]"))
            add(FieldDef(56, "加用毒 AddUsePoi[56]"))
            add(FieldDef(57, "加解毒技 AddMedPoi[57]"))
            add(FieldDef(58, "加抗毒 AddDefPoi[58]"))
            add(FieldDef(59, "加拳 AddFist[59]"))
            add(FieldDef(60, "加剑 AddSword[60]"))
            add(FieldDef(61, "加刀 AddKnife[61]"))
            add(FieldDef(62, "加奇 AddUnusual[62]"))
            add(FieldDef(63, "加暗器 AddHid[63]"))
            add(FieldDef(64, "加学识 AddKnow[64]"))
            add(FieldDef(65, "加道德 AddEthics[65]"))
            add(FieldDef(66, "加连击 AddTwice[66]"))
            add(FieldDef(67, "加攻击带毒 AddAttPoi[67]"))
            add(FieldDef(68, "专属角色 OnlyPracRole[68]"))
            for (w in 69..94) add(FieldDef(w, "保留 Reserved[$w]"))
        }

        /** Magic record fields (up to 111 words for promise, 68 for classic). */
        val MAGIC_FIELDS: List<FieldDef> = buildList {
            add(FieldDef(0, "列表号 ListNum[0]"))
            add(FieldDef(1, "名称 Name[1]", isText = true, wordCount = 5))
            add(FieldDef(6, "保留 Reserved[6]"))
            add(FieldDef(7, "耗生命 NeedHP[7]"))
            add(FieldDef(8, "保留 Reserved[8]"))
            add(FieldDef(9, "保留 Reserved[9]"))
            add(FieldDef(10, "事件 EventNum[10]"))
            add(FieldDef(11, "音效 SoundNum[11]"))
            add(FieldDef(12, "类别 MagicType[12]"))
            add(FieldDef(13, "特效索引 AmiNum[13]"))
            add(FieldDef(14, "伤害类型 HurtType[14]"))
            add(FieldDef(15, "保留 Reserved[15]"))
            add(FieldDef(16, "耗内力 NeedMP[16]"))
            add(FieldDef(17, "带毒 Poision[17]"))
            add(FieldDef(18, "威力1 Hurt0[18]"))
            add(FieldDef(19, "威力2 Hurt1[19]"))
            add(FieldDef(20, "威力3 Hurt2[20]"))
            add(FieldDef(21, "威力4 Hurt3[21]"))
            add(FieldDef(22, "威力5 Hurt4[22]"))
            add(FieldDef(23, "威力6 Hurt5[23]"))
            add(FieldDef(24, "威力7 Hurt6[24]"))
            add(FieldDef(25, "威力8 Hurt7[25]"))
            add(FieldDef(26, "威力9 Hurt8[26]"))
            add(FieldDef(27, "威力10 Hurt9[27]"))
            for (w in 28..79) add(FieldDef(w, "保留 Reserved[$w]"))
            add(FieldDef(80, "内功最高级 MaxLevel[80]"))
            for (w in 81..110) add(FieldDef(w, "保留 Reserved[$w]"))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveEditorScreen() {
    val ctx = LocalContext.current as android.content.Context
    val app = ctx.applicationContext as KysEditorApp
    val ec = app.editorContext

    val ranger by ec.ranger.collectAsStateWithLifecycle()
    val isLoading by ec.isLoading.collectAsStateWithLifecycle()
    val profile by ec.profile.collectAsStateWithLifecycle()

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val arch = ranger
    if (arch == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("请先选择游戏数据目录并加载存档")
        }
        return
    }

    var subTab by remember { mutableStateOf(SaveTab.Inventory) }
    val tabs = listOf("总览", "人物", "物品定义", "武功", "背包", "商店", "场景")
    val tabEnums = SaveTab.entries

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = subTab.ordinal) {
            tabs.forEachIndexed { i, t ->
                Tab(selected = subTab.ordinal == i, onClick = { subTab = tabEnums[i] }, text = { Text(t) })
            }
        }
        when (subTab) {
            SaveTab.Overview -> OverviewTab(arch)
            SaveTab.Inventory -> InventoryTab(arch)
            SaveTab.Roles -> {
                var selRole by remember { mutableIntStateOf(0) }
                RolesTab(arch, selRole, { selRole = it })
            }
            SaveTab.Items -> {
                var selItem by remember { mutableIntStateOf(0) }
                ItemsTab(arch, selItem, { selItem = it })
            }
            SaveTab.Magics -> {
                var selMagic by remember { mutableIntStateOf(0) }
                MagicsTab(arch, selMagic, { selMagic = it })
            }
            SaveTab.Shops -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("商店 - ${arch.shops.count} 条记录") }
            SaveTab.Scenes -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("场景元数据 - ${arch.scenes.count} 条记录") }
        }
    }
}

@Composable
fun OverviewTab(arch: RangerArchive) {
    val h = arch.header
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Text("存档头部", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row {
            IntField(h.inShip, { h.inShip = it }, "in_ship")
            Spacer(Modifier.width(8.dp))
            IntField(h.where, { h.where = it }, "where")
            Spacer(Modifier.width(8.dp))
            IntField(h.my, { h.my = it }, "my")
        }
        Spacer(Modifier.height(8.dp))
        Row {
            IntField(h.mx, { h.mx = it }, "mx")
            Spacer(Modifier.width(8.dp))
            IntField(h.sy, { h.sy = it }, "sy")
            Spacer(Modifier.width(8.dp))
            IntField(h.sx, { h.sx = it }, "sx")
        }
        Spacer(Modifier.height(8.dp))
        Row {
            IntField(h.mface, { h.mface = it }, "主角头像")
            Spacer(Modifier.width(8.dp))
            IntField(h.sface, { h.sface = it }, "场景头像")
            Spacer(Modifier.width(8.dp))
            IntField(h.money, { h.money = it }, "银两", min = -32768, max = 32767)
        }
        Spacer(Modifier.height(12.dp))
        Text("队伍成员", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        h.team.forEachIndexed { i, v ->
            var tv by remember(i, v) { mutableStateOf(v.toString()) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("队友${i + 1}:", Modifier.width(72.dp))
                OutlinedTextField(
                    value = tv, onValueChange = { s ->
                        tv = s; s.toIntOrNull()?.let { h.team[i] = it }
                    },
                    modifier = Modifier.width(120.dp), singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                val name = if (v in 0 until arch.roles.count) arch.roleName(v) else "(空)"
                Text(name, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun InventoryTab(arch: RangerArchive) {
    LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(4.dp)) {
                Text("槽", Modifier.width(48.dp), style = MaterialTheme.typography.labelLarge)
                Text("物品ID", Modifier.width(80.dp), style = MaterialTheme.typography.labelLarge)
                Text("名称", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                Text("数量", Modifier.width(80.dp), style = MaterialTheme.typography.labelLarge)
            }
            HorizontalDivider()
        }
        itemsIndexed(arch.header.inventory) { i, slot ->
            InventoryRow(i, slot, arch)
        }
    }
}

@Composable
private fun InventoryRow(index: Int, slot: InventorySlot, arch: RangerArchive) {
    var idText by remember(index, slot.number) { mutableStateOf(slot.number.toString()) }
    var amtText by remember(index, slot.amount) { mutableStateOf(slot.amount.toString()) }
    val itemName = remember(slot.number) {
        if (slot.number in 0 until arch.items.count) arch.itemName(slot.number)
        else if (slot.number == -1) "(空)" else "(无效)"
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("#${index + 1}", Modifier.width(48.dp))
        OutlinedTextField(
            value = idText, onValueChange = { s ->
                idText = s; s.toIntOrNull()?.let { slot.number = it }
            },
            modifier = Modifier.width(80.dp), singleLine = true, textStyle = MaterialTheme.typography.bodySmall
        )
        Text(itemName, Modifier.weight(1f).padding(horizontal = 4.dp), style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = amtText, onValueChange = { s ->
                amtText = s; s.toIntOrNull()?.let { slot.amount = it }
            },
            modifier = Modifier.width(80.dp), singleLine = true, textStyle = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * Renders a scrollable list of record fields with proper labels.
 * Integer fields use [IntField]; text fields (Name, Nick, Introduction) use
 * [OutlinedTextField] backed by [RecordTable.getName] / [RecordTable.setName].
 */
@Composable
private fun RecordFieldList(
    fields: List<FieldDef>,
    table: RecordTable,
    sel: Int,
    encoding: Charset
) {
    val rec = table.records[sel]
    Column(Modifier.fillMaxWidth()) {
        fields.forEach { field ->
            if (field.word >= rec.size) return@forEach
            if (field.isText) {
                var textState by remember(sel, field.word) {
                    mutableStateOf(table.getName(sel, field.word, field.wordCount, encoding))
                }
                OutlinedTextField(
                    value = textState,
                    onValueChange = {
                        textState = it
                        table.setName(sel, it, field.word, field.wordCount, encoding)
                    },
                    label = { Text(field.label) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(4.dp))
            } else {
                IntField(
                    value = rec[field.word],
                    onValueChange = { rec[field.word] = it },
                    label = field.label,
                    min = field.min,
                    max = field.max
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun RolesTab(arch: RangerArchive, selected: Int, onSelect: (Int) -> Unit) {
    var sel by remember { mutableIntStateOf(selected) }
    Row(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.width(140.dp)) {
            items(arch.roles.count) { i ->
                val name = arch.roleName(i)
                Surface(
                    onClick = { sel = i; onSelect(i) },
                    color = if (sel == i) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("#$i $name", Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        VerticalDivider()
        Column(Modifier.weight(1f).padding(8.dp).verticalScroll(rememberScrollState())) {
            if (sel in 0 until arch.roles.count) {
                Text(arch.roleName(sel), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                RecordFieldList(FieldDef.ROLE_FIELDS, arch.roles, sel, arch.textEncoding)
            }
        }
    }
}

@Composable
fun ItemsTab(arch: RangerArchive, selected: Int, onSelect: (Int) -> Unit) {
    var sel by remember { mutableIntStateOf(selected) }
    Row(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.width(140.dp)) {
            items(arch.items.count) { i ->
                Surface(
                    onClick = { sel = i; onSelect(i) },
                    color = if (sel == i) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("#$i ${arch.itemName(i)}", Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        VerticalDivider()
        Column(Modifier.weight(1f).padding(8.dp).verticalScroll(rememberScrollState())) {
            if (sel in 0 until arch.items.count) {
                Text(arch.itemName(sel), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                RecordFieldList(FieldDef.ITEM_FIELDS, arch.items, sel, arch.textEncoding)
            }
        }
    }
}

@Composable
fun MagicsTab(arch: RangerArchive, selected: Int, onSelect: (Int) -> Unit) {
    var sel by remember { mutableIntStateOf(selected) }
    Row(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.width(140.dp)) {
            items(arch.magics.count) { i ->
                Surface(
                    onClick = { sel = i; onSelect(i) },
                    color = if (sel == i) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("#$i ${arch.magicName(i)}", Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        VerticalDivider()
        Column(Modifier.weight(1f).padding(8.dp).verticalScroll(rememberScrollState())) {
            if (sel in 0 until arch.magics.count) {
                Text(arch.magicName(sel), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                RecordFieldList(FieldDef.MAGIC_FIELDS, arch.magics, sel, arch.textEncoding)
            }
        }
    }
}
