package cn.xm1221.HexGuide.scrying

import at.petrak.hexcasting.api.HexAPI
import at.petrak.hexcasting.api.casting.ActionRegistryEntry
import at.petrak.hexcasting.api.casting.math.HexCoord
import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.api.mod.HexTags
import at.petrak.hexcasting.common.lib.hex.HexActions
import com.mojang.datafixers.util.Pair
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import vazkii.patchouli.client.book.ClientBookRegistry
import vazkii.patchouli.common.book.BookRegistry
import java.util.*

/**
 * Utility to look up a HexPattern in The HexBook (Patchouli) and find the corresponding entry/page.
 * Ported and adapted from HexBoard's PatternLookUpUtil.
 */
object PatternBookLookup {
    /** Categories in thehexbook to search for pattern references. */
    private val SEARCH_CATEGORIES = listOf(
        "hexcasting:patterns",
        "hexcasting:greatwork",
        "hexcasting:casting",
        "hexcasting:basics"
    )

    /** Cached lookup result: entry ID in the book, and page number. */
    data class LookupResult(
        val entry: ResourceLocation?,
        val page: Int,
        val actionName: String?
    ) {
        fun found(): Boolean = entry != null
    }

    /** Open The HexBook to the given entry/page. */
    fun openBook(result: LookupResult) {
        if (result.found()) {
            ClientBookRegistry.INSTANCE.displayBookGui(
                HexAPI.modLoc("thehexbook"),
                result.entry,
                result.page
            )
        }
    }

    /** Look up a pattern in the common action registry (exact signature match). */
    fun lookUpIdCommon(pattern: HexPattern): Optional<MutableMap.MutableEntry<ResourceKey<ActionRegistryEntry>, ActionRegistryEntry>> {
        return HexActions.REGISTRY.entrySet().stream()
            .filter { pattern.sigsEqual(it.value.prototype()) }
            .findAny()
    }

    /** Look up a pattern in the per-world action registry (allows rotation). */
    fun lookUpIdPerWorld(pattern: HexPattern): Optional<MutableMap.MutableEntry<ResourceKey<ActionRegistryEntry>, ActionRegistryEntry>> {
        val targets = mutableListOf<List<HexCoord>>()
        targets.add(
            pattern.positions().distinct()
                .sortedWith(compareBy({ it.q }, { it.r }))
        )
        for (dir in HexDir.entries) {
            if (dir == pattern.startDir) continue
            val p = HexPattern(dir, pattern.angles)
            targets.add(
                p.positions().distinct()
                    .sortedWith(compareBy({ it.q }, { it.r }))
            )
        }
        return HexActions.REGISTRY.entrySet().stream()
            .filter { entry ->
                val holder = HexActions.REGISTRY.getHolder(entry.key)
                if (!holder.map { it.`is`(HexTags.Actions.PER_WORLD_PATTERN) }.orElse(false))
                    return@filter false
                if (entry.value.prototype().angles.size == pattern.angles.size) {
                    val now = entry.value.prototype().positions().distinct()
                        .sortedWith(compareBy({ it.q }, { it.r }))
                    for (target in targets) {
                        if (target.size == now.size && target.size >= 2) {
                            val o1 = target[1]
                            val o2 = now[1]
                            var match = true
                            for (i in target.indices) {
                                val c1 = target[i]
                                val c2 = now[i]
                                if (c1.q - o1.q != c2.q - o2.q || c1.r - o1.r != c2.r - o2.r) {
                                    match = false
                                    break
                                }
                            }
                            if (match) return@filter true
                        }
                    }
                }
                false
            }
            .findAny()
    }

    /** Find the book entry and page for a given action registry ID. */
    fun lookUpIdPage(id: ResourceLocation): Pair<Optional<ResourceLocation>, Optional<Int>> {
        val ids = id.toString()
        val book = BookRegistry.INSTANCE.books[HexAPI.modLoc("thehexbook")] ?:
            return Pair.of(Optional.empty(), Optional.empty())
        for (cidStr in SEARCH_CATEGORIES) {
            val cid = ResourceLocation.tryParse(cidStr) ?: continue
            val category = book.contents.categories[cid] ?: continue
            for (e in category.entries) {
                val pages = e.pages
                for (i in pages.indices) {
                    val root = pages[i].sourceObject
                    try {
                        if (root.has("op_id") && root.get("op_id").asString == ids) {
                            return Pair.of(Optional.of(e.id), Optional.of(i))
                        }
                    } catch (_: Exception) {}
                }
            }
        }
        return Pair.of(Optional.empty(), Optional.empty())
    }

    /** Find the book entry and page by matching the pattern's angle signature. */
    fun lookUpPatternPage(pattern: HexPattern): Pair<Optional<ResourceLocation>, Optional<Int>> {
        val patSig = pattern.anglesSignature()
        val book = BookRegistry.INSTANCE.books[HexAPI.modLoc("thehexbook")] ?:
            return Pair.of(Optional.empty(), Optional.empty())
        for (cidStr in SEARCH_CATEGORIES) {
            val cid = ResourceLocation.tryParse(cidStr) ?: continue
            val category = book.contents.categories[cid] ?: continue
            for (e in category.entries) {
                val pages = e.pages
                for (i in pages.indices) {
                    val root = pages[i].sourceObject
                    try {
                        if (root.has("patterns")) {
                            val patsJson = root.getAsJsonArray("patterns").asList()
                                .map { it.asJsonObject }
                            for (jsonObject in patsJson) {
                                if (jsonObject.has("signature") && jsonObject.get("signature").asString == patSig) {
                                    return Pair.of(Optional.of(e.id), Optional.of(i))
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
        return Pair.of(Optional.empty(), Optional.empty())
    }

    /** Full lookup: returns the book entry, page, and action name for a given pattern. */
    fun lookup(pattern: HexPattern): LookupResult? {
        var actionId: ResourceLocation? = null
        var actionName: String? = null

        // Try common registry
        val common = lookUpIdCommon(pattern)
        if (common.isPresent) {
            actionId = common.get().key.location()
            actionName = "hexcasting.action.$actionId"
        } else {
            // Try per-world registry
            val perWorld = lookUpIdPerWorld(pattern)
            if (perWorld.isPresent) {
                actionId = perWorld.get().key.location()
                actionName = "hexcasting.action.$actionId"
            }
        }

        // Look up book entry/page by action ID
        if (actionId != null) {
            val idPageResult = lookUpIdPage(actionId)
            if (idPageResult.first.isPresent && idPageResult.second.isPresent) {
                return LookupResult(idPageResult.first.get(), idPageResult.second.get(), actionName)
            }
        }

        // Fallback: look up by pattern signature
        val patPageResult = lookUpPatternPage(pattern)
        if (patPageResult.first.isPresent && patPageResult.second.isPresent) {
            return LookupResult(patPageResult.first.get(), patPageResult.second.get(), actionName)
        }

        // Found action but couldn't find book page
        if (actionName != null) {
            return LookupResult(null, 0, actionName)
        }

        return null
    }
}
