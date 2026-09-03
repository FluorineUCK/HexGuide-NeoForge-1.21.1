package cn.xm1221.HexGuide.neo

import cn.xm1221.HexGuide.HexGuide
import cn.xm1221.HexGuide.HexGuideClient
import cn.xm1221.HexGuide.client.HexGuideClientBridge
import cn.xm1221.HexGuide.client.screen.NoteEditorScreen
import cn.xm1221.HexGuide.networking.msg.MsgNotesSaveC2S
import cn.xm1221.HexGuide.networking.HexGuideNetworking
import cn.xm1221.HexGuide.networking.handler.applyOnClient
import cn.xm1221.HexGuide.scrying.HexGuideKeybinds
import cn.xm1221.HexGuide.scrying.PatternBookLookup
import cn.xm1221.HexGuide.scrying.ScryingBookOverlay
import cn.xm1221.HexGuide.registry.HexGuideItems
import cn.xm1221.HexGuide.patchouli.BookSpellcastingAccess
import cn.xm1221.HexGuide.patchouli.EmbeddedSpellResultAccess
import at.petrak.hexcasting.client.gui.GuiSpellcasting
import at.petrak.hexcasting.common.msgs.MsgNewSpellPatternS2C
import com.samsthenerd.inline.api.client.InlineClientAPI
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.ItemStack
import at.petrak.hexcasting.api.casting.iota.PatternIota
import at.petrak.hexcasting.api.item.IotaHolderItem
import vazkii.patchouli.common.book.BookRegistry
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModList
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
import net.neoforged.neoforge.client.event.RenderGuiEvent
import net.neoforged.neoforge.common.NeoForge

@EventBusSubscriber(modid = HexGuide.MODID, value = [Dist.CLIENT])
object HexGuideNeoClient {
    private var initialized = false
    private var probeTicks = 0
    private var probeFinished = false
    private var exclusionProbeSerial = -1
    private var exclusionProbeRequestTick = -1

    @JvmStatic
    @SubscribeEvent
    fun onClientSetup(event: FMLClientSetupEvent) {
        event.enqueueWork {
            if (!initialized) {
                initialized = true
                HexGuideClient.init()
                HexGuideClientBridge.installNoteEditorOpener { player ->
                    val minecraft = net.minecraft.client.Minecraft.getInstance()
                    minecraft.setScreen(NoteEditorScreen(
                        playerName = player.gameProfile.name,
                        onSave = { title, pages -> MsgNotesSaveC2S(title, pages).sendToServer() }
                    ))
                }
                HexGuideClientBridge.installResourceTextLoader { id ->
                    Minecraft.getInstance().resourceManager.getResource(id).orElse(null)?.openAsReader()?.use {
                        reader -> reader.readText()
                    }
                }
                HexGuideNetworking.installClientHandler { payload, context -> payload.applyOnClient(context) }
                NeoForge.EVENT_BUS.addListener(::onRenderGui)
                NeoForge.EVENT_BUS.addListener(::onClientTick)
            }
        }
    }

    @JvmStatic
    @SubscribeEvent
    fun registerKeyMappings(event: RegisterKeyMappingsEvent) {
        HexGuideKeybinds.allBinds().forEach(event::register)
    }

    private fun onRenderGui(event: RenderGuiEvent.Post) {
        ScryingBookOverlay.onHudRender(
            event.guiGraphics,
            event.partialTick.getGameTimeDeltaPartialTick(true)
        )
    }

