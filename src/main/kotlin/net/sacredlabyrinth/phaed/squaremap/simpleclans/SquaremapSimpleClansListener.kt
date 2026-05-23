package net.sacredlabyrinth.phaed.squaremap.simpleclans

import net.sacredlabyrinth.phaed.simpleclans.Clan
import net.sacredlabyrinth.phaed.simpleclans.Kill
import net.sacredlabyrinth.phaed.simpleclans.events.*
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority.MONITOR
import org.bukkit.event.Listener
import java.time.LocalDateTime

class SquaremapSimpleClansListener(private val plugin: SquaremapSimpleClans) : Listener {

    @EventHandler(priority = MONITOR, ignoreCancelled = true)
    fun onSetHome(event: PlayerHomeSetEvent) = scheduleUpsert(event.clan)

    @EventHandler(priority = MONITOR, ignoreCancelled = true)
    fun onHomeClear(event: PlayerHomeClearEvent) = deleteMarkers(event.clan.tag)

    @EventHandler(priority = MONITOR, ignoreCancelled = true)
    fun onDisband(event: DisbandClanEvent) = deleteMarkers(event.clan.tag)

    @EventHandler(priority = MONITOR, ignoreCancelled = true)
    fun onTagChange(event: TagChangeEvent) = scheduleUpsert(event.clan)

    @EventHandler(priority = MONITOR, ignoreCancelled = true)
    fun onKill(event: AddKillEvent) {
        plugin.getKillsLayer()?.createMarker(Kill(event.attacker, event.victim, LocalDateTime.now()))
    }

    private fun deleteMarkers(clanTag: String) {
        plugin.getHomesLayer()?.deleteMarker(clanTag)
        plugin.getLandsLayer()?.deleteMarker(clanTag)
    }

    // Run on the next tick so all clan data is flushed before we read it
    private fun scheduleUpsert(clan: Clan) {
        Bukkit.getScheduler().runTask(plugin, Runnable {
            plugin.getHomesLayer()?.upsertMarker(clan)
            plugin.getLandsLayer()?.upsertMarker(clan)
        })
    }
}
