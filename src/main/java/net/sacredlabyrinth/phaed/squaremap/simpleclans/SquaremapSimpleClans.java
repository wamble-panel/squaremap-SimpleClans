package net.sacredlabyrinth.phaed.squaremap.simpleclans;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import net.sacredlabyrinth.phaed.simpleclans.managers.ClanManager;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.command.ClanMapCommand;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.config.Messages;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.config.Settings;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.layer.HomesLayer;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.layer.KillsLayer;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.layer.LandsLayer;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.layer.MapLayer;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.listener.ClanListener;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.listener.DiplomacyListener;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.menu.MenuListener;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.render.Tooltips;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.api.SquaremapProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public final class SquaremapSimpleClans extends JavaPlugin {

    /** Delay before an event-triggered refresh, so bursts of changes cost one rebuild. */
    private static final long REFRESH_DEBOUNCE_TICKS = 40L;

    private SimpleClans simpleClans;
    private Squaremap squaremap;

    private Settings settings;
    private Messages messages;
    private IconRegistry icons;
    private Tooltips tooltips;

    private @Nullable HomesLayer homes;
    private @Nullable LandsLayer lands;
    private @Nullable KillsLayer kills;
    private @Nullable BukkitTask pendingRefresh;
    private boolean pendingLands;

    @Override
    public void onEnable() {
        migrateOldConfig();
        saveDefaultConfig();

        simpleClans = (SimpleClans) getServer().getPluginManager().getPlugin("SimpleClans");
        squaremap = SquaremapProvider.get();

        load();

        getServer().getPluginManager().registerEvents(new ClanListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        try {
            getServer().getPluginManager().registerEvents(new DiplomacyListener(this), this);
        } catch (LinkageError error) {
            getLogger().info("This SimpleClans version lacks some events; ally/rival/war changes will show "
                    + "on the next periodic refresh instead of instantly.");
        }
        ClanMapCommand.register(this);
    }

    @Override
    public void onDisable() {
        unload();
    }

    /** Re-reads config.yml and rebuilds every layer. */
    public void reload() {
        reloadConfig();
        unload();
        load();
    }

    private void load() {
        settings = Settings.load(getConfig(), getLogger());
        messages = new Messages(getConfig().getConfigurationSection("messages"));

        icons = new IconRegistry(this, squaremap);
        icons.load(settings.homes().defaultIcon());

        tooltips = new Tooltips(settings.tooltip(), clanManager(), clan -> lands != null ? lands.territory(clan) : null);

        if (settings.lands().layer().enabled()) {
            lands = enable(new LandsLayer(this, settings.lands()));
        }
        if (settings.homes().layer().enabled()) {
            homes = enable(new HomesLayer(this, settings.homes()));
        }
        if (settings.kills().layer().enabled()) {
            kills = enable(new KillsLayer(this, settings.kills()));
        }
    }

    private <T extends MapLayer> @Nullable T enable(T layer) {
        try {
            layer.enable();
            return layer;
        } catch (RuntimeException ex) {
            getLogger().log(Level.SEVERE, "Could not enable a map layer", ex);
            layer.disable();
            return null;
        }
    }

    private void unload() {
        MenuListener.closeAll(this);
        if (pendingRefresh != null) {
            pendingRefresh.cancel();
            pendingRefresh = null;
        }
        layers().forEach(MapLayer::disable);
        homes = null;
        lands = null;
        kills = null;
        if (icons != null) {
            icons.unregisterAll();
        }
    }

    // -- updates -----------------------------------------------------------

    /** Re-renders one clan now (home icon) and its territory soon. */
    public void updateClan(@NotNull Clan clan) {
        if (homes != null) {
            homes.update(clan);
        }
        requestRefresh(true);
    }

    public void removeClan(@NotNull String tag) {
        if (homes != null) {
            homes.remove(tag);
        }
        if (lands != null) {
            lands.remove(tag);
        }
    }

    public void refreshHomes() {
        if (homes != null) {
            homes.refresh();
        }
    }

    /**
     * Schedules a rebuild shortly. Home tooltips are cheap; territory asks every protection
     * plugin for land, so only pass {@code includeLands} when land ownership may have changed.
     */
    public void requestRefresh(boolean includeLands) {
        pendingLands |= includeLands;
        if (pendingRefresh != null) {
            return;
        }
        pendingRefresh = getServer().getScheduler().runTaskLater(this, () -> {
            pendingRefresh = null;
            if (pendingLands && lands != null) {
                lands.refresh(); // also refreshes homes
            } else {
                refreshHomes();
            }
            pendingLands = false;
        }, REFRESH_DEBOUNCE_TICKS);
    }

    public void worldLoaded(@NotNull World world) {
        // squaremap sets the world up on the same event; give it a tick
        getServer().getScheduler().runTask(this, () -> {
            for (MapLayer layer : layers()) {
                layer.attach(world);
                layer.refresh();
            }
        });
    }

    public void worldUnloaded(@NotNull World world) {
        layers().forEach(layer -> layer.detach(world));
    }

    private List<MapLayer> layers() {
        List<MapLayer> layers = new ArrayList<>(3);
        if (lands != null) layers.add(lands);
        if (homes != null) layers.add(homes);
        if (kills != null) layers.add(kills);
        return layers;
    }

    /** 1.x configs have a different layout; keep them for reference and start fresh. */
    private void migrateOldConfig() {
        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            return;
        }
        if (YamlConfiguration.loadConfiguration(file).getInt("config-version", 1) < Settings.CONFIG_VERSION) {
            File backup = new File(getDataFolder(), "config-v1.yml");
            if (file.renameTo(backup)) {
                getLogger().warning("Your config.yml was from squaremap-SimpleClans 1.x and has been replaced. "
                        + "The old file was saved as config-v1.yml.");
            }
        }
    }

    public void debug(@NotNull String message) {
        if (settings != null && settings.debug()) {
            getLogger().info("[debug] " + message);
        }
    }

    // -- accessors ---------------------------------------------------------

    public @NotNull SimpleClans simpleClans() {
        return simpleClans;
    }

    public @NotNull ClanManager clanManager() {
        return simpleClans.getClanManager();
    }

    public @NotNull Squaremap squaremap() {
        return squaremap;
    }

    public @NotNull Settings settings() {
        return settings;
    }

    public @NotNull Messages messages() {
        return messages;
    }

    public @NotNull IconRegistry icons() {
        return icons;
    }

    public @NotNull Tooltips tooltips() {
        return tooltips;
    }

    public @Nullable HomesLayer homes() {
        return homes;
    }

    public @Nullable KillsLayer kills() {
        return kills;
    }
}
