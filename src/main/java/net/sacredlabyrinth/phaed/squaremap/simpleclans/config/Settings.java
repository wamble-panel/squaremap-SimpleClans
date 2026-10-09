package net.sacredlabyrinth.phaed.squaremap.simpleclans.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/** Immutable, validated view of config.yml. Rebuilt on every reload. */
public record Settings(boolean debug, Tooltip tooltip, Homes homes, Lands lands, Kills kills, Menu menu) {

    public static final int CONFIG_VERSION = 2;

    /** Rows a clan popup can show, in the order configured. */
    public enum Row { LEADERS, MEMBERS, KDR, KILLS, ALLIES, RIVALS, WARS, TERRITORY, FOUNDED, DESCRIPTION }

    public enum KillType { CIVILIAN, NEUTRAL, RIVAL, ALLY, WAR }

    public enum TerritoryOwners { LEADERS, MEMBERS }

    public record Layer(boolean enabled, String label, boolean showControls, boolean defaultHidden,
                        int layerPriority, int zIndex, int updateIntervalSeconds, Set<String> hiddenWorlds) {

        public boolean isWorldHidden(@NotNull String worldName) {
            return hiddenWorlds.contains(worldName.toLowerCase(Locale.ROOT));
        }
    }

    public record Tooltip(List<Row> rows, int maxListed, String headUrl, DateTimeFormatter dateFormat,
                          Map<String, String> labels) {

        public @NotNull String label(@NotNull String key) {
            return labels.getOrDefault(key, key);
        }
    }

    public record Homes(Layer layer, int iconSize, String defaultIcon, Set<String> hiddenClans) {

        public boolean isClanHidden(@NotNull String tag) {
            return hiddenClans.contains(tag.toLowerCase(Locale.ROOT));
        }
    }

    public record Lands(Layer layer, TerritoryOwners owners, Set<String> hiddenClans, boolean colorFromTag,
                        int fillColor, double fillOpacity, int strokeColor, double strokeOpacity, int strokeWeight) {

        public boolean isClanHidden(@NotNull String tag) {
            return hiddenClans.contains(tag.toLowerCase(Locale.ROOT));
        }
    }

    public record Kills(Layer layer, int durationSeconds, int maxMarkers, int iconSize,
                        DateTimeFormatter timeFormat, Set<KillType> types) {
    }

    /** The /clanmap icon picker. Banner icons always show as their banner item. */
    public record Menu(boolean showLocked, Material customIconItem, Map<String, Material> items) {

        public @NotNull Material itemFor(@NotNull String icon) {
            return items.getOrDefault(icon, customIconItem);
        }
    }

    // -- loading -----------------------------------------------------------

