package cn.xm1221.HexGuide.networking.msg

import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf

/**
 * 客户端→服务端：新建一节，内容为整个节（NoteIota 列表，一个 NoteIota 一页）。
 */
class MsgNotesNewSectionC2S(
    val iotas: List<CompoundTag>
) : HexGuideMessageC2S {
    companion object : HexGuideMessageCompanion<MsgNotesNewSectionC2S> {
        override val type = MsgNotesNewSectionC2S::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgNotesNewSectionC2S(
            iotas = run {
                val out = mutableListOf<CompoundTag>()
                val n = buf.readVarInt()
                repeat(n) {
                    val t = buf.readNbt()
                    if (t != null) out.add(t)
                }
                out
            }
        )

        override fun MsgNotesNewSectionC2S.encode(buf: FriendlyByteBuf) {
            buf.writeVarInt(iotas.size)
            for (t in iotas) buf.writeNbt(t)
        }
    }
}
