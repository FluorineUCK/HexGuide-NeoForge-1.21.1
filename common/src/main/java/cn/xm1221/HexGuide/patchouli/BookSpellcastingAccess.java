package cn.xm1221.HexGuide.patchouli;

import at.petrak.hexcasting.api.casting.math.HexPattern;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec2;

import java.util.List;

/**
 * 由 MixinGuiSpellcasting 实现，用于书页内嵌模式：
 * - 拦截 drawEnd 发包（书页练习不施法）
 * - 覆盖 hexSize（"点之间间隔"可配置）
 * - 覆盖 coordsOffset（网格居中于画布，消除空隙）
 * - 只绘制不施法模式：完成的图案标 ESCAPED（黄色）
 * - 清屏 / 读取图案与栈（供清屏与真实施法用）
 */
public interface BookSpellcastingAccess {
    void setBlockSending$hexguide(boolean block);
    void setHexSizeOverride$hexguide(float hexSize);
    void setCoordsOffset$hexguide(Vec2 offset);
    void setWriteMode$hexguide(boolean write);
    void clearPatterns$hexguide();
    void setStackClear$hexguide();
    int patternCount$hexguide();
    HexPattern getPattern$hexguide(int index);
    List<CompoundTag> getStack$hexguide();
    List<CompoundTag> takePendingSync$hexguide();
}
