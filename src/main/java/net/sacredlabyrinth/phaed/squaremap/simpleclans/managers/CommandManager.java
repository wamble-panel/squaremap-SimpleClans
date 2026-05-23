package net.sacredlabyrinth.phaed.squaremap.simpleclans.managers;

import net.sacredlabyrinth.phaed.squaremap.simpleclans.SquaremapSimpleClans;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import net.sacredlabyrinth.phaed.simpleclans.Flags;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static net.sacredlabyrinth.phaed.squaremap.simpleclans.SquaremapSimpleClans.lang;

public final class CommandManager implements TabExecutor {

    private final @NotNull SquaremapSimpleClans plugin;

    public CommandManager(@NotNull SquaremapSimpleClans plugin) {
        this.plugin = plugin;
        PluginCommand cmd = Objects.requireNonNull(plugin.getCommand("clanmap"));
        cmd.setExecutor(this);
        cmd.setTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!command.getName().equalsIgnoreCase("clanmap")) return false;

        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
            help(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("reload")) return reload(sender);

        if (sub.equals("seticon") && args.length == 2 && sender instanceof Player p) {
            return setIcon(p, args[1]);
        }

        help(sender);
        return true;
    }

    private void help(@NotNull CommandSender sender) {
        plugin.getConfig().getStringList("help-command")
                .forEach(s -> sender.sendMessage(ChatColor.translateAlternateColorCodes('&', s)));
    }

    private boolean reload(@NotNull CommandSender sender) {
        if (!sender.hasPermission("simpleclans.map.reload")) {
            sender.sendMessage(lang("no-permission"));
            return true;
        }
        sender.sendMessage(plugin.reload() ? lang("reloaded") : lang("error-reload"));
        return true;
    }

    private boolean setIcon(@NotNull Player player, @NotNull String icon) {
        ClanPlayer cp = plugin.getClanManager().getClanPlayer(player);
        if (cp == null) {
            player.sendMessage(lang("not-member"));
            return true;
        }
        Clan clan = Objects.requireNonNull(cp.getClan());

        if (!player.hasPermission("simpleclans.map.seticon") || !cp.isLeader()) {
            player.sendMessage(lang("no-permission"));
            return true;
        }

        if (plugin.getHomesLayer() == null) {
            player.sendMessage(lang("layer-disabled"));
            return true;
        }

        String iconName = icon.toLowerCase();
        if (!plugin.getHomesLayer().getIconStorage().has(iconName)) {
            player.sendMessage(lang("icon-not-found"));
            return true;
        }

        if (!player.hasPermission("simpleclans.map.icon.bypass") &&
                !player.hasPermission("simpleclans.map.icon." + iconName)) {
            player.sendMessage(lang("no-permission"));
            return true;
        }

        Flags flags = new Flags(clan.getFlags());
        flags.put("defaulticon", iconName);
        clan.setFlags(flags.toJSONString());

        plugin.getHomesLayer().upsertMarker(clan);
        player.sendMessage(lang("icon-changed"));
        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                               @NotNull String alias, @NotNull String[] args) {
        if (!command.getName().equalsIgnoreCase("clanmap")) return Collections.emptyList();

        if (args.length == 1) return Arrays.asList("seticon", "reload", "help");

        if (args[0].equalsIgnoreCase("seticon")) {
            if (plugin.getHomesLayer() == null) return Collections.emptyList();
            if (!sender.hasPermission("simpleclans.map.list")) return Collections.emptyList();

            return plugin.getHomesLayer().getIconStorage().getIconNames()
                    .stream().sorted().collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
