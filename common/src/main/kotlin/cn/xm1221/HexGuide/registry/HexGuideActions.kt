package cn.xm1221.HexGuide.registry

import at.petrak.hexcasting.api.casting.ActionRegistryEntry
import at.petrak.hexcasting.api.casting.castables.Action
import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.common.lib.HexRegistries
import at.petrak.hexcasting.common.lib.hex.HexActions
import cn.xm1221.HexGuide.casting.actions.OpTextCopy


object HexGuideActions : HexGuideRegistrar<ActionRegistryEntry>(
    HexRegistries.ACTION,
    { HexActions.REGISTRY },
) {
    //val CONGRATULATE = make("congratulate", HexDir.WEST, "eed", OpCongratulate)

    //val GREAT_CONGRATULATE = make("congratulate/great", HexDir.EAST, "qwwqqqwwqwded", OpCongratulate)
    val COPY = make("copy",HexDir.NORTH_EAST,"dadade", OpTextCopy())

    private fun make(name: String, startDir: HexDir, signature: String, action: Action) =
        make(name, startDir, signature) { action }

    private fun make(name: String, startDir: HexDir, signature: String, getAction: () -> Action) = register(name) {
        ActionRegistryEntry(HexPattern.fromAngles(signature, startDir), getAction())
    }
}
