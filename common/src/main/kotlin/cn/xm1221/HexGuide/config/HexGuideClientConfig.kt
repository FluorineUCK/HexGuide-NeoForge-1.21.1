package cn.xm1221.HexGuide.config

import dev.architectury.event.events.client.ClientPlayerEvent
import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.ConfigData
import me.shedaniel.autoconfig.ConfigHolder
import me.shedaniel.autoconfig.annotation.Config
import me.shedaniel.autoconfig.annotation.ConfigEntry.Category
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.Tooltip
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.TransitiveObject
import me.shedaniel.autoconfig.serializer.PartitioningSerializer
import me.shedaniel.autoconfig.serializer.PartitioningSerializer.GlobalData
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer
import cn.xm1221.HexGuide.HexGuide

object HexGuideClientConfig {
    @JvmStatic
    lateinit var holder: ConfigHolder<GlobalConfig>

    @JvmStatic
    val config get() = holder.config.client

    fun init() {
        holder = AutoConfig.register(
            GlobalConfig::class.java,
            PartitioningSerializer.wrap(::Toml4jConfigSerializer),
        )

        // when we change the server config in the client gui, also sync it to the server config holder
        ConfigHelper.registerAllowSave(holder) { config ->
            HexGuideServerConfig.holder.config = HexGuideServerConfig.GlobalConfig(config.server)
        }

        // when we leave a server, clear our local copy of its config
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register { _ ->
            HexGuideServerConfig.onSyncConfig(null)
        }
    }

    @Config(name = HexGuide.MODID)
    class GlobalConfig : GlobalData() {
        @Category("server")
        @TransitiveObject
        val client = ClientConfig()

        // this should only be used inside of this class; use HexGuideServerConfig.config to access the server-side config in other code
        @Category("server")
        @TransitiveObject
        val server = HexGuideServerConfig.ServerConfig()
    }

    @Config(name = "client")
    class ClientConfig : ConfigData {
        @Tooltip
        val dummyClientConfigOption: Boolean = true
    }
}
