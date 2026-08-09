package cn.xm1221.HexGuide.registry

import at.petrak.hexcasting.api.casting.ActionRegistryEntry
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.api.mod.HexTags
import at.petrak.hexcasting.common.lib.HexBlocks
import at.petrak.hexcasting.common.lib.HexItems
import at.petrak.hexcasting.common.lib.hex.HexActions
import cn.xm1221.HexGuide.HexGuide
import cn.xm1221.HexGuide.networking.HexGuideNetworking
import cn.xm1221.HexGuide.networking.msg.MsgRequestExcludedPatternsC2S
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

    /** 服务端返回的排除图案 id（卓越法术 / Per-World）；null = 尚未获取 */
    @Volatile
    private var excludedPatterns: Set<String>? = null

    /** 服务端 tag 数据完整，传回的排除列表 */
    fun setExcludedPatterns(ids: Set<String>) {
        excludedPatterns = ids
        cachedEntries = null // 触发重建
    }

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
                // 先加石板（按图案签名去重——注册表可能存在同图案的 action，重复 add 会崩 "same item stack twice"）
                val seenSlate = HashSet<String>()
                for (info in entries) {
                    if (!seenSlate.add(info.entry.prototype().anglesSignature())) continue
                    val name = Component.translatable(info.actionName).string
                    val slate = ItemStack(HexItems.SLATE)
                    writePatternToSlate(slate, info.entry.prototype())
                    slate.setHoverName(Component.translatable("tab.hexguide.slate_name", name))
                    output.accept(slate)
                }
                // 再加卷轴（同样去重）
                val seenScroll = HashSet<String>()
                for (info in entries) {
                    if (!seenScroll.add(info.entry.prototype().anglesSignature())) continue
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
        val excluded = excludedPatterns
        if (excluded == null) {
            // 服务端列表未到：惰性请求；暂时按客户端 holder.is（Forge 客户端可能查不到 tag，先放行）
            HexGuideNetworking.CHANNEL.sendToServer(MsgRequestExcludedPatternsC2S())
        }
        val result = HexActions.REGISTRY.entrySet()
            .filter { (key, _) ->
                if (excluded != null) {
                    !excluded.contains(key.location().toString())
                } else {
                    // 不用 getHolder(key).is(tag)：会在注册表里创建 STAND_ALONE holder，
                    // 而 bindTags 跳过 STAND_ALONE → holder 永远无 tag，还会污染后续查询。
                    // 改用 registry.getTag(tagKey)（不创建 holder）。
                    !inTag(key, HexTags.Actions.PER_WORLD_PATTERN) &&
                        !inTag(key, HexTags.Actions.REQUIRES_ENLIGHTENMENT)
                }
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

    /** 查注册表 tag 集合（不创建 holder，不污染注册表） */
    private fun inTag(
        key: ResourceKey<ActionRegistryEntry>,
        tagKey: net.minecraft.tags.TagKey<ActionRegistryEntry>
    ): Boolean {
        val tag = HexActions.REGISTRY.getTag(tagKey)
        if (tag.isEmpty) return false
        return tag.get().stream().anyMatch { it.`is`(key) }
    }

    private fun writePatternToSlate(stack: ItemStack, pattern: HexPattern) {
        try { HexItems.SLATE.writeDatum(stack, PatternIota(pattern)) } catch (_: Exception) {}
    }

    private fun writePatternToScroll(stack: ItemStack, pattern: HexPattern) {
        try { HexItems.SCROLL_LARGE.writeDatum(stack, PatternIota(pattern)) } catch (_: Exception) {}
    }
}
