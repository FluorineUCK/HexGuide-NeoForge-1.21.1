package cn.xm1221.HexGuide.networking.msg

import net.minecraft.network.FriendlyByteBuf

/** 服务端→客户端：应排除的图案 id 列表（服务端 tag 数据完整，跨平台可靠） */
class MsgExcludedPatternsS2C(val ids: List<String>) : HexGuideMessageS2C {
    companion object : HexGuideMessageCompanion<MsgExcludedPatternsS2C> {
        override val type = MsgExcludedPatternsS2C::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgExcludedPatternsS2C(
            ids = List(buf.readVarInt()) { buf.readUtf(128) }
        )

        override fun MsgExcludedPatternsS2C.encode(buf: FriendlyByteBuf) {
            buf.writeVarInt(ids.size)
            ids.forEach { buf.writeUtf(it) }
        }
    }
}
