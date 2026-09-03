package cn.xm1221.HexGuide.api.notes

import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import cn.xm1221.HexGuide.registry.HexGuideIotaTypes
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.ChatFormatting
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.codec.StreamCodec

class NoteIota(
    val title: String,
    val body: String,
    val author: String,
    val id: String,
    val time: Long,
) : Iota({ HexGuideIotaTypes.NOTE.value }) {

    override fun isTruthy(): Boolean = body.isNotEmpty() || title.isNotEmpty()

    // Preserve the upstream semantic: every NoteIota is mutually tolerant.
    // Hex Casting uses toleratesOther for fuzzy iota equality, not Kotlin object identity.
    override fun toleratesOther(that: Iota): Boolean = typesMatch(this, that) && that is NoteIota

    override fun display(): Component {
        if (title.isNotEmpty()) {
            val heading = Component.literal(title).withStyle(ChatFormatting.AQUA)
            return if (author.isEmpty()) heading
            else heading.append(Component.literal(" — $author").withStyle(ChatFormatting.GRAY))
        }
        return Component.translatable("hexguide.notes.unnamed").withStyle(ChatFormatting.GRAY)
    }

    // All NoteIotas are equal under the upstream tolerance rule, so their hash
    // must also be independent of payload contents.
    override fun hashCode(): Int = 0x4e4f5445

    companion object {
        @JvmField
        val TYPE: IotaType<NoteIota> = object : IotaType<NoteIota>() {
            private val codec: MapCodec<NoteIota> = RecordCodecBuilder.mapCodec { instance ->
                instance.group(
                    Codec.STRING.optionalFieldOf("title", "").forGetter(NoteIota::title),
                    Codec.STRING.optionalFieldOf("body", "").forGetter(NoteIota::body),
                    Codec.STRING.optionalFieldOf("author", "").forGetter(NoteIota::author),
                    Codec.STRING.optionalFieldOf("id", "").forGetter(NoteIota::id),
                    Codec.LONG.optionalFieldOf("time", 0L).forGetter(NoteIota::time),
                ).apply(instance, ::NoteIota)
            }

            private val streamCodec = object : StreamCodec<RegistryFriendlyByteBuf, NoteIota> {
                override fun decode(buf: RegistryFriendlyByteBuf): NoteIota = NoteIota(
                    buf.readUtf(MAX_TITLE),
                    buf.readUtf(MAX_BODY),
                    buf.readUtf(MAX_AUTHOR),
                    buf.readUtf(MAX_ID),
                    buf.readLong(),
                )

                override fun encode(buf: RegistryFriendlyByteBuf, value: NoteIota) {
                    buf.writeUtf(value.title, MAX_TITLE)
                    buf.writeUtf(value.body, MAX_BODY)
                    buf.writeUtf(value.author, MAX_AUTHOR)
                    buf.writeUtf(value.id, MAX_ID)
                    buf.writeLong(value.time)
                }
            }

            override fun codec(): MapCodec<NoteIota> = codec
            override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, NoteIota> = streamCodec
            override fun color(): Int = 0xff55ffff.toInt()
        }

        const val MAX_TITLE = 64
        const val MAX_BODY = 2048
        const val MAX_AUTHOR = 64
        const val MAX_ID = 64
    }
}
