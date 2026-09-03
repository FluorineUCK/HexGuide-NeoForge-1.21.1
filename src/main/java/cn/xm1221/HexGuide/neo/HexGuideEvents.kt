package cn.xm1221.HexGuide.neo

import cn.xm1221.HexGuide.api.notes.PlayerNotes
import cn.xm1221.HexGuide.networking.handler.syncNotes
import cn.xm1221.HexGuide.networking.msg.MsgExcludedPatternsS2C
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.server.ServerStartedEvent
import at.petrak.hexcasting.api.mod.HexTags
import at.petrak.hexcasting.common.lib.hex.HexActions

object HexGuideEvents {
    fun register() {
        NeoForge.EVENT_BUS.addListener(::onPlayerLogin)
        NeoForge.EVENT_BUS.addListener(::onServerStarted)
    }

    private fun onServerStarted(event: ServerStartedEvent) {
        HexGuideProbe.run(event.server)
    }

    private fun onPlayerLogin(event: PlayerEvent.PlayerLoggedInEvent) {
        val player = event.entity as? net.minecraft.server.level.ServerPlayer ?: return
        val great = HexActions.REGISTRY.getTag(HexTags.Actions.REQUIRES_ENLIGHTENMENT)
        val perWorld = HexActions.REGISTRY.getTag(HexTags.Actions.PER_WORLD_PATTERN)
        val excluded = HexActions.REGISTRY.entrySet().asSequence()
            .filter { (key, _) ->
                (great.isPresent && great.get().stream().anyMatch { it.`is`(key) }) ||
                    (perWorld.isPresent && perWorld.get().stream().anyMatch { it.`is`(key) })
            }
            .map { (key, _) -> key.location().toString() }
            .toList()
        MsgExcludedPatternsS2C(excluded).sendToPlayer(player)
        syncNotes(player, PlayerNotes.get(player.serverLevel()))
    }
}
