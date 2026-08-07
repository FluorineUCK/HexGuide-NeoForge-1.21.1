package cn.xm1221.HexGuide.casting.actions

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getList
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadCaster
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import cn.xm1221.HexGuide.api.notes.NoteIota
import cn.xm1221.HexGuide.api.notes.PlayerNotes
import cn.xm1221.HexGuide.items.NoteScrapItem
import cn.xm1221.HexGuide.networking.handler.syncNotes
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import java.util.UUID

/**
 * note/import：从【副手】的"笔记残页"读取 NoteIota，作为一节（单页）导入自己的笔记库。
 * 用于接收其他玩家交换的笔记残页。
 */
class OpNoteImport : ConstMediaAction {
    override val argc: Int
        get() = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val notes = args.getList(0,argc).toMutableList()
        val caster = env.castingEntity
        if(caster !is ServerPlayer) {
            throw MishapBadCaster()
        }
        for (note in notes) {
            if(note !is NoteIota) {
                throw MishapInvalidIota.of(note,0,"notes")
            }
        }
        val playerNotes= PlayerNotes.get(env.world)
        playerNotes.newSection(caster.uuid,notes as List<NoteIota>)
        syncNotes(caster,playerNotes)
        return emptyList()
    }
}
