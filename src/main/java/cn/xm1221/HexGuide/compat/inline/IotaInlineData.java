package cn.xm1221.HexGuide.compat.inline;

import cn.xm1221.HexGuide.HexGuide;
import cn.xm1221.HexGuide.client.HexGuideClientBridge;
import at.petrak.hexcasting.api.casting.iota.Iota;
import cn.xm1221.HexGuide.hexcompat.HexCodecCompat;
import cn.xm1221.HexGuide.hexcompat.IotaTextCodec;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.neoforged.fml.loading.FMLPaths;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import com.samsthenerd.inline.api.InlineData;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Inline 内联 —— iota:&lt;a85&gt; 或 iota:&lt;ns:path.json&gt;。
 * a85: NBT→NbtIo→deflate→Ascii85(!~u)
 * json: assets/&lt;ns&gt;/iotas/&lt;path&gt;.json → IotaType.deserialize
 */
public class IotaInlineData implements InlineData<IotaInlineData> {

    public static final ResourceLocation RENDERER_ID = ResourceLocation.fromNamespaceAndPath(HexGuide.MODID, "iota");

    private final String raw;
    private Iota cached;
    /** 资源文件引用，非 null 时不走 Ascii85 解码 */
    private final String resourceRef;
    /** 强制渲染颜色（ARGB）；-1 = 继承当前文本样式颜色（trContext.usableColor） */
    private final int forcedColor;
    /** 强制换行（iota:xxx**n）：显示文本拆成多行逐行向下渲染 */
    private final boolean wrapNewline;

    public IotaInlineData(String raw) { this(raw, null, -1, false); }
    private IotaInlineData(String raw, String ref) { this(raw, ref, -1, false); }
    private IotaInlineData(String raw, String ref, int color) { this(raw, ref, color, false); }
    private IotaInlineData(String raw, String ref, int color, boolean wrap) {
        this.raw = raw; this.resourceRef = ref; this.forcedColor = color; this.wrapNewline = wrap;
    }

    /** 强制渲染颜色（ARGB），-1 表示继承文本样式 */
    public int getForcedColor() { return forcedColor; }

    /** 是否强制换行显示（**n 后缀） */
    public boolean isWrapNewline() { return wrapNewline; }

    public Iota getOrDeserialize() {
        if (cached == null) {
            if (resourceRef != null) cached = loadFromResource(resourceRef);
            else cached = IotaTextCodec.decode(raw);
        }
        return cached;
    }

    @Override public InlineDataType<IotaInlineData> getType() { return IotaInlineDataType.INSTANCE; }
    @Override public ResourceLocation getRendererId() { return RENDERER_ID; }

