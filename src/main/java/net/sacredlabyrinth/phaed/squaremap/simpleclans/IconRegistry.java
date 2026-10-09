package net.sacredlabyrinth.phaed.squaremap.simpleclans;

import org.bukkit.DyeColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.Squaremap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Clan home icons, registered with squaremap's icon registry.
 *
 * <p>The plugin ships one banner per Minecraft dye colour ({@code banner_red}, ...).
 * Admins can add their own PNGs to {@code plugins/squaremap-SimpleClans/icons/}; the file
 * name without .png is the icon's name, and a file named like a bundled banner replaces it.
 */
public final class IconRegistry {

    /** Dye colours in the order the creative inventory lists banners. */
    public static final List<String> BANNER_COLORS = List.of(
            "white", "light_gray", "gray", "black", "brown", "red", "orange", "yellow",
            "lime", "green", "cyan", "light_blue", "blue", "purple", "magenta", "pink");
    public static final String BANNER_PREFIX = "banner_";
    public static final String FALLBACK_ICON = "banner_red";
    /** 2.0.0's default icon, now the red banner. */
    private static final String LEGACY_DEFAULT = "clanhome";
    private static final Pattern VALID_NAME = Pattern.compile("[a-z0-9_.-]+");

    private final JavaPlugin plugin;
    private final Squaremap squaremap;
    private final Map<String, Key> icons = new HashMap<>();
    private final Map<String, Integer> colors = new HashMap<>();
    private final Set<Key> registered = new HashSet<>();
    private List<String> ordered = List.of();
    private String defaultIcon = FALLBACK_ICON;
    private @Nullable Key killIcon;

    public IconRegistry(@NotNull JavaPlugin plugin, @NotNull Squaremap squaremap) {
        this.plugin = plugin;
        this.squaremap = squaremap;
    }

    public void load(@NotNull String configuredDefault) {
        // admin icons first, so they win over bundled ones with the same name
        loadDirectory(new File(plugin.getDataFolder(), "icons"));
        // where icons lived in 1.x
        loadDirectory(new File(plugin.getDataFolder(), "images/clanhome"));
        for (String color : BANNER_COLORS) {
            String name = BANNER_PREFIX + color;
            if (!icons.containsKey(name)) {
                BufferedImage image = readResource("icons/" + name + ".png");
                if (image != null) {
                    icons.put(name, register("simpleclans_icon_" + name, image));
                    colors.put(name, averageColor(image));
                }
            }
        }

        List<String> banners = new ArrayList<>();
        List<String> custom = new ArrayList<>();
        for (String color : BANNER_COLORS) {
            if (icons.containsKey(BANNER_PREFIX + color)) {
                banners.add(BANNER_PREFIX + color);
            }
        }
        icons.keySet().stream().filter(n -> !banners.contains(n)).sorted().forEach(custom::add);
        banners.addAll(custom);
        ordered = List.copyOf(banners);

        defaultIcon = resolve(configuredDefault);
        if (defaultIcon == null) {
            plugin.getLogger().warning("Default icon '" + configuredDefault + "' doesn't exist, using '"
                    + FALLBACK_ICON + "'.");
            defaultIcon = FALLBACK_ICON;
        }

        killIcon = loadKillIcon();
    }

    public void unregisterAll() {
        for (Key key : registered) {
            try {
                squaremap.iconRegistry().unregister(key);
            } catch (RuntimeException ignored) {
                // squaremap may already be shutting down
            }
        }
        registered.clear();
        icons.clear();
        colors.clear();
        ordered = List.of();
        killIcon = null;
    }

    /** Icon names: bundled banners in creative-inventory order, then custom icons A-Z. */
    public @NotNull List<String> names() {
        return ordered;
    }

    public boolean has(@NotNull String name) {
        return resolve(name) != null;
    }

    public @NotNull String defaultIcon() {
        return defaultIcon;
    }

    /** The icon a clan with this setting actually shows (unknown or unset means the default). */
    public @NotNull String effective(@Nullable String name) {
        String resolved = name != null ? resolve(name) : null;
        return resolved != null ? resolved : defaultIcon;
    }

