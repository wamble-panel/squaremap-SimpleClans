package net.sacredlabyrinth.phaed.squaremap.simpleclans.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.ClanFlags;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.IconRegistry;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.Perms;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.SquaremapSimpleClans;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.config.Messages;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The icon picker clan leaders open with {@code /clanmap icon}.
 *
 * <pre>
 *  [ ][ ][ ][ ][clan][ ][ ][ ][ ]     header: the clan and its current icon
 *  [w][l][g][b] |  [b][r][o][y]       icons, eight per row in two groups of four;
 *  [l][g][c][l] |  [b][p][m][p]       banners first, in creative-inventory order
 *  [&lt;][ ][ ][reset][map][x][ ][ ][&gt;]  footer: pages, reset, show/hide, close
 * </pre>
 * Each banner icon is shown as the banner item of the same dye colour.
 */
public final class IconMenu implements InventoryHolder {

    private static final int COLUMNS = 8;
    private static final int MAX_ICON_ROWS = 4;
    private static final int PER_PAGE = COLUMNS * MAX_ICON_ROWS;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    private final SquaremapSimpleClans plugin;
    private final Player player;
    private final Clan clan;
    private final List<String> icons;
    private final int iconRows;
    private final Inventory inventory;
    private final Map<Integer, Runnable> actions = new HashMap<>();
    private int page;

    private IconMenu(SquaremapSimpleClans plugin, Player player, Clan clan) {
        this.plugin = plugin;
        this.player = player;
        this.clan = clan;

        boolean showLocked = plugin.settings().menu().showLocked();
        List<String> visible = new ArrayList<>();
        for (String icon : plugin.icons().names()) {
            if (showLocked || Perms.canUseIcon(player, plugin.icons(), icon)) {
                visible.add(icon);
            }
        }
        this.icons = List.copyOf(visible);
        this.iconRows = Math.max(1, Math.min(MAX_ICON_ROWS, (icons.size() + COLUMNS - 1) / COLUMNS));
        this.inventory = Bukkit.createInventory(this, (iconRows + 2) * 9, messages().get("menu.title"));
    }

    public static void open(@NotNull SquaremapSimpleClans plugin, @NotNull Player player, @NotNull Clan clan) {
        IconMenu menu = new IconMenu(plugin, player, clan);
        menu.render();
        player.openInventory(menu.inventory);
        menu.sound(Sound.UI_LOOM_TAKE_RESULT, 1.2f);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    /** A click in the top inventory. */
    void click(int slot) {
        Runnable action = actions.get(slot);
        if (action != null) {
            action.run();
        }
    }

    // -- rendering ---------------------------------------------------------

    private void render() {
        inventory.clear();
        actions.clear();
        ItemStack filler = filler();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }

        String current = plugin.icons().effective(ClanFlags.icon(clan));
        boolean hidden = ClanFlags.hidden(clan);

        // header
        inventory.setItem(4, item(itemFor(current),
                LEGACY.deserialize(clan.getColorTag()).append(Component.text(" " + clan.getName(), NamedTextColor.WHITE)),
                List.of(messages().get("menu.info-icon", icon(current)),
                        messages().get(hidden ? "menu.info-hidden" : "menu.info-shown")),
                true));

        // icons
        int first = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && first + i < icons.size(); i++) {
            String icon = icons.get(first + i);
            int row = 1 + i / COLUMNS;
            int column = i % COLUMNS < 4 ? i % COLUMNS : i % COLUMNS + 1; // skip the middle column
            int slot = row * 9 + column;
            inventory.setItem(slot, iconItem(icon, icon.equals(current)));
            actions.put(slot, () -> choose(icon));
        }

