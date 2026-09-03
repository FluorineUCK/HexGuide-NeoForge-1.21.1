package cn.xm1221.HexGuide.registry

import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes
import cn.xm1221.HexGuide.api.notes.NoteIota
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey

/** 注册 HexGuide 自定义 Iota 类型 */
@Suppress("UNCHECKED_CAST")
object HexGuideIotaTypes : HexGuideRegistrar<IotaType<*>>(
    HexIotaTypes.REGISTRY.key() as ResourceKey<Registry<IotaType<*>>>,
    { HexIotaTypes.REGISTRY },
) {
    val NOTE: Entry<IotaType<NoteIota>> = register("note") { NoteIota.TYPE }
}
