package net.sacredlabyrinth.phaed.squaremap.simpleclans;

import net.sacredlabyrinth.phaed.squaremap.simpleclans.IconStorage.DefaultIcons;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.layers.HomesLayer;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.layers.KillsLayer;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.layers.LandsLayer;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.layers.LayerConfig;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.managers.CommandManager;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import net.sacredlabyrinth.phaed.simpleclans.managers.ClanManager;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.api.SquaremapProvider;

import java.io.File;
import java.util.Objects;

import static org.bukkit.Bukkit.getPluginManager;
import static org.bukkit.Bukkit.getScheduler;

public class SquaremapSimpleClans extends JavaPlugin {

    private static SquaremapSimpleClans instance;

    private Squaremap squaremapApi;
    private SimpleClans simpleclans;

    private @Nullable HomesLayer homesLayer;
    private @Nullable KillsLayer killsLayer;
    private @Nullable LandsLayer landsLayer;

    public static SquaremapSimpleClans getInstance() { return instance; }

    public static String lang(@NotNull String key) {
        String msg = instance.getConfig().getString("language." + key);
        return msg == null
                ? "Missing language key: " + key
                : ChatColor.translateAlternateColorCodes('&', msg);
    }

    public static void debug(String message, boolean respectConfig) {
        if (respectConfig && !instance.getConfig().getBoolean("debug", false)) return;
        instance.getLogger().info("[Debug] " + message);
    }

    public static void debug(String message) { debug(message, true); }

    // -------------------------------------------------------------------------

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        if (reload()) {
            new CommandManager(this);
            getPluginManager().registerEvents(new SquaremapSimpleClansListener(this), this);
        }
    }

    @Override
    public void onDisable() {
        cleanupLayers();
    }

    public boolean reload() {
        reloadConfig();
        cleanupLayers();

        try {
            loadDependencies();
        } catch (IllegalStateException ex) {
            getLogger().severe(ex.getMessage());
            getPluginLoader().disablePlugin(this);
            return false;
        }

        saveDefaultImages();
        loadLayers();
        return true;
    }

    private void loadDependencies() {
        try {
            squaremapApi = SquaremapProvider.get();
        } catch (IllegalStateException ex) {
            throw new IllegalStateException("Squaremap is not available. Make sure squaremap is loaded first.", ex);
        }

        simpleclans = (SimpleClans) getPluginManager().getPlugin("SimpleClans");
        if (simpleclans == null) {
            throw new IllegalStateException("SimpleClans was not found, disabling...");
        }
    }

    private void loadLayers() {
        ConfigurationSection homesSection = Objects.requireNonNull(
                getConfig().getConfigurationSection("layer.homes"));
        ConfigurationSection killsSection = Objects.requireNonNull(
                getConfig().getConfigurationSection("layer.kills"));
        ConfigurationSection landsSection = Objects.requireNonNull(
                getConfig().getConfigurationSection("layer.lands"));

        String defaultHomeIcon = homesSection.getString("default-icon", DefaultIcons.CLANHOME.getName());

        IconStorage homesIcons = new IconStorage(this, "images/clanhome", defaultHomeIcon, squaremapApi);
        IconStorage killsIcons = new IconStorage(this, "images", DefaultIcons.BLOOD.getName(), squaremapApi);

        try {
            homesLayer = new HomesLayer(getClanManager(), homesIcons, new LayerConfig(homesSection), squaremapApi);
        } catch (IllegalStateException ex) {
            debug(ex.getMessage());
        }

        try {
            killsLayer = new KillsLayer(killsIcons, new LayerConfig(killsSection), squaremapApi);
        } catch (IllegalStateException ex) {
            debug(ex.getMessage());
        }

        try {
            // Run on next tick so that ProtectionManager lands coordinates are ready
            getScheduler().runTask(this, () -> {
                try {
                    landsLayer = new LandsLayer(getClanManager(), simpleclans.getProtectionManager(),
                            new LayerConfig(landsSection), squaremapApi);
                } catch (IllegalStateException ex) {
                    debug(ex.getMessage());
                }
            });
        } catch (Exception ex) {
            debug("LandsLayer failed to schedule: " + ex.getMessage());
        }
    }

    private void cleanupLayers() {
        if (homesLayer != null) { homesLayer.cleanup(); homesLayer = null; }
        if (killsLayer != null)  { killsLayer.cleanup();  killsLayer = null; }
        if (landsLayer != null)  { landsLayer.cleanup();  landsLayer = null; }
    }

    private void saveDefaultImages() {
        saveIfMissing(DefaultIcons.CLANHOME.getPath());
        saveIfMissing(DefaultIcons.BLOOD.getPath());
    }

    private void saveIfMissing(String resourcePath) {
        if (!new File(getDataFolder(), resourcePath).exists()) {
            saveResource(resourcePath, false);
        }
    }

    // -------------------------------------------------------------------------

    @NotNull
    public ClanManager getClanManager() { return simpleclans.getClanManager(); }

    @NotNull
    public Squaremap getSquaremapApi() { return squaremapApi; }

    @Nullable
    public HomesLayer getHomesLayer() { return homesLayer; }

    @Nullable
    public KillsLayer getKillsLayer() { return killsLayer; }

    @Nullable
    public LandsLayer getLandsLayer() { return landsLayer; }
}
