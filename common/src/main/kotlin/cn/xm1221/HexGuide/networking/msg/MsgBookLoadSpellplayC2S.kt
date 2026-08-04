package cn.xm1221.HexGuide.networking.msg

import net.minecraft.network.FriendlyByteBuf

/** 客户端→服务端：请求某个演示配置 data/&lt;ns&gt;/spellplays/&lt;name&gt;.json */
class MsgBookLoadSpellplayC2S(
    val ns: String,
    val name: String
) : HexGuideMessageC2S {
    companion object : HexGuideMessageCompanion<MsgBookLoadSpellplayC2S> {
        override val type = MsgBookLoadSpellplayC2S::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgBookLoadSpellplayC2S(
            ns = buf.readUtf(64),
            name = buf.readUtf(128)
        )

        override fun MsgBookLoadSpellplayC2S.encode(buf: FriendlyByteBuf) {
            buf.writeUtf(ns)
            buf.writeUtf(name)
        }
    }
}
