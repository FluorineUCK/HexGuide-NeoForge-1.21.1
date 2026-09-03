@file:JvmName("HexGuidePayloads")

package cn.xm1221.HexGuide.networking.msg

import cn.xm1221.HexGuide.HexGuide
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.neoforged.neoforge.network.PacketDistributor
import java.util.UUID

private fun <T> codec(
    encode: (RegistryFriendlyByteBuf, T) -> Unit,
    decode: (RegistryFriendlyByteBuf) -> T,
): StreamCodec<RegistryFriendlyByteBuf, T> = StreamCodec.of(encode, decode)

private fun RegistryFriendlyByteBuf.writeOptionalText(value: String?, max: Int) {
    writeBoolean(value != null)
    if (value != null) writeUtf(value, max)
}

private fun RegistryFriendlyByteBuf.readOptionalText(max: Int): String? =
    if (readBoolean()) readUtf(max) else null

private fun RegistryFriendlyByteBuf.readCount(max: Int): Int =
    readVarInt().also { require(it in 0..max) { "Invalid collection size: $it" } }

sealed interface HexGuideMessage : CustomPacketPayload

sealed interface HexGuideMessageC2S : HexGuideMessage {
    fun sendToServer() = PacketDistributor.sendToServer(this)
}

sealed interface HexGuideMessageS2C : HexGuideMessage {
    fun sendToPlayer(player: net.minecraft.server.level.ServerPlayer) = PacketDistributor.sendToPlayer(player, this)
    fun sendToPlayers(players: Iterable<net.minecraft.server.level.ServerPlayer>) = players.forEach(::sendToPlayer)
}

data class MsgBookExecDemoC2S(val sig: String, val startDir: String, val image: CompoundTag) : HexGuideMessageC2S {
    override fun type() = TYPE
    companion object {
        @JvmField val TYPE = CustomPacketPayload.Type<MsgBookExecDemoC2S>(HexGuide.id("book_exec_demo_c2s"))
        @JvmField val STREAM_CODEC = codec<MsgBookExecDemoC2S>(
            { b, v -> b.writeUtf(v.sig, 256); b.writeUtf(v.startDir, 64); b.writeNbt(v.image) },
            { b -> MsgBookExecDemoC2S(b.readUtf(256), b.readUtf(64), b.readNbt() ?: CompoundTag()) },
        )
    }
}

data class MsgBookExecDemoS2C(val image: CompoundTag, val resolutionType: String) : HexGuideMessageS2C {
    override fun type() = TYPE
    companion object {
        @JvmField val TYPE = CustomPacketPayload.Type<MsgBookExecDemoS2C>(HexGuide.id("book_exec_demo_s2c"))
        @JvmField val STREAM_CODEC = codec<MsgBookExecDemoS2C>(
            { b, v -> b.writeNbt(v.image); b.writeUtf(v.resolutionType, 64) },
            { b -> MsgBookExecDemoS2C(b.readNbt() ?: CompoundTag(), b.readUtf(64)) },
        )
    }
}

data class MsgBookLoadSpellplayC2S(val ns: String, val name: String) : HexGuideMessageC2S {
    override fun type() = TYPE
    companion object {
        @JvmField val TYPE = CustomPacketPayload.Type<MsgBookLoadSpellplayC2S>(HexGuide.id("book_load_spellplay_c2s"))
        @JvmField val STREAM_CODEC = codec<MsgBookLoadSpellplayC2S>(
            { b, v -> b.writeUtf(v.ns, 64); b.writeUtf(v.name, 128) },
            { b -> MsgBookLoadSpellplayC2S(b.readUtf(64), b.readUtf(128)) },
        )
    }
}

data class MsgBookLoadSpellplayS2C(
    val ns: String,
    val name: String,
    val json: String?,
    val patternVector: String?,
) : HexGuideMessageS2C {
    override fun type() = TYPE
    companion object {
        @JvmField val TYPE = CustomPacketPayload.Type<MsgBookLoadSpellplayS2C>(HexGuide.id("book_load_spellplay_s2c"))
        @JvmField val STREAM_CODEC = codec<MsgBookLoadSpellplayS2C>(
            { b, v -> b.writeUtf(v.ns, 64); b.writeUtf(v.name, 128); b.writeOptionalText(v.json, 65536); b.writeOptionalText(v.patternVector, 65536) },
            { b -> MsgBookLoadSpellplayS2C(b.readUtf(64), b.readUtf(128), b.readOptionalText(65536), b.readOptionalText(65536)) },
        )
    }
}

data class MsgBookPushIotaC2S(val iotaNbt: CompoundTag, val image: CompoundTag) : HexGuideMessageC2S {
    override fun type() = TYPE
    companion object {
        @JvmField val TYPE = CustomPacketPayload.Type<MsgBookPushIotaC2S>(HexGuide.id("book_push_iota_c2s"))
        @JvmField val STREAM_CODEC = codec<MsgBookPushIotaC2S>(
            { b, v -> b.writeNbt(v.iotaNbt); b.writeNbt(v.image) },
            { b -> MsgBookPushIotaC2S(b.readNbt() ?: CompoundTag(), b.readNbt() ?: CompoundTag()) },
        )
    }
}

