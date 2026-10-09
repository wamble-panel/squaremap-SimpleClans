package net.sacredlabyrinth.phaed.squaremap.simpleclans.render;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Legacy text as Adventure components, using {@link LegacyText}'s parser. */
public final class TextComponents {

    private TextComponents() {
    }

    /** Converts legacy text (any colour format {@link LegacyText} knows) into a component. */
    public static @NotNull Component of(@Nullable String legacy) {
        TextComponent.Builder builder = Component.text();
        for (LegacyText.Run run : LegacyText.runs(legacy)) {
            Style.Builder style = Style.style();
            if (run.color() != -1) {
                style.color(TextColor.color(run.color()));
            }
            if (run.bold()) {
                style.decoration(TextDecoration.BOLD, true);
            }
            if (run.italic()) {
                style.decoration(TextDecoration.ITALIC, true);
            }
            if (run.underlined()) {
                style.decoration(TextDecoration.UNDERLINED, true);
            }
            if (run.strikethrough()) {
                style.decoration(TextDecoration.STRIKETHROUGH, true);
            }
            builder.append(Component.text(run.text(), style.build()));
        }
        return builder.build();
    }
}
