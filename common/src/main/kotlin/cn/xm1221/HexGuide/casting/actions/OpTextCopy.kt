package cn.xm1221.HexGuide.casting.actions

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getBool
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import cn.xm1221.HexGuide.compat.inline.IotaInlineData
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import net.minecraft.server.level.ServerPlayer

class OpTextCopy: ConstMediaAction {
    override val argc: Int
        get() = 2

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): List<Iota> {
        val caster = env.castingEntity
        val iota = args.get(0)
        val bool = args.getBool(1,argc)
        var key = IotaInlineData.toPrefixed(iota)
        if(bool){
             key = IotaType.serialize(iota).toString()
        }
        val event = ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD,key)
        val event2 = HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("hexguide.copy.hover"))
        val text = iota.display().copy().withStyle(Style.EMPTY.withClickEvent(event).withHoverEvent(event2))
        if(caster is ServerPlayer){
            caster.sendSystemMessage(text)
            // 自动以 JSON 形式保存到 <gameDir>/<ns>/iotas/，并提示资源引用
            val savedRef = IotaInlineData.saveToGameDir(iota)
            if (savedRef != null) {
                val ref = "iota:$savedRef.json"
                val saveMsg = Component.translatable("hexguide.copy.saved", ref)
                    .withStyle(Style.EMPTY.withClickEvent(ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, ref)))
                caster.sendSystemMessage(saveMsg)
            }
        }
        return listOf()
    }
}