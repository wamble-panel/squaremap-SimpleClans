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

    private static final Pattern COLOR_CODE = Pattern.compile("&(?<color>[\\da-f])(?<text>[^&]+)");
    private static final String HTML_SPAN = "<span style='color: %s;'>%s</span>";

    private Helper() {}

    /**
     * Converts Minecraft color codes (&amp;x) to HTML spans with inline CSS.
     * Pipe characters become &lt;br&gt; line breaks.
     */
    public static String colorToHTML(@Nullable String string) {
        if (string == null) return "";
        string = string.trim().replace("|", "<br>");
        Matcher matcher = COLOR_CODE.matcher(string);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String color = matcher.group("color");
            String text = matcher.group("text");
            ChatColor chatColor = ChatColor.getByChar(color.charAt(0));
            if (chatColor == null) continue;
            matcher.appendReplacement(sb,
                    Matcher.quoteReplacement(String.format(HTML_SPAN, HEXColor.of(chatColor).getCode(), text)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Builds the compact format label (used for click-tooltip or title).
     * Supports all original {placeholders} from config.
     */
    public static String getClanLabel(LayerConfig config, Clan clan) {
        long onlineCount = clan.getMembers().stream().filter(ClanPlayer::isOnline).count();
        String inactive = String.format("%s/%s", clan.getInactiveDays(), clan.getMaxInactiveDays());
        String onlineMembers = String.format("%s/%s", onlineCount, clan.getSize());
        String status = clan.isVerified() ? lang("verified") : lang("unverified");
        String feeEnabled = clan.isMemberFeeEnabled() ? lang("fee-enabled") : lang("fee-disabled");

        String label = config.getString("format", "{clan} &8(home)")
                .replace("{clan}", clan.getName())
                .replace("{tag}", clan.getTag())
                .replace("{member_count}", String.valueOf(clan.getMembers().size()))
                .replace("{inactive}", inactive)
                .replace("{founded}", clan.getFoundedString())
                .replace("{rival}", String.valueOf(clan.getTotalRival()))
                .replace("{neutral}", String.valueOf(clan.getTotalNeutral()))
                .replace("{deaths}", String.valueOf(clan.getTotalDeaths()))
                .replace("{kdr}", String.valueOf(clan.getTotalKDR()))
                .replace("{civilian}", String.valueOf(clan.getTotalCivilian()))
                .replace("{members_online}", onlineMembers)
                .replace("{leaders}", clan.getLeadersString("", ", "))
                .replace("{allies}", clan.getAllyString(", ", null))
                .replace("{rivals}", clan.getRivalString(", ", null))
                .replace("{fee_value}", String.valueOf(clan.getMemberFee()))
                .replace("{status}", status)
                .replace("{fee_enabled}", feeEnabled);

        return colorToHTML(label);
    }

    /**
     * Builds a rich HTML hover tooltip for a clan home marker.
     * Includes members, stats, leaders, allies, rivals, and description.
     */
    public static String buildClanHoverTooltip(Clan clan) {
        long onlineCount = clan.getMembers().stream().filter(ClanPlayer::isOnline).count();
        int totalCount = clan.getSize();

        String leaders = clan.getMembers().stream()
                .filter(ClanPlayer::isLeader)
                .map(ClanPlayer::getName)
                .collect(Collectors.joining(", "));
        if (leaders.isEmpty()) leaders = "-";

        String allies = clan.getAllyString(", ", null);
        if (allies == null || allies.isEmpty()) allies = "-";

        String rivals = clan.getRivalString(", ", null);
        if (rivals == null || rivals.isEmpty()) rivals = "-";

        String desc = clan.getDescription();
        String descHtml = (desc != null && !desc.isEmpty())
                ? "<div style='margin-top:6px;font-style:italic;color:#aaa;border-top:1px solid #333;padding-top:4px;'>" + escapeHtml(desc) + "</div>"
                : "";

        String statusBadge = clan.isVerified()
                ? "<span style='color:#4caf50;'>&#10003; Verified</span>"
                : "<span style='color:#f44336;'>&#10007; Unverified</span>";

        String tagColorHtml = colorToHTML(clan.getColorTag());
        if (tagColorHtml.isEmpty()) tagColorHtml = escapeHtml(clan.getTag());

        return "<div style='font-family:sans-serif;min-width:220px;max-width:320px;'>"
                + "<div style='background:#1a1a2e;color:#e0e0e0;padding:8px 12px;border-radius:6px 6px 0 0;border-bottom:2px solid #4a90d9;'>"
                + "<b style='font-size:1.05em;'>" + tagColorHtml + "</b>"
                + " <span style='color:#ccc;'>" + escapeHtml(clan.getName()) + "</span>"
                + "</div>"
                + "<div style='background:#16213e;color:#ddd;padding:8px 12px;border-radius:0 0 6px 6px;line-height:1.7;'>"
                + "<div>&#128101; Members: <b>" + onlineCount + "</b>/<b>" + totalCount + "</b> online</div>"
                + "<div>&#9876;&#65039; Kills: <b>" + clan.getTotalKills() + "</b> &nbsp;&#128128; Deaths: <b>" + clan.getTotalDeaths() + "</b></div>"
                + "<div>&#9878;&#65039; KDR: <b>" + String.format("%.2f", clan.getTotalKDR()) + "</b></div>"
                + "<div>&#128081; Leader(s): <b>" + escapeHtml(leaders) + "</b></div>"
                + "<div>&#128197; Founded: <b>" + escapeHtml(clan.getFoundedString()) + "</b></div>"
                + "<div>&#9989; Status: " + statusBadge + "</div>"
                + "<div>&#129309; Allies: <b>" + escapeHtml(allies) + "</b></div>"
                + "<div>&#9876;&#65039; Rivals: <b>" + escapeHtml(rivals) + "</b></div>"
                + descHtml
                + "</div>"
                + "</div>";
    }

    /**
     * Builds a compact HTML tooltip for land area markers.
     */
    public static String buildLandHoverTooltip(Clan clan) {
        long onlineCount = clan.getMembers().stream().filter(ClanPlayer::isOnline).count();
        String tagColorHtml = colorToHTML(clan.getColorTag());
        if (tagColorHtml.isEmpty()) tagColorHtml = escapeHtml(clan.getTag());

        return "<div style='font-family:sans-serif;'>"
                + "<b>" + tagColorHtml + "</b> " + escapeHtml(clan.getName()) + "<br>"
                + "&#128101; " + onlineCount + "/" + clan.getSize() + " online<br>"
                + "&#9876;&#65039; KDR: " + String.format("%.2f", clan.getTotalKDR())
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
