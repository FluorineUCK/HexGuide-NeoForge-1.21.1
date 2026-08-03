package cn.xm1221.HexGuide.mixin;

import at.petrak.hexcasting.api.casting.eval.ResolvedPattern;
import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import at.petrak.hexcasting.api.casting.iota.PatternIota;
import at.petrak.hexcasting.api.casting.math.HexCoord;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.client.gui.GuiSpellcasting;
import at.petrak.hexcasting.common.msgs.IMessage;
import at.petrak.hexcasting.common.msgs.MsgNewSpellPatternC2S;
import at.petrak.hexcasting.xplat.IClientXplatAbstractions;
import cn.xm1221.HexGuide.patchouli.BookSpellcastingAccess;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 让书页能内嵌真实 GuiSpellcasting：
 * - 拦截 drawEnd 的 sendPacketToServer（书页练习不施法）
 * - 只绘制不施法模式：完成的图案标 ESCAPED（黄色）
 * - 把新图案加入 cachedStack 并重算栈显示（服务端不回包，需本地维护）
 * - 覆盖 hexSize（点间距可配置）、清屏、读取栈
 * 注：Screen 的 minecraft/width/height 用公共 Screen.init(mc, w, h) 设置。
 */
@Mixin(GuiSpellcasting.class)
public abstract class MixinGuiSpellcasting implements BookSpellcastingAccess {

    @Shadow(remap = false) private List<ResolvedPattern> patterns;
    @Shadow(remap = false) private Set<HexCoord> usedSpots;
    @Shadow(remap = false) private List<CompoundTag> cachedStack;
    @Shadow(remap = false) private void calculateIotaDisplays() { }

    // 默认不拦截、不写模式——只有书页内嵌时才由页面显式开启，正常法杖施法不受影响
    @Unique private boolean blockSending$hexguide = false;
    @Unique private boolean writeMode$hexguide = false;
    @Unique private float hexSizeOverride$hexguide = Float.NaN;
    @Unique private Vec2 coordsOffsetOverride$hexguide;
    // 写模式记录的、尚未推送到法杖栈的图案（切到施法模式时补发，不执行）
    @Unique private List<CompoundTag> pendingSync$hexguide = new ArrayList<>();

    @WrapWithCondition(
        method = "drawEnd",
        at = @At(value = "INVOKE",
            target = "Lat/petrak/hexcasting/xplat/IClientXplatAbstractions;sendPacketToServer(Lat/petrak/hexcasting/common/msgs/IMessage;)V",
            remap = false),
        remap = false
    )
    private boolean blockPacket$hexguide(IClientXplatAbstractions inst, IMessage msg) {
        if (msg instanceof MsgNewSpellPatternC2S s) {
            // 写模式：图案标 ESCAPED（黄色）+ 本地维护栈（记录）；施法模式（原版行为）放行发包
            if (writeMode$hexguide) {
                int idx = s.resolvedPatterns().size() - 1;
                if (idx >= 0 && idx < patterns.size()) {
                    patterns.get(idx).setType(ResolvedPatternType.ESCAPED);
                }
                CompoundTag rec = IotaType.serialize(new PatternIota(s.pattern()));
                List<CompoundTag> newStack = new ArrayList<>(cachedStack);
                newStack.add(rec);
                cachedStack = newStack;
                pendingSync$hexguide.add(rec);
                this.calculateIotaDisplays();
            }
        }
        return !blockSending$hexguide;
    }

    @ModifyReturnValue(method = "hexSize", at = @At("RETURN"), remap = false)
    private float overrideHexSize$hexguide(float original) {
        return Float.isNaN(hexSizeOverride$hexguide) ? original : hexSizeOverride$hexguide;
    }

    @ModifyReturnValue(method = "coordsOffset", at = @At("RETURN"), remap = false)
    private Vec2 overrideCoordsOffset$hexguide(Vec2 original) {
        return coordsOffsetOverride$hexguide != null ? coordsOffsetOverride$hexguide : original;
    }

    @Override public void setBlockSending$hexguide(boolean block) { blockSending$hexguide = block; }
    @Override public void setHexSizeOverride$hexguide(float hexSize) { hexSizeOverride$hexguide = hexSize; }
    @Override public void setCoordsOffset$hexguide(Vec2 offset) { coordsOffsetOverride$hexguide = offset; }
    @Override public void setWriteMode$hexguide(boolean write) { writeMode$hexguide = write; }
    @Override public void clearPatterns$hexguide() {
        patterns.clear();
        usedSpots.clear();
        // 不清除 cachedStack：转义的栈记录保留（栈显示继续累积）
        this.calculateIotaDisplays();
    }

    /**
     * 施法结果栈清空时调用：清空栈显示（不关闭 GUI）。
     * 原版 recvServerUpdate 在 isStackClear 时会 setScreen(null)，书页内嵌不能关书。
     */
    @Override public void setStackClear$hexguide() {
        cachedStack = new ArrayList<>();
        this.calculateIotaDisplays();
    }

    /**
     * 切到施法模式时调用：把尚未同步到服务端的本地图案补发（MsgNewSpellPatternC2S），
     * 让服务端施法 VM 从与本地相同的栈继续执行——本地栈与施法栈是同一个。
     */
    @Override public int patternCount$hexguide() { return patterns.size(); }
    @Override public HexPattern getPattern$hexguide(int index) { return patterns.get(index).getPattern(); }
    @Override public List<CompoundTag> getStack$hexguide() { return cachedStack; }

    /** 取走待同步到法杖栈的写模式记录（并清空） */
    @Override public List<CompoundTag> takePendingSync$hexguide() {
        List<CompoundTag> out = new ArrayList<>(pendingSync$hexguide);
        pendingSync$hexguide = new ArrayList<>();
        return out;
    }
}
