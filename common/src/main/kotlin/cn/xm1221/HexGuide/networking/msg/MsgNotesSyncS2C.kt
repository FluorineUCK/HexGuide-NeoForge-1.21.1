package cn.xm1221.HexGuide.networking.msg

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.network.FriendlyByteBuf
import java.util.UUID

/**
 * 服务端→客户端：某玩家的笔记全量同步。
 * sections = 节的列表，每节 = NoteIota 的 CompoundTag 列表（一个 NoteIota 一页）。
 */
class MsgNotesSyncS2C(
    val uuid: UUID,
    val sections: List<List<CompoundTag>>
) : HexGuideMessageS2C {
    companion object : HexGuideMessageCompanion<MsgNotesSyncS2C> {
        override val type = MsgNotesSyncS2C::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgNotesSyncS2C(
            uuid = buf.readUUID(),
            sections = run {
                val out = mutableListOf<List<CompoundTag>>()
                val count = buf.readVarInt()
                repeat(count) {
                    val sec = mutableListOf<CompoundTag>()
                    val len = buf.readVarInt()
                    repeat(len) {
                        val t = buf.readNbt()
                        if (t != null) sec.add(t)
                    }
                    out.add(sec)
                }
                out
            }
        )

        override fun MsgNotesSyncS2C.encode(buf: FriendlyByteBuf) {
            buf.writeUUID(uuid)
            buf.writeVarInt(sections.size)
            for (sec in sections) {
                buf.writeVarInt(sec.size)
                for (t in sec) buf.writeNbt(t)
            }
        }
    }
}
