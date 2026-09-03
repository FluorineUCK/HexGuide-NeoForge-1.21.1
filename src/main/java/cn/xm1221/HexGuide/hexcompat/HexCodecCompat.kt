@file:JvmName("HexCodecCompat")

package cn.xm1221.HexGuide.hexcompat

import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.eval.vm.ContinuationFrame
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation
import at.petrak.hexcasting.api.casting.iota.BooleanIota
import at.petrak.hexcasting.api.casting.iota.ContinuationIota
import at.petrak.hexcasting.api.casting.iota.DoubleIota
import at.petrak.hexcasting.api.casting.iota.GarbageIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.iota.NullIota
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.casting.iota.Vec3Iota
import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.api.casting.math.HexPattern
import com.mojang.serialization.Codec
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.NumericTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3
import cn.xm1221.HexGuide.api.notes.NoteIota

private const val LEGACY_TYPE_KEY = "hexcasting:type"
private const val LEGACY_DATA_KEY = "hexcasting:data"

private fun requireCompound(tag: Tag?, what: String): CompoundTag =
    tag as? CompoundTag ?: throw IllegalArgumentException("$what did not encode to a CompoundTag")

fun serializeIota(iota: Iota): CompoundTag =
    requireCompound(IotaType.TYPED_CODEC.encodeStart(NbtOps.INSTANCE, iota).result().orElse(null), "Iota")

fun deserializeIota(tag: Tag, level: ServerLevel? = null): Iota? {
    val iota = IotaType.TYPED_CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(null) ?: return null
    // TYPED_CODEC uses GarbageIota as an error fallback. A compound explicitly
    // typed as modern GarbageIota is valid; malformed/legacy data must continue
    // into compatibility decoding instead of being accepted as garbage.
    if (iota is GarbageIota && (tag as? CompoundTag)?.getString("type") != "hexcasting:garbage") return null
    if (level == null) return iota
    @Suppress("UNCHECKED_CAST")
    return if ((iota.type as IotaType<Iota>).validate(iota, level)) iota else null
}

/**
 * Decode Hex 0.10/0.11 Iota compounds. HexGuide's bundled
 * `assets/<namespace>/iotas` resources
 * are authored in this format, while Hex 0.12/pre39's public TYPED_CODEC uses
 * `{type, ...payload fields}`. Keeping the conversion at this boundary avoids
 * rewriting upstream art/content resources and also preserves old exported
 * Inline files.
 */
fun deserializeLegacyIota(tag: Tag, level: ServerLevel? = null): Iota? {
    val compound = tag as? CompoundTag ?: return null
    val type = compound.getString(LEGACY_TYPE_KEY)
    if (type.isEmpty()) return null
    val payload = compound.get(LEGACY_DATA_KEY)
    val decoded = when (type) {
        "hexcasting:null" -> NullIota()
        "hexcasting:garbage" -> GarbageIota()
        "hexcasting:double" -> (payload as? NumericTag)?.asDouble?.let(::DoubleIota)
        "hexcasting:boolean" -> (payload as? NumericTag)?.asByte?.let { BooleanIota(it.toInt() != 0) }
        "hexcasting:vec3" -> decodeLegacyVector(payload)
        "hexcasting:pattern" -> decodeLegacyPattern(payload)
        "hexcasting:list" -> decodeLegacyList(payload, level)
        "hexcasting:continuation" -> decodeLegacyContinuation(payload, level)
        else -> decodeLegacyRegisteredIota(type, payload)
    } ?: return null
    if (level == null) return decoded
    @Suppress("UNCHECKED_CAST")
    return if ((decoded.type as IotaType<Iota>).validate(decoded, level)) decoded else null
}

private fun decodeLegacyVector(payload: Tag?): Iota? {
    val vector = payload as? CompoundTag ?: return null
    return Vec3Iota(Vec3(vector.getDouble("x"), vector.getDouble("y"), vector.getDouble("z")))
}

private fun decodeLegacyPattern(payload: Tag?): Iota? {
    val pattern = payload as? CompoundTag ?: return null
    val startDir = HexDir.values().getOrNull(pattern.getByte("start_dir").toInt()) ?: return null
    val signature = StringBuilder()
    for (angle in pattern.getByteArray("angles")) {
        signature.append(when (angle.toInt()) {
            0 -> "w"
            1 -> "e"
            2 -> "d"
            3 -> "s"
            4 -> "a"
            5 -> "q"
            else -> return null
        })
    }
    return runCatching { PatternIota(HexPattern.fromAnglesUnchecked(signature.toString(), startDir)) }.getOrNull()
}

