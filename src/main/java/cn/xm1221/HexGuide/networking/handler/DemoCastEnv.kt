package cn.xm1221.HexGuide.networking.handler

import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.eval.env.StaffCastEnv
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand

/**
 * 演示用施法环境：把本地 CastingImage 上传到服务端运行。
 * 继承 StaffCastEnv（获得完整的施法语义：媒质、施法者、音效），但：
 * - 不写玩家的法杖栈（图像由调用方显式传入 CastingVM，不经过 getStaffcastVM）
 * - 屏蔽 postCast 的螺旋图案（演示在书内进行，不需要世界内特效）
 */
class DemoCastEnv(caster: ServerPlayer, hand: InteractionHand) : StaffCastEnv(caster, hand) {
    override fun postCast(image: CastingImage) {
        // 不发送 MsgNewSpiralPatternsS2C（不产生世界内螺旋图案）
    }
}
