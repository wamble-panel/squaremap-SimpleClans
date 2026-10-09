package net.sacredlabyrinth.phaed.squaremap.simpleclans.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.ClanFlags;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.IconRegistry;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.Perms;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.SquaremapSimpleClans;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.config.Messages;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.config.Settings;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.render.LegacyText;
import net.sacredlabyrinth.phaed.squaremap.simpleclans.render.TextComponents;
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
 *  ▒▒▒▒[clan]▒▒▒▒                       header: the clan, its icon and unlock progress
 *  [w][l][g][b] ┃ [b][r][o][y]          icons, eight per row in two groups of four;
 *  [l][g][c][l] ┃ [b][p][m][p]          banners first, in creative-inventory order
 *  ▒[&lt;]▒[reset][map][x]▒[&gt;]▒           footer: pages, reset, show/hide, close
 * </pre>
 * The frame (▒) is stained glass in the colour of the clan's current icon, so picking an
 * icon recolours the whole menu. Banner icons are shown as the banner of the same colour.
 */
public final class IconMenu implements InventoryHolder {

    private static final int COLUMNS = 8;
    private static final int MAX_ICON_ROWS = 4;
    private static final int PER_PAGE = COLUMNS * MAX_ICON_ROWS;
    private static final int MAX_BAR = 32;
    private static final TextColor LOCKED = TextColor.color(0x5A5A60);
    private static final TextColor BAR_LOCKED = TextColor.color(0x3A3A3F);

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
        for (String icon : registry().names()) {
            if (showLocked || unlocked(icon)) {
                visible.add(icon);
            }
        }
        this.icons = List.copyOf(visible);
        this.iconRows = Math.max(1, Math.min(MAX_ICON_ROWS, (icons.size() + COLUMNS - 1) / COLUMNS));
        this.inventory = Bukkit.createInventory(this, (iconRows + 2) * 9,
                messages().get("menu.title", Placeholder.component("clan", TextComponents.of(clan.getColorTag()))));
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

        String current = registry().effective(ClanFlags.icon(clan));
        boolean hidden = ClanFlags.hidden(clan);
        int footer = (iconRows + 1) * 9;

