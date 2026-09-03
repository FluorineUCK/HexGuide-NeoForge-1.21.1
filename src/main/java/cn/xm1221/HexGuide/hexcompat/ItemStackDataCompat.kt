package cn.xm1221.HexGuide.hexcompat

import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData

object ItemStackDataCompat {
    @JvmStatic
    fun customData(stack: ItemStack): CompoundTag =
        stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()

    @JvmStatic
    fun update(stack: ItemStack, action: (CompoundTag) -> Unit) {
        val tag = customData(stack)
        action(tag)
        if (tag.isEmpty) stack.remove(DataComponents.CUSTOM_DATA)
        else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
    }
}

