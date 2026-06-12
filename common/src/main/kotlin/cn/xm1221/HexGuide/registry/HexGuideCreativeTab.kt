package cn.xm1221.HexGuide.registry

import at.petrak.hexcasting.api.casting.ActionRegistryEntry
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.api.mod.HexTags
import at.petrak.hexcasting.common.lib.HexBlocks
import at.petrak.hexcasting.common.lib.HexItems
import at.petrak.hexcasting.common.lib.hex.HexActions
import cn.xm1221.HexGuide.HexGuide
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack

/**
 * Creative mode tab that contains slates and large scrolls with every registered Hex Casting pattern.
 */
object HexGuideCreativeTab {

    @Volatile
    private var cachedEntries: List<ActionInfo>? = null

    private class ActionInfo(
        val key: ResourceKey<ActionRegistryEntry>,
        val entry: ActionRegistryEntry,
        val id: ResourceLocation,
        val actionName: String
    )

    val TAB: CreativeModeTab = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
        .title(Component.translatable("tab.hexguide.patterns"))
        .icon { ItemStack(HexBlocks.SLATE.asItem()) }
        .displayItems { _, output ->
            val entries = getActionEntries()
            // All slates first
            for (info in entries) {
                val actionNameStr = Component.translatable(info.actionName).string
                val slate = ItemStack(HexItems.SLATE)
                writePatternToSlate(slate, info.entry.prototype())
                slate.setHoverName(Component.translatable("tab.hexguide.slate_name", actionNameStr))
                output.accept(slate)
            }
            // Then all large scrolls
            for (info in entries) {
                val actionNameStr = Component.translatable(info.actionName).string
                val scroll = ItemStack(HexItems.SCROLL_LARGE)
                writePatternToScroll(scroll, info.entry.prototype())
                scroll.setHoverName(Component.translatable("tab.hexguide.scroll_name", actionNameStr))
                output.accept(scroll)
            }
        }
        .build()

    fun register() {
        Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            HexGuide.id("patterns"),
            TAB
        )
    }

    private fun getActionEntries(): List<ActionInfo> {
        cachedEntries?.let { return it }
        val result = HexActions.REGISTRY.entrySet()
            .filter { (key, _) ->
                // Exclude per-world patterns
                !HexActions.REGISTRY.getHolder(key)
                    .map { it.`is`(HexTags.Actions.PER_WORLD_PATTERN) }
                    .orElse(false)
            }
            .map { (key, entry) ->
                ActionInfo(
                    key = key,
                    entry = entry,
                    id = key.location(),
                    actionName = "hexcasting.action.${key.location()}"
                )
            }
            .sortedBy { it.id.toString() }
        cachedEntries = result
        return result
    }

    private fun writePatternToSlate(stack: ItemStack, pattern: HexPattern) {
        try {
            HexItems.SLATE.writeDatum(stack, PatternIota(pattern))
        } catch (_: Exception) {
            // Silently skip patterns that can't be written
        }
    }

    private fun writePatternToScroll(stack: ItemStack, pattern: HexPattern) {
        try {
            HexItems.SCROLL_LARGE.writeDatum(stack, PatternIota(pattern))
        } catch (_: Exception) {
            // Silently skip patterns that can't be written
        }
    }
}