        // footer
        int footer = (iconRows + 1) * 9;
        int pages = Math.max(1, (icons.size() + PER_PAGE - 1) / PER_PAGE);
        TagResolver pageInfo = TagResolver.resolver(Placeholder.unparsed("page", String.valueOf(page + 1)),
                Placeholder.unparsed("pages", String.valueOf(pages)));
        if (page > 0) {
            button(footer, Material.ARROW, "menu.previous", List.of(messages().get("menu.page", pageInfo)), () -> turn(-1));
        }
        if (page < pages - 1) {
            button(footer + 8, Material.ARROW, "menu.next", List.of(messages().get("menu.page", pageInfo)), () -> turn(1));
        }
        button(footer + 3, Material.BRUSH, "menu.reset",
                List.of(messages().get("menu.reset-lore", icon(plugin.icons().defaultIcon()))), () -> choose(null));
        if (player.hasPermission(Perms.HIDE)) {
            button(footer + 4, hidden ? Material.ENDER_PEARL : Material.ENDER_EYE,
                    hidden ? "menu.hidden" : "menu.shown",
                    List.of(messages().get(hidden ? "menu.click-to-show" : "menu.click-to-hide")),
                    () -> setHidden(!hidden));
        }
        button(footer + 5, Material.BARRIER, "menu.close", List.of(), player::closeInventory);
    }

    private ItemStack iconItem(String icon, boolean selected) {
        boolean unlocked = Perms.canUseIcon(player, plugin.icons(), icon);
        List<Component> lore = new ArrayList<>();
        if (selected) {
            lore.add(messages().get("menu.icon-selected"));
        } else if (unlocked) {
            lore.add(messages().get("menu.icon-available"));
        } else {
            lore.add(messages().get("menu.icon-locked"));
        }
        if (icon.equals(plugin.icons().defaultIcon())) {
            lore.add(messages().get("menu.icon-default"));
        }
        TextColor color = unlocked ? colorOf(icon) : NamedTextColor.DARK_GRAY;
        return item(itemFor(icon), Component.text(displayName(icon), color), lore, selected);
    }

    private void button(int slot, Material material, String nameKey, List<Component> lore, Runnable action) {
        inventory.setItem(slot, item(material, messages().get(nameKey), lore, false));
        actions.put(slot, action);
    }

    // -- actions -----------------------------------------------------------

    private void choose(@Nullable String icon) {
        if (!stillLeader()) {
            return;
        }
        String current = plugin.icons().effective(ClanFlags.icon(clan));
        String target = icon != null ? icon : plugin.icons().defaultIcon();
        if (target.equals(current) && (icon != null || ClanFlags.icon(clan) == null)) {
            sound(Sound.UI_BUTTON_CLICK, 1.5f);
            return;
        }
        if (icon != null && !Perms.canUseIcon(player, plugin.icons(), icon)) {
            sound(Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f);
            player.sendActionBar(messages().get("menu.locked-feedback", icon(icon)));
            return;
        }
        ClanFlags.setIcon(plugin.simpleClans(), clan, icon);
        plugin.updateClan(clan);
        sound(Sound.UI_LOOM_SELECT_PATTERN, 1f);
        player.sendActionBar(messages().get("menu.selected-feedback", icon(target)));
        render();
    }

    private void setHidden(boolean hidden) {
        if (!stillLeader() || !player.hasPermission(Perms.HIDE)) {
            return;
        }
        ClanFlags.setHidden(plugin.simpleClans(), clan, hidden);
        if (hidden) {
            plugin.removeClan(clan.getTag());
        }
        plugin.updateClan(clan);
        sound(hidden ? Sound.ENTITY_ENDERMAN_TELEPORT : Sound.BLOCK_END_PORTAL_FRAME_FILL, 1.2f);
        player.sendActionBar(messages().get(hidden ? "clan-hidden" : "clan-shown"));
        render();
    }

    private void turn(int delta) {
        page += delta;
        sound(Sound.ITEM_BOOK_PAGE_TURN, 1f);
        render();
    }

    /** Leadership can change while the menu is open. */
    private boolean stillLeader() {
        ClanPlayer clanPlayer = plugin.clanManager().getClanPlayer(player);
        if (clanPlayer == null || clanPlayer.getClan() != clan || !clanPlayer.isLeader()) {
            player.closeInventory();
            messages().send(player, "not-leader");
            return false;
        }
        return true;
    }

    // -- items -------------------------------------------------------------

    private Material itemFor(String icon) {
        Settings.Menu menu = plugin.settings().menu();
        if (menu.items().containsKey(icon)) {
            return menu.items().get(icon);
        }
        String color = IconRegistry.bannerColor(icon);
        if (color != null) {
            Material banner = Material.matchMaterial(color.toUpperCase(Locale.ROOT) + "_BANNER");
            if (banner != null) {
                return banner;
            }
        }
        return menu.customIconItem();
    }

    private static ItemStack item(Material material, Component name, List<Component> lore, boolean glint) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(plain(name));
            meta.lore(lore.stream().map(IconMenu::plain).toList());
            meta.addItemFlags(ItemFlag.values());
            if (glint) {
                meta.setEnchantmentGlintOverride(true);
            }
        });
        return item;
    }

    private static ItemStack filler() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        pane.editMeta(meta -> meta.setHideTooltip(true));
        return pane;
    }

    /** Item text is italic by default; menus read better upright. */
    private static Component plain(Component component) {
        return component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /** "banner_light_blue" becomes "Light Blue Banner", "my_castle" becomes "My Castle". */
    static String displayName(String icon) {
        String color = IconRegistry.bannerColor(icon);
        String words = color != null ? color + " banner" : icon;
        StringBuilder out = new StringBuilder();
        for (String word : words.split("[_\\-. ]+")) {
            if (!word.isEmpty()) {
                out.append(out.isEmpty() ? "" : " ").append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        return out.toString();
    }

    private static TextColor colorOf(String icon) {
        String color = IconRegistry.bannerColor(icon);
        if (color == null) {
            return NamedTextColor.WHITE;
        }
        if (color.equals("black")) {
            return TextColor.color(0x6E6E75); // dye black is unreadable on the tooltip background
        }
        return TextColor.color(DyeColor.valueOf(color.toUpperCase(Locale.ROOT)).getColor().asRGB());
    }

    private TagResolver icon(String icon) {
        return Placeholder.component("icon", Component.text(displayName(icon), colorOf(icon)));
    }

    private Messages messages() {
        return plugin.messages();
    }

    private void sound(Sound sound, float pitch) {
        player.playSound(player.getLocation(), sound, 0.7f, pitch);
    }
}
