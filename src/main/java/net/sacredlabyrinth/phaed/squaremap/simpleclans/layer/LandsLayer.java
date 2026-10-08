package net.sacredlabyrinth.phaed.squaremap.simpleclans.layer;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import net.sacredlabyrinth.phaed.simpleclans.hooks.protection.Coordinate;
import net.sacredlabyrinth.phaed.simpleclans.hooks.protection.Land;
import net.sacredlabyrinth.phaed.simpleclans.managers.ProtectionManager;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.ClanFlags;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.SquaremapSimpleClans;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.config.Settings;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.render.LegacyText;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.render.Territory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.Point;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.MarkerOptions;
import xyz.jpenilla.squaremap.api.marker.MultiPolygon;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Clan territory: the protected lands (WorldGuard, GriefPrevention, ... via SimpleClans'
 * protection hooks) owned by the clan's leaders or members, one shape per claim.
 */
public final class LandsLayer extends MapLayer {

    private final Settings.Lands lands;
    private volatile Map<String, Territory> territories = Map.of();

    public LandsLayer(@NotNull SquaremapSimpleClans plugin, @NotNull Settings.Lands lands) {
        super(plugin, "lands", lands.layer());
        this.lands = lands;
    }

    /** Total territory of a clan across all worlds, from the last refresh. */
    public @Nullable Territory territory(@NotNull Clan clan) {
        return territories.get(clan.getTag().toLowerCase(Locale.ROOT));
    }

    @Override
    public void refresh() {
        ProtectionManager protection = plugin.simpleClans().getProtectionManager();
        Collection<World> worlds = worlds();

        // First work out every clan's shapes, so popups built below see fresh totals.
        Map<Clan, Map<World, Shapes>> shapes = new LinkedHashMap<>();
        Map<String, Territory> totals = new HashMap<>();
        for (Clan clan : plugin.clanManager().getClans()) {
            if (lands.isClanHidden(clan.getTag()) || ClanFlags.hidden(clan)) {
                continue;
            }
            try {
                Map<World, Shapes> perWorld = shapes(clan, protection, worlds);
                if (!perWorld.isEmpty()) {
                    shapes.put(clan, perWorld);
                    perWorld.values().forEach(s -> totals.merge(clan.getTag().toLowerCase(Locale.ROOT),
                            s.territory(), Territory::plus));
                }
            } catch (RuntimeException ex) {
                plugin.debug("Could not read lands of clan " + clan.getTag() + ": " + ex);
            }
        }
        territories = Map.copyOf(totals);

        Map<WorldIdentifier, Map<Key, Marker>> desired = newDesired();
        shapes.forEach((clan, perWorld) -> perWorld.forEach((world, s) ->
                want(desired, world, markerKey(clan.getTag().toLowerCase(Locale.ROOT)), marker(clan, s))));
        sync(desired);

        // home popups show territory too; give them the new numbers
        plugin.refreshHomes();
    }

    public void remove(@NotNull String tag) {
        removeEverywhere(markerKey(tag.toLowerCase(Locale.ROOT)));
    }

    // -- lands -------------------------------------------------------------

    private Map<World, Shapes> shapes(Clan clan, ProtectionManager protection, Collection<World> worlds) {
        Map<World, Set<Land>> claims = new HashMap<>();

        Set<UUID> members = new HashSet<>();
        clan.getMembers().forEach(cp -> members.add(cp.getUniqueId()));

        // land the home sits in, if a clan member owns it
        Location home = clan.getHomeLocation();
        if (home != null && home.getWorld() != null && worlds.contains(home.getWorld())) {
            for (Land land : protection.getLandsAt(home)) {
                if (!Collections.disjoint(land.getOwners(), members)) {
                    claims.computeIfAbsent(home.getWorld(), w -> new LinkedHashSet<>()).add(land);
                }
            }
        }

        List<ClanPlayer> owners = lands.owners() == Settings.TerritoryOwners.LEADERS
                ? clan.getLeaders() : clan.getMembers();
        for (ClanPlayer owner : owners) {
            var player = Bukkit.getOfflinePlayer(owner.getUniqueId());
            for (World world : worlds) {
                Set<Land> owned = protection.getLandsOf(player, world);
                if (!owned.isEmpty()) {
                    claims.computeIfAbsent(world, w -> new LinkedHashSet<>()).addAll(owned);
                }
            }
        }

        Map<World, Shapes> out = new HashMap<>();
        claims.forEach((world, set) -> {
            Shapes s = new Shapes();
            for (Land land : set) {
                List<Point> outline = outline(land.getCoordinates());
                if (outline != null) {
                    s.parts.add(MultiPolygon.part(outline));
                    s.blocks += Math.round(area(outline));
                }
            }
            if (!s.parts.isEmpty()) {
                out.put(world, s);
            }
        });
        return out;
    }

    private Marker marker(Clan clan, Shapes s) {
        int tagColor = lands.colorFromTag() ? LegacyText.firstColor(clan.getColorTag()) : -1;
        Color fill = new Color(tagColor != -1 ? tagColor : lands.fillColor());
        Color stroke = new Color(tagColor != -1 ? tagColor : lands.strokeColor());

        return Marker.multiPolygon(s.parts).markerOptions(MarkerOptions.builder()
                .fill(true)
                .fillColor(fill)
                .fillOpacity(lands.fillOpacity())
                .stroke(lands.strokeWeight() > 0)
                .strokeColor(stroke)
                .strokeOpacity(lands.strokeOpacity())
                .strokeWeight(lands.strokeWeight())
                .hoverTooltip(plugin.tooltips().clanHover(clan, s.territory()))
                .clickTooltip(plugin.tooltips().clanPopup(clan)));
    }

    /**
     * Turns a land's coordinates into a map outline. Two points (GriefPrevention) or an
     * axis-aligned box are block corners, so the far edge is pushed out one block to cover
     * the whole claim; anything else is drawn as the polygon it is.
     */
    static @Nullable List<Point> outline(@NotNull List<Coordinate> coordinates) {
        if (coordinates.size() < 2) {
            return null;
        }
        double minX = Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        Set<Double> xs = new HashSet<>();
        Set<Double> zs = new HashSet<>();
        for (Coordinate c : coordinates) {
            minX = Math.min(minX, c.getX());
            maxX = Math.max(maxX, c.getX());
            minZ = Math.min(minZ, c.getZ());
            maxZ = Math.max(maxZ, c.getZ());
            xs.add(c.getX());
            zs.add(c.getZ());
        }
        if (coordinates.size() == 2 || (xs.size() <= 2 && zs.size() <= 2)) {
            return List.of(Point.of(minX, minZ), Point.of(maxX + 1, minZ),
                    Point.of(maxX + 1, maxZ + 1), Point.of(minX, maxZ + 1));
        }
        List<Point> points = new ArrayList<>(coordinates.size());
        coordinates.forEach(c -> points.add(Point.of(c.getX(), c.getZ())));
        return points;
    }

    /** Shoelace formula. */
    static double area(@NotNull List<Point> outline) {
        double sum = 0;
        for (int i = 0; i < outline.size(); i++) {
            Point a = outline.get(i);
            Point b = outline.get((i + 1) % outline.size());
            sum += a.x() * b.z() - b.x() * a.z();
        }
        return Math.abs(sum) / 2;
    }

    private static final class Shapes {
        final List<MultiPolygon.MultiPolygonPart> parts = new ArrayList<>();
        long blocks;

        Territory territory() {
            return new Territory(parts.size(), blocks);
        }
    }
}
