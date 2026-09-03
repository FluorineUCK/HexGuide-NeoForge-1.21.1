package cn.xm1221.HexGuide.casting.actions

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getInt
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadCaster
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import cn.xm1221.HexGuide.api.notes.PlayerNotes
import cn.xm1221.HexGuide.networking.handler.syncNotes
import net.minecraft.server.level.ServerPlayer

/**
 * note/delete：按节索引（0-based，栈顶数字）删除一条笔记，并同步客户端。
 * 索引越界抛 MishapIndexInRange。
 */
class OpNoteDelete : ConstMediaAction {
    override val argc: Int get() = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val caster = env.castingEntity
        if (caster !is ServerPlayer) throw MishapBadCaster()
        val index = args.getInt(0, argc)
        val notes = PlayerNotes.get(env.world)
        val sections = notes.sections(caster.uuid)
        if (index < 0 || index >= sections.size) throw MishapInvalidIota.of(args.get(0), index, "note_index")
        notes.removeSection(caster.uuid, index)
        syncNotes(caster, notes)
        return emptyList()
    }
}
