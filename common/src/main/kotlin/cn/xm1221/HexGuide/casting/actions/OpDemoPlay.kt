package cn.xm1221.HexGuide.casting.actions

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import cn.xm1221.HexGuide.networking.msg.MsgOpenDemoS2C
import net.minecraft.server.level.ServerPlayer

class OpDemoPlay : ConstMediaAction {
    override val argc: Int get() = 0

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val caster = env.castingEntity
        if (caster is ServerPlayer)
            MsgOpenDemoS2C("hexguide", "text").sendToPlayer(caster)
        return listOf()
    }
}