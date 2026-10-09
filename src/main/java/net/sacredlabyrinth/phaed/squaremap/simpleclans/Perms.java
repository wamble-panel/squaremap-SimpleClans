package net.sacredlabyrinth.phaed.squaremap.simpleclans;

import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;

/** Permission nodes, declared in plugin.yml. */
public final class Perms {

    public static final String RELOAD = "simpleclans.map.reload";
    public static final String LIST = "simpleclans.map.list";
    public static final String SET_ICON = "simpleclans.map.seticon";
    public static final String ICON_BYPASS = "simpleclans.map.icon.bypass";
    public static final String ICON_PREFIX = "simpleclans.map.icon.";
    public static final String HIDE = "simpleclans.map.hide";

    private Perms() {
    }

    /** Everyone may use the default icon; any other needs simpleclans.map.icon.&lt;name&gt; or the bypass. */
    public static boolean canUseIcon(@NotNull Permissible who, @NotNull IconRegistry icons, @NotNull String icon) {
        return icon.equals(icons.defaultIcon())
                || who.hasPermission(ICON_BYPASS)
                || who.hasPermission(ICON_PREFIX + icon);
    }
}
