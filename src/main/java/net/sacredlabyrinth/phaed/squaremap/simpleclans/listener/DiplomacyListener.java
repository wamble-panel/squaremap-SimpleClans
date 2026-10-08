package net.sacredlabyrinth.phaed.squaremap.simpleclans.listener;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.events.AllyClanAddEvent;
import net.sacredlabyrinth.phaed.simpleclans.events.AllyClanRemoveEvent;
import net.sacredlabyrinth.phaed.simpleclans.events.PlayerDemoteEvent;
import net.sacredlabyrinth.phaed.simpleclans.events.PlayerJoinedClanEvent;
import net.sacredlabyrinth.phaed.simpleclans.events.PlayerKickedClanEvent;
import net.sacredlabyrinth.phaed.simpleclans.events.PlayerPromoteEvent;
import net.sacredlabyrinth.phaed.simpleclans.events.RivalClanAddEvent;
import net.sacredlabyrinth.phaed.simpleclans.events.RivalClanRemoveEvent;
import net.sacredlabyrinth.phaed.simpleclans.events.TagChangeEvent;
import net.sacredlabyrinth.phaed.simpleclans.events.WarEndEvent;
import net.sacredlabyrinth.phaed.simpleclans.events.WarStartEvent;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.SquaremapSimpleClans;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * Membership and diplomacy changes. Kept apart from {@link ClanListener} because older
 * SimpleClans versions lack some of these events; if one is missing only this listener fails.
 */
public final class DiplomacyListener implements Listener {

    private final SquaremapSimpleClans plugin;

    public DiplomacyListener(@NotNull SquaremapSimpleClans plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTagChange(TagChangeEvent event) {
        Clan clan = event.getClan();
        plugin.removeClan(clan.getTag()); // markers are keyed by the old tag
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.updateClan(clan));
    }

    // who owns land changes with membership and leadership
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoined(PlayerJoinedClanEvent event) {
        plugin.requestRefresh(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKicked(PlayerKickedClanEvent event) {
        plugin.requestRefresh(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPromote(PlayerPromoteEvent event) {
        plugin.requestRefresh(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDemote(PlayerDemoteEvent event) {
        plugin.requestRefresh(true);
    }

    // relations shown in popups
    @EventHandler(priority = EventPriority.MONITOR)
    public void onAllyAdd(AllyClanAddEvent event) {
        plugin.requestRefresh(false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAllyRemove(AllyClanRemoveEvent event) {
        plugin.requestRefresh(false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRivalAdd(RivalClanAddEvent event) {
        plugin.requestRefresh(false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRivalRemove(RivalClanRemoveEvent event) {
        plugin.requestRefresh(false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWarStart(WarStartEvent event) {
        plugin.requestRefresh(false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWarEnd(WarEndEvent event) {
        plugin.requestRefresh(false);
    }
}
