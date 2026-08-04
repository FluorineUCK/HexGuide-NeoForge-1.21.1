package cn.xm1221.HexGuide.networking.msg

import net.minecraft.network.FriendlyByteBuf

/** 服务端→客户端：演示配置内容（json 为 null 表示数据包中未找到） */
class MsgBookLoadSpellplayS2C(
    val ns: String,
    val name: String,
    val json: String?
) : HexGuideMessageS2C {
    companion object : HexGuideMessageCompanion<MsgBookLoadSpellplayS2C> {
        override val type = MsgBookLoadSpellplayS2C::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgBookLoadSpellplayS2C(
            ns = buf.readUtf(64),
            name = buf.readUtf(128),
            json = if (buf.readBoolean()) buf.readUtf(65536) else null
        )

        override fun MsgBookLoadSpellplayS2C.encode(buf: FriendlyByteBuf) {
            buf.writeUtf(ns)
            buf.writeUtf(name)
            if (json != null) {
                buf.writeBoolean(true)
                buf.writeUtf(json, 65536)
            } else {
                buf.writeBoolean(false)
            }
        }
    }
}