    public @NotNull Key key(@Nullable String name) {
        Key key = icons.get(effective(name));
        return key != null ? key : Key.of("simpleclans_icon_" + FALLBACK_ICON);
    }

    public @NotNull Key killIcon() {
        return killIcon != null ? killIcon : key(null);
    }

    /**
     * The colour that represents an icon in menus: a banner's dye colour, otherwise the
     * average colour of the image. -1 if the icon doesn't exist.
     */
    public int color(@NotNull String icon) {
        String dye = bannerColor(icon);
        if (dye != null) {
            return DyeColor.valueOf(dye.toUpperCase(Locale.ROOT)).getColor().asRGB();
        }
        return colors.getOrDefault(icon, -1);
    }

    /** The dye colour of a bundled-style banner icon, e.g. "light_blue", or null. */
    public static @Nullable String bannerColor(@NotNull String icon) {
        if (!icon.startsWith(BANNER_PREFIX)) {
            return null;
        }
        String color = icon.substring(BANNER_PREFIX.length());
        return BANNER_COLORS.contains(color) ? color : null;
    }

    private @Nullable String resolve(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (icons.containsKey(lower)) {
            return lower;
        }
        if (lower.equals(LEGACY_DEFAULT) && icons.containsKey(FALLBACK_ICON)) {
            return FALLBACK_ICON;
        }
        return null;
    }

    // -- loading -----------------------------------------------------------

    private void loadDirectory(File dir) {
        File[] files = dir.listFiles((d, n) -> n.toLowerCase(Locale.ROOT).endsWith(".png"));
        if (files == null) {
            return;
        }
        Arrays.sort(files);
        for (File file : files) {
            String fileName = file.getName();
            String name = fileName.substring(0, fileName.length() - 4).toLowerCase(Locale.ROOT);
            if (!VALID_NAME.matcher(name).matches()) {
                plugin.getLogger().warning("Skipping icon '" + fileName + "': use only a-z, 0-9, '_', '-' and '.'");
                continue;
            }
            if (icons.containsKey(name)) {
                continue;
            }
            BufferedImage image = read(file);
            if (image != null) {
                icons.put(name, register("simpleclans_icon_" + name, image));
                colors.put(name, averageColor(image));
            }
        }
    }

    private @Nullable Key loadKillIcon() {
        File custom = new File(plugin.getDataFolder(), "kill.png");
        BufferedImage image = custom.exists() ? read(custom) : null;
        if (image == null) {
            image = readResource("assets/kill.png");
        }
        return image != null ? register("simpleclans_kill", image) : null;
    }

    private @Nullable BufferedImage readResource(String path) {
        try (InputStream in = plugin.getResource(path)) {
            return in != null ? ImageIO.read(in) : null;
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not read bundled image " + path + ": " + ex.getMessage());
            return null;
        }
    }

    private @Nullable BufferedImage read(File file) {
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                plugin.getLogger().warning("'" + file.getName() + "' is not a readable PNG image.");
            }
            return image;
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not read icon '" + file.getName() + "': " + ex.getMessage());
            return null;
        }
    }

    /** Average of the clearly visible pixels, ignoring transparency and near-black outlines. */
    private static int averageColor(BufferedImage image) {
        long r = 0, g = 0, b = 0, count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int red = (argb >> 16) & 0xFF, green = (argb >> 8) & 0xFF, blue = argb & 0xFF;
                if ((argb >>> 24) < 128 || red + green + blue < 60) {
                    continue;
                }
                r += red;
                g += green;
                b += blue;
                count++;
            }
        }
        if (count == 0) {
            return 0xAAAAAA;
        }
        return (int) (r / count) << 16 | (int) (g / count) << 8 | (int) (b / count);
    }

    private Key register(String id, BufferedImage image) {
        Key key = Key.of(id);
        if (squaremap.iconRegistry().hasEntry(key)) {
            squaremap.iconRegistry().unregister(key);
        }
        squaremap.iconRegistry().register(key, image);
        registered.add(key);
        return key;
    }
}
