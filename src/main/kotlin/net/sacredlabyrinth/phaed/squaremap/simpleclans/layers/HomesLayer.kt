package net.sacredlabyrinth.phaed.squaremap.simpleclans.layers

import com.google.gson.JsonParser
import net.sacredlabyrinth.phaed.squaremap.simpleclans.Helper
import net.sacredlabyrinth.phaed.squaremap.simpleclans.IconStorage
import net.sacredlabyrinth.phaed.squaremap.simpleclans.SquaremapSimpleClans
import net.sacredlabyrinth.phaed.simpleclans.Clan
import net.sacredlabyrinth.phaed.simpleclans.managers.ClanManager
import xyz.jpenilla.squaremap.api.Point
import xyz.jpenilla.squaremap.api.Squaremap
import xyz.jpenilla.squaremap.api.marker.Marker
import xyz.jpenilla.squaremap.api.marker.MarkerOptions

class HomesLayer(
    private val clanManager: ClanManager,
    val iconStorage: IconStorage,
    config: LayerConfig,
    squaremap: Squaremap
) : Layer("homes", config, squaremap) {

    init {
        // Populate all existing clan homes on startup
        clansWithHome().forEach(::upsertMarker)
    }

    fun upsertMarker(clan: Clan) {
        val tag = clan.tag
        val loc = clan.homeLocation ?: return
        val world = loc.world ?: return

        val provider = providerFor(world)
        if (provider == null) {
            SquaremapSimpleClans.debug("[HomesLayer] No squaremap layer for world '${world.name}' – is it enabled in squaremap?")
            return
        }

        if (isHidden(tag, world.name)) {
            SquaremapSimpleClans.debug("[HomesLayer] Skipping hidden clan '$tag' or world '${world.name}'")
            provider.removeMarker(markerKey(tag))
            return
        }

        // Resolve custom icon stored in clan flags JSON
        val customIcon = runCatching {
            JsonParser.parseString(clan.flags).asJsonObject.get("defaulticon")?.asString
        }.getOrNull()
        val iconKey = iconStorage.getIconKey(customIcon)

        val hoverTooltip = Helper.buildClanHoverTooltip(clan)
        val clickTooltip = Helper.getClanLabel(config, clan)

        val options = MarkerOptions.builder()
            .hoverTooltip(hoverTooltip)
            .clickTooltip(clickTooltip)
            .build()

        val marker = Marker.icon(Point.of(loc.x, loc.z), iconKey, 32, 32)
            .markerOptions(options)

        provider.addMarker(markerKey(tag), marker)
    }

    fun deleteMarker(clanTag: String) {
        allProviders().forEach { it.removeMarker(markerKey(clanTag)) }
    }

    private fun isHidden(tag: String, worldName: String): Boolean {
        val hidden = config.getStringList("hidden-markers")
        return hidden.contains(tag) || hidden.contains("world:$worldName")
    }

    private fun clansWithHome(): List<Clan> =
        clanManager.clans.filter { it.homeLocation?.world != null }
}
