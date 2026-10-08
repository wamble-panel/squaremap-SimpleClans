package net.sacredlabyrinth.phaed.squaremap.simpleclans.layer;

import net.sacredlabyrinth.phaed.squaremap.simpleclans.SquaremapSimpleClans;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jpenilla.squaremap.api.BukkitAdapter;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.Point;
import xyz.jpenilla.squaremap.api.SimpleLayerProvider;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.api.marker.Marker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** One squaremap layer, registered on every map world the plugin isn't told to skip. */
public abstract class MapLayer {

    protected final SquaremapSimpleClans plugin;
    protected final Settings.Layer settings;
    private final String id;
    private final Key layerKey;
    private final Map<WorldIdentifier, SimpleLayerProvider> providers = new ConcurrentHashMap<>();
    private @Nullable BukkitTask refreshTask;

    protected MapLayer(@NotNull SquaremapSimpleClans plugin, @NotNull String id, @NotNull Settings.Layer settings) {
        this.plugin = plugin;
        this.settings = settings;
        this.id = id;
        this.layerKey = Key.of("simpleclans_" + id);
    }

    public void enable() {
        for (MapWorld world : plugin.squaremap().mapWorlds()) {
            attach(world);
        }
        // First fill on the next tick: by then every world is mapped and SimpleClans'
        // protection hooks have registered their providers.
        long period = refreshPeriodTicks();
        refreshTask = period > 0
                ? Bukkit.getScheduler().runTaskTimer(plugin, this::refresh, 1L, period)
                : Bukkit.getScheduler().runTask(plugin, this::refresh);
    }

    public void disable() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        for (WorldIdentifier world : providers.keySet()) {
            plugin.squaremap().getWorldIfEnabled(world).ifPresent(mapWorld -> {
                try {
                    mapWorld.layerRegistry().unregister(layerKey);
                } catch (RuntimeException ignored) {
                    // already gone, e.g. squaremap disabled first
                }
            });
        }
        providers.clear();
    }

    /** Called when a world loads after startup. */
    public void attach(@NotNull World world) {
        plugin.squaremap().getWorldIfEnabled(BukkitAdapter.worldIdentifier(world)).ifPresent(this::attach);
    }

    /** Called when a world unloads; squaremap drops the world's layers itself. */
    public void detach(@NotNull World world) {
        providers.remove(BukkitAdapter.worldIdentifier(world));
    }

    /** Rebuilds every marker in this layer from current clan data. Runs on the main thread. */
    public abstract void refresh();

    /** How often {@link #refresh()} runs on its own, in ticks; 0 to only refresh on events. */
    protected long refreshPeriodTicks() {
        return settings.updateIntervalSeconds() * 20L;
    }

    // -- helpers for subclasses --------------------------------------------

    protected @Nullable SimpleLayerProvider provider(@NotNull World world) {
        return providers.get(BukkitAdapter.worldIdentifier(world));
    }

    protected @NotNull Map<WorldIdentifier, SimpleLayerProvider> providers() {
        return providers;
    }

    protected @NotNull Key markerKey(@NotNull String suffix) {
        StringBuilder key = new StringBuilder(id).append('_');
        for (char c : suffix.toCharArray()) {
            boolean valid = c == '_' || c == '-' || c == '.' || (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
            key.append(valid ? c : '_');
        }
        return Key.of(key.toString());
    }

    /** Removes a marker from every world. */
    protected void removeEverywhere(@NotNull Key key) {
        providers.values().forEach(provider -> provider.removeMarker(key));
    }

    /** Puts a marker on {@code world}, removing it from any other world it was on. */
    protected void place(@NotNull World world, @NotNull Key key, @NotNull Marker marker) {
        SimpleLayerProvider target = provider(world);
        providers.values().forEach(provider -> {
            if (provider != target) {
                provider.removeMarker(key);
            }
        });
        if (target != null && !marker.equals(target.registeredMarkers().get(key))) {
            target.addMarker(key, marker);
        }
    }

    /**
     * Makes every provider hold exactly the given markers. Unchanged markers are left alone so
     * squaremap only re-sends a layer to browsers when something in it really changed.
     */
    protected void sync(@NotNull Map<WorldIdentifier, Map<Key, Marker>> desired) {
        providers.forEach((world, provider) -> {
            Map<Key, Marker> wanted = desired.getOrDefault(world, Map.of());
            for (Key key : new ArrayList<>(provider.registeredMarkers().keySet())) {
                if (!wanted.containsKey(key)) {
                    provider.removeMarker(key);
                }
            }
            wanted.forEach((key, marker) -> {
                if (!marker.equals(provider.registeredMarkers().get(key))) {
                    provider.addMarker(key, marker);
                }
            });
        });
    }

    protected static @NotNull Map<WorldIdentifier, Map<Key, Marker>> newDesired() {
        return new HashMap<>();
    }

    protected static void want(@NotNull Map<WorldIdentifier, Map<Key, Marker>> desired, @NotNull World world,
                               @NotNull Key key, @NotNull Marker marker) {
        desired.computeIfAbsent(BukkitAdapter.worldIdentifier(world), w -> new HashMap<>()).put(key, marker);
    }

    /** Centre of the block at {@code location}, in map coordinates. */
    protected static @NotNull Point blockCenter(@NotNull Location location) {
        return Point.of(location.getBlockX() + 0.5, location.getBlockZ() + 0.5);
    }

    protected @NotNull Collection<World> worlds() {
        Collection<World> worlds = new ArrayList<>();
        for (WorldIdentifier id : providers.keySet()) {
            plugin.squaremap().getWorldIfEnabled(id).ifPresent(w -> worlds.add(BukkitAdapter.bukkitWorld(w)));
        }
        return worlds;
    }

    private void attach(MapWorld mapWorld) {
        if (providers.containsKey(mapWorld.identifier())) {
            return;
        }
        World world = Bukkit.getWorld(BukkitAdapter.namespacedKey(mapWorld.identifier()));
        if (world == null || settings.isWorldHidden(world.getName())) {
            return;
        }
        SimpleLayerProvider provider = SimpleLayerProvider.builder(settings.label())
                .showControls(settings.showControls())
                .defaultHidden(settings.defaultHidden())
                .layerPriority(settings.layerPriority())
                .zIndex(settings.zIndex())
                .build();
        if (mapWorld.layerRegistry().hasEntry(layerKey)) {
            mapWorld.layerRegistry().unregister(layerKey);
        }
        mapWorld.layerRegistry().register(layerKey, provider);
        providers.put(mapWorld.identifier(), provider);
    }
}
