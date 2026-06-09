@file:JvmName("HexGuideAbstractionsImpl")

package cn.xm1221.HexGuide.fabric

import cn.xm1221.HexGuide.registry.HexGuideRegistrar
import net.minecraft.core.Registry

fun <T : Any> initRegistry(registrar: HexGuideRegistrar<T>) {
    val registry = registrar.registry
    registrar.init { id, value -> Registry.register(registry, id, value) }
}
