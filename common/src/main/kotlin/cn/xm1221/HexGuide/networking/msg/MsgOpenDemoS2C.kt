package cn.xm1221.HexGuide.networking.msg

import net.minecraft.network.FriendlyByteBuf

/** 服务端→客户端：打开 demo 回放 GUI */
class MsgOpenDemoS2C(val ns: String, val name: String) : HexGuideMessageS2C {
    companion object : HexGuideMessageCompanion<MsgOpenDemoS2C> {
        override val type = MsgOpenDemoS2C::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgOpenDemoS2C(
            ns = buf.readUtf(), name = buf.readUtf()
        )

        override fun MsgOpenDemoS2C.encode(buf: FriendlyByteBuf) {
            buf.writeUtf(ns); buf.writeUtf(name)
        }
    }
}
