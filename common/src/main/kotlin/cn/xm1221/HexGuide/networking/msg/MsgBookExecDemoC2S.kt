package cn.xm1221.HexGuide.networking.msg

import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf

/** 客户端→服务端：把本地的 CastingImage 上传，让服务端运行一个图案（演示"真执行"步骤） */
class MsgBookExecDemoC2S(
    val sig: String,
    val startDir: String,
    val image: CompoundTag
) : HexGuideMessageC2S {
    companion object : HexGuideMessageCompanion<MsgBookExecDemoC2S> {
        override val type = MsgBookExecDemoC2S::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgBookExecDemoC2S(
            sig = buf.readUtf(256),
            startDir = buf.readUtf(64),
            image = buf.readNbt() ?: CompoundTag()
        )

        override fun MsgBookExecDemoC2S.encode(buf: FriendlyByteBuf) {
            buf.writeUtf(sig)
            buf.writeUtf(startDir)
            buf.writeNbt(image)
        }
    }
}
