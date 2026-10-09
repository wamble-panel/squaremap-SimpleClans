package net.sacredlabyrinth.phaed.squaremap.simpleclans.render;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyTextTest {

    /** "Illyria" with one Bukkit hex colour per letter, as SimpleClans stores it. */
    static final String ILLYRIA = "§x§f§f§0§0§0§0I§x§f§f§8§0§0§0l§x§f§f§f§f§0§0l"
            + "§x§0§0§f§f§0§0y§x§0§0§f§f§f§fr§x§0§0§0§0§f§fi§x§8§0§0§0§f§fa";

    private static String span(int rgb, String text) {
        return "<span style=\"color:" + LegacyText.hex(rgb) + ";text-shadow:" + LegacyText.textShadow(rgb) + ";\">"
                + text + "</span>";
    }

    @Test
    void bukkitHexPerLetterRendersEveryLetterColoured() {
        String html = LegacyText.toHtml(ILLYRIA);
        assertFalse(html.contains("§"), html);
        assertFalse(html.contains("&x"), html);
        assertTrue(html.startsWith(span(0xFF0000, "I")), html);
        assertTrue(html.contains("color:#8000FF;"), html);
        assertEquals("Illyria", LegacyText.strip(ILLYRIA));
        assertEquals(7, LegacyText.runs(ILLYRIA).size());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "&x&a&b&c&d&e&fEternal  | Eternal | ABCDEF",
            "&#FFAA00Crown          | Crown   | FFAA00",
            "§#ffaa00Crown          | Crown   | FFAA00",
            "<#123456>Germany       | Germany | 123456",
            "{#123456}french        | french  | 123456",
            "§8None                 | None    | 555555",
            "&cOwner                | Owner   | FF5555",
    })
    void everyColourFormatIsTranslated(String input, String plain, String color) {
        int rgb = Integer.parseInt(color, 16);
        assertEquals(plain, LegacyText.strip(input));
        assertEquals(span(rgb, plain), LegacyText.toHtml(input));
        assertEquals(List.of(new LegacyText.Run(plain, rgb, false, false, false, false)), LegacyText.runs(input));
    }

    @Test
    void brightColoursKeepTheMinecraftShadow() {
        assertEquals("1px 1px 0 #3F2A00", LegacyText.textShadow(0xFFAA00)); // §6 gold
        assertEquals("1px 1px 0 #153F15", LegacyText.textShadow(0x55FF55)); // §a green
        assertEquals("1px 1px 0 #3F3F3F", LegacyText.textShadow(0xFFFFFF)); // §f white
    }

    @Test
    void darkColoursGlowSoTheyStayReadable() {
        for (int dark : new int[]{0x000000, 0x0000AA, 0xAA0000, 0x555555, 0x123456}) {
            assertTrue(LegacyText.isDark(dark), LegacyText.hex(dark));
            assertTrue(LegacyText.textShadow(dark).contains("rgba(255,255,255"), LegacyText.hex(dark));
        }
        for (int bright : new int[]{0xFFFF55, 0x55FFFF, 0xFF5555, 0x5555FF, 0xFFAA00}) {
            assertFalse(LegacyText.isDark(bright), LegacyText.hex(bright));
        }
    }

    @Test
    void plainTextPassesThroughEscaped() {
        assertEquals("Germany", LegacyText.toHtml("Germany"));
        assertEquals("Rock &amp; Roll &lt;b&gt;", LegacyText.toHtml("Rock & Roll <b>"));
    }

    @Test
    void formattingAndResetMatchTheGame() {
        assertEquals("<span style=\"color:#FFAA00;text-shadow:1px 1px 0 #3F2A00;font-weight:bold;\">A</span>B",
                LegacyText.toHtml("§6§lA§rB"));
        // a colour code clears bold, like in game
        assertEquals("<span style=\"font-weight:bold;\">A</span>" + span(0x55FF55, "B"),
                LegacyText.toHtml("§lA§aB"));
    }

    @Test
    void malformedCodesNeverLeak() {
        assertEquals("ab", LegacyText.strip("a§x§fb"));
        assertEquals("tag", LegacyText.strip("§ztag§"));
        assertEquals("&xyz", LegacyText.strip("&xyz"));
        assertEquals("&#12", LegacyText.strip("&#12"));
    }

    @Test
    void firstColorFindsTagColour() {
        assertEquals(0xFF5555, LegacyText.firstColor("§cRED"));
        assertEquals(0xABCDEF, LegacyText.firstColor("§x§a§b§c§d§e§fX"));
        assertEquals(-1, LegacyText.firstColor("plain"));
        assertEquals(-1, LegacyText.firstColor("§c"));
    }

    @Test
    void withoutShadow() {
        assertEquals("<span style=\"color:#FF5555;\">x</span>", LegacyText.toHtml("§cx", false));
    }
}