    public static @NotNull Settings load(@NotNull ConfigurationSection root, @NotNull Logger logger) {
        Reader r = new Reader(logger);
        ConfigurationSection layers = r.section(root, "layers");

        ConfigurationSection homes = r.section(layers, "homes");
        ConfigurationSection lands = r.section(layers, "lands");
        ConfigurationSection landsStyle = r.section(lands, "style");
        ConfigurationSection kills = r.section(layers, "kills");

        return new Settings(
                root.getBoolean("debug", false),
                r.tooltip(r.section(root, "tooltip")),
                new Homes(
                        r.layer(homes, "Clan Homes", 20, 30),
                        clamp(homes.getInt("icon-size", 32), 8, 128),
                        homes.getString("default-icon", "banner_red").toLowerCase(Locale.ROOT),
                        lowerSet(homes.getStringList("hidden-clans"))),
                new Lands(
                        r.layer(lands, "Clan Territory", 10, 300),
                        r.enumValue(lands, "territory-owners", TerritoryOwners.LEADERS),
                        lowerSet(lands.getStringList("hidden-clans")),
                        landsStyle.getBoolean("color-from-tag", true),
                        r.color(landsStyle, "fill-color", 0x57B356),
                        clamp(landsStyle.getDouble("fill-opacity", 0.25), 0, 1),
                        r.color(landsStyle, "stroke-color", 0x2D682D),
                        clamp(landsStyle.getDouble("stroke-opacity", 0.9), 0, 1),
                        clamp(landsStyle.getInt("stroke-weight", 2), 0, 20)),
                new Kills(
                        r.layer(kills, "Recent Kills", 30, 0),
                        Math.max(10, kills.getInt("duration-seconds", 300)),
                        clamp(kills.getInt("max-markers", 100), 1, 1000),
                        clamp(kills.getInt("icon-size", 16), 8, 128),
                        r.formatter(kills, "time-format", "HH:mm"),
                        r.enumSet(kills, "types", KillType.class)),
                r.menu(r.section(root, "menu")));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Set<String> lowerSet(List<String> values) {
        Set<String> out = new HashSet<>();
        values.forEach(v -> out.add(v.toLowerCase(Locale.ROOT)));
        return Set.copyOf(out);
    }

    /** Reads values leniently, warning about (and replacing) anything invalid. */
    private record Reader(Logger logger) {

        ConfigurationSection section(ConfigurationSection parent, String path) {
            ConfigurationSection section = parent.getConfigurationSection(path);
            return section != null ? section : new MemoryConfiguration();
        }

        Layer layer(ConfigurationSection s, String label, int priority, int interval) {
            return new Layer(
                    s.getBoolean("enabled", true),
                    s.getString("label", label),
                    s.getBoolean("show-controls", true),
                    s.getBoolean("default-hidden", false),
                    s.getInt("layer-priority", priority),
                    s.getInt("z-index", priority),
                    Math.max(0, s.getInt("update-interval-seconds", interval)),
                    lowerSet(s.getStringList("hidden-worlds")));
        }

        Tooltip tooltip(ConfigurationSection s) {
            List<Row> rows = new ArrayList<>();
            List<String> configured = s.isList("rows") ? s.getStringList("rows")
                    : List.of("leaders", "members", "kdr", "allies", "rivals", "wars", "territory", "description");
            for (String name : configured) {
                try {
                    rows.add(Row.valueOf(name.trim().toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ex) {
                    logger.warning("Unknown tooltip row '" + name + "', ignoring it.");
                }
            }

            Map<String, String> labels = new HashMap<>(DEFAULT_LABELS);
            ConfigurationSection labelSection = s.getConfigurationSection("labels");
            if (labelSection != null) {
                for (String key : labelSection.getKeys(false)) {
                    labels.put(key, labelSection.getString(key, labels.getOrDefault(key, key)));
                }
            }

            return new Tooltip(List.copyOf(rows),
                    clamp(s.getInt("max-listed", 5), 1, 50),
                    s.getString("head-url", "https://mc-heads.net/avatar/{uuid}/16"),
                    formatter(s, "date-format", "d MMM yyyy"),
                    Map.copyOf(labels));
        }

        Menu menu(ConfigurationSection s) {
            Map<String, Material> items = new HashMap<>();
            ConfigurationSection itemSection = s.getConfigurationSection("items");
            if (itemSection != null) {
                for (String icon : itemSection.getKeys(false)) {
                    Material material = material(itemSection, icon, null);
                    if (material != null) {
                        items.put(icon.toLowerCase(Locale.ROOT), material);
                    }
                }
            }
            return new Menu(s.getBoolean("show-locked", true),
                    material(s, "custom-icon-item", Material.PAINTING),
                    Map.copyOf(items));
        }

        @Contract("_, _, !null -> !null")
        @Nullable Material material(ConfigurationSection s, String path, @Nullable Material def) {
            String value = s.getString(path);
            if (value == null) {
                return def;
            }
            Material material = Material.matchMaterial(value);
            if (material == null || !material.isItem() || material.isAir()) {
                logger.warning("'" + value + "' at " + path + " isn't an item, using "
                        + (def != null ? def.name() : "nothing") + ".");
                return def;
            }
            return material;
        }

        DateTimeFormatter formatter(ConfigurationSection s, String path, String def) {
            String pattern = s.getString(path, def);
            try {
                return DateTimeFormatter.ofPattern(pattern, Locale.ROOT);
            } catch (IllegalArgumentException ex) {
                logger.warning("Invalid " + path + " '" + pattern + "', using '" + def + "'.");
                return DateTimeFormatter.ofPattern(def, Locale.ROOT);
            }
        }

        int color(ConfigurationSection s, String path, int def) {
            String value = s.getString(path);
            if (value == null) {
                return def;
            }
            try {
                return Integer.parseInt(value.trim().replaceFirst("^#", ""), 16) & 0xFFFFFF;
            } catch (NumberFormatException ex) {
                logger.warning("Invalid colour '" + value + "' for " + path + ", expected #RRGGBB.");
                return def;
            }
        }

        <E extends Enum<E>> E enumValue(ConfigurationSection s, String path, E def) {
            String value = s.getString(path);
            if (value == null) {
                return def;
            }
            try {
                return Enum.valueOf(def.getDeclaringClass(), value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                logger.warning("Invalid " + path + " '" + value + "', using '" + def.name().toLowerCase(Locale.ROOT) + "'.");
                return def;
            }
        }

        <E extends Enum<E>> Set<E> enumSet(ConfigurationSection s, String path, Class<E> type) {
            if (!s.isList(path)) {
                return EnumSet.allOf(type);
            }
            EnumSet<E> out = EnumSet.noneOf(type);
            for (String value : s.getStringList(path)) {
                try {
                    out.add(Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ex) {
                    logger.warning("Unknown value '" + value + "' in " + path + ", ignoring it.");
                }
            }
            return out;
        }
    }

    private static final Map<String, String> DEFAULT_LABELS = Map.ofEntries(
            Map.entry("leaders", "Leaders"),
            Map.entry("members", "Members"),
            Map.entry("online", "online"),
            Map.entry("kdr", "KDR"),
            Map.entry("kills", "Kills"),
            Map.entry("deaths", "deaths"),
            Map.entry("allies", "Allies"),
            Map.entry("rivals", "Rivals"),
            Map.entry("wars", "At war"),
            Map.entry("territory", "Territory"),
            Map.entry("claim", "claim"),
            Map.entry("claims", "claims"),
            Map.entry("blocks", "blocks"),
            Map.entry("founded", "Founded"),
            Map.entry("more", "+{count} more"),
            Map.entry("kill-civilian", "Civilian kill"),
            Map.entry("kill-neutral", "Kill"),
            Map.entry("kill-rival", "Rival kill"),
            Map.entry("kill-ally", "Ally kill"),
            Map.entry("kill-war", "War kill"));
}