    private fun onClientTick(event: net.neoforged.neoforge.client.event.ClientTickEvent.Post) {
        if (!java.lang.Boolean.getBoolean("hexguide.probe.validateClient") || probeFinished) return
        probeTicks++
        val client = Minecraft.getInstance()
        val worldProbe = java.lang.Boolean.getBoolean("hexguide.probe.validate")
        if (worldProbe && exclusionProbeSerial < 0 && client.level != null && client.player != null && client.connection != null) {
            exclusionProbeSerial = cn.xm1221.HexGuide.registry.HexGuideCreativeTab.exclusionSyncSerial()
            exclusionProbeRequestTick = probeTicks
            cn.xm1221.HexGuide.registry.HexGuideCreativeTab.requestExcludedPatterns()
        }
        if (probeTicks < 120 || client.resourceManager == null) return
        // Patchouli's full BookContents and dynamic creative-tab parameters are
        // connection/world state. Never turn a title-screen run into a false
        // negative, but require that state for the client-world validation.
        if (worldProbe && (client.level == null || client.player == null || client.connection == null)) return
        if (worldProbe && (exclusionProbeRequestTick < 0 || probeTicks < exclusionProbeRequestTick + 20)) return
        probeFinished = true
        val failures = mutableListOf<String>()
        var inlineResourceCount = 0
        var inlineDecodedCount = 0

        fun check(label: String, body: () -> Unit) {
            try {
                body()
                HexGuide.LOGGER.info("[HEXGUIDE-CLIENT-PROBE] {}=PASS", label)
            } catch (failure: Throwable) {
                failures += label
                HexGuide.LOGGER.error("[HEXGUIDE-CLIENT-PROBE] {}=FAIL", label, failure)
            }
        }

        check("translations") {
            val keys = listOf(
                "item.hexguide.amethyst_pen",
                "item.hexguide.note_scrap",
                "tab.hexguide.patterns",
                "tab.hexguide.slate_name",
                "tab.hexguide.scroll_name",
                "key.hexguide.open_hexbook",
                "category.hexguide.scrying",
                "hexguide.notes.need_paper",
                "hexguide.notes.need_more_paper",
                "hexguide.notes.unnamed",
                "hexguide.notes.empty",
                "hexguide.notes.empty_section",
                "hexguide.notes.more",
                "hexguide.notes.editor_title",
                "hexguide.notes.title",
                "hexguide.notes.prev_page",
                "hexguide.notes.next_page",
                "hexguide.notes.save",
                "hexguide.notes.cancel",
                "hexguide.notes.page_indicator",
                "hexguide.copy.hover",
                "hexguide.copy.saved",
                "hexguide.search.placeholder",
                "hexcasting.action.hexguide:copy",
                "hexcasting.action.hexguide:demo",
                "hexcasting.action.hexguide:note/import",
                "hexcasting.action.hexguide:note/list",
                "hexcasting.action.hexguide:note/get",
                "hexcasting.action.hexguide:note/delete",
            )
            require(keys.all(I18n::exists))
        }

        check("models") {
            val client = Minecraft.getInstance()
            val missing = client.modelManager.missingModel
            for (item in listOf(HexGuideItems.AMETHYST_PEN.value, HexGuideItems.NOTE_SCRAP.value)) {
                require(client.itemRenderer.getModel(
                    net.minecraft.world.item.ItemStack(item), client.level, client.player, 0
                ) !== missing)
            }
            val resources = listOf(
                HexGuide.id("textures/item/amethyst_pen.png"),
                HexGuide.id("textures/item/note_scrap.png"),
                HexGuide.id("textures/gui/note_editor.png"),
                HexGuide.id("textures/gui/note_buttons.png"),
            )
            require(resources.all { client.resourceManager.getResource(it).isPresent })
        }

        check("patchouli_pages") {
            val pageTypes = vazkii.patchouli.client.book.ClientBookRegistry.INSTANCE.pageTypes
            val expected = listOf("component_text", "spellcasting", "spellcast_demo", "note_page", "note_index")
            require(expected.all { pageTypes.containsKey(HexGuide.id(it)) })
        }

        check("patchouli_book") {
            val bookId = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hexcasting", "thehexbook")
            val book = BookRegistry.INSTANCE.books[bookId]
                ?: error("Patchouli book not loaded: $bookId")
            if (worldProbe) {
                val contents = book.contents ?: error("Patchouli contents are null: $bookId")
                require(!contents.isErrored) {
                    "Patchouli failed to build $bookId: ${contents.exception}"
                }
                val expectedCategories = listOf("guide", "notes", "guide/mods", "guide/spells")
                    .map { net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hexcasting", it) }
                val missingCategories = expectedCategories.filterNot(contents.categories::containsKey)
                require(missingCategories.isEmpty()) {
                    "Missing HexGuide Patchouli categories: $missingCategories; available=${contents.categories.keys}"
                }
                val expectedEntries = mutableListOf(
                    "basics/great",
                    "casting/hexguide",
                    "guide/actions",
                    "guide/continuation",
                    "guide/escape",
                    "guide/foreach",
                    "guide/hexguide_patterns",
                    "guide/iotas",
                    "guide/logics",
                    "guide/meta",
                    "guide/search",
                    "guide/stack",
                    "guide/tips",
                    "guide/mods/hexal",
                    "guide/mods/moreiotas",
                    "guide/spells/arrow",
                    "guide/spells/blocks",
                    "guide/spells/boom",
                    "guide/spells/lights",
                    "guide/spells/media",
                    "notes/items",
                    "notes/notes",
                    "notes/notes_index",
                )
                if (ModList.get().isLoaded("hexcassettes")) {
                    expectedEntries += "guide/mods/hexcassettes"
                }
                val expectedEntryIds = expectedEntries.map {
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hexcasting", it)
                }
                val missing = expectedEntryIds.filterNot(contents.entries::containsKey)
                require(missing.isEmpty()) {
                    "Missing HexGuide Patchouli entries: $missing; available=${contents.entries.keys}"
                }
                val empty = expectedEntryIds.filter { contents.entries[it]?.pages?.isEmpty() != false }
                require(empty.isEmpty()) { "HexGuide Patchouli entries without pages: $empty" }
                for (action in listOf("copy", "demo", "note/import", "note/list", "note/get", "note/delete")) {
                    val lookup = PatternBookLookup.lookUpIdPage(HexGuide.id(action))
                    require(lookup.first.isPresent && lookup.second.isPresent) {
                        "No HexBook page for hexguide:$action"
                    }
                }
            }
        }

        check("keybind") {
            require(client.options.keyMappings.any { it === HexGuideKeybinds.OPEN_HEXBOOK })
        }

        check("mixins") {
            require(BookSpellcastingAccess::class.java.isAssignableFrom(GuiSpellcasting::class.java))
            require(EmbeddedSpellResultAccess::class.java.isAssignableFrom(MsgNewSpellPatternS2C::class.java))
        }

        check("inline") {
            require(InlineClientAPI.INSTANCE.getMatcher(HexGuide.id("iota")) != null)
            require(InlineClientAPI.INSTANCE.getRenderer(HexGuide.id("iota")) != null)

            // Upstream HexGuide ships 36 historical Hex 0.11 Iota resources.
            // A representative sample missed regressions in nested lists and
            // continuations, so validate every bundled file through the exact
            // Inline resource-loading path used by Patchouli pages.
            val bundled = client.resourceManager.listResources("iotas") { id ->
                id.namespace == HexGuide.MODID && id.path.endsWith(".json")
            }.keys.sortedBy { it.toString() }
            inlineResourceCount = bundled.size
            require(inlineResourceCount == 36) {
                "Expected 36 bundled HexGuide iota resources, found $inlineResourceCount: $bundled"
            }
            val moreIotasLoaded = ModList.get().isLoaded("moreiotas")
            for (id in bundled) {
                val name = id.path.removePrefix("iotas/")
                if (name == "c9412e.json" && !moreIotasLoaded) {
                    // This single upstream fixture is deliberately typed as a
                    // MoreIotas EntityTypeIota. HexGuide does not require that
                    // optional addon, so absence must not break standalone use.
                    continue
                }
                val resource = cn.xm1221.HexGuide.compat.inline.IotaInlineData.parse(name)
                    ?: error("Bundled iota resource $name did not parse")
                require(resource.getOrDeserialize() != null) {
                    "Bundled iota resource $name decoded to null"
                }
                inlineDecodedCount++
            }
            require(inlineDecodedCount == if (moreIotasLoaded) 36 else 35) {
                "Decoded $inlineDecodedCount bundled iotas; moreiotas=$moreIotasLoaded"
            }

            // Preserve explicit type checks for every legacy decoder branch.
            val expected = mapOf(
                "null.json" to at.petrak.hexcasting.api.casting.iota.NullIota::class.java,
                "double0.json" to at.petrak.hexcasting.api.casting.iota.DoubleIota::class.java,
                "bool0.json" to at.petrak.hexcasting.api.casting.iota.BooleanIota::class.java,
                "vec0.json" to at.petrak.hexcasting.api.casting.iota.Vec3Iota::class.java,
                "garbage.json" to at.petrak.hexcasting.api.casting.iota.GarbageIota::class.java,
                "list2.json" to at.petrak.hexcasting.api.casting.iota.ListIota::class.java,
                "jump.json" to at.petrak.hexcasting.api.casting.iota.ContinuationIota::class.java,
            )
            for ((name, type) in expected) {
                val resource = cn.xm1221.HexGuide.compat.inline.IotaInlineData.parse(name)
                    ?: error("Bundled iota resource $name did not parse")
                require(type.isInstance(resource.getOrDeserialize())) {
                    "Bundled iota resource $name decoded as ${resource.getOrDeserialize()?.javaClass?.name}"
                }
            }
            if (moreIotasLoaded) {
                val resource = cn.xm1221.HexGuide.compat.inline.IotaInlineData.parse("c9412e.json")
                    ?: error("Bundled MoreIotas resource c9412e.json did not parse")
                require(resource.getOrDeserialize()?.javaClass?.name ==
                    "ram.talia.moreiotas.api.casting.iota.EntityTypeIota")
            }
        }

        check("network_roundtrip") {
            if (worldProbe) {
                require(exclusionProbeSerial >= 0) { "C2S exclusion request was not sent" }
                require(cn.xm1221.HexGuide.registry.HexGuideCreativeTab.exclusionSyncSerial() > exclusionProbeSerial) {
                    "No S2C exclusion response after explicit C2S request"
                }
            }
        }

        check("creative") {
            val tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(HexGuide.id("patterns"))
                ?: error("Missing HexGuide pattern tab")
            val connection = client.connection
            val holders = connection?.registryAccess() ?: client.level?.registryAccess()
            if (worldProbe) {
                require(holders != null) { "Missing client registry access in world probe" }
                val features = connection?.enabledFeatures() ?: FeatureFlags.DEFAULT_FLAGS
                tab.buildContents(CreativeModeTab.ItemDisplayParameters(features, true, holders))
                require(tab.displayItems.isNotEmpty()) { "HexGuide pattern tab has no generated stacks" }
                require(tab.displayItems.size % 2 == 0)
                val slateItem = at.petrak.hexcasting.common.lib.HexItems.SLATE.get()
                val scrollItem = at.petrak.hexcasting.common.lib.HexItems.SCROLL_LARGE.get()
                val slateStacks = tab.displayItems.filter { it.`is`(slateItem) }
                val scrollStacks = tab.displayItems.filter { it.`is`(scrollItem) }
                require(slateStacks.isNotEmpty() && slateStacks.size == scrollStacks.size)
                require((slateItem as IotaHolderItem).readIota(slateStacks.first()) is PatternIota)
                require((scrollItem as IotaHolderItem).readIota(scrollStacks.first()) is PatternIota)
            }
            require(BuiltInRegistries.ITEM.containsKey(HexGuide.id("amethyst_pen")))
            require(BuiltInRegistries.ITEM.containsKey(HexGuide.id("note_scrap")))
            val pen = ItemStack(HexGuideItems.AMETHYST_PEN.value)
            val scrap = ItemStack(HexGuideItems.NOTE_SCRAP.value)
            val tools = BuiltInRegistries.CREATIVE_MODE_TAB.get(CreativeModeTabs.TOOLS_AND_UTILITIES)
                ?: error("Missing vanilla tools tab")
            val ingredients = BuiltInRegistries.CREATIVE_MODE_TAB.get(CreativeModeTabs.INGREDIENTS)
                ?: error("Missing vanilla ingredients tab")
            if (worldProbe) {
                require(holders != null)
                val parameters = CreativeModeTab.ItemDisplayParameters(
                    connection?.enabledFeatures() ?: FeatureFlags.DEFAULT_FLAGS,
                    true,
                    holders
                )
                tools.buildContents(parameters)
                ingredients.buildContents(parameters)
                require(tools.contains(pen)) { "Amethyst Pen absent from vanilla creative inventory" }
                require(ingredients.contains(scrap)) { "Note Scrap absent from vanilla creative inventory" }
            }
        }

        if (failures.isEmpty()) {
            HexGuide.LOGGER.info(
                "[HEXGUIDE-CLIENT-PROBE] aggregate=PASS world={} translations=29 models=2 textures=4 patchouli_pages=5 patchouli_entries={} lookups=6 keybind=true mixins=2 inline_resources={} inline_decoded={} network=true creative=true",
                worldProbe,
                if (worldProbe) (if (ModList.get().isLoaded("hexcassettes")) 24 else 23) else 0,
                inlineResourceCount,
                inlineDecodedCount
            )
        } else {
            HexGuide.LOGGER.error(
                "[HEXGUIDE-CLIENT-PROBE] aggregate=FAIL failure_count={} failures={}",
                failures.size,
                failures.joinToString(",")
            )
        }
    }
}

