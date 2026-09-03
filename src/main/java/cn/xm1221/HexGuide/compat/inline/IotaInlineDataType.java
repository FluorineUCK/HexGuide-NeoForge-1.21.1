package cn.xm1221.HexGuide.compat.inline;

import cn.xm1221.HexGuide.HexGuide;
import com.mojang.serialization.Codec;
import com.samsthenerd.inline.api.InlineData;
import net.minecraft.resources.ResourceLocation;

public class IotaInlineDataType implements InlineData.InlineDataType<IotaInlineData> {

    public static final IotaInlineDataType INSTANCE = new IotaInlineDataType();
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(HexGuide.MODID, "iota");

    @Override
    public ResourceLocation getId() { return ID; }

    @Override
    public Codec<IotaInlineData> getCodec() {
        return Codec.STRING.xmap(IotaInlineData::new, IotaInlineData::getRaw);
    }
}

