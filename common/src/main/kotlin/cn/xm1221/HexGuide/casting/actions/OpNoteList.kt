package cn.xm1221.HexGuide.casting.actions

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadCaster
import cn.xm1221.HexGuide.api.notes.PlayerNotes
import net.minecraft.server.level.ServerPlayer

/**
 * note/list：获取施法者目前所有已记录笔记——
 * 返回"所有章节的笔记列表所组成的列表"（ListIota，每个元素是一节的 NoteIota 列表）。
 */
class OpNoteList : ConstMediaAction {
    override val argc: Int get() = 0

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val caster = env.castingEntity
        if (caster !is ServerPlayer) throw MishapBadCaster()
        val sections = PlayerNotes.get(env.world).sections(caster.uuid)
        val lists = sections.map { sec -> ListIota(sec.map { it as Iota }) }
        return listOf(ListIota(lists))
    }
}
