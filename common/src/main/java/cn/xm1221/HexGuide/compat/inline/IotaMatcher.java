package cn.xm1221.HexGuide.compat.inline;

import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import cn.xm1221.HexGuide.HexGuide;
import com.samsthenerd.inline.api.InlineAPI;
import com.samsthenerd.inline.api.matching.InlineMatch;
import com.samsthenerd.inline.api.matching.MatchContext;
import com.samsthenerd.inline.api.matching.MatcherInfo;
import com.samsthenerd.inline.api.matching.RegexMatcher;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * 匹配形如 iota:{...NBT...} 的文本，解析为 Iota 并交由 IotaInlineData 内联渲染。
 */
public class IotaMatcher implements RegexMatcher {

    public static final IotaMatcher INSTANCE = new IotaMatcher();
    private static final ResourceLocation ID = new ResourceLocation(HexGuide.MODID, "iota");
    private static final MatcherInfo INFO = MatcherInfo.fromId(ID);

    /** 匹配 iota: 后跟大括号包裹的 NBT（支持一层嵌套） */
    private static final Pattern REGEX = Pattern.compile(
        "(?<escaped>\\\\\\\\)?iota:(?<nbt>\\{(?:[^{}]|\\{[^{}]*\\})*\\})",
        Pattern.CASE_INSENSITIVE);

    @Override
    public Pattern getRegex() { return REGEX; }

    @Override
    public InlineMatch getMatch(MatchResult mr, MatchContext ctx) { return null; }

    @Override
    public Tuple<InlineMatch, Integer> getMatchAndGroup(MatchResult mr, MatchContext ctx) {
        String escaped = mr.group(1);
        if (escaped != null && !escaped.isEmpty())
            return new Tuple<>(new InlineMatch.TextMatch(Component.literal("")), 1);

        String nbtStr = mr.group(2);
        try {
            var tag = TagParser.parseTag(nbtStr);
            Iota iota = IotaType.deserialize(tag, null);
            if (iota == null) return new Tuple<>(null, 0);

            IotaInlineData data = new IotaInlineData(nbtStr);
            return new Tuple<>(new InlineMatch.DataMatch(data, data.getExtraStyle()), 0);
        } catch (Exception e) {
            return new Tuple<>(null, 0);
        }
    }

    @Override
    public MatcherInfo getInfo() { return INFO; }

    @Override
    public ResourceLocation getId() { return ID; }
}
