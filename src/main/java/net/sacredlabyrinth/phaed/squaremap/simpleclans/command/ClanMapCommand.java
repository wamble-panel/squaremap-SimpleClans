package net.sacredlabyrinth.phaed.squaremap.simpleclans.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.ClanFlags;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.Perms;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.SquaremapSimpleClans;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.config.Messages;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.menu.IconMenu;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.logging.Level;

/**
 * {@code /clanmap} (alias {@code /cmap}): icon (opens the picker), icons, hide, show, reload.
 * Registered through Paper's Brigadier API, so players get proper tab completion.
 */
public final class ClanMapCommand {

    private static final String PERM_RELOAD = Perms.RELOAD;
    private static final String PERM_LIST = Perms.LIST;
    private static final String PERM_ICON = Perms.SET_ICON;
    private static final String PERM_HIDE = Perms.HIDE;

    private final SquaremapSimpleClans plugin;

    private ClanMapCommand(SquaremapSimpleClans plugin) {
        this.plugin = plugin;
    }

    public static void register(@NotNull SquaremapSimpleClans plugin) {
        ClanMapCommand command = new ClanMapCommand(plugin);
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(command.tree(), "Clan map settings for squaremap", List.of("cmap")));
    }

    private LiteralCommandNode<CommandSourceStack> tree() {
        return Commands.literal("clanmap")
                .executes(ctx -> help(sender(ctx)))
                .then(Commands.literal("help").executes(ctx -> help(sender(ctx))))
                .then(Commands.literal("icon")
                        .requires(permission(PERM_ICON))
                        .executes(ctx -> openMenu(sender(ctx)))
                        .then(Commands.literal("reset").executes(ctx -> setIcon(sender(ctx), null)))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(this::suggestIcons)
                                .executes(ctx -> setIcon(sender(ctx), StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("icons")
                        .requires(permission(PERM_LIST).or(permission(PERM_ICON)))
                        .executes(ctx -> listIcons(sender(ctx))))
                .then(Commands.literal("hide")
                        .requires(permission(PERM_HIDE))
                        .executes(ctx -> setHidden(sender(ctx), true)))
                .then(Commands.literal("show")
                        .requires(permission(PERM_HIDE))
                        .executes(ctx -> setHidden(sender(ctx), false)))
                .then(Commands.literal("reload")
                        .requires(permission(PERM_RELOAD))
                        .executes(ctx -> reload(sender(ctx))))
                .build();
    }

    // -- subcommands -------------------------------------------------------

    private int help(CommandSender sender) {
        Messages messages = plugin.messages();
        messages.send(sender, "help.header");
        if (sender.hasPermission(PERM_ICON)) {
            messages.send(sender, "help.icon");
        }
        if (sender.hasPermission(PERM_LIST) || sender.hasPermission(PERM_ICON)) {
            messages.send(sender, "help.icons");
        }
        if (sender.hasPermission(PERM_HIDE)) {
            messages.send(sender, "help.hide");
        }
        if (sender.hasPermission(PERM_RELOAD)) {
            messages.send(sender, "help.reload");
        }
        return Command.SINGLE_SUCCESS;
    }

    private int openMenu(CommandSender sender) {
        Clan clan = leaderClan(sender);
        if (clan == null) {
            return Command.SINGLE_SUCCESS;
        }
        if (plugin.homes() == null) {
            plugin.messages().send(sender, "layer-disabled");
            return Command.SINGLE_SUCCESS;
        }
        IconMenu.open(plugin, (Player) sender, clan);
        return Command.SINGLE_SUCCESS;
    }

    private int setIcon(CommandSender sender, @Nullable String requested) {
        Messages messages = plugin.messages();
        Clan clan = leaderClan(sender);
        if (clan == null) {
            return Command.SINGLE_SUCCESS;
        }
        if (plugin.homes() == null) {
            messages.send(sender, "layer-disabled");
            return Command.SINGLE_SUCCESS;
        }
        if (requested == null) {
            ClanFlags.setIcon(plugin.simpleClans(), clan, null);
            plugin.updateClan(clan);
            messages.send(sender, "icon-reset");
            return Command.SINGLE_SUCCESS;
        }

        if (!plugin.icons().has(requested)) {
            messages.send(sender, "icon-not-found", Placeholder.unparsed("icon", requested.toLowerCase(Locale.ROOT)));
            return Command.SINGLE_SUCCESS;
        }
        String icon = plugin.icons().effective(requested);
        if (!canUse(sender, icon)) {
            messages.send(sender, "no-permission");
            return Command.SINGLE_SUCCESS;
        }
        ClanFlags.setIcon(plugin.simpleClans(), clan, icon);
        plugin.updateClan(clan);
        messages.send(sender, "icon-changed", Placeholder.unparsed("icon", icon));
        return Command.SINGLE_SUCCESS;
    }

    private int listIcons(CommandSender sender) {
        Messages messages = plugin.messages();
        List<Component> entries = new ArrayList<>();
        for (String icon : plugin.icons().names()) {
            if (!canUse(sender, icon)) {
                continue;
            }
            entries.add(Component.text(icon, NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.runCommand("/clanmap icon " + icon))
                    .hoverEvent(HoverEvent.showText(messages.get("icons-hover", Placeholder.unparsed("icon", icon)))));
        }
        if (entries.isEmpty()) {
            messages.send(sender, "icons-empty");
            return Command.SINGLE_SUCCESS;
        }
        messages.send(sender, "icons-header", Placeholder.unparsed("count", String.valueOf(entries.size())));
        sender.sendMessage(Component.join(JoinConfiguration.separator(Component.text(", ", NamedTextColor.DARK_GRAY)),
                entries));
        return Command.SINGLE_SUCCESS;
    }

    private int setHidden(CommandSender sender, boolean hidden) {
        Clan clan = leaderClan(sender);
        if (clan == null) {
            return Command.SINGLE_SUCCESS;
        }
        ClanFlags.setHidden(plugin.simpleClans(), clan, hidden);
        if (hidden) {
            plugin.removeClan(clan.getTag());
        }
        plugin.updateClan(clan);
        plugin.messages().send(sender, hidden ? "clan-hidden" : "clan-shown");
        return Command.SINGLE_SUCCESS;
    }

    private int reload(CommandSender sender) {
        try {
            plugin.reload();
            plugin.messages().send(sender, "reloaded");
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.SEVERE, "Reload failed", ex);
            plugin.messages().send(sender, "reload-failed");
        }
        return Command.SINGLE_SUCCESS;
    }

    // -- helpers -----------------------------------------------------------

    /** The sender's clan if they lead one; otherwise tells them why not and returns null. */
    private @Nullable Clan leaderClan(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "players-only");
            return null;
        }
        ClanPlayer clanPlayer = plugin.clanManager().getClanPlayer(player);
        Clan clan = clanPlayer != null ? clanPlayer.getClan() : null;
        if (clan == null) {
            plugin.messages().send(sender, "not-in-clan");
            return null;
        }
        if (!clanPlayer.isLeader()) {
            plugin.messages().send(sender, "not-leader");
            return null;
        }
        return clan;
    }

    private boolean canUse(CommandSender sender, String icon) {
        return Perms.canUseIcon(sender, plugin.icons(), icon);
    }

    private CompletableFuture<Suggestions> suggestIcons(CommandContext<CommandSourceStack> ctx,
                                                        SuggestionsBuilder builder) {
        CommandSender sender = sender(ctx);
        String typed = builder.getRemainingLowerCase();
        if ("reset".startsWith(typed)) {
            builder.suggest("reset");
        }
        for (String icon : plugin.icons().names()) {
            if (icon.startsWith(typed) && canUse(sender, icon)) {
                builder.suggest(icon);
            }
        }
        return builder.buildFuture();
    }

    private static CommandSender sender(CommandContext<CommandSourceStack> ctx) {
        return ctx.getSource().getSender();
    }

    private static Predicate<CommandSourceStack> permission(String permission) {
        return source -> source.getSender().hasPermission(permission);
    }
}
