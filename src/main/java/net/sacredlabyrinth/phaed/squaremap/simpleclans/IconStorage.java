package net.sacredlabyrinth.phaed.squaremap.simpleclans;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.Squaremap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.*;

public class IconStorage {

    private static final String NAMESPACE = "squaremap-simpleclans";

    private final @NotNull Squaremap squaremap;
    private final @NotNull JavaPlugin plugin;
    private final @NotNull Set<String> registeredIconNames = new HashSet<>();
    private final @NotNull String defaultIconName;

    /**
     * Loads all PNG icons from the given plugin data-folder subdirectory and
     * registers them with squaremap's icon registry.
     *
     * @param plugin          plugin instance
     * @param workingPath     subdirectory path relative to plugin data folder (e.g. "/images/clanhome")
     * @param defaultIconName name (without .png) of the fallback icon
     * @param squaremap       squaremap API instance
     */
    public IconStorage(@NotNull JavaPlugin plugin, @NotNull String workingPath,
                       @NotNull String defaultIconName, @NotNull Squaremap squaremap) {
        this.plugin = plugin;
        this.squaremap = squaremap;
        this.defaultIconName = defaultIconName;

        if (!isValidPath(workingPath)) {
            throw new IllegalArgumentException("Invalid workingPath: " + workingPath);
        }

        // Ensure the default icon is registered, falling back to the bundled resource
        if (!registerFromDataFolder(defaultIconName, workingPath)) {
            registerFromResource(defaultIconName, workingPath + "/" + defaultIconName + ".png");
        }

        // Register every .png in the data-folder directory
        File dir = new File(plugin.getDataFolder(), workingPath);
        File[] files = dir.listFiles(f -> f.getName().endsWith(".png"));
        if (files != null) {
            for (File f : files) {
                String name = stripExtension(f.getName());
                if (!name.equals(defaultIconName)) {
                    registerFromDataFolder(name, workingPath);
                }
            }
        }
    }

    /** Returns the squaremap Key for the given icon name, or the default if not found. */
    public @NotNull Key getIconKey(@Nullable String iconName) {
        if (iconName != null && registeredIconNames.contains(iconName.toLowerCase())) {
            return Key.of(NAMESPACE,iconName.toLowerCase());
        }
        return Key.of(NAMESPACE,defaultIconName);
    }

    public @NotNull String getDefaultIconName() { return defaultIconName; }

    public boolean has(@NotNull String iconName) {
        return registeredIconNames.contains(iconName.toLowerCase());
    }

    public @NotNull Set<String> getIconNames() {
        return Collections.unmodifiableSet(registeredIconNames);
    }

    // -- private helpers --

    private boolean registerFromDataFolder(String name, String workingPath) {
        File file = new File(plugin.getDataFolder(), workingPath + "/" + name + ".png");
        if (!file.exists()) return false;
        try (FileInputStream fis = new FileInputStream(file)) {
            return registerImage(name, fis);
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not read icon file " + file.getName() + ": " + ex.getMessage());
            return false;
        }
    }

    private void registerFromResource(String name, String resourcePath) {
        try (InputStream is = plugin.getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                plugin.getLogger().warning("Built-in resource not found: " + resourcePath);
                return;
            }
            registerImage(name, is);
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not read resource " + resourcePath + ": " + ex.getMessage());
        }
    }

    private boolean registerImage(String name, InputStream stream) throws IOException {
        BufferedImage img = ImageIO.read(stream);
        if (img == null) {
            plugin.getLogger().warning("ImageIO could not decode icon: " + name);
            return false;
        }
        String safeName = name.toLowerCase();
        Key key = Key.of(NAMESPACE,safeName);
        squaremap.iconRegistry().register(key, img);
        registeredIconNames.add(safeName);
        return true;
    }

    private static boolean isValidPath(String path) {
        try { Paths.get(path); } catch (InvalidPathException ex) { return false; }
        return true;
    }

    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(0, dot).toLowerCase() : filename.toLowerCase();
    }

    public enum DefaultIcons {
        CLANHOME("clanhome", "images/clanhome/clanhome.png"),
        BLOOD("blood", "images/blood.png");

        private final String name;
        private final String path;

        DefaultIcons(String name, String path) {
            this.name = name;
            this.path = path;
        }

        public String getPath() { return path; }
        public String getName() { return name; }
    }
}
