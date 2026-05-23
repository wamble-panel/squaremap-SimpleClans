package net.sacredlabyrinth.phaed.squaremap.simpleclans.layers

import net.sacredlabyrinth.phaed.squaremap.simpleclans.Helper
import net.sacredlabyrinth.phaed.squaremap.simpleclans.IconStorage
import net.sacredlabyrinth.phaed.squaremap.simpleclans.SquaremapSimpleClans
import net.sacredlabyrinth.phaed.squaremap.simpleclans.layers.LayerConfig.LayerField.FORMAT
import net.sacredlabyrinth.phaed.simpleclans.Kill
import org.bukkit.scheduler.BukkitRunnable
import xyz.jpenilla.squaremap.api.Point
import xyz.jpenilla.squaremap.api.Squaremap
import xyz.jpenilla.squaremap.api.marker.Marker
import xyz.jpenilla.squaremap.api.marker.MarkerOptions
import java.time.format.DateTimeFormatter

class KillsLayer(
    private val iconStorage: IconStorage,
    config: LayerConfig,
    squaremap: Squaremap
) : Layer("kills", config, squaremap) {

    private val timeFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern(config.getString("time-format", "HH:mm:ss"))

    fun createMarker(kill: Kill) {
        val vclan = kill.victim.clan
        val vPlayer = kill.victim.toPlayer() ?: return
        val loc = vPlayer.location
        val world = loc.world ?: return

        if (config.getBoolean("hide.clan-players", false) && vclan != null) return
        if (config.getBoolean("hide.civilians", false) && vclan == null) return

        val provider = providerFor(world) ?: return

        val hoverTooltip = buildKillTooltip(kill)
        val clickTooltip = formatLabel(kill)

        val options = MarkerOptions.builder()
            .hoverTooltip(hoverTooltip)
            .clickTooltip(clickTooltip)
            .build()

        val uniqueKey = markerKey("${kill.time.toEpochSecond(java.time.ZoneOffset.UTC)}_${vPlayer.name}")
        val marker = Marker.icon(Point.of(loc.x, loc.z), iconStorage.getIconKey(iconStorage.defaultIconName), 24, 24)
            .markerOptions(options)

        provider.addMarker(uniqueKey, marker)

        // Auto-expire the marker after visible-seconds
        val visibleTicks = config.getInt("visible-seconds", 300) * 20L
        object : BukkitRunnable() {
            override fun run() { provider.removeMarker(uniqueKey) }
        }.runTaskLater(SquaremapSimpleClans.getInstance(), visibleTicks)
    }

    private fun buildKillTooltip(kill: Kill): String {
        val aClanTag = kill.killer.clan?.tag?.let { Helper.escapeHtml(it) } ?: "-"
        val vClanTag = kill.victim.clan?.tag?.let { Helper.escapeHtml(it) } ?: "-"
        val timeStr = Helper.escapeHtml(kill.time.format(timeFormat))
        val killerName = Helper.escapeHtml(kill.killer.name)
        val victimName = Helper.escapeHtml(kill.victim.name)

        return "<div style='font-family:sans-serif;min-width:180px;'>" +
               "<div style='background:#1a1a2e;color:#e0e0e0;padding:6px 10px;border-radius:6px 6px 0 0;" +
               "border-bottom:2px solid #c0392b;text-align:center;'>" +
               "<b style='color:#e74c3c;'>&#9876;&#65039; Kill Event</b></div>" +
               "<div style='background:#16213e;color:#ddd;padding:8px 10px;border-radius:0 0 6px 6px;line-height:1.8;'>" +
               "<table style='width:100%;text-align:center;border-collapse:collapse;'>" +
               "<tr>" +
               "<td style='color:#e74c3c;padding:2px 6px;'><b>$killerName</b><br><span style='font-size:0.85em;color:#aaa;'>[$aClanTag]</span></td>" +
               "<td style='font-size:1.3em;'>&#9876;&#65039;</td>" +
               "<td style='color:#3498db;padding:2px 6px;'><b>$victimName</b><br><span style='font-size:0.85em;color:#aaa;'>[$vClanTag]</span></td>" +
               "</tr>" +
               "</table>" +
               "<div style='text-align:center;margin-top:4px;font-size:0.85em;color:#aaa;'>&#128336; $timeStr</div>" +
               "</div></div>"
    }

    private fun formatLabel(kill: Kill): String {
        val label = config.getString(FORMAT)
            .replace("{victim}", Helper.escapeHtml(kill.victim.name))
            .replace("{attacker}", Helper.escapeHtml(kill.killer.name))
            .replace("{vtag}", kill.victim.clan?.tag ?: "")
            .replace("{atag}", kill.killer.clan?.tag ?: "")
            .replace("{time}", kill.time.format(timeFormat))
        return Helper.colorToHTML(label)
    }
}
