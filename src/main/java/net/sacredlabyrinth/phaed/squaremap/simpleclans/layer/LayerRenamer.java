package net.sacredlabyrinth.phaed.squaremap.simpleclans.layer;

import net.sacredlabyrinth.phaed.squaremap.simpleclans.SquaremapSimpleClans;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.LayerProvider;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.Pair;
import xyz.jpenilla.squaremap.api.Registry;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.api.marker.Marker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Renames layers that other plugins add to squaremap (e.g. a claims addon whose layer is
 * called "HuskClaims"), as set under {@code rename-layers} in config.yml.
 *
 * <p>squaremap reads a layer's name from its provider, so the provider is swapped for a
 * wrapper that reports the new name and passes everything else, markers included, straight
 * through. The other plugin keeps updating its own markers as normal. Wrappers are swapped
 * back for the originals when this plugin disables, so nothing of ours stays inside squaremap.
 */
public final class LayerRenamer {

    /** Other plugins may register their layers late, or again after a reload. */
    private static final long RECHECK_TICKS = 20L * 30;

    private final SquaremapSimpleClans plugin;
    /** Current name or layer key, lower-cased, to the new name. */
    private final Map<String, String> renames;
    private final Map<WorldIdentifier, Map<Key, Renamed>> applied = new HashMap<>();
    private @Nullable BukkitTask task;

    public LayerRenamer(@NotNull SquaremapSimpleClans plugin, @NotNull Map<String, String> renames) {
        this.plugin = plugin;
        this.renames = new HashMap<>();
        renames.forEach((from, to) -> this.renames.put(from.trim().toLowerCase(Locale.ROOT), to));
    }

    public void enable() {
        if (renames.isEmpty()) {
            return;
        }
        // first pass on the next tick, once every plugin has registered its layers
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::apply, 1L, RECHECK_TICKS);
    }

    public void disable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        applied.forEach((world, layers) -> plugin.squaremap().getWorldIfEnabled(world).ifPresent(mapWorld ->
                layers.forEach((key, renamed) -> {
                    Registry<LayerProvider> registry = mapWorld.layerRegistry();
                    try {
                        if (registry.hasEntry(key) && registry.get(key) == renamed) {
                            registry.unregister(key);
                            registry.register(key, renamed.delegate);
                        }
                    } catch (RuntimeException ignored) {
                        // squaremap or the other plugin is already shutting down
                    }
                })));
        applied.clear();
    }

    /** Wraps every matching layer that isn't wrapped yet. */
    public void apply() {
        for (MapWorld mapWorld : plugin.squaremap().mapWorlds()) {
            Registry<LayerProvider> registry = mapWorld.layerRegistry();
            List<Pair<Key, LayerProvider>> entries = new ArrayList<>();
            registry.entries().forEach(entries::add);

            for (Pair<Key, LayerProvider> entry : entries) {
                LayerProvider provider = entry.right();
                if (provider instanceof Renamed) {
                    continue;
                }
                String name = newName(entry.left(), provider);
                if (name == null) {
                    continue;
                }
                Renamed renamed = new Renamed(provider, name);
                registry.unregister(entry.left());
                registry.register(entry.left(), renamed);
                applied.computeIfAbsent(mapWorld.identifier(), w -> new HashMap<>()).put(entry.left(), renamed);
                plugin.debug("Renamed squaremap layer '" + entry.left() + "' to '" + name + "'");
            }
        }
    }

    private @Nullable String newName(Key key, LayerProvider provider) {
        String byKey = renames.get(key.getKey().toLowerCase(Locale.ROOT));
        if (byKey != null) {
            return byKey;
        }
        String label;
        try {
            label = provider.getLabel();
        } catch (RuntimeException ex) {
            return null;
        }
        return renames.get(label.trim().toLowerCase(Locale.ROOT));
    }

    /** Another plugin's layer under a different name; everything else is passed through. */
    private record Renamed(LayerProvider delegate, String label) implements LayerProvider {

        @Override
        public @NotNull String getLabel() {
            return label;
        }

        @Override
        public boolean showControls() {
            return delegate.showControls();
        }

        @Override
        public boolean defaultHidden() {
            return delegate.defaultHidden();
        }

        @Override
        public int layerPriority() {
            return delegate.layerPriority();
        }

        @Override
        public int zIndex() {
            return delegate.zIndex();
        }

        @Override
        public @NotNull Collection<Marker> getMarkers() {
            return delegate.getMarkers();
        }
    }
}
