package cn.xm1221.HexGuide.compat.inline;

import cn.xm1221.HexGuide.HexGuide;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import com.samsthenerd.inline.api.InlineData;

import java.io.*;
import java.util.Optional;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

/**
 * Inline 内联 —— iota:&lt;a85&gt; 或 iota:&lt;ns:path.json&gt;。
 * a85: NBT→NbtIo→deflate→Ascii85(!~u)
 * json: assets/&lt;ns&gt;/iotas/&lt;path&gt;.json → IotaType.deserialize
 */
public class IotaInlineData implements InlineData<IotaInlineData> {

    public static final ResourceLocation RENDERER_ID = new ResourceLocation(HexGuide.MODID, "iota");
    private static final int A85_BASE = 33;

    private final String raw;
    private Iota cached;
    /** 资源文件引用，非 null 时不走 Ascii85 解码 */
    private final String resourceRef;

    public IotaInlineData(String raw) { this.raw = raw; this.resourceRef = null; }
    private IotaInlineData(String raw, String ref) { this.raw = raw; this.resourceRef = ref; }

    public Iota getOrDeserialize() {
        if (cached == null) {
            if (resourceRef != null) cached = loadFromResource(resourceRef);
            else try {
                byte[] comp = decodeA85(raw);
                var out = new ByteArrayOutputStream();
                var infl = new InflaterOutputStream(out);
                infl.write(comp); infl.flush(); infl.close();
                var tag = NbtIo.read(new DataInputStream(new ByteArrayInputStream(out.toByteArray())));
                if (tag != null) cached = IotaType.deserialize((CompoundTag) tag, null);
            } catch (Exception ignored) {}
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
        return i == null ? Component.literal("\u274C") : i.display();
    }

    public String getRaw() { return raw; }

    // ─── parse ───────────────────────────────────────────────

    /** 工厂：含 .json 且含 : 则走资源文件，否则 Ascii85 */
    @org.jetbrains.annotations.Nullable
    public static IotaInlineData parse(String raw) {
        if (raw.contains(".json") && raw.contains(":")) {
            var data = new IotaInlineData(raw, raw);
            if (data.getOrDeserialize() != null) return data;
            return null;
        }
        return new IotaInlineData(raw);
    }

    /** 从 assets/&lt;ns&gt;/iotas/&lt;path&gt;.json 加载 Iota */
    private static Iota loadFromResource(String ref) {
        try {
            int colon = ref.lastIndexOf(':');
            if (colon <= 0) return null;
            String ns = ref.substring(0, colon);
            String path = ref.substring(colon + 1);
            if (!path.endsWith(".json")) path += ".json";
            ResourceLocation rl = new ResourceLocation(ns, "iotas/" + path);
            var mgr = Minecraft.getInstance().getResourceManager();
            Optional<Resource> opt = mgr.getResource(rl);
            if (opt.isEmpty()) return null;
            try (var reader = new InputStreamReader(opt.get().open())) {
                var json = JsonParser.parseReader(reader).toString();
                return IotaType.deserialize(TagParser.parseTag(json), null);
            }
        } catch (Exception ignored) { return null; }
    }

    // ─── encode ───────────────────────────────────────────────

    public static String encode(Iota iota) {
        var tag = IotaType.serialize(iota);
        var baos = new ByteArrayOutputStream();
        try { NbtIo.write(tag, new DataOutputStream(baos)); } catch (IOException ignored) {}
        byte[] b = baos.toByteArray();
        try { var o = new ByteArrayOutputStream(); var d = new DeflaterOutputStream(o);
            d.write(b); d.flush(); d.close(); b = o.toByteArray(); } catch (Exception ignored) {}
        return encodeA85(b);
    }

    public static String toPrefixed(Iota iota) { return "iota:" + encode(iota); }

    // ─── Ascii85 (!-u, z=0000) ──────────────────────────────────

    static String encodeA85(byte[] d) {
        var sb = new StringBuilder();
        int i = 0;
        while (i + 4 <= d.length) {
            long v = ((d[i++] & 0xFFL) << 24) | ((d[i++] & 0xFFL) << 16)
                   | ((d[i++] & 0xFFL) << 8) | (d[i++] & 0xFFL);
            if (v == 0) { sb.append('z'); continue; }
            sb.append((char)(A85_BASE + v / 52200625)); v %= 52200625;
            sb.append((char)(A85_BASE + v / 614125));   v %= 614125;
            sb.append((char)(A85_BASE + v / 7225));     v %= 7225;
            sb.append((char)(A85_BASE + v / 85));       v %= 85;
            sb.append((char)(A85_BASE + v));
        }
        if (i < d.length) {
            long v = 0; int rem = d.length - i;
            for (int j = i; j < d.length; j++) v = (v << 8) | (d[j] & 0xFF);
            v <<= (4 - rem) * 8;
            for (int j = 0; j <= rem; j++) { sb.append((char)(A85_BASE + v / 52200625)); v = (v % 52200625) * 85; }
        }
        return sb.toString();
    }

    static byte[] decodeA85(String s) {
        var out = new ByteArrayOutputStream();
        int[] g = new int[5]; int gi = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == 'z') { out.write(new byte[4], 0, 4); gi = 0; continue; }
            g[gi++] = c - A85_BASE;
            if (gi == 5) {
                long v = ((((g[0] * 85L + g[1]) * 85 + g[2]) * 85 + g[3]) * 85 + g[4]);
                out.write((int)(v >> 24)); out.write((int)(v >> 16));
                out.write((int)(v >> 8)); out.write((int)(v & 0xFF));
                gi = 0;
            }
        }
        if (gi > 0) {
            for (int j = gi; j < 5; j++) g[j] = 84;
            long v = ((((g[0] * 85L + g[1]) * 85 + g[2]) * 85 + g[3]) * 85 + g[4]);
            for (int j = 0; j < gi - 1; j++) out.write((int)(v >> (24 - 8 * j)) & 0xFF);
        }
        return out.toByteArray();
    }
}
