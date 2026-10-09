package net.sacredlabyrinth.phaed.squaremap.simpleclans.render;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextComponentsTest {

    private static String content(Component component) {
        StringBuilder out = new StringBuilder();
        if (component instanceof TextComponent text) {
            out.append(text.content());
        }
        component.children().forEach(child -> out.append(content(child)));
        return out.toString();
    }

    @Test
    void everyHexFormatBecomesARealColour() {
        for (String tag : List.of("&#FFAA00Crown", "§x§f§f§a§a§0§0Crown", "&x&F&F&A&A&0&0Crown", "<#FFAA00>Crown")) {
            Component component = TextComponents.of(tag);
            assertEquals("Crown", content(component), tag);
            assertEquals(TextColor.color(0xFFAA00), component.children().get(0).color(), tag);
        }
    }

    @Test
    void perLetterGradientKeepsEveryColour() {
        Component component = TextComponents.of(LegacyTextTest.ILLYRIA);
        assertEquals("Illyria", content(component));
        assertEquals(7, component.children().size());
        assertEquals(TextColor.color(0x8000FF), component.children().get(6).color());
    }

    @Test
    void formattingCarriesOver() {
        Component bold = TextComponents.of("§6§lWAR").children().get(0);
        assertEquals(TextColor.color(0xFFAA00), bold.color());
        assertTrue(bold.hasDecoration(TextDecoration.BOLD));
    }

    @Test
    void emptyInputIsEmpty() {
        assertEquals("", content(TextComponents.of(null)));
        assertEquals("", content(TextComponents.of("")));
    }
}
