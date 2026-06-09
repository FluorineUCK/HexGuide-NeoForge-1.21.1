@file:JvmName("HexGuideAbstractionsImpl")

package cn.xm1221.HexGuide.forge

import cn.xm1221.HexGuide.registry.HexGuideRegistrar
import net.minecraftforge.registries.RegisterEvent
import thedarkcolour.kotlinforforge.forge.MOD_BUS

fun <T : Any> initRegistry(registrar: HexGuideRegistrar<T>) {
    MOD_BUS.addListener { event: RegisterEvent ->
        event.register(registrar.registryKey) { helper ->
            registrar.init(helper::register)
        }
    }
}
