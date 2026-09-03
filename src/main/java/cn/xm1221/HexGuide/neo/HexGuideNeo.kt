package cn.xm1221.HexGuide.neo

import cn.xm1221.HexGuide.HexGuide
import cn.xm1221.HexGuide.commands.HexGuideCommands
import cn.xm1221.HexGuide.compat.inline.InlineHexGuide
import cn.xm1221.HexGuide.networking.HexGuideNetworking
import cn.xm1221.HexGuide.registry.HexGuideActions
import cn.xm1221.HexGuide.registry.HexGuideCreativeTab
import cn.xm1221.HexGuide.registry.HexGuideIotaTypes
import cn.xm1221.HexGuide.registry.HexGuideItems
import cn.xm1221.HexGuide.registry.HexGuideRegistrar
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.registries.RegisterEvent
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import net.minecraft.world.item.CreativeModeTabs

@Mod(HexGuide.MODID)
class HexGuideNeo(modBus: IEventBus) {
    init {
        INSTANCE = this
        BUS = modBus
        modBus.addListener(HexGuideNetworking::register)
        modBus.addListener(::addVanillaCreativeItems)
        registerRegistrar(HexGuideActions)
        registerRegistrar(HexGuideCreativeTab)
        registerRegistrar(HexGuideIotaTypes)
        registerRegistrar(HexGuideItems)
        InlineHexGuide.init()
        HexGuideEvents.register()
        HexGuideCommands.register()
    }

    private fun addVanillaCreativeItems(event: BuildCreativeModeTabContentsEvent) {
        when (event.tabKey) {
            CreativeModeTabs.TOOLS_AND_UTILITIES -> event.accept(cn.xm1221.HexGuide.registry.HexGuideItems.AMETHYST_PEN.value)
            CreativeModeTabs.INGREDIENTS -> event.accept(cn.xm1221.HexGuide.registry.HexGuideItems.NOTE_SCRAP.value)
        }
    }

    companion object {
        private lateinit var INSTANCE: HexGuideNeo
        private lateinit var BUS: IEventBus

        @JvmStatic
        fun <T : Any> registerRegistrar(registrar: HexGuideRegistrar<T>) {
            BUS.addListener { event: RegisterEvent ->
                if (event.registryKey == registrar.registryKey) {
                    event.register(registrar.registryKey) { helper ->
                        registrar.init { id: ResourceLocation, value: T -> helper.register(id, value) }
                    }
                }
            }
        }
    }
}
