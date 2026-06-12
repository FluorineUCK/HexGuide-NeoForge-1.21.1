package cn.xm1221.HexGuide.forge

import cn.xm1221.HexGuide.HexGuideClient
import cn.xm1221.HexGuide.scrying.HexGuideKeybinds
import cn.xm1221.HexGuide.scrying.ScryingBookOverlay
import net.minecraftforge.client.ConfigScreenHandler
import net.minecraftforge.client.event.RegisterKeyMappingsEvent
import net.minecraftforge.client.event.RenderGuiEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import thedarkcolour.kotlinforforge.forge.LOADING_CONTEXT

object ForgeHexGuideClient {
    @Suppress("UNUSED_PARAMETER")
    fun init(event: FMLClientSetupEvent) {
        HexGuideClient.init()

        LOADING_CONTEXT.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory::class.java) {
            ConfigScreenHandler.ConfigScreenFactory { _, parent -> HexGuideClient.getConfigScreen(parent) }
        }

        // Register Forge events
        val evBus = MinecraftForge.EVENT_BUS
        evBus.addListener { e: RegisterKeyMappingsEvent ->
            for (bind in HexGuideKeybinds.allBinds()) {
                e.register(bind)
            }
        }
        evBus.addListener { e: RenderGuiEvent.Post ->
            ScryingBookOverlay.onHudRender(e.guiGraphics, e.partialTick)
        }
    }
}
