package net.sacredlabyrinth.phaed.squaremap.simpleclans.render;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import net.sacredlabyrinth.phaed.simpleclans.managers.ClanManager;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.config.Settings;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.config.Settings.KillType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

import static net.sacredlabyrinth.phaed.squaremap.simpleclans.render.LegacyText.escape;

/**
 * Builds the HTML squaremap shows on hover (tooltip) and click (popup).
 *
 * <p>Content sits on Leaflet's light tooltip, like squaremap's own markers. Clan tags are
 * drawn as dark nameplates, the same look as squaremap's player nameplates, so Minecraft
 * colours stay readable whatever they are.
 */
public final class Tooltips {

    private static final String NAMEPLATE = "display:inline-block;margin:1px 0;padding:0 4px;"
            + "background:rgba(0,0,0,.6);border-radius:2px;color:#FFFFFF;font-weight:bold;"
            + "text-shadow:1px 1px 0 #3F3F3F;line-height:1.5;white-space:nowrap";
    private static final String MUTED = "color:#666";
    private static final String LABEL_CELL = "color:#777;padding:1px 10px 1px 0;vertical-align:top;white-space:nowrap";
    private static final String VALUE_CELL = "padding:1px 0;max-width:240px;white-space:normal";

    private final Settings.Tooltip settings;
    private final ClanManager clans;
    private final Function<Clan, Territory> territories;

    public Tooltips(@NotNull Settings.Tooltip settings, @NotNull ClanManager clans,
                    @NotNull Function<Clan, Territory> territories) {
        this.settings = settings;
        this.clans = clans;
        this.territories = territories;
    }

    // -- clans -------------------------------------------------------------

    /** Compact hover: nameplate, name and how many members are online. */
    public @NotNull String clanHover(@NotNull Clan clan, @Nullable Territory territory) {
        StringBuilder html = new StringBuilder(header(clan)).append(muted(online(clan)));
        if (territory != null && territory.claims() > 0) {
            html.append(muted(territory(territory)));
        }
        return html.toString();
    }

    /** Full popup with the rows chosen in config; empty rows are left out. */
    public @NotNull String clanPopup(@NotNull Clan clan) {
        StringBuilder rows = new StringBuilder();
        String description = null;
        for (Settings.Row row : settings.rows()) {
            switch (row) {
                case LEADERS -> row(rows, "leaders", players(clan.getLeaders()));
                case MEMBERS -> row(rows, "members", online(clan));
                case KDR -> row(rows, "kdr", String.format(Locale.ROOT, "%.2f", clan.getTotalKDR()));
                case KILLS -> row(rows, "kills", number(clan.getTotalKills()) + " <span style=\"" + MUTED + "\">· "
                        + number(clan.getTotalDeaths()) + " " + escape(settings.label("deaths")) + "</span>");
                case ALLIES -> row(rows, "allies", nameplates(resolve(clan.getAllies())));
                case RIVALS -> row(rows, "rivals", nameplates(resolve(clan.getRivals())));
                case WARS -> row(rows, "wars", nameplates(clan.getWarringClans()));
                case TERRITORY -> {
                    Territory territory = territories.apply(clan);
                    if (territory != null && territory.claims() > 0) {
                        row(rows, "territory", territory(territory));
                    }
                }
                case FOUNDED -> {
                    if (clan.getFounded() > 0) {
                        row(rows, "founded", escape(settings.dateFormat().format(
                                LocalDateTime.ofInstant(Instant.ofEpochMilli(clan.getFounded()), ZoneId.systemDefault()))));
                    }
                }
                case DESCRIPTION -> description = LegacyText.strip(clan.getDescription()).trim();
            }
        }

        StringBuilder html = new StringBuilder("<div style=\"min-width:160px\">").append(header(clan));
        if (!rows.isEmpty()) {
            html.append("<table style=\"border-collapse:collapse;margin-top:4px\">").append(rows).append("</table>");
        }
        if (description != null && !description.isEmpty()) {
            html.append("<div style=\"margin-top:4px;max-width:260px;white-space:normal;font-style:italic;")
                    .append(MUTED).append("\">").append(escape(description)).append("</div>");
        }
        return html.append("</div>").toString();
    }

