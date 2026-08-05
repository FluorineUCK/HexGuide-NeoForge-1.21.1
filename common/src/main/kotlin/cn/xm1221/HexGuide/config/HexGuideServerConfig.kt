package cn.xm1221.HexGuide.config

import dev.architectury.event.events.common.PlayerEvent
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
import net.minecraft.network.FriendlyByteBuf
import cn.xm1221.HexGuide.HexGuide
import cn.xm1221.HexGuide.networking.msg.MsgSyncConfigS2C

object HexGuideServerConfig {
    @JvmStatic
    lateinit var holder: ConfigHolder<GlobalConfig>

    @JvmStatic
    val config get() = syncedServerConfig ?: holder.config.server

    // only used on the client
    private var syncedServerConfig: ServerConfig? = null

    fun init() {
        holder = AutoConfig.register(
            GlobalConfig::class.java,
            PartitioningSerializer.wrap(::Toml4jConfigSerializer),
        )
        // Prevent server config from auto-saving (it syncs from client config gui)
        ConfigHelper.registerPreventSave(holder)
    }

    fun initServer() {
        PlayerEvent.PLAYER_JOIN.register { player ->
            MsgSyncConfigS2C(holder.config.server).sendToPlayer(player)
        }
    }

    fun onSyncConfig(serverConfig: ServerConfig?) {
        syncedServerConfig = serverConfig
    }

    @Config(name = HexGuide.MODID)
    class GlobalConfig(
        @Category("server")
        @TransitiveObject
        val server: ServerConfig = ServerConfig(),
    ) : GlobalData()

    @Config(name = "server")
    class ServerConfig : ConfigData {
        @Tooltip
        var dummyServerConfigOption: Int = 64
            private set

        /** 互联 tag 修复（Fabric 路径 → hexcasting:action 注册表），默认开启 */
        @Tooltip
        var fixTags: Boolean = true

        fun encode(buf: FriendlyByteBuf) {
            buf.writeInt(dummyServerConfigOption)
            buf.writeBoolean(fixTags)
        }

        fun decode(buf: FriendlyByteBuf): ServerConfig {
            dummyServerConfigOption = buf.readInt()
            fixTags = buf.readBoolean()
            return this
        }
    }
}
