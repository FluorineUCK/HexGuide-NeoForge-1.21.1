package cn.xm1221.HexGuide.forge.datagen

import at.petrak.hexcasting.forge.datagen.TagsProviderEFHSetter
import cn.xm1221.HexGuide.datagen.HexGuideActionTags
import net.minecraft.data.DataProvider
import net.minecraft.data.PackOutput
import net.minecraftforge.data.event.GatherDataEvent

object ForgeHexGuideDatagen {
    fun init(event: GatherDataEvent) {
        event.apply {
            // common datagen
            if (System.getProperty("hexguide.common-datagen") == "true") {
                // TODO: add datagen providers
            }

            // Forge-only datagen
            if (System.getProperty("hexguide.forge-datagen") == "true") {
                addVanillaProvider(includeServer()) { HexGuideActionTags(it, lookupProvider) }
            }
        }
    }
}

private fun <T : DataProvider> GatherDataEvent.addProvider(run: Boolean, factory: (PackOutput) -> T) =
    generator.addProvider(run, DataProvider.Factory { factory(it) })

private fun <T : DataProvider> GatherDataEvent.addVanillaProvider(run: Boolean, factory: (PackOutput) -> T) =
    addProvider(run) { packOutput ->
        factory(packOutput).also {
            (it as TagsProviderEFHSetter).setEFH(existingFileHelper)
        }
    }
