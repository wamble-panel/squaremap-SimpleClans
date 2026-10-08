package net.sacredlabyrinth.phaed.squaremap.simpleclans.layer;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.ClanFlags;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.SquaremapSimpleClans;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.config.Settings;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.api.marker.Icon;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.MarkerOptions;

import java.util.Map;

/** An icon at every clan home. */
public final class HomesLayer extends MapLayer {

    private final Settings.Homes homes;

    public HomesLayer(@NotNull SquaremapSimpleClans plugin, @NotNull Settings.Homes homes) {
        super(plugin, "homes", homes.layer());
        this.homes = homes;
    }

    @Override
    public void refresh() {
        Map<WorldIdentifier, Map<Key, Marker>> desired = newDesired();
        for (Clan clan : plugin.clanManager().getClans()) {
            Location home = visibleHome(clan);
            if (home != null) {
                want(desired, home.getWorld(), key(clan.getTag()), marker(clan, home));
            }
        }
        sync(desired);
    }

    /** Updates a single clan right away, e.g. after its home was set. */
    public void update(@NotNull Clan clan) {
        Location home = visibleHome(clan);
        if (home == null) {
            remove(clan.getTag());
        } else {
            place(home.getWorld(), key(clan.getTag()), marker(clan, home));
        }
    }

    public void remove(@NotNull String tag) {
        removeEverywhere(key(tag));
    }

    private @Nullable Location visibleHome(Clan clan) {
        Location home = clan.getHomeLocation();
        if (home == null || home.getWorld() == null) {
            return null;
        }
        World world = home.getWorld();
        if (provider(world) == null || homes.isClanHidden(clan.getTag()) || ClanFlags.hidden(clan)) {
            return null;
        }
        return home;
    }

    private Icon marker(Clan clan, Location home) {
        Icon icon = Marker.icon(blockCenter(home), plugin.icons().key(ClanFlags.icon(clan)), homes.iconSize());
        icon.markerOptions(MarkerOptions.builder()
                .hoverTooltip(plugin.tooltips().clanHover(clan, null))
                .clickTooltip(plugin.tooltips().clanPopup(clan)));
        return icon;
    }

    private Key key(String tag) {
        return markerKey(tag.toLowerCase(java.util.Locale.ROOT));
    }
}
