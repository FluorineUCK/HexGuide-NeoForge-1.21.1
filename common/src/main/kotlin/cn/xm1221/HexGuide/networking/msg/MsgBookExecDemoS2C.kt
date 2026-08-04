package cn.xm1221.HexGuide.networking.msg

import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf

/** 服务端→客户端：演示"真执行"的结果（运行后的 CastingImage + 解析类型名） */
class MsgBookExecDemoS2C(
    val image: CompoundTag,
    val resolutionType: String
) : HexGuideMessageS2C {
    companion object : HexGuideMessageCompanion<MsgBookExecDemoS2C> {
        override val type = MsgBookExecDemoS2C::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgBookExecDemoS2C(
            image = buf.readNbt() ?: CompoundTag(),
            resolutionType = buf.readUtf(64)
        )

        override fun MsgBookExecDemoS2C.encode(buf: FriendlyByteBuf) {
            buf.writeNbt(image)
            buf.writeUtf(resolutionType)
        }
    }
}
