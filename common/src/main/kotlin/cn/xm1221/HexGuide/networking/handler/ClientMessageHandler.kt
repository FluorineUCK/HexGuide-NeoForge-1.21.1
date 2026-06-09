package cn.xm1221.HexGuide.networking.handler

import dev.architectury.networking.NetworkManager.PacketContext
import cn.xm1221.HexGuide.config.HexGuideServerConfig
import cn.xm1221.HexGuide.networking.msg.*

fun HexGuideMessageS2C.applyOnClient(ctx: PacketContext) = ctx.queue {
    when (this) {
        is MsgSyncConfigS2C -> {
            HexGuideServerConfig.onSyncConfig(serverConfig)
        }

        // add more client-side message handlers here
    }
}
