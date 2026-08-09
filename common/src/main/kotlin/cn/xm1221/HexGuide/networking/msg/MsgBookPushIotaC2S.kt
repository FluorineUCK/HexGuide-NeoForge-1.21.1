package cn.xm1221.HexGuide.networking.msg

import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf

/**
 * 客户端→服务端：push_in 演示步骤——上传本地 CastingImage 与要压入的 iota，
 * 服务端按原版逻辑执行（escapeNext → 转义进括号/压栈；parenCount>0 → 进括号；否则压主栈），
 * 回传 MsgBookExecDemoS2C 更新栈与括号/转义状态。
 */
class MsgBookPushIotaC2S(
    val iotaNbt: CompoundTag,
    val image: CompoundTag
) : HexGuideMessageC2S {
    companion object : HexGuideMessageCompanion<MsgBookPushIotaC2S> {
        override val type = MsgBookPushIotaC2S::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgBookPushIotaC2S(
            iotaNbt = buf.readNbt() ?: CompoundTag(),
            image = buf.readNbt() ?: CompoundTag()
        )

        override fun MsgBookPushIotaC2S.encode(buf: FriendlyByteBuf) {
            buf.writeNbt(iotaNbt)
            buf.writeNbt(image)
        }
    }
}
