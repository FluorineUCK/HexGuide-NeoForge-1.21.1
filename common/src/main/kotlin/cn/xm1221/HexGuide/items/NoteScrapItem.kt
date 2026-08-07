package cn.xm1221.HexGuide.items

import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.item.IotaHolderItem
import cn.xm1221.HexGuide.api.notes.NoteIota
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import org.jetbrains.annotations.Nullable

/**
 * 笔记残页：由"纸"在保存笔记时转化而来，携带一个 NoteIota（一页笔记）。
 * 实现 HexMod 的 IotaHolderItem——HexMod 的读/写 iota 法术（hexcasting:get_iota 等）可直接读写。
 * 作为交换载体：把残页交给其他玩家，对方用 note/import 法术导入自己的笔记库。
 */
class NoteScrapItem(props: Properties) : Item(props), IotaHolderItem {

    override fun appendHoverText(stack: ItemStack, level: Level?, tooltip: MutableList<Component>, flag: TooltipFlag) {
        // 标准 iota 持有物品显示（标题 + 数据）
        IotaHolderItem.appendHoverText(this, stack, tooltip, flag)
        super.appendHoverText(stack, level, tooltip, flag)
    }

    // ---- IotaHolderItem ----

    override fun readIotaTag(stack: ItemStack): CompoundTag? {
        val tag = stack.tag ?: return null
        return if (tag.contains(TAG_NOTE)) tag.getCompound(TAG_NOTE) else null
    }

    override fun writeable(stack: ItemStack): Boolean = true

    override fun canWrite(stack: ItemStack, iota: @Nullable Iota?): Boolean =
        iota == null || iota is NoteIota // 只允许写笔记（或清空）

    override fun writeDatum(stack: ItemStack, iota: @Nullable Iota?) {
        if (iota != null) {
            stack.getOrCreateTag().put(TAG_NOTE, IotaType.serialize(iota))
        } else {
            stack.removeTagKey(TAG_NOTE)
        }
    }

    companion object {
        const val TAG_NOTE = "note"

        /** 便捷：把 NoteIota 写入残页 */
        fun setNote(stack: ItemStack, iota: Iota) {
            stack.getOrCreateTag().put(TAG_NOTE, IotaType.serialize(iota))
        }

        /** 便捷：读取残页中的 NoteIota（null 表示无） */
        fun getNote(stack: ItemStack): NoteIota? {
            val tag = stack.tag ?: return null
            if (!tag.contains(TAG_NOTE)) return null
            return NoteIota.TYPE.deserialize(tag.getCompound(TAG_NOTE), null)
        }
    }
}
