package net.sacredlabyrinth.phaed.squaremap.simpleclans.layers

import net.sacredlabyrinth.phaed.squaremap.simpleclans.layers.LayerConfig.LayerField.*
import xyz.jpenilla.squaremap.api.BukkitAdapter
import xyz.jpenilla.squaremap.api.Key
import xyz.jpenilla.squaremap.api.SimpleLayerProvider
import xyz.jpenilla.squaremap.api.Squaremap
import xyz.jpenilla.squaremap.api.WorldIdentifier
import org.bukkit.World

abstract class Layer(
    private val layerKey: String,
    protected val config: LayerConfig,
    protected val squaremap: Squaremap
) {
    companion object {
        const val NAMESPACE = "squaremap-simpleclans"
    }

    /** Providers keyed by world identifier string for O(1) lookup. */
    private val providers = mutableMapOf<WorldIdentifier, SimpleLayerProvider>()

    init {
        check(config.getBoolean(ENABLE)) { "Layer $layerKey is disabled!" }
        // Register on every currently enabled squaremap world
        squaremap.mapWorlds().forEach { registerOnWorld(it.identifier()) }
    }

    /** Removes this layer from every world and clears internal state. */
    fun cleanup() {
        providers.forEach { (worldId, _) ->
            squaremap.getWorldIfEnabled(worldId).ifPresent { mapWorld ->
                runCatching { mapWorld.layerRegistry().unregister(key()) }
            }
        }
        providers.clear()
    }

    protected fun key(): Key = Key.of("${NAMESPACE}_$layerKey")

    protected fun markerKey(id: String): Key = Key.of("${NAMESPACE}_${layerKey}_$id")

    /** Returns the SimpleLayerProvider for the given Bukkit World, or null if not registered. */
    protected fun providerFor(world: World): SimpleLayerProvider? =
        providers[BukkitAdapter.worldIdentifier(world)]

    protected fun allProviders(): Collection<SimpleLayerProvider> = providers.values

    private fun registerOnWorld(worldId: WorldIdentifier) {
        squaremap.getWorldIfEnabled(worldId).ifPresent { mapWorld ->
            val provider = SimpleLayerProvider.builder(config.getString(LABEL))
                .showControls(true)
                .defaultHidden(config.getBoolean(HIDDEN))
                .layerPriority(config.getInt(PRIORITY))
                .zIndex(500)
                .build()
            mapWorld.layerRegistry().register(key(), provider)
            providers[worldId] = provider
        }
    }
}
