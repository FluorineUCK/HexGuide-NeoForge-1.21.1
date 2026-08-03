package cn.xm1221.HexGuide.networking.msg

import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf

/** 客户端→服务端：把写模式记录的图案推入法杖栈（不执行，只让法杖栈与本地同步） */
class MsgBookSyncStackC2S(val patterns: List<CompoundTag>) : HexGuideMessageC2S {
    companion object : HexGuideMessageCompanion<MsgBookSyncStackC2S> {
        override val type = MsgBookSyncStackC2S::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgBookSyncStackC2S(
            patterns = List(buf.readVarInt()) { buf.readNbt() ?: CompoundTag() }
        )

        override fun MsgBookSyncStackC2S.encode(buf: FriendlyByteBuf) {
            buf.writeVarInt(patterns.size)
            patterns.forEach { buf.writeNbt(it) }
        }
    }
}
