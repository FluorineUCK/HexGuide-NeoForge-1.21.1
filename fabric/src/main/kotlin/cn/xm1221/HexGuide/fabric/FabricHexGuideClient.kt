package cn.xm1221.HexGuide.fabric

import cn.xm1221.HexGuide.HexGuideClient
import cn.xm1221.HexGuide.scrying.HexGuideKeybinds
import cn.xm1221.HexGuide.scrying.ScryingBookOverlay
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback

object FabricHexGuideClient : ClientModInitializer {
    override fun onInitializeClient() {
        HexGuideClient.init()

        // Register keybinding
        KeyBindingHelper.registerKeyBinding(HexGuideKeybinds.OPEN_HEXBOOK)

        // Register HUD overlay for wall scroll entities and keybinding check
        HudRenderCallback.EVENT.register { graphics, tickDelta ->
            ScryingBookOverlay.onHudRender(graphics, tickDelta)
        }
    }
}