private fun decodeLegacyList(payload: Tag?, level: ServerLevel?): Iota? {
    val list = payload as? ListTag ?: return null
    val decoded = ArrayList<Iota>(list.size)
    for (child in list) {
        decoded += deserializeLegacyIota(child, level) ?: return null
    }
    return ListIota(decoded)
}

private fun decodeLegacyContinuation(payload: Tag?, level: ServerLevel?): Iota? {
    val continuation = payload as? CompoundTag ?: return null
    val frames = continuation.get("frame") as? ListTag ?: return null
    val converted = ListTag()
    for (frame in frames) {
        val legacyFrame = frame as? CompoundTag ?: return null
        val type = legacyFrame.getString(LEGACY_TYPE_KEY)
        val data = legacyFrame.get(LEGACY_DATA_KEY) as? CompoundTag ?: CompoundTag()
        val modern = CompoundTag()
        modern.putString("type", type)
        when (type) {
            "hexcasting:evaluate" -> {
                val patterns = data.get("patterns") as? ListTag ?: return null
                val decodedPatterns = ListTag()
                for (pattern in patterns) {
                    decodedPatterns.add(serializeIota(deserializeLegacyIota(pattern, level) ?: return null))
                }
                modern.put("patterns", decodedPatterns)
                modern.putBoolean("isMetacasting", data.getBoolean("isMetacasting"))
            }
            "hexcasting:end" -> Unit
            else -> return null
        }
        converted.add(modern)
    }
    val decoded: List<ContinuationFrame> = ContinuationFrame.Type.Companion.TYPED_CODEC
        .listOf()
        .parse(NbtOps.INSTANCE, converted)
        .result()
        .orElse(null) ?: return null
    var continuationValue: SpellContinuation = SpellContinuation.Done
    for (index in decoded.indices.reversed()) {
        continuationValue = SpellContinuation.NotDone(decoded[index], continuationValue)
    }
    return ContinuationIota(continuationValue)
}

private fun decodeLegacyRegisteredIota(type: String, payload: Tag?): Iota? {
    val id = net.minecraft.resources.ResourceLocation.tryParse(type) ?: return null
    if (at.petrak.hexcasting.xplat.IXplatAbstractions.INSTANCE
        .iotaTypeRegistry
        .get(id) == null) return null
    val modern = CompoundTag().also { converted ->
        converted.putString("type", type)
        when (type) {
            // MoreIotas 0.11 stored a bare ResourceLocation string.
            "moreiotas:entity_type" -> {
                val entityType = (payload as? StringTag)?.asString ?: return null
                converted.putString("entityType", entityType)
            }
            else -> converted.put("value", payload?.copy() ?: CompoundTag())
        }
    }
    val decoded = IotaType.TYPED_CODEC.parse(NbtOps.INSTANCE, modern).result().orElse(null) ?: return null
    return decoded.takeUnless { it is GarbageIota && type != "hexcasting:garbage" }
}

/** Decode both pre39 typed Iotas and HexGuide's historical bare Note payload. */
fun deserializeIotaCompat(tag: Tag, level: ServerLevel? = null): Iota? {
    deserializeIota(tag, level)?.let { return it }
    deserializeLegacyIota(tag, level)?.let { return it }
    val compound = tag as? CompoundTag ?: return null
    val payload = sequenceOf("data", "payload", "value", "hexcasting:data")
        .mapNotNull { key -> compound.get(key) as? CompoundTag }
        .firstOrNull { candidate ->
            candidate.contains("title") || candidate.contains("body") || candidate.contains("author")
        } ?: compound
    if (!payload.contains("title") && !payload.contains("body") && !payload.contains("author")) return null
    val note = NoteIota(
        title = payload.getString("title"),
        body = payload.getString("body"),
        author = payload.getString("author"),
        id = payload.getString("id"),
        time = payload.getLong("time"),
    )
    if (level == null) return note
    @Suppress("UNCHECKED_CAST")
    return if ((note.type as IotaType<Iota>).validate(note, level)) note else null
}

fun serializeCastingImage(image: CastingImage): CompoundTag =
    requireCompound(CastingImage.CODEC.encodeStart(NbtOps.INSTANCE, image).result().orElse(null), "CastingImage")

fun deserializeCastingImage(tag: Tag): CastingImage? =
    CastingImage.CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(null)
