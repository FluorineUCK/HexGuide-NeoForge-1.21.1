package cn.xm1221.HexGuide.networking.msg

import cn.xm1221.HexGuide.config.HexGuideServerConfig
import net.minecraft.network.FriendlyByteBuf

data class MsgSyncConfigS2C(val serverConfig: HexGuideServerConfig.ServerConfig) : HexGuideMessageS2C {
    companion object : HexGuideMessageCompanion<MsgSyncConfigS2C> {
        override val type = MsgSyncConfigS2C::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgSyncConfigS2C(
            serverConfig = HexGuideServerConfig.ServerConfig().decode(buf),
        )

        override fun MsgSyncConfigS2C.encode(buf: FriendlyByteBuf) {
            serverConfig.encode(buf)
        }
    }
}
