package cn.xm1221.HexGuide.networking.handler

import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.api.mod.HexTags
import at.petrak.hexcasting.api.utils.TreeList
import at.petrak.hexcasting.common.lib.hex.HexActions
import at.petrak.hexcasting.xplat.IXplatAbstractions
import cn.xm1221.HexGuide.HexGuide
import cn.xm1221.HexGuide.api.notes.NoteIota
import cn.xm1221.HexGuide.api.notes.PlayerNotes
import cn.xm1221.HexGuide.hexcompat.deserializeCastingImage
import cn.xm1221.HexGuide.hexcompat.deserializeIota
import cn.xm1221.HexGuide.hexcompat.serializeCastingImage
import cn.xm1221.HexGuide.hexcompat.serializeIota
import cn.xm1221.HexGuide.networking.msg.*
import cn.xm1221.HexGuide.registry.HexGuideItems
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.network.handling.IPayloadContext
import java.util.UUID

private val SAFE_NAMESPACE = Regex("[a-z0-9_.-]+")
private val SAFE_SPELLPLAY = Regex("[a-z0-9_./-]+")

/** Merge all server-data pattern-vector resources in deterministic id order. */
private fun readPatternVector(player: ServerPlayer): String? = try {
    val resources = player.server.resourceManager.listResources("pattern_vector") { id ->
        id.namespace == HexGuide.MODID && id.path.endsWith(".json")
    }
    val merged = JsonObject()
    for ((id, resource) in resources.toSortedMap(compareBy(ResourceLocation::toString))) {
        try {
            val objectValue = resource.openAsReader().use(JsonParser::parseReader).asJsonObject
            for ((key, value) in objectValue.entrySet()) merged.add(key, value)
        } catch (exception: Exception) {
            HexGuide.LOGGER.warn("Unable to parse pattern-vector resource {}", id, exception)
        }
    }
    merged.takeIf { it.size() > 0 }?.toString()
} catch (exception: Exception) {
    HexGuide.LOGGER.warn("Unable to read pattern-vector resources", exception)
    null
}

fun HexGuideMessageC2S.applyOnServer(context: IPayloadContext) {
    val player = context.player() as? ServerPlayer ?: return
    when (this) {
        is MsgBookSyncStackC2S -> {
            val vm = IXplatAbstractions.INSTANCE.getStaffcastVM(player, InteractionHand.MAIN_HAND)
            val additions = patterns.mapNotNull { deserializeIota(it, player.serverLevel()) }
            val newStack = TreeList.from(vm.image.stack).appendedAll(additions)
            IXplatAbstractions.INSTANCE.setStaffcastImage(player, vm.image.copy(stack = newStack))
        }

        is MsgBookExecDemoC2S -> {
            val world = player.serverLevel()
            val initial = deserializeCastingImage(image) ?: CastingImage()
            val pattern = try {
                HexPattern.fromAngles(sig, HexDir.valueOf(startDir))
            } catch (_: Exception) {
                return
            }
            val vm = CastingVM(initial, DemoCastEnv(player, InteractionHand.MAIN_HAND))
            val result = vm.queueExecuteAndWrapIota(PatternIota(pattern), world)
            MsgBookExecDemoS2C(serializeCastingImage(vm.image), result.resolutionType.name).sendToPlayer(player)
        }

        is MsgBookPushIotaC2S -> {
            val world = player.serverLevel()
            val initial = deserializeCastingImage(image) ?: CastingImage()
            val pushed = deserializeIota(iotaNbt, world) ?: return
            val vm = CastingVM(initial, DemoCastEnv(player, InteractionHand.MAIN_HAND))
            val result = vm.queueExecuteAndWrapIota(pushed, world)
            MsgBookExecDemoS2C(serializeCastingImage(vm.image), result.resolutionType.name).sendToPlayer(player)
        }

        is MsgBookLoadSpellplayC2S -> {
            if (!SAFE_NAMESPACE.matches(ns) || !SAFE_SPELLPLAY.matches(name) || ".." in name) return
            val id = ResourceLocation.tryBuild(ns, "spellplays/$name.json") ?: return
            val json = try {
                player.server.resourceManager.getResource(id)
                    .map { resource -> resource.openAsReader().use { it.readText() } }
                    .orElse(null)
            } catch (exception: Exception) {
                HexGuide.LOGGER.warn("Unable to read spellplay {}", id, exception)
                null
            }
            MsgBookLoadSpellplayS2C(ns, name, json, readPatternVector(player)).sendToPlayer(player)
        }

        is MsgRequestExcludedPatternsC2S -> {
            val great = HexActions.REGISTRY.getTag(HexTags.Actions.REQUIRES_ENLIGHTENMENT)
            val perWorld = HexActions.REGISTRY.getTag(HexTags.Actions.PER_WORLD_PATTERN)
            val excluded = HexActions.REGISTRY.entrySet().asSequence()
                .filter { (key, _) ->
                    (great.isPresent && great.get().stream().anyMatch { it.`is`(key) }) ||
                        (perWorld.isPresent && perWorld.get().stream().anyMatch { it.`is`(key) })
                }
                .map { (key, _) -> key.location().toString() }
                .toList()
            MsgExcludedPatternsS2C(excluded).sendToPlayer(player)
        }

        is MsgNotesAppendC2S -> {
            val note = deserializeIota(iota, player.serverLevel()) as? NoteIota ?: return
            val notes = PlayerNotes.get(player.serverLevel())
            notes.appendIota(player.uuid, sectionIndex, note)
            syncNotes(player, notes)
        }

        is MsgNotesNewSectionC2S -> {
            val decoded = iotas.mapNotNull { deserializeIota(it, player.serverLevel()) as? NoteIota }
            val notes = PlayerNotes.get(player.serverLevel())
            notes.newSection(player.uuid, decoded)
            syncNotes(player, notes)
        }

        is MsgNotesSaveC2S -> {
            val offhand = player.offhandItem
            if (!offhand.`is`(net.minecraft.world.item.Items.PAPER) || pages.isEmpty()) return
            if (offhand.count < pages.size) {
                player.displayClientMessage(Component.translatable("hexguide.notes.need_more_paper"), true)
                return
            }
            val now = System.currentTimeMillis()
            pages.forEachIndexed { index, body ->
                val note = NoteIota(
                    title = if (index == 0) title else "",
                    body = body,
                    author = player.gameProfile.name,
                    id = UUID.randomUUID().toString(),
                    time = now,
                )
                offhand.shrink(1)
                val scrap = ItemStack(HexGuideItems.NOTE_SCRAP.value)
                cn.xm1221.HexGuide.items.NoteScrapItem.setNote(scrap, note)
                if (!player.addItem(scrap)) player.drop(scrap, false)
            }
        }
    }
}

fun syncNotes(player: ServerPlayer, notes: PlayerNotes) {
    val sections = notes.sections(player.uuid).map { section -> section.map(::serializeIota) }
    MsgNotesSyncS2C(player.uuid, sections).sendToPlayer(player)
}