data class MsgBookSyncStackC2S(val patterns: List<CompoundTag>) : HexGuideMessageC2S {
    override fun type() = TYPE
    companion object {
        @JvmField val TYPE = CustomPacketPayload.Type<MsgBookSyncStackC2S>(HexGuide.id("book_sync_stack_c2s"))
        @JvmField val STREAM_CODEC = codec<MsgBookSyncStackC2S>(
            { b, v -> require(v.patterns.size <= 1024); b.writeVarInt(v.patterns.size); v.patterns.forEach(b::writeNbt) },
            { b -> MsgBookSyncStackC2S(List(b.readCount(1024)) { b.readNbt() ?: CompoundTag() }) },
        )
    }
}

data class MsgExcludedPatternsS2C(val ids: List<String>) : HexGuideMessageS2C {
    override fun type() = TYPE
    companion object {
        @JvmField val TYPE = CustomPacketPayload.Type<MsgExcludedPatternsS2C>(HexGuide.id("excluded_patterns_s2c"))
        @JvmField val STREAM_CODEC = codec<MsgExcludedPatternsS2C>(
            { b, v -> require(v.ids.size <= 8192); b.writeVarInt(v.ids.size); v.ids.forEach { b.writeUtf(it, 128) } },
            { b -> MsgExcludedPatternsS2C(List(b.readCount(8192)) { b.readUtf(128) }) },
        )
    }
}

data class MsgIotaSyncS2C(val ref: String, val iotaNbt: CompoundTag) : HexGuideMessageS2C {
    override fun type() = TYPE
    companion object {
        @JvmField val TYPE = CustomPacketPayload.Type<MsgIotaSyncS2C>(HexGuide.id("iota_sync_s2c"))
        @JvmField val STREAM_CODEC = codec<MsgIotaSyncS2C>(
            { b, v -> b.writeUtf(v.ref, 64); b.writeNbt(v.iotaNbt) },
            { b -> MsgIotaSyncS2C(b.readUtf(64), b.readNbt() ?: CompoundTag()) },
        )
    }
}

data class MsgNotesAppendC2S(val sectionIndex: Int, val iota: CompoundTag) : HexGuideMessageC2S {
    override fun type() = TYPE
    companion object {
        @JvmField val TYPE = CustomPacketPayload.Type<MsgNotesAppendC2S>(HexGuide.id("notes_append_c2s"))
        @JvmField val STREAM_CODEC = codec<MsgNotesAppendC2S>(
            { b, v -> b.writeVarInt(v.sectionIndex); b.writeNbt(v.iota) },
            { b -> MsgNotesAppendC2S(b.readVarInt(), b.readNbt() ?: CompoundTag()) },
        )
    }
}

data class MsgNotesNewSectionC2S(val iotas: List<CompoundTag>) : HexGuideMessageC2S {
    override fun type() = TYPE
    companion object {
        @JvmField val TYPE = CustomPacketPayload.Type<MsgNotesNewSectionC2S>(HexGuide.id("notes_new_section_c2s"))
        @JvmField val STREAM_CODEC = codec<MsgNotesNewSectionC2S>(
            { b, v -> require(v.iotas.size <= 256); b.writeVarInt(v.iotas.size); v.iotas.forEach(b::writeNbt) },
            { b -> MsgNotesNewSectionC2S(List(b.readCount(256)) { b.readNbt() ?: CompoundTag() }) },
        )
    }
}

data class MsgNotesSaveC2S(val title: String, val pages: List<String>) : HexGuideMessageC2S {
    override fun type() = TYPE
    companion object {
        @JvmField val TYPE = CustomPacketPayload.Type<MsgNotesSaveC2S>(HexGuide.id("notes_save_c2s"))
        @JvmField val STREAM_CODEC = codec<MsgNotesSaveC2S>(
            { b, v -> require(v.pages.size <= 256); b.writeUtf(v.title, 64); b.writeVarInt(v.pages.size); v.pages.forEach { b.writeUtf(it, 2048) } },
            { b -> MsgNotesSaveC2S(b.readUtf(64), List(b.readCount(256)) { b.readUtf(2048) }) },
        )
    }
}

data class MsgNotesSyncS2C(val uuid: UUID, val sections: List<List<CompoundTag>>) : HexGuideMessageS2C {
    override fun type() = TYPE
    companion object {
        @JvmField val TYPE = CustomPacketPayload.Type<MsgNotesSyncS2C>(HexGuide.id("notes_sync_s2c"))
        @JvmField val STREAM_CODEC = codec<MsgNotesSyncS2C>(
            { b, v ->
                require(v.sections.size <= 1024)
                b.writeUUID(v.uuid); b.writeVarInt(v.sections.size)
                v.sections.forEach { section ->
                    require(section.size <= 256)
                    b.writeVarInt(section.size)
                    section.forEach(b::writeNbt)
                }
            },
            { b ->
                val uuid = b.readUUID()
                MsgNotesSyncS2C(uuid, List(b.readCount(1024)) { List(b.readCount(256)) { b.readNbt() ?: CompoundTag() } })
            },
        )
    }
}

class MsgRequestExcludedPatternsC2S : HexGuideMessageC2S {
    override fun type() = TYPE
    companion object {
        @JvmField val TYPE = CustomPacketPayload.Type<MsgRequestExcludedPatternsC2S>(HexGuide.id("request_excluded_patterns_c2s"))
        @JvmField val STREAM_CODEC = codec<MsgRequestExcludedPatternsC2S>({ _, _ -> }, { MsgRequestExcludedPatternsC2S() })
    }
}
