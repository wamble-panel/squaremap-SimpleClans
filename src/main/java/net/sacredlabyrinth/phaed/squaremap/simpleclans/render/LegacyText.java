package net.sacredlabyrinth.phaed.squaremap.simpleclans.render;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Converts Minecraft legacy-formatted text into HTML for squaremap tooltips.
 *
 * <p>Understands every colour format SimpleClans and common chat plugins produce,
 * with either {@code §} or {@code &} as the prefix:
 * <ul>
 *   <li>named colours {@code §0}–{@code §f}</li>
 *   <li>Bukkit hex {@code §x§R§R§G§G§B§B}</li>
 *   <li>short hex {@code §#RRGGBB}, plus {@code <#RRGGBB>} and {@code {#RRGGBB}}</li>
 *   <li>formatting {@code §l §o §n §m §k} and reset {@code §r}</li>
 * </ul>
 * Colours use the vanilla palette, and coloured text gets the same drop shadow the
 * game draws: the text colour at a quarter of its brightness, offset by one pixel.
 */
public final class LegacyText {

    /** Vanilla chat colours, indexed by code 0-f. */
    private static final int[] PALETTE = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };

    private LegacyText() {
    }

    /** Renders legacy text as HTML, with Minecraft-style shadows on coloured text. */
    public static @NotNull String toHtml(@Nullable String text) {
        return toHtml(text, true);
    }

    public static @NotNull String toHtml(@Nullable String text, boolean shadow) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        HtmlSink sink = new HtmlSink(shadow);
        parse(text, sink);
        return sink.finish();
    }

    /** Removes every colour and formatting code, returning plain text. */
    public static @NotNull String strip(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length());
        parse(text, new Sink() {
            @Override
            public void text(char c, Style style) {
                out.append(c);
            }
        });
        return out.toString();
    }

    /** The RGB value of the first colour code in {@code text}, or -1 if it has none. */
    public static int firstColor(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return -1;
        }
        int[] found = {-1};
        parse(text, new Sink() {
            @Override
            public void text(char c, Style style) {
                if (found[0] == -1 && style.color != -1) {
                    found[0] = style.color;
                }
            }
        });
        return found[0];
    }

    public static @NotNull String escape(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            appendEscaped(out, text.charAt(i));
        }
        return out.toString();
    }

    /** Minecraft's text shadow colour: each channel divided by four. */
    public static int shadowOf(int rgb) {
        return (rgb & 0xFCFCFC) >> 2;
    }

    public static @NotNull String hex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    // -- parser ------------------------------------------------------------

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
                    int rgb = i + 8 <= len ? parseHex(s, i + 2) : -1;
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

    private static void appendEscaped(StringBuilder out, char c) {
        switch (c) {
            case '&' -> out.append("&amp;");
            case '<' -> out.append("&lt;");
            case '>' -> out.append("&gt;");
            case '"' -> out.append("&quot;");
            case '\'' -> out.append("&#39;");
            default -> out.append(c);
        }
    }

    // -- style + sinks -----------------------------------------------------

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
                case 'k' -> this; // obfuscated text can't be animated in a tooltip
                case 'r' -> new Style();
                default -> null;
            };
        }

        boolean isPlain() {
            return color == -1 && !bold && !italic && !underline && !strike;
        }
    }

    private interface Sink {
        void text(char c, Style style);
    }

    private static final class HtmlSink implements Sink {
        private final boolean shadow;
        private final StringBuilder out = new StringBuilder();
        private final StringBuilder run = new StringBuilder();
        private @Nullable Style runStyle;

        HtmlSink(boolean shadow) {
            this.shadow = shadow;
        }

        @Override
        public void text(char c, Style style) {
            if (!style.equals(runStyle)) {
                flush();
                runStyle = style;
            }
            appendEscaped(run, c);
        }

        String finish() {
            flush();
            return out.toString();
        }

        private void flush() {
            if (run.isEmpty() || runStyle == null) {
                return;
            }
            if (runStyle.isPlain()) {
                out.append(run);
            } else {
                out.append("<span style=\"").append(css(runStyle)).append("\">").append(run).append("</span>");
            }
            run.setLength(0);
        }

        private String css(Style style) {
            StringBuilder css = new StringBuilder();
            if (style.color != -1) {
                css.append("color:").append(hex(style.color)).append(';');
                if (shadow) {
                    css.append("text-shadow:1px 1px 0 ").append(hex(shadowOf(style.color))).append(';');
                }
            }
            if (style.bold) {
                css.append("font-weight:bold;");
            }
            if (style.italic) {
                css.append("font-style:italic;");
            }
            if (style.underline || style.strike) {
                css.append("text-decoration:")
                        .append(style.underline ? "underline" : "")
                        .append(style.underline && style.strike ? " " : "")
                        .append(style.strike ? "line-through" : "")
                        .append(';');
            }
            return css.toString();
        }
    }
}
