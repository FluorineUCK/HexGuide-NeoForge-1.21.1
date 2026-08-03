package cn.xm1221.HexGuide.networking.handler

import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.xplat.IXplatAbstractions
import dev.architectury.networking.NetworkManager.PacketContext
import cn.xm1221.HexGuide.networking.msg.*
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand

fun HexGuideMessageC2S.applyOnServer(ctx: PacketContext) = ctx.queue {
    when (this) {
        // 把写模式记录的图案推入法杖栈（不执行），让法杖栈与本地栈同步
        is MsgBookSyncStackC2S -> {
            val player = ctx.player as? ServerPlayer ?: return@queue
            val vm = IXplatAbstractions.INSTANCE.getStaffcastVM(player, InteractionHand.MAIN_HAND)
            val newStack = ArrayList(vm.image.stack)
            for (tag in patterns) {
                try {
                    newStack.add(IotaType.deserialize(tag, player.serverLevel()) ?: continue)
                } catch (e: Exception) {
                    // 忽略无法解析的图案
                }
            }
            IXplatAbstractions.INSTANCE.setStaffcastImage(player, vm.image.copy(stack = newStack))
        }

        else -> {}
    }
}
