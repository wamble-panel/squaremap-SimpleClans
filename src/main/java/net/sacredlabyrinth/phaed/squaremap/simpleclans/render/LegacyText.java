package net.sacredlabyrinth.phaed.squaremap.simpleclans.render;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses Minecraft legacy-formatted text. Both the web map ({@link #toHtml}) and in-game
 * text ({@link TextComponents}) are built from the same {@link Run}s, so a clan tag looks
 * the same everywhere.
 *
 * <p>Understands every colour format SimpleClans and common chat plugins produce,
 * with either {@code §} or {@code &} as the prefix:
 * <ul>
 *   <li>named colours {@code §0}–{@code §f}</li>
 *   <li>Bukkit hex {@code §x§R§R§G§G§B§B}</li>
 *   <li>short hex {@code §#RRGGBB}, plus {@code <#RRGGBB>} and {@code {#RRGGBB}}</li>
 *   <li>formatting {@code §l §o §n §m §k} and reset {@code §r}</li>
 * </ul>
 */
public final class LegacyText {

    /** Vanilla chat colours, indexed by code 0-f. */
    private static final int[] PALETTE = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };

    /** Below this luminance a colour is hard to read on a dark nameplate. */
    private static final double DARK_LUMINANCE = 0.12;

    /** A stretch of text sharing one style. {@code color} is RGB, or -1 for none. */
    public record Run(String text, int color, boolean bold, boolean italic, boolean underlined, boolean strikethrough) {
    }

    private LegacyText() {
    }

    /** Splits legacy text into styled runs. */
    public static @NotNull List<Run> runs(@Nullable String text) {
        List<Run> runs = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return runs;
        }
        StringBuilder current = new StringBuilder();
        Style[] currentStyle = {null};
        parse(text, (c, style) -> {
            if (!style.equals(currentStyle[0])) {
                flush(runs, current, currentStyle[0]);
                currentStyle[0] = style;
            }
            current.append(c);
        });
        flush(runs, current, currentStyle[0]);
        return runs;
    }

    /**
     * Renders legacy text as HTML for a dark nameplate: coloured text gets Minecraft's drop
     * shadow, and colours too dark to read there get a soft light glow instead.
     */
    public static @NotNull String toHtml(@Nullable String text) {
        return toHtml(text, true);
    }

    public static @NotNull String toHtml(@Nullable String text, boolean shadow) {
        StringBuilder out = new StringBuilder();
        for (Run run : runs(text)) {
            String css = css(run, shadow);
            if (css.isEmpty()) {
                out.append(escape(run.text()));
            } else {
                out.append("<span style=\"").append(css).append("\">").append(escape(run.text())).append("</span>");
            }
        }
        return out.toString();
    }

    /** Removes every colour and formatting code, returning plain text. */
    public static @NotNull String strip(@Nullable String text) {
        StringBuilder out = new StringBuilder();
        runs(text).forEach(run -> out.append(run.text()));
        return out.toString();
    }

    /** The RGB value of the first coloured character in {@code text}, or -1 if it has none. */
    public static int firstColor(@Nullable String text) {
        for (Run run : runs(text)) {
            if (run.color() != -1) {
                return run.color();
            }
        }
        return -1;
    }

    public static @NotNull String escape(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    /** Minecraft's text shadow colour: each channel divided by four. */
    public static int shadowOf(int rgb) {
        return (rgb & 0xFCFCFC) >> 2;
    }

    /** The CSS text-shadow used for {@code rgb} on a dark nameplate. */
    public static @NotNull String textShadow(int rgb) {
        return isDark(rgb) ? "0 0 2px rgba(255,255,255,.85)" : "1px 1px 0 " + hex(shadowOf(rgb));
    }

    /** WCAG relative luminance below {@link #DARK_LUMINANCE}. */
    public static boolean isDark(int rgb) {
        return luminance(rgb) < DARK_LUMINANCE;
    }

    public static double luminance(int rgb) {
        return 0.2126 * channel(rgb >> 16) + 0.7152 * channel(rgb >> 8) + 0.0722 * channel(rgb);
    }

    public static @NotNull String hex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    // -- internals ---------------------------------------------------------

    private static double channel(int value) {
        double c = (value & 0xFF) / 255.0;
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    private static String css(Run run, boolean shadow) {
        StringBuilder css = new StringBuilder();
        if (run.color() != -1) {
            css.append("color:").append(hex(run.color())).append(';');
            if (shadow) {
                css.append("text-shadow:").append(textShadow(run.color())).append(';');
            }
        }
        if (run.bold()) {
            css.append("font-weight:bold;");
        }
        if (run.italic()) {
            css.append("font-style:italic;");
        }
        if (run.underlined() || run.strikethrough()) {
            css.append("text-decoration:")
                    .append(run.underlined() ? "underline" : "")
                    .append(run.underlined() && run.strikethrough() ? " " : "")
                    .append(run.strikethrough() ? "line-through" : "")
                    .append(';');
        }
        return css.toString();
    }

    private static void flush(List<Run> runs, StringBuilder text, @Nullable Style style) {
        if (text.isEmpty() || style == null) {
            return;
        }
        runs.add(new Run(text.toString(), style.color, style.bold, style.italic, style.underline, style.strike));
        text.setLength(0);
    }

    private static void parse(String s, Sink sink) {
        Style style = new Style();
        int len = s.length();
        int i = 0;
        while (i < len) {
            char c = s.charAt(i);

            if ((c == '<' || c == '{') && i + 8 < len && s.charAt(i + 1) == '#'
                    && s.charAt(i + 8) == (c == '<' ? '>' : '}')) {
                int rgb = parseHex(s, i + 2);
                if (rgb != -1) {
                    style = style.withColor(rgb);
                    i += 9;
                    continue;
                }
            }

            if ((c == '§' || c == '&') && i + 1 < len) {
                boolean section = c == '§';
                char code = Character.toLowerCase(s.charAt(i + 1));

                if (code == 'x') {
                    int rgb = parseBukkitHex(s, i + 2);
                    if (rgb != -1) {
                        style = style.withColor(rgb);
                        i += 14;
                        continue;
                    }
                } else if (code == '#') {
                    int rgb = parseHex(s, i + 2);
                    if (rgb != -1) {
                        style = style.withColor(rgb);
                        i += 8;
                        continue;
                    }
                } else {
                    int named = Character.digit(code, 16);
                    if (named != -1) {
                        style = style.withColor(PALETTE[named]);
                        i += 2;
                        continue;
                    }
                    Style formatted = style.withFormat(code);
                    if (formatted != null) {
                        style = formatted;
                        i += 2;
                        continue;
                    }
                }
                // Unknown or malformed code: the game hides stray § codes, while a
                // stray & is just an ampersand in someone's clan name.
                if (section) {
                    i += 2;
                    continue;
                }
            } else if (c == '§') {
                i++;
                continue;
            }

            sink.text(c, style);
            i++;
        }
    }

    /** Parses the six {@code §h} pairs that follow {@code §x}. */
    private static int parseBukkitHex(String s, int start) {
        if (start + 12 > s.length()) {
            return -1;
        }
        int rgb = 0;
        for (int pair = 0; pair < 6; pair++) {
            char prefix = s.charAt(start + pair * 2);
            int digit = Character.digit(s.charAt(start + pair * 2 + 1), 16);
            if ((prefix != '§' && prefix != '&') || digit == -1) {
                return -1;
            }
            rgb = (rgb << 4) | digit;
        }
        return rgb;
    }

    private static int parseHex(String s, int start) {
        if (start + 6 > s.length()) {
            return -1;
        }
        int rgb = 0;
        for (int k = 0; k < 6; k++) {
            int digit = Character.digit(s.charAt(start + k), 16);
            if (digit == -1) {
                return -1;
            }
            rgb = (rgb << 4) | digit;
        }
        return rgb;
    }

    private record Style(int color, boolean bold, boolean italic, boolean underline, boolean strike) {

        Style() {
            this(-1, false, false, false, false);
        }

        /** Like the game, a colour code clears any active formatting. */
        Style withColor(int rgb) {
            return new Style(rgb, false, false, false, false);
        }

        @Nullable Style withFormat(char code) {
            return switch (code) {
                case 'l' -> new Style(color, true, italic, underline, strike);
                case 'o' -> new Style(color, bold, true, underline, strike);
                case 'n' -> new Style(color, bold, italic, true, strike);
                case 'm' -> new Style(color, bold, italic, underline, true);
                case 'k' -> this; // obfuscated text can't be animated here
                case 'r' -> new Style();
                default -> null;
            };
        }
    }

    private interface Sink {
        void text(char c, Style style);
    }
}
