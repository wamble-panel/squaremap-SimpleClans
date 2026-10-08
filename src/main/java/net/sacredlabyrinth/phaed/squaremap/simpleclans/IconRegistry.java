package net.sacredlabyrinth.phaed.squaremap.simpleclans;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Loads clan home icons from {@code plugins/squaremap-SimpleClans/icons/*.png} into
 * squaremap's icon registry. The file name (without .png) is the icon's name.
 */
public final class IconRegistry {

    public static final String BUNDLED_ICON = "clanhome";
    private static final Pattern VALID_NAME = Pattern.compile("[a-z0-9_.-]+");

    private final JavaPlugin plugin;
    private final Squaremap squaremap;
    private final Map<String, Key> icons = new TreeMap<>();
    private final Set<Key> registered = new HashSet<>();
    private String defaultIcon = BUNDLED_ICON;
    private @Nullable Key killIcon;

    public IconRegistry(@NotNull JavaPlugin plugin, @NotNull Squaremap squaremap) {
        this.plugin = plugin;
        this.squaremap = squaremap;
    }

    public void load(@NotNull String configuredDefault) {
        File dir = new File(plugin.getDataFolder(), "icons");
        if (!new File(dir, BUNDLED_ICON + ".png").exists()) {
            plugin.saveResource("icons/" + BUNDLED_ICON + ".png", false);
        }
        loadDirectory(dir);
        // icons from 1.x lived here; keep them working without making admins move files
        loadDirectory(new File(plugin.getDataFolder(), "images/clanhome"));

        defaultIcon = configuredDefault;
        if (!icons.containsKey(defaultIcon)) {
            plugin.getLogger().warning("Default icon '" + configuredDefault + "' not found in icons/, using '"
                    + BUNDLED_ICON + "'.");
            defaultIcon = BUNDLED_ICON;
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
        killIcon = null;
    }

    public @NotNull Set<String> names() {
        return Collections.unmodifiableSet(icons.keySet());
    }

    public boolean has(@NotNull String name) {
        return icons.containsKey(name.toLowerCase(Locale.ROOT));
    }

    /** The key for {@code name}, falling back to the default icon. */
    public @NotNull Key key(@Nullable String name) {
        Key key = name != null ? icons.get(name.toLowerCase(Locale.ROOT)) : null;
        if (key == null) {
            key = icons.get(defaultIcon);
        }
        return key != null ? key : Key.of("simpleclans_icon_" + BUNDLED_ICON);
    }

    public @NotNull Key killIcon() {
        return killIcon != null ? killIcon : key(null);
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
            }
        }
    }

    private @Nullable Key loadKillIcon() {
        File custom = new File(plugin.getDataFolder(), "kill.png");
        BufferedImage image = custom.exists() ? read(custom) : null;
        if (image == null) {
            try (InputStream in = plugin.getResource("assets/kill.png")) {
                image = in != null ? ImageIO.read(in) : null;
            } catch (IOException ex) {
                plugin.getLogger().warning("Could not read the bundled kill icon: " + ex.getMessage());
            }
        }
        return image != null ? register("simpleclans_kill", image) : null;
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
