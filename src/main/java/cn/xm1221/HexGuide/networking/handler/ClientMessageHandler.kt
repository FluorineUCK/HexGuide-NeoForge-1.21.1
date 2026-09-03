package cn.xm1221.HexGuide.networking.handler

import cn.xm1221.HexGuide.networking.msg.*
import cn.xm1221.HexGuide.patchouli.SpellcastDemoPage
import cn.xm1221.HexGuide.registry.HexGuideCreativeTab
import net.neoforged.neoforge.network.handling.IPayloadContext

fun HexGuideMessageS2C.applyOnClient(ctx: IPayloadContext) {
    when (this) {
        is MsgBookExecDemoS2C -> SpellcastDemoPage.ACTIVE.forEach { it.onExecResult(image, resolutionType) }
        is MsgBookLoadSpellplayS2C -> SpellcastDemoPage.ACTIVE.filter { it.matches(ns, name) }
            .forEach { it.onSpellplayLoaded(json, patternVector) }
        is MsgExcludedPatternsS2C -> HexGuideCreativeTab.setExcludedPatterns(ids.toSet())
        is MsgNotesSyncS2C -> cn.xm1221.HexGuide.api.notes.ClientNotes.applySync(uuid, sections)
        is MsgIotaSyncS2C -> cn.xm1221.HexGuide.hexcompat.IotaTextCodec.saveToGameDirRef(ref, iotaNbt)
    }
}
