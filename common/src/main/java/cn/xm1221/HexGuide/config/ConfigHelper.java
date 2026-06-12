package cn.xm1221.HexGuide.config;

import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.ConfigData;

import java.util.function.Consumer;

/**
 * Helper to work around Fabric/Yarn mapping issues with InteractionResult in Kotlin lambdas.
 */
public class ConfigHelper {
    private ConfigHelper() {}

    public static <T extends ConfigData> void registerPreventSave(ConfigHolder<T> holder) {
        holder.registerSaveListener((h, c) -> net.minecraft.world.InteractionResult.FAIL);
    }

    public static <T extends ConfigData> void registerAllowSave(ConfigHolder<T> holder, Consumer<T> beforeSave) {
        holder.registerSaveListener((h, c) -> {
            if (beforeSave != null) beforeSave.accept(c);
            return net.minecraft.world.InteractionResult.PASS;
        });
    }
}
