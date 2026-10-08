package net.sacredlabyrinth.phaed.squaremap.simpleclans.render;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyTextTest {

    /** "Illyria" with one Bukkit hex colour per letter, as SimpleClans stores it. */
    private static final String ILLYRIA = "§x§f§f§0§0§0§0I§x§f§f§8§0§0§0l§x§f§f§f§f§0§0l"
            + "§x§0§0§f§f§0§0y§x§0§0§f§f§f§fr§x§0§0§0§0§f§fi§x§8§0§0§0§f§fa";

    @Test
    void bukkitHexPerLetterRendersEveryLetterColoured() {
        String html = LegacyText.toHtml(ILLYRIA);
        assertFalse(html.contains("§"), html);
        assertFalse(html.contains("&x"), html);
        assertTrue(html.startsWith("<span style=\"color:#FF0000;text-shadow:1px 1px 0 #3F0000;\">I</span>"), html);
        assertTrue(html.contains("color:#8000FF;"), html);
        assertEquals("Illyria", LegacyText.strip(ILLYRIA));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "&x&a&b&c&d&e&fEternal  | Eternal | #ABCDEF",
            "&#FFAA00Crown          | Crown   | #FFAA00",
            "§#ffaa00Crown          | Crown   | #FFAA00",
            "<#123456>Germany       | Germany | #123456",
            "{#123456}french        | french  | #123456",
            "§8None                 | None    | #555555",
            "&cOwner                | Owner   | #FF5555",
    })
    void everyColourFormatIsTranslated(String input, String plain, String color) {
        assertEquals(plain, LegacyText.strip(input));
        assertEquals("<span style=\"color:" + color + ";text-shadow:1px 1px 0 "
                + LegacyText.hex(LegacyText.shadowOf(Integer.parseInt(color.substring(1), 16)))
                + ";\">" + plain + "</span>", LegacyText.toHtml(input));
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
        assertEquals("<span style=\"font-weight:bold;\">A</span>"
                        + "<span style=\"color:#55FF55;text-shadow:1px 1px 0 #153F15;\">B</span>",
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
