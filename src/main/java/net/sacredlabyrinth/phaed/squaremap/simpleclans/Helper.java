package net.sacredlabyrinth.phaed.squaremap.simpleclans;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.layers.LayerConfig;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.sacredlabyrinth.phaed.squaremap.simpleclans.SquaremapSimpleClans.debug;
import static net.sacredlabyrinth.phaed.squaremap.simpleclans.SquaremapSimpleClans.lang;

public final class Helper {

    /** Matches &x color codes where x is a hex digit (after § has been normalised to &). */
    private static final Pattern COLOR_CODE = Pattern.compile("&(?<color>[\\da-f])(?<text>[^&]+)");
    /** Strips any remaining &x or §x codes (color + formatting) that weren't converted. */
    private static final Pattern STRIP_CODES = Pattern.compile("[&§][\\da-fk-orA-FK-OR]");
    private static final String HTML_SPAN = "<span style='color: %s;'>%s</span>";

    private Helper() {}

    /**
     * Converts Minecraft color codes (&amp;x or §x) to HTML spans with inline CSS.
     * Pipe characters become &lt;br&gt; line breaks.
     * Remaining unmatched color/format codes are stripped from the output.
     */
    public static String colorToHTML(@Nullable String string) {
        if (string == null) return "";
        // Normalize § (section sign) to & so one regex handles both prefixes
        string = string.replace("§", "&").trim().replace("|", "<br>");
        Matcher matcher = COLOR_CODE.matcher(string);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String color = matcher.group("color");
            String text  = matcher.group("text");
            ChatColor chatColor = ChatColor.getByChar(color.charAt(0));
            if (chatColor == null) continue;
            matcher.appendReplacement(sb,
                    Matcher.quoteReplacement(
                            String.format(HTML_SPAN, HEXColor.of(chatColor).getCode(), text)));
        }
        matcher.appendTail(sb);
        // Remove any leftover codes that had no text (e.g. &8 at end-of-string, &l, &r)
        return STRIP_CODES.matcher(sb.toString()).replaceAll("");
    }

    /**
     * Strips all Minecraft color/format codes (§x or &amp;x) from a string,
     * returning plain text suitable for use in click-labels.
     */
    public static String stripColorCodes(@Nullable String s) {
        if (s == null) return "";
        return STRIP_CODES.matcher(s.replace("§", "&")).replaceAll("");
    }

    /**
     * Builds the compact format label used for the click-tooltip / marker title.
     * Supports all {placeholders} from config.
     */
    public static String getClanLabel(LayerConfig config, Clan clan) {
        long onlineCount = clan.getMembers().stream().filter(cp -> cp.toPlayer() != null).count();
        String inactive      = String.format("%s/%s", clan.getInactiveDays(), clan.getMaxInactiveDays());
        String onlineMembers = String.format("%s/%s", onlineCount, clan.getSize());
        String status        = clan.isVerified() ? lang("verified") : lang("unverified");
        String feeEnabled    = clan.isMemberFeeEnabled() ? lang("fee-enabled") : lang("fee-disabled");

        String label = config.getString("format", "{clan} &8(home)")
                .replace("{clan}",           clan.getName())
                .replace("{tag}",            clan.getTag())
                .replace("{member_count}",   String.valueOf(clan.getMembers().size()))
                .replace("{inactive}",       inactive)
                .replace("{founded}",        clan.getFoundedString())
                .replace("{rival}",          String.valueOf(clan.getTotalRival()))
                .replace("{neutral}",        String.valueOf(clan.getTotalNeutral()))
                .replace("{deaths}",         String.valueOf(clan.getTotalDeaths()))
                .replace("{kdr}",            String.valueOf(clan.getTotalKDR()))
                .replace("{civilian}",       String.valueOf(clan.getTotalCivilian()))
                .replace("{members_online}", onlineMembers)
                .replace("{leaders}",        clan.getLeadersString("", ", "))
                // Strip §-codes from ally/rival strings so they display as plain text in the label
                .replace("{allies}",         stripColorCodes(clan.getAllyString(", ", null)))
                .replace("{rivals}",         stripColorCodes(clan.getRivalString(", ", null)))
                .replace("{fee_value}",      String.valueOf(clan.getMemberFee()))
                .replace("{status}",         status)
                .replace("{fee_enabled}",    feeEnabled);

        return colorToHTML(label);
    }

    /**
     * Builds a rich HTML hover card for a clan home marker.
     * Layout: header (tag + status + founded) → stat row → social section.
     */
    public static String buildClanHoverTooltip(Clan clan) {
        long onlineCount = clan.getMembers().stream().filter(cp -> cp.toPlayer() != null).count();
        int  totalCount  = clan.getSize();

        String leaders = clan.getMembers().stream()
                .filter(ClanPlayer::isLeader)
                .map(cp -> escapeHtml(cp.getName()))
                .collect(Collectors.joining(", "));
        if (leaders.isEmpty()) leaders = "-";

        // Convert §-coded ally/rival strings to HTML color spans
        String alliesRaw = clan.getAllyString(", ", null);
        String rivalsRaw = clan.getRivalString(", ", null);
        String alliesHtml = (alliesRaw == null || alliesRaw.isEmpty()) ? "-" : colorToHTML(alliesRaw);
        String rivalsHtml = (rivalsRaw == null || rivalsRaw.isEmpty()) ? "-" : colorToHTML(rivalsRaw);

        String desc = clan.getDescription();
        String descHtml = (desc != null && !desc.isEmpty())
                ? "<div style='margin-top:8px;padding-top:6px;border-top:1px solid #313244;"
                  + "font-style:italic;color:#a6adc8;font-size:0.88em;'>"
                  + colorToHTML(desc) + "</div>"
                : "";

        String statusBadge = clan.isVerified()
                ? "<span style='color:#a6e3a1;'>&#10003; Verified</span>"
                : "<span style='color:#f38ba8;'>&#10007; Unverified</span>";

        // Use the colored tag; fall back to plain tag if color conversion yields nothing
        String tagColorHtml = colorToHTML(clan.getColorTag());
        if (tagColorHtml.isEmpty()) tagColorHtml = escapeHtml(clan.getTag());

        // Show the full name next to the tag only when they differ (avoids "Owner Owner")
        String nameHtml = clan.getName().equalsIgnoreCase(clan.getTag()) ? ""
                : " <span style='color:#a6adc8;font-size:0.92em;font-weight:normal;'>"
                  + escapeHtml(clan.getName()) + "</span>";

        String kdr = String.format("%.2f", clan.getTotalKDR());

        return "<div style='font-family:sans-serif;min-width:220px;max-width:290px;"
                + "background:#1e1e2e;border-radius:8px;overflow:hidden;color:#cdd6f4;'>"

                // ── Header ──────────────────────────────────────────────────────────
                + "<div style='background:#313244;padding:10px 14px;border-bottom:2px solid #89b4fa;'>"
                + "<div style='font-size:1.05em;font-weight:bold;'>" + tagColorHtml + nameHtml + "</div>"
                + "<div style='margin-top:4px;font-size:0.8em;color:#a6adc8;'>"
                + statusBadge + "&nbsp;&nbsp;&#128197;&nbsp;" + escapeHtml(clan.getFoundedString())
                + "</div>"
                + "</div>"

                // ── Stat boxes ──────────────────────────────────────────────────────
                + "<table style='width:100%;border-collapse:collapse;background:#181825;'><tr>"
                + statCell(onlineCount + "<span style='color:#585b70;'>/" + totalCount + "</span>",
                           "Online",  "#89b4fa")
                + statCell(String.valueOf(clan.getTotalKills()),  "Kills",  "#a6e3a1")
                + statCell(String.valueOf(clan.getTotalDeaths()), "Deaths", "#f38ba8")
                + statCell(kdr,                                   "KDR",    "#fab387")
                + "</tr></table>"

                // ── Social ──────────────────────────────────────────────────────────
                + "<div style='padding:10px 14px;font-size:0.88em;line-height:1.85;'>"
                + "<div>&#128081;&nbsp;<b>Leaders:</b> " + leaders     + "</div>"
                + "<div>&#129309;&nbsp;<b>Allies:</b> "  + alliesHtml  + "</div>"
                + "<div>&#9876;&#65039;&nbsp;<b>Rivals:</b> " + rivalsHtml + "</div>"
                + descHtml
                + "</div>"
                + "</div>";
    }

    /** Renders a single stat box (large coloured value + small grey label). */
    private static String statCell(String value, String label, String color) {
        return "<td style='text-align:center;padding:8px 2px;border-right:1px solid #313244;'>"
                + "<div style='font-size:1.1em;font-weight:bold;color:" + color + ";'>" + value + "</div>"
                + "<div style='font-size:0.72em;color:#6c7086;margin-top:1px;'>" + label + "</div>"
                + "</td>";
    }

    /**
     * Builds a compact HTML tooltip for land/territory area markers.
     */
    public static String buildLandHoverTooltip(Clan clan) {
        long onlineCount = clan.getMembers().stream().filter(cp -> cp.toPlayer() != null).count();

        String tagColorHtml = colorToHTML(clan.getColorTag());
        if (tagColorHtml.isEmpty()) tagColorHtml = escapeHtml(clan.getTag());

        // Avoid repeating name when it matches the tag
        String nameHtml = clan.getName().equalsIgnoreCase(clan.getTag()) ? ""
                : " " + escapeHtml(clan.getName());

        return "<div style='font-family:sans-serif;background:#1e1e2e;color:#cdd6f4;"
                + "padding:8px 12px;border-radius:6px;border-left:3px solid #89b4fa;'>"
                + "<div style='font-weight:bold;'>" + tagColorHtml + nameHtml + "</div>"
                + "<div style='font-size:0.85em;color:#a6adc8;margin-top:3px;'>"
                + "&#128101;&nbsp;" + onlineCount + "/" + clan.getSize() + " online"
                + "&nbsp;&nbsp;&#9878;&#65039;&nbsp;KDR:&nbsp;"
                + String.format("%.2f", clan.getTotalKDR())
                + "</div>"
                + "</div>";
    }

    public static String escapeHtml(@Nullable String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public enum HEXColor {
        WHITE("#FFFFFF"),
        BLACK("#000000"),
        DARK_GRAY("#555555"),
        GRAY("#AAAAAA"),
        DARK_PURPLE("#AA00AA"),
        LIGHT_PURPLE("#FF55FF"),
        DARK_BLUE("#0000AA"),
        BLUE("#5555FF"),
        DARK_AQUA("#00AAAA"),
        AQUA("#55FFFF"),
        GREEN("#55FF55"),
        DARK_GREEN("#00AA00"),
        YELLOW("#FFFF55"),
        GOLD("#FFAA00"),
        RED("#FF5555"),
        DARK_RED("#AA0000");

        private final String code;

        HEXColor(String code) { this.code = code; }

        public static HEXColor of(@NotNull ChatColor chatColor) {
            try {
                return HEXColor.valueOf(chatColor.name());
            } catch (IllegalArgumentException ex) {
                debug(String.format("Error parsing hex color for %s: %s", chatColor.name(), ex.getMessage()));
                return HEXColor.WHITE;
            }
        }

        public static HEXColor of(@NotNull String chatColorChar) {
            String color = chatColorChar.replace("§", "").replace("&", "");
            ChatColor chatColor = color.isEmpty() ? null : ChatColor.getByChar(color.charAt(0));
            return chatColor != null ? of(chatColor) : HEXColor.WHITE;
        }

        public String getCode() { return code; }
    }
}
