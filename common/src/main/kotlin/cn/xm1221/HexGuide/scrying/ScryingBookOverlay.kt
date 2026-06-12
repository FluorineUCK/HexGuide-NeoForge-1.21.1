package cn.xm1221.HexGuide.scrying

import at.petrak.hexcasting.api.client.ScryingLensOverlayRegistry
import at.petrak.hexcasting.common.blocks.circles.BlockEntitySlate
import at.petrak.hexcasting.common.blocks.circles.BlockSlate
import at.petrak.hexcasting.common.entities.EntityWallScroll
import at.petrak.hexcasting.common.lib.HexAttributes
import at.petrak.hexcasting.common.lib.HexBlocks
import at.petrak.hexcasting.common.lib.HexItems
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.datafixers.util.Pair
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.player.LocalPlayer
import net.minecraft.locale.Language
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.network.chat.Style
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult

/**
 * HUD overlay that:
 * 1. Registers slate overlay via ScryingLensOverlayRegistry (block display)
 * 2. Renders EntityWallScroll pattern info via HUD callback
 * 3. Tracks the current looked-at pattern for the hexbook keybind
 */
object ScryingBookOverlay {

    /** Tracks current lookup for the keybinding. */
    private var currentLookupResult: PatternBookLookup.LookupResult? = null

    /** Register the slate overlay with HexMod's scrying system. */
    fun registerSlateOverlay() {
        try {
            ScryingLensOverlayRegistry.addDisplayer(HexBlocks.SLATE) { lines, _, pos, _, world, _ ->
                val tile = world.getBlockEntity(pos)
                if (tile is BlockEntitySlate) {
                    val pattern = tile.pattern ?: return@addDisplayer
                    val lookupResult = PatternBookLookup.lookup(pattern)
                    // Line 1: pattern name
                    if (lookupResult != null && lookupResult.actionName != null) {
                        lines.add(Pair.of(
                            ItemStack(HexBlocks.SLATE.asItem()),
                            Component.translatable(lookupResult.actionName)
                        ))
                    } else {
                        lines.add(Pair.of(
                            ItemStack(HexBlocks.SLATE.asItem()),
                            Component.translatable("block.hexcasting.slate.written")
                        ))
                    }
                    // Line 2: hint to press key
                    if (lookupResult != null && lookupResult.found()) {
                        lines.add(Pair.of(
                            ItemStack.EMPTY,
                            Component.translatable(
                                "hexguide.scrying.open_book",
                                HexGuideKeybinds.OPEN_HEXBOOK.translatedKeyMessage
                            )
                        ))
                    }
                }
            }
        } catch (e: IllegalArgumentException) {
            // Slate already registered (e.g. by another mod) — skip silently
        }
    }

    /**
     * Called every frame from the HUD render event.
     * Handles entity wall scroll overlay and keybinding check.
     */
    fun onHudRender(graphics: GuiGraphics, partialTicks: Float) {
        try {
            val mc = Minecraft.getInstance()
            val player: LocalPlayer = mc.player ?: return
            val level = mc.level ?: return

            // Must have scrying sight
            if (player.getAttributeValue(HexAttributes.SCRY_SIGHT) <= 0.0) return
            if (player.getAttributeValue(HexAttributes.FEEBLE_MIND) > 0.0) return

            val hitRes = mc.hitResult ?: return

            if (hitRes.type == HitResult.Type.ENTITY) {
                val ehr = hitRes as EntityHitResult
                val entity = ehr.entity
                if (entity is EntityWallScroll) {
                    val pattern = entity.pattern
                    if (pattern != null) {
                        val lookup = PatternBookLookup.lookup(pattern)
                        currentLookupResult = lookup
                        renderWallScrollOverlay(graphics, mc, lookup)
                    } else {
                        // Wall scroll without pattern — clear tracking
                        currentLookupResult = null
                    }
                } else {
                    // Non-wall-scroll entity — clear tracking
                    currentLookupResult = null
                }
            } else if (hitRes.type == HitResult.Type.BLOCK) {
                val bhr = hitRes as BlockHitResult
                val bs = level.getBlockState(bhr.blockPos)
                // Track pattern for slate blocks (so keybinding works)
                if (bs.block is BlockSlate) {
                    val tile = level.getBlockEntity(bhr.blockPos)
                    if (tile is BlockEntitySlate) {
                        val pattern = tile.pattern
                        if (pattern != null) {
                            currentLookupResult = PatternBookLookup.lookup(pattern)
                        } else {
                            currentLookupResult = null
                        }
                    } else {
                        currentLookupResult = null
                    }
                } else {
                    currentLookupResult = null
                }
            } else {
                currentLookupResult = null
            }

            // Keybinding check
            if (HexGuideKeybinds.OPEN_HEXBOOK.isDown) {
                val result = currentLookupResult
                if (result != null && result.found()) {
                    PatternBookLookup.openBook(result)
                }
            }
        } catch (_: Exception) {
            // Silently ignore rendering errors to avoid breaking the HUD
        }
    }

