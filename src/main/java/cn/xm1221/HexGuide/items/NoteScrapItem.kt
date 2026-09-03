package cn.xm1221.HexGuide.items

import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.item.IotaHolderItem
import cn.xm1221.HexGuide.api.notes.NoteIota
import cn.xm1221.HexGuide.hexcompat.ItemStackDataCompat
import cn.xm1221.HexGuide.hexcompat.deserializeIotaCompat
import cn.xm1221.HexGuide.hexcompat.serializeIota
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag

class NoteScrapItem(props: Properties) : Item(props), IotaHolderItem {
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltip: MutableList<Component>,
        flag: TooltipFlag,
    ) {
        IotaHolderItem.appendHoverText(this, stack, tooltip, flag)
        super.appendHoverText(stack, context, tooltip, flag)
    }

    override fun readIota(stack: ItemStack): Iota? = getNote(stack)
    override fun writeable(stack: ItemStack): Boolean = true
    override fun canWrite(stack: ItemStack, iota: Iota?): Boolean = iota == null || iota is NoteIota

    override fun writeDatum(stack: ItemStack, iota: Iota?) {
        ItemStackDataCompat.update(stack) { tag ->
            if (iota == null) tag.remove(TAG_NOTE)
            else tag.put(TAG_NOTE, serializeIota(iota))
        }
    }

    companion object {
        const val TAG_NOTE = "note"

        @JvmStatic
        fun setNote(stack: ItemStack, iota: Iota) {
            ItemStackDataCompat.update(stack) { it.put(TAG_NOTE, serializeIota(iota)) }
        }

        @JvmStatic
        fun getNote(stack: ItemStack): NoteIota? {
            val tag = ItemStackDataCompat.customData(stack)
            if (!tag.contains(TAG_NOTE)) return null
            return deserializeIotaCompat(tag.get(TAG_NOTE) ?: return null) as? NoteIota
        }
    }
}
