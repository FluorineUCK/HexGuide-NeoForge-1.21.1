package cn.xm1221.HexGuide.networking.msg

import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf

/**
 * 客户端→服务端：向某玩家的第 sectionIndex 节追加一个 NoteIota（编辑/导入用）。
 */
class MsgNotesAppendC2S(
    val sectionIndex: Int,
    val iota: CompoundTag
) : HexGuideMessageC2S {
    companion object : HexGuideMessageCompanion<MsgNotesAppendC2S> {
        override val type = MsgNotesAppendC2S::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgNotesAppendC2S(
            sectionIndex = buf.readVarInt(),
            iota = buf.readNbt() ?: CompoundTag()
        )

        override fun MsgNotesAppendC2S.encode(buf: FriendlyByteBuf) {
            buf.writeVarInt(sectionIndex)
            buf.writeNbt(iota)
        }
    }
}
