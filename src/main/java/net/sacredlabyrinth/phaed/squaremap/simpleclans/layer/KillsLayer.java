package net.sacredlabyrinth.phaed.squaremap.simpleclans.layer;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.SquaremapSimpleClans;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.config.Settings;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.config.Settings.KillType;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.SimpleLayerProvider;
import xyz.jpenilla.squaremap.api.marker.Icon;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.MarkerOptions;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;

/** A short-lived marker where each PvP kill happened. */
public final class KillsLayer extends MapLayer {

    private static final long SWEEP_TICKS = 100L;

    private final Settings.Kills kills;
    private final Deque<Entry> entries = new ArrayDeque<>();
    private long counter;

    private record Entry(Key key, long expiresAt) {
    }

    public KillsLayer(@NotNull SquaremapSimpleClans plugin, @NotNull Settings.Kills kills) {
        super(plugin, "kills", kills.layer());
        this.kills = kills;
    }

    public void record(@NotNull ClanPlayer attacker, @NotNull ClanPlayer victim) {
        Player player = victim.toPlayer();
        if (player == null) {
            return;
        }
        Location location = player.getLocation();
        World world = location.getWorld();
        SimpleLayerProvider provider = world != null ? provider(world) : null;
        if (provider == null) {
            return;
        }
        KillType type = classify(attacker, victim);
        if (!kills.types().contains(type)) {
            return;
        }

        String html = plugin.tooltips().kill(attacker, victim, type, LocalDateTime.now(), kills.timeFormat());
        Icon icon = Marker.icon(blockCenter(location), plugin.icons().killIcon(), kills.iconSize());
        icon.markerOptions(MarkerOptions.builder().hoverTooltip(html).clickTooltip(html));

        Key key = markerKey("kill_" + (++counter));
        provider.addMarker(key, icon);
        entries.addLast(new Entry(key, System.currentTimeMillis() + kills.durationSeconds() * 1000L));
        while (entries.size() > kills.maxMarkers()) {
            removeEverywhere(entries.removeFirst().key());
        }
    }

    /** Clears kills that have been on the map long enough. */
    @Override
    public void refresh() {
        long now = System.currentTimeMillis();
        while (!entries.isEmpty() && entries.peekFirst().expiresAt() <= now) {
            removeEverywhere(entries.removeFirst().key());
        }
    }

    @Override
    protected long refreshPeriodTicks() {
        return SWEEP_TICKS;
    }

    @Override
    public void disable() {
        super.disable();
        entries.clear();
    }

    static @NotNull KillType classify(@NotNull ClanPlayer attacker, @NotNull ClanPlayer victim) {
        Clan victimClan = victim.getClan();
        Clan attackerClan = attacker.getClan();
        if (victimClan == null) {
            return KillType.CIVILIAN;
        }
        if (attackerClan == null) {
            return KillType.NEUTRAL;
        }
        if (attackerClan.isWarring(victimClan)) {
            return KillType.WAR;
        }
        if (attackerClan.isRival(victimClan.getTag())) {
            return KillType.RIVAL;
        }
        if (attackerClan.isAlly(victimClan.getTag())) {
            return KillType.ALLY;
        }
        return KillType.NEUTRAL;
    }
}
