package cn.xm1221.HexGuide.scrying;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * Keybinding for opening The HexBook when looking at a pattern-bearing object through a scrying lens.
 */
public class HexGuideKeybinds {
    public static final String CATEGORY = "category.hexguide.scrying";

    public static final KeyMapping OPEN_HEXBOOK = new KeyMapping(
        "key.hexguide.open_hexbook",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_R,
        CATEGORY
    );

    public static KeyMapping[] allBinds() {
        return new KeyMapping[] { OPEN_HEXBOOK };
    }
}
