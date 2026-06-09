@file:JvmName("HexGuideAbstractions")

package cn.xm1221.HexGuide

import dev.architectury.injectables.annotations.ExpectPlatform
import cn.xm1221.HexGuide.registry.HexGuideRegistrar

fun initRegistries(vararg registries: HexGuideRegistrar<*>) {
    for (registry in registries) {
        initRegistry(registry)
    }
}

@ExpectPlatform
fun <T : Any> initRegistry(registrar: HexGuideRegistrar<T>) {
    throw AssertionError()
}
