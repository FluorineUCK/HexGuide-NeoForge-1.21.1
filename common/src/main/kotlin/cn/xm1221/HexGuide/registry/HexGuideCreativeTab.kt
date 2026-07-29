package cn.xm1221.HexGuide.registry

import at.petrak.hexcasting.api.casting.ActionRegistryEntry
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.api.mod.HexTags
import at.petrak.hexcasting.common.lib.HexBlocks
import at.petrak.hexcasting.common.lib.HexItems
import at.petrak.hexcasting.common.lib.hex.HexActions
import cn.xm1221.HexGuide.HexGuide
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack

/**
 * 创造模式物品栏 —— 展示所有非 Per-World 图案的石板和大型卷轴。
 * 使用 HexGuideRegistrar 以保证 Forge 端通过 RegisterEvent 安全注册。
 */
object HexGuideCreativeTab : HexGuideRegistrar<CreativeModeTab>(
    Registries.CREATIVE_MODE_TAB,
    { net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB },
) {

    @Volatile
    private var cachedEntries: List<ActionInfo>? = null

    private class ActionInfo(
        val key: ResourceKey<ActionRegistryEntry>,
        val entry: ActionRegistryEntry,
        val id: ResourceLocation,
        val actionName: String
    )

    val PATTERNS = register("patterns") {
        CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
            .title(Component.translatable("tab.hexguide.patterns"))
            .icon { ItemStack(HexBlocks.SLATE.asItem()) }
            .displayItems { _, output ->
                val entries = getActionEntries()
                // 先加石板
                for (info in entries) {
                    val name = Component.translatable(info.actionName).string
                    val slate = ItemStack(HexItems.SLATE)
                    writePatternToSlate(slate, info.entry.prototype())
                    slate.setHoverName(Component.translatable("tab.hexguide.slate_name", name))
                    output.accept(slate)
                }
                // 再加卷轴
                for (info in entries) {
                    val name = Component.translatable(info.actionName).string
                    val scroll = ItemStack(HexItems.SCROLL_LARGE)
                    writePatternToScroll(scroll, info.entry.prototype())
                    scroll.setHoverName(Component.translatable("tab.hexguide.scroll_name", name))
                    output.accept(scroll)
                }
            }
            .build()
    }

    private fun getActionEntries(): List<ActionInfo> {
        cachedEntries?.let { return it }
        val result = HexActions.REGISTRY.entrySet()
            .filter { (key, _) ->
                val holder = HexActions.REGISTRY.getHolder(key)
                val isPerWorld = holder.map { it.`is`(HexTags.Actions.PER_WORLD_PATTERN) }.orElse(false)
                val isGreat = holder.map { it.`is`(HexTags.Actions.REQUIRES_ENLIGHTENMENT) }.orElse(false)
                !isPerWorld && !isGreat
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
        try { HexItems.SLATE.writeDatum(stack, PatternIota(pattern)) } catch (_: Exception) {}
    }

    private fun writePatternToScroll(stack: ItemStack, pattern: HexPattern) {
        try { HexItems.SCROLL_LARGE.writeDatum(stack, PatternIota(pattern)) } catch (_: Exception) {}
    }
}
