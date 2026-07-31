package cn.xm1221.HexGuide.networking.handler

import at.petrak.hexcasting.api.casting.eval.ResolvedPattern
import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType
import at.petrak.hexcasting.api.casting.math.HexCoord
import at.petrak.hexcasting.client.gui.GuiSpellcasting
import cn.xm1221.HexGuide.config.HexGuideServerConfig
import cn.xm1221.HexGuide.demo.DemoData
import cn.xm1221.HexGuide.networking.msg.*
import dev.architectury.networking.NetworkManager.PacketContext
import net.minecraft.client.Minecraft
import net.minecraft.world.InteractionHand

fun HexGuideMessageS2C.applyOnClient(ctx: PacketContext) = ctx.queue {
    when (this) {
        is MsgSyncConfigS2C -> HexGuideServerConfig.onSyncConfig(serverConfig)
        is MsgOpenDemoS2C -> {
            val data = DemoData.load(ns, name) ?: return@queue
            val patterns = data.patterns.mapIndexed { i, pat ->
                ResolvedPattern(pat, HexCoord(i * 2, 0), ResolvedPatternType.EVALUATED)
            }.toMutableList()
            val screen = GuiSpellcasting(
                InteractionHand.MAIN_HAND,
                patterns,
                listOf(),
                null,
                0
            )
            Minecraft.getInstance().setScreen(screen)
        }
    }
}
