package cn.xm1221.HexGuide.fabric.datagen

import cn.xm1221.HexGuide.datagen.HexGuideActionTags
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator

object FabricHexGuideDatagen : DataGeneratorEntrypoint {
    override fun onInitializeDataGenerator(gen: FabricDataGenerator) {
        val pack = gen.createPack()

        pack.addProvider(::HexGuideActionTags)
    }
}
