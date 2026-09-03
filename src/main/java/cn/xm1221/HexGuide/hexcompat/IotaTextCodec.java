package cn.xm1221.HexGuide.hexcompat;

import at.petrak.hexcasting.api.casting.iota.Iota;
import cn.xm1221.HexGuide.HexGuide;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

/**
 * Side-neutral codec and disk persistence used by both spell actions and the
 * client Inline renderer. Keeping this separate prevents server action classes
 * from loading {@code net.minecraft.client} through {@code IotaInlineData}.
 */
public final class IotaTextCodec {
    private static final int A85_BASE = 33;

    private IotaTextCodec() {
    }

    @Nullable
    public static Iota decode(String encoded) {
        try {
            byte[] compressed = decodeA85(encoded);
            var uncompressed = new ByteArrayOutputStream();
            try (var inflater = new InflaterOutputStream(uncompressed)) {
                inflater.write(compressed);
            }
            CompoundTag tag = NbtIo.read(
                new DataInputStream(new ByteArrayInputStream(uncompressed.toByteArray()))
            );
            return tag == null ? null : HexCodecCompat.deserializeIota(tag, null);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String encode(Iota iota) {
        CompoundTag tag = HexCodecCompat.serializeIota(iota);
        var nbtBytes = new ByteArrayOutputStream();
        try {
            NbtIo.write(tag, new DataOutputStream(nbtBytes));
        } catch (Exception ignored) {
            return "";
        }

        byte[] bytes = nbtBytes.toByteArray();
        try {
            var compressed = new ByteArrayOutputStream();
            try (var deflater = new DeflaterOutputStream(compressed)) {
                deflater.write(bytes);
            }
            bytes = compressed.toByteArray();
        } catch (Exception ignored) {
            return "";
        }
        return encodeA85(bytes);
    }

    public static String toPrefixed(Iota iota) {
        return "iota:" + encode(iota);
    }

    /**
     * Saves an Iota under {@code <gameDir>/hexguide/iotas} and returns its
     * short reference without the {@code .json} suffix.
     */
    @Nullable
    public static String saveToGameDir(Iota iota) {
        try {
            String tagString = HexCodecCompat.serializeIota(iota).toString();
            String hash = shortHash(tagString);
            Path directory = FMLPaths.GAMEDIR.get()
                .resolve(HexGuide.MODID)
                .resolve("iotas");
            Files.createDirectories(directory);

            String name = hash;
            Path file = directory.resolve(name + ".json");
            int counter = 0;
            while (Files.exists(file) && counter < 100) {
                counter++;
                name = hash + "-" + counter;
                file = directory.resolve(name + ".json");
            }
            Files.writeString(file, saveJson(tagString));
            return name;
        } catch (Exception exception) {
            HexGuide.LOGGER.warn("Unable to save iota resource", exception);
            return null;
        }
    }

    /**
     * Stores a server-synchronized Iota using a constrained shared reference.
     */
    public static void saveToGameDirRef(String reference, CompoundTag iotaNbt) {
        try {
            if (reference == null || !reference.matches("[a-z0-9_.-]{1,64}")) {
                return;
            }
            Path directory = FMLPaths.GAMEDIR.get()
                .resolve(HexGuide.MODID)
                .resolve("iotas");
            Files.createDirectories(directory);
            Files.writeString(
                directory.resolve(reference + ".json"),
                saveJson(iotaNbt.toString())
            );
        } catch (Exception exception) {
            HexGuide.LOGGER.warn("Unable to save synchronized iota resource {}", reference, exception);
        }
    }

    private static String saveJson(String tagString) {
        JsonObject object = new JsonObject();
        object.add("nbt", new JsonPrimitive(tagString));
        return object.toString();
    }

    private static String shortHash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (int index = 0; index < 3; index++) {
                result.append(String.format("%02x", digest[index] & 0xFF));
            }
            return result.toString();
        } catch (Exception ignored) {
            return Integer.toHexString(value.hashCode() & 0xFFFFFF);
        }
    }

    static String encodeA85(byte[] data) {
        var result = new StringBuilder();
        int index = 0;
        while (index + 4 <= data.length) {
            long value = ((data[index++] & 0xFFL) << 24)
                | ((data[index++] & 0xFFL) << 16)
                | ((data[index++] & 0xFFL) << 8)
                | (data[index++] & 0xFFL);
            if (value == 0) {
                result.append('z');
                continue;
            }
            result.append((char) (A85_BASE + value / 52200625));
            value %= 52200625;
            result.append((char) (A85_BASE + value / 614125));
            value %= 614125;
            result.append((char) (A85_BASE + value / 7225));
            value %= 7225;
            result.append((char) (A85_BASE + value / 85));
            value %= 85;
            result.append((char) (A85_BASE + value));
        }
        if (index < data.length) {
            long value = 0;
            int remaining = data.length - index;
            for (int cursor = index; cursor < data.length; cursor++) {
                value = (value << 8) | (data[cursor] & 0xFF);
            }
            value <<= (4 - remaining) * 8;
            for (int cursor = 0; cursor <= remaining; cursor++) {
                result.append((char) (A85_BASE + value / 52200625));
                value = (value % 52200625) * 85;
            }
        }
        return result.toString();
    }

    static byte[] decodeA85(String encoded) {
        var output = new ByteArrayOutputStream();
        int[] group = new int[5];
        int groupIndex = 0;
        for (int index = 0; index < encoded.length(); index++) {
            char character = encoded.charAt(index);
            if (character == 'z') {
                output.write(new byte[4], 0, 4);
                groupIndex = 0;
                continue;
            }
            if (character < '!' || character > 'u') {
                throw new IllegalArgumentException("Invalid Ascii85 character");
            }
            group[groupIndex++] = character - A85_BASE;
            if (groupIndex == 5) {
                long value = ((((group[0] * 85L + group[1]) * 85 + group[2]) * 85 + group[3]) * 85 + group[4]);
                output.write((int) (value >> 24));
                output.write((int) (value >> 16));
                output.write((int) (value >> 8));
                output.write((int) value);
                groupIndex = 0;
            }
        }
        if (groupIndex > 0) {
            if (groupIndex == 1) {
                throw new IllegalArgumentException("Invalid trailing Ascii85 group");
            }
            for (int index = groupIndex; index < 5; index++) {
                group[index] = 84;
            }
            long value = ((((group[0] * 85L + group[1]) * 85 + group[2]) * 85 + group[3]) * 85 + group[4]);
            for (int index = 0; index < groupIndex - 1; index++) {
                output.write((int) (value >> (24 - 8 * index)) & 0xFF);
            }
        }
        return output.toByteArray();
    }
}
