package cn.xm1221.HexGuide.compat.inline;

import at.petrak.hexcasting.api.HexAPI;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import com.samsthenerd.inline.api.InlineData;

import org.jetbrains.annotations.NotNull;

/**
 * Inline 内联数据 —— 存储 iota:{NBT} 解析出的 Iota，
 * 在聊天等文本中以内联方式渲染 Iota.display()。
 */
public class IotaInlineData implements InlineData<IotaInlineData> {

    public static final ResourceLocation RENDERER_ID = new ResourceLocation(HexAPI.MOD_ID, "iota");

    private final String nbtStr;
    private Iota cached;

    public IotaInlineData(String nbtStr) {
        this.nbtStr = nbtStr;
    }

    @NotNull
    public Iota getOrDeserialize() {
        if (cached == null) {
            try { cached = IotaType.deserialize(TagParser.parseTag(nbtStr), null); }
            catch (Exception e) { return null; }
        }
        return cached;
    }

    @Override
    public InlineDataType<IotaInlineData> getType() {
        return IotaInlineDataType.INSTANCE;
    }

    @Override
    public ResourceLocation getRendererId() {
        return RENDERER_ID;
    }

    @Override
    public Style getExtraStyle() {
        Iota i = getOrDeserialize();
        if (i == null) return Style.EMPTY;
        HoverEvent he = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("hexguide.copy.hover"));
        ClickEvent ce = new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, "iota:" + nbtStr);
        return Style.EMPTY.withHoverEvent(he).withClickEvent(ce);
    }

    @Override
    public Component asText(boolean withExtra) {
        Iota i = getOrDeserialize();
        if (i == null) return Component.literal("Broken Iota").withStyle(s -> s.withColor(0xFF4444));
        return i.display();
    }

    /** 仅用于 codec 序列化 */
    public String getNbtStr() { return nbtStr; }
}
