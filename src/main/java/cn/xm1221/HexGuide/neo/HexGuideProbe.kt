package cn.xm1221.HexGuide.neo

import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.NullIota
import at.petrak.hexcasting.common.lib.HexRegistries
import cn.xm1221.HexGuide.HexGuide
import cn.xm1221.HexGuide.api.notes.NoteIota
import cn.xm1221.HexGuide.api.notes.PlayerNotes
import cn.xm1221.HexGuide.hexcompat.IotaTextCodec
import cn.xm1221.HexGuide.hexcompat.deserializeIota
import cn.xm1221.HexGuide.hexcompat.deserializeIotaCompat
import cn.xm1221.HexGuide.hexcompat.serializeIota
import cn.xm1221.HexGuide.registry.HexGuideActions
import cn.xm1221.HexGuide.registry.HexGuideIotaTypes
import cn.xm1221.HexGuide.registry.HexGuideItems
import cn.xm1221.HexGuide.items.NoteScrapItem
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import java.util.UUID

/** Runtime feature probe enabled only by the bounded validation launcher. */
object HexGuideProbe {
    private const val PROPERTY = "hexguide.probe.validate"

    @JvmStatic
    fun run(server: MinecraftServer): Boolean {
        if (!java.lang.Boolean.getBoolean(PROPERTY)) return true
        val failures = mutableListOf<String>()

        fun check(label: String, body: () -> Unit) {
            try {
                body()
                HexGuide.LOGGER.info("[HEXGUIDE-PROBE] {}=PASS", label)
            } catch (failure: Throwable) {
                failures += label
                HexGuide.LOGGER.error("[HEXGUIDE-PROBE] {}=FAIL", label, failure)
            }
        }

        check("registries") {
            val itemIds = listOf("amethyst_pen", "note_scrap").map(HexGuide::id)
            require(itemIds.all(BuiltInRegistries.ITEM::containsKey))
            val actions = server.registryAccess().registryOrThrow(HexRegistries.ACTION)
            val actionIds = listOf(
                "copy", "demo", "note/import", "note/list", "note/get", "note/delete"
            ).map(HexGuide::id)
            require(actionIds.all(actions::containsKey))
            val iotas = server.registryAccess().registryOrThrow(HexRegistries.IOTA_TYPE)
            require(iotas.containsKey(HexGuide.id("note")))
            require(HexGuideItems.AMETHYST_PEN.value === BuiltInRegistries.ITEM.get(HexGuide.id("amethyst_pen")))
            require(HexGuideItems.NOTE_SCRAP.value === BuiltInRegistries.ITEM.get(HexGuide.id("note_scrap")))
            require(HexGuideActions.COPY.value === actions.get(HexGuide.id("copy")))
            require(HexGuideIotaTypes.NOTE.value === iotas.get(HexGuide.id("note")))
        }

        check("recipe") {
            require(server.recipeManager.byKey(HexGuide.id("amethyst_pen")).isPresent) {
                "Missing runtime recipe hexguide:amethyst_pen"
            }
        }

        check("commands") {
            val root = server.commands.dispatcher.root.getChild("hexguide")
                ?: error("Missing /hexguide command root")
            require(listOf("export", "import", "authority").all { root.getChild(it) != null })
        }

        check("note_codec") {
            val original = NoteIota("Probe", "Page", "Codex", "probe", 39L)
            val decoded = deserializeIota(serializeIota(original), server.overworld()) as? NoteIota
            require(decoded != null)
            require(decoded.title == original.title && decoded.body == original.body)
            require(decoded.author == original.author && decoded.id == original.id && decoded.time == original.time)

            val legacy = CompoundTag().also {
                it.putString("title", original.title)
                it.putString("body", original.body)
                it.putString("author", original.author)
                it.putString("id", original.id)
                it.putLong("time", original.time)
            }
            val legacyDecoded = deserializeIotaCompat(legacy, server.overworld()) as? NoteIota
            require(legacyDecoded != null && legacyDecoded.body == original.body)

            val other = NoteIota("Different", "Payload", "Other", "different", 1L)
            require(original == other) { "Upstream NoteIota type-tolerance semantics changed" }
            require(original.hashCode() == other.hashCode()) { "NoteIota equals/hashCode contract broken" }
        }

        check("note_scrap") {
            val original = NoteIota("Scrap", "Payload", "Codex", "scrap", 39L)
            val stack = net.minecraft.world.item.ItemStack(HexGuideItems.NOTE_SCRAP.value)
            NoteScrapItem.setNote(stack, original)
            val decoded = NoteScrapItem.getNote(stack)
            require(decoded != null)
            require(decoded.title == original.title && decoded.body == original.body)
            val holder = HexGuideItems.NOTE_SCRAP.value as at.petrak.hexcasting.api.item.IotaHolderItem
            require(holder.readIota(stack) is NoteIota)
            holder.writeDatum(stack, null)
            require(holder.readIota(stack) == null)
        }

        check("inline_codec") {
            val original: Iota = NullIota()
            val encoded = IotaTextCodec.encode(original)
            require(encoded.isNotBlank())
            require(IotaTextCodec.decode(encoded) is NullIota)
        }

        check("saved_data") {
            val id = UUID.nameUUIDFromBytes("hexguide-runtime-probe".toByteArray())
            val notes = PlayerNotes.get(server.overworld())
            val before = notes.sections(id).size
            notes.newSection(id, listOf(NoteIota("Probe", "Saved", "Codex", "saved", 39L)))
            require(notes.sections(id).size == before + 1)
            require(notes.removeSection(id, before))
            require(notes.sections(id).size == before)
        }

        if (failures.isEmpty()) {
            HexGuide.LOGGER.info(
                "[HEXGUIDE-PROBE] aggregate=PASS items=2 actions=6 iotas=1 codec=true persistence=true"
            )
            return true
        }
        HexGuide.LOGGER.error(
            "[HEXGUIDE-PROBE] aggregate=FAIL failure_count={} failures={}",
            failures.size,
            failures.joinToString(",")
        )
        return false
    }
}