        // frame and background
        ItemStack frame = pane(frameFor(current));
        ItemStack divider = pane(Material.GRAY_STAINED_GLASS_PANE);
        ItemStack empty = pane(Material.BLACK_STAINED_GLASS_PANE);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            boolean edge = slot < 9 || slot >= footer;
            inventory.setItem(slot, edge ? frame : slot % 9 == 4 ? divider : empty);
        }

        inventory.setItem(4, header(current, hidden));

        // icons
        int first = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && first + i < icons.size(); i++) {
            String icon = icons.get(first + i);
            int column = i % COLUMNS < 4 ? i % COLUMNS : i % COLUMNS + 1; // skip the divider
            int slot = (1 + i / COLUMNS) * 9 + column;
            inventory.setItem(slot, iconItem(icon, icon.equals(current)));
            actions.put(slot, () -> choose(icon));
        }

        // footer
        int pages = Math.max(1, (icons.size() + PER_PAGE - 1) / PER_PAGE);
        TagResolver pageInfo = TagResolver.resolver(Placeholder.unparsed("page", String.valueOf(page + 1)),
                Placeholder.unparsed("pages", String.valueOf(pages)));
        if (page > 0) {
            button(footer + 1, Material.ARROW, "menu.previous", List.of(messages().get("menu.page", pageInfo)), () -> turn(-1));
        }
        if (page < pages - 1) {
            button(footer + 7, Material.ARROW, "menu.next", List.of(messages().get("menu.page", pageInfo)), () -> turn(1));
        }
        button(footer + 3, Material.BRUSH, "menu.reset",
                List.of(messages().get("menu.reset-lore", iconName(registry().defaultIcon()))), () -> choose(null));
        if (player.hasPermission(Perms.HIDE)) {
            button(footer + 4, hidden ? Material.ENDER_PEARL : Material.ENDER_EYE,
                    hidden ? "menu.hidden" : "menu.shown",
                    List.of(messages().get(hidden ? "menu.click-to-show" : "menu.click-to-hide")),
                    () -> setHidden(!hidden));
        }
        button(footer + 5, Material.BARRIER, "menu.close", List.of(), player::closeInventory);
    }

    private ItemStack header(String current, boolean hidden) {
        List<Component> lore = new ArrayList<>();
        lore.add(messages().get("menu.info-icon", iconName(current)));
        int color = registry().color(current);
        if (color != -1) {
            lore.add(messages().get("menu.info-color", swatch(color)));
        }
        lore.add(Component.empty());

        int unlocked = 0;
        for (String icon : registry().names()) {
            if (unlocked(icon)) {
                unlocked++;
            }
        }
        lore.add(messages().get("menu.info-unlocked",
                Placeholder.unparsed("unlocked", String.valueOf(unlocked)),
                Placeholder.unparsed("total", String.valueOf(registry().names().size()))));
        if (registry().names().size() <= MAX_BAR) {
            lore.add(progressBar());
        }
        lore.add(Component.empty());
        lore.add(messages().get(hidden ? "menu.info-hidden" : "menu.info-shown"));

        Component name = TextComponents.of(clan.getColorTag())
                .append(Component.text(" " + LegacyText.strip(clan.getName()), NamedTextColor.WHITE));
        return item(itemFor(current), name, lore, true);
    }

    private ItemStack iconItem(String icon, boolean selected) {
        boolean unlocked = unlocked(icon);
        List<Component> lore = new ArrayList<>();
        int color = registry().color(icon);
        if (color != -1) {
            lore.add(messages().get("menu.icon-color", swatch(color)));
            lore.add(Component.empty());
        }
        if (selected) {
            lore.add(messages().get("menu.icon-selected"));
        } else if (unlocked) {
            lore.add(messages().get("menu.icon-available"));
        } else {
            lore.add(messages().get("menu.icon-locked"));
            if (!messages().isBlank("menu.icon-locked-hint")) {
                lore.add(messages().get("menu.icon-locked-hint"));
            }
        }
        if (icon.equals(registry().defaultIcon())) {
            lore.add(messages().get("menu.icon-default"));
        }

        Component name = Component.text(displayName(icon), unlocked ? readable(color) : LOCKED);
        if (selected) {
            name = name.decorate(TextDecoration.BOLD);
        }
        return item(itemFor(icon), name, lore, selected);
    }

    /** One square per icon, in the icon's colour when unlocked. */
    private Component progressBar() {
        TextComponent.Builder bar = Component.text();
        for (String icon : registry().names()) {
            bar.append(Component.text("■", unlocked(icon) ? readable(registry().color(icon)) : BAR_LOCKED));
        }
        return bar.build();
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
        String current = registry().effective(ClanFlags.icon(clan));
        String target = icon != null ? icon : registry().defaultIcon();
        if (target.equals(current) && (icon != null || ClanFlags.icon(clan) == null)) {
            sound(Sound.UI_BUTTON_CLICK, 1.5f);
            return;
        }
        if (icon != null && !unlocked(icon)) {
            sound(Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f);
            player.sendActionBar(messages().get("menu.locked-feedback", iconName(icon)));
            return;
        }
        ClanFlags.setIcon(plugin.simpleClans(), clan, icon);
        plugin.updateClan(clan);
        sound(Sound.UI_LOOM_SELECT_PATTERN, 1f);
        player.sendActionBar(messages().get("menu.selected-feedback", iconName(target)));
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

    // -- items and colours -------------------------------------------------

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

    /** Stained glass in the dye colour closest to the icon's colour. */
    private Material frameFor(String icon) {
        int rgb = registry().color(icon);
        if (rgb == -1) {
            return Material.GRAY_STAINED_GLASS_PANE;
        }
        DyeColor nearest = DyeColor.WHITE;
        long best = Long.MAX_VALUE;
        for (DyeColor dye : DyeColor.values()) {
            int other = dye.getColor().asRGB();
            long dr = (rgb >> 16 & 0xFF) - (other >> 16 & 0xFF);
            long dg = (rgb >> 8 & 0xFF) - (other >> 8 & 0xFF);
            long db = (rgb & 0xFF) - (other & 0xFF);
            long distance = dr * dr * 3 + dg * dg * 4 + db * db * 2;
            if (distance < best) {
                best = distance;
                nearest = dye;
            }
        }
        Material pane = Material.matchMaterial(nearest.name() + "_STAINED_GLASS_PANE");
        return pane != null ? pane : Material.GRAY_STAINED_GLASS_PANE;
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

    private static ItemStack pane(Material material) {
        ItemStack pane = new ItemStack(material);
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

    /** The colour itself, lifted toward white when it would vanish on the dark tooltip. */
    private static TextColor readable(int rgb) {
        if (rgb == -1) {
            return NamedTextColor.WHITE;
        }
        if (!LegacyText.isDark(rgb)) {
            return TextColor.color(rgb);
        }
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF;
        return TextColor.color(r + (255 - r) * 2 / 5, g + (255 - g) * 2 / 5, b + (255 - b) * 2 / 5);
    }

    /** {@code <swatch>} (a block of the colour) and {@code <hex>} for menu texts. */
    private static TagResolver swatch(int rgb) {
        return TagResolver.resolver(
                Placeholder.component("swatch", Component.text("■■", readable(rgb))),
                Placeholder.unparsed("hex", LegacyText.hex(rgb)));
    }

    private TagResolver iconName(String icon) {
        return Placeholder.component("icon", Component.text(displayName(icon), readable(registry().color(icon))));
    }

    private boolean unlocked(String icon) {
        return Perms.canUseIcon(player, registry(), icon);
    }

    private IconRegistry registry() {
        return plugin.icons();
    }

    private Messages messages() {
        return plugin.messages();
    }

    private void sound(Sound sound, float pitch) {
        player.playSound(player.getLocation(), sound, 0.7f, pitch);
    }
}
