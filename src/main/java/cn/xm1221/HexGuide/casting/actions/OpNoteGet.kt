package cn.xm1221.HexGuide.casting.actions

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getInt
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadCaster
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import cn.xm1221.HexGuide.api.notes.PlayerNotes
import net.minecraft.server.level.ServerPlayer

/**
 * note/get：按节索引（0-based，栈顶数字）获取单个笔记——返回该节所有 NoteIota 的 ListIota。
 * 索引越界抛 MishapIndexInRange。
 */
class OpNoteGet : ConstMediaAction {
    override val argc: Int get() = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val caster = env.castingEntity
        if (caster !is ServerPlayer) throw MishapBadCaster()
        val index = args.getInt(0, argc)
        val sections = PlayerNotes.get(env.world).sections(caster.uuid)
        if (index < 0 || index >= sections.size) throw MishapInvalidIota.of(args.get(0), index, "note_index")
        return listOf(ListIota(sections[index].map { it as Iota }))
    }
}