    /** Render the wall scroll pattern overlay on the HUD. */
    private fun renderWallScrollOverlay(
        graphics: GuiGraphics,
        mc: Minecraft,
        lookup: PatternBookLookup.LookupResult?
    ) {
        val lines = mutableListOf<Pair<ItemStack, Component>>()
        if (lookup != null && lookup.actionName != null) {
            lines.add(Pair.of(
                ItemStack(HexItems.SCROLL_LARGE.asItem()),
                Component.translatable(lookup.actionName)
            ))
        } else {
            lines.add(Pair.of(
                ItemStack(HexItems.SCROLL_LARGE.asItem()),
                Component.translatable("entity.hexcasting.wall_scroll")
            ))
        }
        if (lookup != null && lookup.found()) {
            lines.add(Pair.of(
                ItemStack.EMPTY,
                Component.translatable(
                    "hexguide.scrying.open_book",
                    HexGuideKeybinds.OPEN_HEXBOOK.translatedKeyMessage
                )
            ))
        }
        renderLines(graphics, mc, lines)
    }

    /** Render a list of (icon, text) pairs as a HUD overlay. */
    private fun renderLines(
        graphics: GuiGraphics,
        mc: Minecraft,
        lines: List<Pair<ItemStack, Component>>
    ) {
        if (lines.isEmpty()) return

        val ps: PoseStack = graphics.pose()
        var totalHeight = 8
        val window = mc.window
        val maxWidth = (window.guiScaledWidth / 2f * 0.8f).toInt()

        // Calculate height
        val actualLines = mutableListOf<Pair<ItemStack, List<FormattedText>>>()
        for (pair in lines) {
            totalHeight += mc.font.lineHeight + 6
            val text = pair.second
            val textLines = mc.font.splitter.splitLines(text, maxWidth, Style.EMPTY)
            actualLines.add(Pair.of(pair.first, textLines))
            if (textLines.size > 1) {
                totalHeight += mc.font.lineHeight * (textLines.size - 1)
            }
        }

        val x = window.guiScaledWidth / 2f + 8f
        val y = window.guiScaledHeight / 2f - totalHeight

        ps.pushPose()
        ps.translate(x.toDouble(), y.toDouble(), 0.0)

        for (pair in actualLines) {
            val stack = pair.first
            if (!stack.isEmpty) {
                graphics.renderItem(stack, 0, 0)
            }
            val tx = if (stack.isEmpty) 0 else 18
            val ty = 5

            for (line in pair.second) {
                val visualOrder = Language.getInstance().getVisualOrder(line)
                graphics.drawString(mc.font, visualOrder, tx, ty, 0xFFFFFFFF.toInt())
                ps.translate(0.0, mc.font.lineHeight.toDouble(), 0.0)
            }
            if (pair.second.isEmpty()) {
                ps.translate(0.0, mc.font.lineHeight.toDouble(), 0.0)
            }
            ps.translate(0.0, 6.0, 0.0)
        }

        ps.popPose()
    }
}
