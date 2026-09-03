package cn.xm1221.HexGuide.networking

import cn.xm1221.HexGuide.networking.handler.applyOnServer
import cn.xm1221.HexGuide.networking.msg.*
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.handling.IPayloadContext

object HexGuideNetworking {
    @Volatile
    private var clientHandler: ((HexGuideMessageS2C, IPayloadContext) -> Unit)? = null

    fun installClientHandler(handler: (HexGuideMessageS2C, IPayloadContext) -> Unit) {
        clientHandler = handler
    }

    fun register(event: RegisterPayloadHandlersEvent) {
        val registrar = event.registrar("1")
        registrar.playToServer(MsgBookExecDemoC2S.TYPE, MsgBookExecDemoC2S.STREAM_CODEC, ::handleServer)
        registrar.playToClient(MsgBookExecDemoS2C.TYPE, MsgBookExecDemoS2C.STREAM_CODEC, ::handleClient)
        registrar.playToServer(MsgBookLoadSpellplayC2S.TYPE, MsgBookLoadSpellplayC2S.STREAM_CODEC, ::handleServer)
        registrar.playToClient(MsgBookLoadSpellplayS2C.TYPE, MsgBookLoadSpellplayS2C.STREAM_CODEC, ::handleClient)
        registrar.playToServer(MsgBookPushIotaC2S.TYPE, MsgBookPushIotaC2S.STREAM_CODEC, ::handleServer)
        registrar.playToServer(MsgBookSyncStackC2S.TYPE, MsgBookSyncStackC2S.STREAM_CODEC, ::handleServer)
        registrar.playToClient(MsgExcludedPatternsS2C.TYPE, MsgExcludedPatternsS2C.STREAM_CODEC, ::handleClient)
        registrar.playToClient(MsgIotaSyncS2C.TYPE, MsgIotaSyncS2C.STREAM_CODEC, ::handleClient)
        registrar.playToServer(MsgNotesAppendC2S.TYPE, MsgNotesAppendC2S.STREAM_CODEC, ::handleServer)
        registrar.playToServer(MsgNotesNewSectionC2S.TYPE, MsgNotesNewSectionC2S.STREAM_CODEC, ::handleServer)
        registrar.playToServer(MsgNotesSaveC2S.TYPE, MsgNotesSaveC2S.STREAM_CODEC, ::handleServer)
        registrar.playToClient(MsgNotesSyncS2C.TYPE, MsgNotesSyncS2C.STREAM_CODEC, ::handleClient)
        registrar.playToServer(MsgRequestExcludedPatternsC2S.TYPE, MsgRequestExcludedPatternsC2S.STREAM_CODEC, ::handleServer)
    }

    private fun <T> handleServer(payload: T, context: IPayloadContext) where T : HexGuideMessageC2S {
        context.enqueueWork { payload.applyOnServer(context) }
    }

    private fun <T> handleClient(payload: T, context: IPayloadContext) where T : HexGuideMessageS2C {
        context.enqueueWork {
            clientHandler?.invoke(payload, context)
        }
    }
}
