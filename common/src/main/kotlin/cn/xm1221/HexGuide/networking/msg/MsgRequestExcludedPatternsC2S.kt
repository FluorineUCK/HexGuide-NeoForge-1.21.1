package cn.xm1221.HexGuide.networking.msg

import net.minecraft.network.FriendlyByteBuf

/** 客户端→服务端：请求创造标签页应排除的图案 id 列表（卓越法术 / Per-World 图案） */
class MsgRequestExcludedPatternsC2S : HexGuideMessageC2S {
    companion object : HexGuideMessageCompanion<MsgRequestExcludedPatternsC2S> {
        override val type = MsgRequestExcludedPatternsC2S::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgRequestExcludedPatternsC2S()
        override fun MsgRequestExcludedPatternsC2S.encode(buf: FriendlyByteBuf) {}
    }
}
