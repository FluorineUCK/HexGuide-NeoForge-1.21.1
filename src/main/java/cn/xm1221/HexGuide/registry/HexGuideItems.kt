package cn.xm1221.HexGuide.registry

import cn.xm1221.HexGuide.items.AmethystPenItem
import cn.xm1221.HexGuide.items.NoteScrapItem
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.Item

/** 注册 HexGuide 物品 */
object HexGuideItems : HexGuideRegistrar<Item>(
    Registries.ITEM,
    { BuiltInRegistries.ITEM },
) {
    /** 紫水晶笔：主手右键（副手需纸）打开笔记编辑器 */
    val AMETHYST_PEN: Entry<AmethystPenItem> = register("amethyst_pen") {
        AmethystPenItem(Item.Properties().stacksTo(1))
    }

    /** 笔记残页：由纸转化，携带一个 NoteIota，可交换/导入 */
    val NOTE_SCRAP: Entry<NoteScrapItem> = register("note_scrap") {
        NoteScrapItem(Item.Properties().stacksTo(1))
    }
}
