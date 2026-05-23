package net.sacredlabyrinth.phaed.squaremap.simpleclans.layers

import net.sacredlabyrinth.phaed.squaremap.simpleclans.Helper
import net.sacredlabyrinth.phaed.squaremap.simpleclans.Helper.HEXColor
import net.sacredlabyrinth.phaed.squaremap.simpleclans.SquaremapSimpleClans
import net.sacredlabyrinth.phaed.simpleclans.Clan
import net.sacredlabyrinth.phaed.simpleclans.hooks.protection.Coordinate
import net.sacredlabyrinth.phaed.simpleclans.hooks.protection.Land
import net.sacredlabyrinth.phaed.simpleclans.managers.ClanManager
import net.sacredlabyrinth.phaed.simpleclans.managers.ProtectionManager
import xyz.jpenilla.squaremap.api.Point
import xyz.jpenilla.squaremap.api.Squaremap
import xyz.jpenilla.squaremap.api.marker.Marker
import xyz.jpenilla.squaremap.api.marker.MarkerOptions
import java.awt.Color

class LandsLayer(
    clanManager: ClanManager,
    private val protectionManager: ProtectionManager,
    config: LayerConfig,
    squaremap: Squaremap
) : Layer("lands", config, squaremap) {

    init {
        clanManager.clans.forEach(::upsertMarker)
    }

    fun upsertMarker(clan: Clan) {
        val tag = clan.tag
        val loc = clan.homeLocation ?: return
        val world = loc.world ?: return

        val provider = providerFor(world)
        if (provider == null) {
            SquaremapSimpleClans.debug("[LandsLayer] No squaremap layer for world '${world.name}'")
            return
        }

        if (isHidden(tag)) {
            SquaremapSimpleClans.debug("[LandsLayer] Skipping hidden clan '$tag'")
            provider.removeMarker(markerKey(tag))
            return
        }

        val lands = protectionManager.getLandsAt(loc)
        val coordinates = lands.flatMap { it.coordinates }

        if (coordinates.isEmpty()) {
            SquaremapSimpleClans.debug("[LandsLayer] No land coordinates for clan '$tag'")
            return
        }

        val points = coordinates.map { Point.of(it.x, it.z) }

        val fillColor = resolveFillColor(clan)
        val lineColor = resolveLineColor(clan)

        val fillOpacity = config.getDouble("style.fill.opacity", 0.35)
        val lineOpacity = config.getDouble("style.line.opacity", 0.8)
        val lineWeight = config.getInt("style.line.weight", 3)

        val tooltip = Helper.buildLandHoverTooltip(clan)
        val clickLabel = Helper.getClanLabel(config, clan)

        val options = MarkerOptions.builder()
            .hoverTooltip(tooltip)
            .clickTooltip(clickLabel)
            .fill(true)
            .fillColor(fillColor)
            .fillOpacity(fillOpacity)
            .stroke(true)
            .strokeColor(lineColor)
            .strokeOpacity(lineOpacity)
            .strokeWeight(lineWeight)
            .build()

        val marker = Marker.polygon(points).markerOptions(options)
        provider.addMarker(markerKey(tag), marker)
    }

    fun deleteMarker(clanTag: String) {
        allProviders().forEach { it.removeMarker(markerKey(clanTag)) }
    }

    private fun resolveFillColor(clan: Clan): Color {
        if (config.getBoolean("style.based-on-tag", false)) {
            return hexToColor(HEXColor.of(clan.color).code)
        }
        return hexToColor(config.getString("style.fill.color", "#57b356"))
    }

    private fun resolveLineColor(clan: Clan): Color {
        if (config.getBoolean("style.based-on-tag", false)) {
            return hexToColor(HEXColor.of(clan.color).code)
        }
        return hexToColor(config.getString("style.line.color", "#2d682d"))
    }

    private fun hexToColor(hex: String): Color {
        return try {
            val rgb = hex.trimStart('#').toInt(16)
            Color((rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF)
        } catch (e: NumberFormatException) {
            Color(0x57, 0xb3, 0x56)
        }
    }

    private fun isHidden(tag: String) = config.getStringList("hidden-lands").contains(tag)
}
