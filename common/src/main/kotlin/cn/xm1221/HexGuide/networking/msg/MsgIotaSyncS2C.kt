package cn.xm1221.HexGuide.networking.msg

import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf

/**
 * 服务端→客户端：同步一个已保存的 iota（OpTextCopy 保存时广播给所有玩家）。
 * 客户端把该 iota 以相同 ref 保存到自己的 <gameDir>/hexguide/iotas/，之后 iota:<ref>.json 可加载。
 */
class MsgIotaSyncS2C(
    val ref: String,
    val iotaNbt: CompoundTag
) : HexGuideMessageS2C {
    companion object : HexGuideMessageCompanion<MsgIotaSyncS2C> {
        override val type = MsgIotaSyncS2C::class.java

        override fun decode(buf: FriendlyByteBuf) = MsgIotaSyncS2C(
            ref = buf.readUtf(64),
            iotaNbt = buf.readNbt() ?: CompoundTag()
        )

        override fun MsgIotaSyncS2C.encode(buf: FriendlyByteBuf) {
            buf.writeUtf(ref)
            buf.writeNbt(iotaNbt)
        }
    }
}
