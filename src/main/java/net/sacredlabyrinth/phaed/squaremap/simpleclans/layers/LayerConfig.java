package net.sacredlabyrinth.phaed.squaremap.simpleclans.layers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LayerConfig {

    private final @NotNull ConfigurationSection section;

    public LayerConfig(@NotNull ConfigurationSection section) {
        this.section = section;
    }

    public @NotNull List<String> getStringList(@NotNull String key) {
        return section.getStringList(key);
    }

    public double getDouble(@NotNull String key, double def) {
        return section.getDouble(key, def);
    }

    public int getInt(@NotNull LayerField setting) {
        return section.getInt(setting.path, NumberConversions.toInt(setting.def));
    }

    public int getInt(@NotNull String key, int def) {
        return section.getInt(key, def);
    }

    public @NotNull String getString(@NotNull LayerField setting) {
        return section.getString(setting.path, String.valueOf(setting.def));
    }

    public @NotNull String getString(@NotNull String key, @NotNull String def) {
        return section.getString(key, def);
    }

    public boolean getBoolean(@NotNull LayerField setting) {
        return section.getBoolean(setting.path, (Boolean) setting.def);
    }

    public boolean getBoolean(@NotNull String key, boolean def) {
        return section.getBoolean(key, def);
    }

    public enum LayerField {
        ENABLE("enable", true),
        PRIORITY("layer-priority", 1),
        LABEL("label", "Label"),
        FORMAT("format", ""),
        HIDDEN("hide-by-default", false),
        MINZOOM("min-zoom", 0);

        final String path;
        final Object def;

        LayerField(String path, Object def) {
            this.path = path;
            this.def = def;
        }
    }
}
