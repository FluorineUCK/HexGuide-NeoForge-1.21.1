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
    // 注意：此前注册过 congratulate/great（卓越法术）并 datagen 生成了
    // data/hexcasting/tags/*/requires_enlightenment.json 等 tag 文件，残留文件会污染 HexMod 的 tag。
    // 该 action 已弃用，tag 残留文件也已在 fabric/forge 的 src/generated/resources 中删除。
    val COPY = make("copy",HexDir.NORTH_EAST,"dadade", OpTextCopy())

    private fun make(name: String, startDir: HexDir, signature: String, action: Action) =
        make(name, startDir, signature) { action }

    private fun make(name: String, startDir: HexDir, signature: String, getAction: () -> Action) = register(name) {
        ActionRegistryEntry(HexPattern.fromAngles(signature, startDir), getAction())
    }
}
