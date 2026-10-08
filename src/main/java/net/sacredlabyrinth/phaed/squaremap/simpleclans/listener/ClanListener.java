package net.sacredlabyrinth.phaed.squaremap.simpleclans.listener;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.events.AddKillEvent;
import net.sacredlabyrinth.phaed.simpleclans.events.DisbandClanEvent;
import net.sacredlabyrinth.phaed.simpleclans.events.PlayerHomeClearEvent;
import net.sacredlabyrinth.phaed.simpleclans.events.PlayerHomeSetEvent;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.SquaremapSimpleClans;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.layer.KillsLayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.jetbrains.annotations.NotNull;

/** Events every supported SimpleClans version has. */
public final class ClanListener implements Listener {

    private final SquaremapSimpleClans plugin;

    public ClanListener(@NotNull SquaremapSimpleClans plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHomeSet(PlayerHomeSetEvent event) {
        nextTick(event.getClan());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHomeClear(PlayerHomeClearEvent event) {
        nextTick(event.getClan());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDisband(DisbandClanEvent event) {
        plugin.removeClan(event.getClan().getTag());
        plugin.requestRefresh(false); // allies/rivals listing this clan
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(AddKillEvent event) {
        KillsLayer kills = plugin.kills();
        if (kills != null) {
            kills.record(event.getAttacker(), event.getVictim());
        }
    }

    // online counts in tooltips
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        plugin.requestRefresh(false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.requestRefresh(false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        plugin.worldLoaded(event.getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        plugin.worldUnloaded(event.getWorld());
    }

    /** The event fires before SimpleClans stores the change, so read the clan a tick later. */
    private void nextTick(Clan clan) {
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.updateClan(clan));
    }
}