    @Override public Style getExtraStyle() {
        Iota i = getOrDeserialize();
        if (i == null) return Style.EMPTY;
        String ref = resourceRef != null ? "iota:" + resourceRef : "iota:" + raw;
        return Style.EMPTY
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, i.display()))
            .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, ref));
    }

    @Override public Component asText(boolean w) {
        Iota i = getOrDeserialize();
        return i == null ? Component.literal("❌") : i.display();
    }

    public String getRaw() { return raw; }

    // ─── parse ───────────────────────────────────────────────

    /**
     * 工厂：含 .json 则走资源文件（可省略命名空间，默认 hexguide），否则 Ascii85。
     * 颜色后缀（会从引用中剥离，不影响解码/资源名）：
     * - *b          → 强制黑色
     * - *w          → 强制白色
     * - *RRGGBB     → 强制指定 RGB 颜色（如 *FF0000 红色）
     * - **n         → 强制换行显示（长文本拆多行；独立成行/行尾场景效果最佳，行中会向下覆盖）
     * - 无后缀      → 继承当前文本样式颜色（trContext.usableColor，书页/tooltip 自适应）
     */
    @org.jetbrains.annotations.Nullable
    public static IotaInlineData parse(String raw) {
        boolean wrap = raw.endsWith("**n");
        if (wrap) raw = raw.substring(0, raw.length() - 3); // 剥离 **n（3 字符）
        int forced = parseForcedColor(raw);
        if (forced != -1) raw = stripColorSuffix(raw);
        // 资源引用以 .json 结尾（用 endsWith 而非 contains，避免 Ascii85 编码里碰巧含 ".json" 子串被误判）
        if (raw.endsWith(".json")) {
            var data = new IotaInlineData(raw, raw, forced, wrap);
            if (data.getOrDeserialize() != null) return data;
            HexGuide.LOGGER.warn("[IotaInlineData] 资源 iota 解析失败: {}", raw);
            return null;
        }
        return new IotaInlineData(raw, null, forced, wrap);
    }

    /** 解析颜色后缀；无后缀返回 -1 */
    private static int parseForcedColor(String s) {
        if (s.endsWith("*b")) return 0xFF000000;
        if (s.endsWith("*w")) return 0xFFFFFFFF;
        if (s.length() >= 7 && s.charAt(s.length() - 7) == '*') {
            try {
                return 0xFF000000 | Integer.parseInt(s.substring(s.length() - 6), 16);
            } catch (Exception ignored) {}
        }
        return -1;
    }

    /** 剥离颜色后缀 */
    private static String stripColorSuffix(String s) {
        if (s.endsWith("*b") || s.endsWith("*w")) return s.substring(0, s.length() - 2);
        if (s.length() >= 7 && s.charAt(s.length() - 7) == '*') return s.substring(0, s.length() - 7);
        return s;
    }

    /** 加载 Iota：优先资源管理器 assets/&lt;ns&gt;/iotas/&lt;path&gt;.json，回退游戏目录 &lt;gameDir&gt;/&lt;ns&gt;/iotas/&lt;path&gt;.json */
    private static Iota loadFromResource(String ref) {
        try {
            // 无冒号 → 默认命名空间 hexguide（短引用 iota:name.json）
            int colon = ref.lastIndexOf(':');
            String ns, path;
            if (colon > 0) {
                ns = ref.substring(0, colon);
                path = ref.substring(colon + 1);
            } else {
                ns = HexGuide.MODID;
                path = ref;
            }
            if (!path.endsWith(".json")) path += ".json";
            if (!isSafeResourcePath(path)) {
                HexGuide.LOGGER.warn("[IotaInlineData] 拒绝不安全的 iota 资源路径: {}", ref);
                return null;
            }

            // 1) 客户端入口安装的资源加载器（模组资源/资源包）。
            // 公共 InlineData 不直接链接 net.minecraft.client，专服可安全加载该类。
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(ns, "iotas/" + path);
            String resourceText = HexGuideClientBridge.loadResourceText(rl);
            if (resourceText != null) {
                return deserializeJson(JsonParser.parseString(resourceText));
            }

            // 2) 游戏目录（运行时自动保存的文件）
            Path root = FMLPaths.GAMEDIR.get().resolve(ns).resolve("iotas").toAbsolutePath().normalize();
            Path file = root.resolve(path).normalize();
            if (!file.startsWith(root)) return null;
            if (Files.exists(file)) {
                return deserializeJson(JsonParser.parseString(Files.readString(file)));
            }
            HexGuide.LOGGER.warn("[IotaInlineData] 找不到 iota 资源: assets/{}/iotas/{} 或游戏目录 {}", ns, path, file);
            return null;
        } catch (Exception e) {
            HexGuide.LOGGER.warn("[IotaInlineData] 加载 iota 资源异常: {}", ref, e);
            return null;
        }
    }

    private static boolean isSafeResourcePath(String path) {
        if (path.startsWith("/") || path.startsWith("\\\\") || path.contains("..") || path.contains("\\\\")) {
            return false;
        }
        return ResourceLocation.tryBuild(HexGuide.MODID, "iotas/" + path) != null;
    }

    /** 解析 iota 资源文件：{"nbt":"<nbt字符串>"} 或直接 NBT JSON */
    private static Iota deserializeJson(JsonElement elem) throws CommandSyntaxException {
        if (elem != null && elem.isJsonObject() && elem.getAsJsonObject().has("nbt")) {
            String nbt = elem.getAsJsonObject().get("nbt").getAsString();
            return HexCodecCompat.deserializeIotaCompat(TagParser.parseTag(nbt), null);
        }
        return HexCodecCompat.deserializeIotaCompat(TagParser.parseTag(elem.toString()), null);
    }

    // ─── save to game dir ──────────────────────────────────────

    /**
     * 将 Iota 以 JSON 形式自动保存到 &lt;gameDir&gt;/&lt;ns&gt;/iotas/&lt;hash&gt;[-&lt;counter&gt;].json。
     * 返回短资源名 "hash[-counter]"（无命名空间，完整引用为 iota:hash.json，可放进书本 116px 页面不被换行截断）。
     * 失败返回 null。
     */
    @org.jetbrains.annotations.Nullable
    public static String saveToGameDir(Iota iota) {
        return IotaTextCodec.saveToGameDir(iota);
    }

    /**
     * 按指定 ref 把 iota NBT 保存到 &lt;gameDir&gt;/&lt;ns&gt;/iotas/&lt;ref&gt;.json。
     * 用于同步（MsgIotaSyncS2C）：所有玩家以同一 ref 保存，保证 iota:&lt;ref&gt;.json 在各端可加载。
     */
    public static void saveToGameDirRef(String ref, net.minecraft.nbt.CompoundTag iotaNbt) {
        IotaTextCodec.saveToGameDirRef(ref, iotaNbt);
    }

    // ─── encode ───────────────────────────────────────────────

    /** 根据 "ns:path"（可带 .json）加载 Iota 并返回其 display Component；失败返回原引用文本 */
    public static Component displayFromRef(String ref) {
        Iota iota = loadFromResource(ref);
        return iota != null ? iota.display() : Component.literal(ref);
    }

    public static String encode(Iota iota) {
        return IotaTextCodec.encode(iota);
    }

    public static String toPrefixed(Iota iota) { return IotaTextCodec.toPrefixed(iota); }
}


