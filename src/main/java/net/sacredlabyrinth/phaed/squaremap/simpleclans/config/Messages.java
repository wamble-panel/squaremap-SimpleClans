package net.sacredlabyrinth.phaed.squaremap.simpleclans.config;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Player-facing messages, written in MiniMessage format in config.yml. */
public final class Messages {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final @Nullable ConfigurationSection section;
    private final TagResolver prefix;

    public Messages(@Nullable ConfigurationSection section) {
        this.section = section;
        this.prefix = Placeholder.parsed("prefix", raw("prefix", ""));
    }

    public void send(@NotNull Audience audience, @NotNull String key, @NotNull TagResolver... placeholders) {
        String raw = raw(key, "<prefix><red>Missing message '" + key + "' in config.yml");
        if (!raw.isEmpty()) {
            audience.sendMessage(parse(raw, placeholders));
        }
    }

    public @NotNull Component get(@NotNull String key, @NotNull TagResolver... placeholders) {
        return parse(raw(key, key), placeholders);
    }

    private Component parse(String raw, TagResolver... placeholders) {
        return MINI_MESSAGE.deserialize(raw, TagResolver.resolver(prefix, TagResolver.resolver(placeholders)));
    }

    private String raw(String key, String def) {
        if (section == null) {
            return def;
        }
        String value = section.getString(key);
        return value != null ? value : def;
    }
}
