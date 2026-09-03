package cn.xm1221.HexGuide.client

import net.minecraft.world.entity.player.Player
import net.minecraft.resources.ResourceLocation

/** Common-side callback installed only by the NeoForge client entrypoint. */
object HexGuideClientBridge {
    @Volatile
    private var noteEditorOpener: ((Player) -> Unit)? = null

    @Volatile
    private var resourceTextLoader: ((ResourceLocation) -> String?)? = null

    fun installNoteEditorOpener(opener: (Player) -> Unit) {
        noteEditorOpener = opener
    }

    fun openNoteEditor(player: Player) {
        noteEditorOpener?.invoke(player)
    }

    fun installResourceTextLoader(loader: (ResourceLocation) -> String?) {
        resourceTextLoader = loader
    }

    @JvmStatic
    fun loadResourceText(id: ResourceLocation): String? = resourceTextLoader?.invoke(id)
}