    /** A clan tag drawn as a squaremap-style nameplate in its Minecraft colours. */
    public @NotNull String nameplate(@NotNull Clan clan) {
        String tag = LegacyText.toHtml(clan.getColorTag());
        if (tag.isEmpty()) {
            tag = escape(clan.getTag());
        }
        return "<span style=\"" + NAMEPLATE + "\">" + tag + "</span>";
    }

    // -- kills -------------------------------------------------------------

    public @NotNull String kill(@NotNull ClanPlayer attacker, @NotNull ClanPlayer victim,
                                @NotNull KillType type, @NotNull LocalDateTime time,
                                @NotNull DateTimeFormatter timeFormat) {
        String typeColor = switch (type) {
            case WAR -> "#C62828";
            case RIVAL -> "#E65100";
            case ALLY -> "#2E7D32";
            default -> "#666666";
        };
        return "<div style=\"white-space:nowrap\">" + killer(attacker)
                + " <span style=\"" + MUTED + "\">&#9876;</span> " + killer(victim) + "</div>"
                + "<div style=\"" + MUTED + "\"><span style=\"color:" + typeColor + ";font-weight:bold\">"
                + escape(settings.label("kill-" + type.name().toLowerCase(Locale.ROOT))) + "</span> · "
                + escape(time.format(timeFormat)) + "</div>";
    }

    // -- pieces ------------------------------------------------------------

    private String header(Clan clan) {
        String name = LegacyText.strip(clan.getName()).trim();
        String tag = LegacyText.strip(clan.getColorTag()).trim();
        String nameHtml = name.isEmpty() || name.equalsIgnoreCase(tag) || name.equalsIgnoreCase(clan.getTag())
                ? "" : " <b>" + escape(name) + "</b>";
        return "<div style=\"white-space:nowrap\">" + nameplate(clan) + nameHtml + "</div>";
    }

    private String online(Clan clan) {
        return "<b>" + clan.getOnlineMembers().size() + "</b> " + escape(settings.label("online"))
                + " / " + clan.getSize();
    }

    private String territory(Territory territory) {
        String claims = settings.label(territory.claims() == 1 ? "claim" : "claims");
        return number(territory.claims()) + " " + escape(claims) + " · "
                + number(territory.blocks()) + " " + escape(settings.label("blocks"));
    }

    private String killer(ClanPlayer player) {
        Clan clan = player.getClan();
        return head(player) + "<b>" + escape(player.getName()) + "</b>" + (clan != null ? " " + nameplate(clan) : "");
    }

    private String players(Collection<ClanPlayer> players) {
        List<String> parts = new ArrayList<>();
        for (ClanPlayer player : players) {
            parts.add("<span style=\"white-space:nowrap\">" + head(player) + escape(player.getName()) + "</span>");
        }
        return limited(parts, ", ");
    }

    private String head(ClanPlayer player) {
        String url = settings.headUrl();
        if (url == null || url.isBlank()) {
            return "";
        }
        url = url.replace("{uuid}", String.valueOf(player.getUniqueId())).replace("{name}", player.getName());
        return "<img src=\"" + escape(url) + "\" width=\"16\" height=\"16\" alt=\"\" "
                + "style=\"vertical-align:-3px;margin-right:3px;image-rendering:pixelated\">";
    }

    private String nameplates(Collection<Clan> list) {
        List<String> parts = new ArrayList<>();
        for (Clan clan : list) {
            parts.add(nameplate(clan));
        }
        return limited(parts, " ");
    }

    private List<Clan> resolve(Collection<String> tags) {
        return tags.stream().map(clans::getClan).filter(Objects::nonNull).toList();
    }

    private String limited(List<String> parts, String separator) {
        if (parts.isEmpty()) {
            return "";
        }
        int max = settings.maxListed();
        if (parts.size() <= max) {
            return String.join(separator, parts);
        }
        return String.join(separator, parts.subList(0, max)) + " <span style=\"" + MUTED + "\">"
                + escape(settings.label("more").replace("{count}", String.valueOf(parts.size() - max))) + "</span>";
    }

    private void row(StringBuilder rows, String labelKey, String valueHtml) {
        if (valueHtml.isEmpty()) {
            return;
        }
        rows.append("<tr><td style=\"").append(LABEL_CELL).append("\">").append(escape(settings.label(labelKey)))
                .append("</td><td style=\"").append(VALUE_CELL).append("\">").append(valueHtml).append("</td></tr>");
    }

    private static String muted(String html) {
        return "<div style=\"" + MUTED + "\">" + html + "</div>";
    }

    private static String number(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
