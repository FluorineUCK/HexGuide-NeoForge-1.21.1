package cn.xm1221.HexGuide.items

import cn.xm1221.HexGuide.client.HexGuideClientBridge
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level

/**
 * 紫水晶笔：主手右键打开笔记编辑器。
 * 打开要求副手持有纸；保存时每页将副手一张纸转换为"笔记残页"（携带该页 NoteIota）。
 */
class AmethystPenItem(props: Properties) : Item(props) {

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        // 检查副手是否有纸
        val off = player.offhandItem
        if (!off.`is`(Items.PAPER)) {
            if (level.isClientSide) {
                player.displayClientMessage(Component.translatable("hexguide.notes.need_paper"), true)
            }
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide)
        }

        if (level.isClientSide) HexGuideClientBridge.openNoteEditor(player)
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide)
    }
}
