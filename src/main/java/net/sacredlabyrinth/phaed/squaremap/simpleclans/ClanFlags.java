package net.sacredlabyrinth.phaed.squaremap.simpleclans;

import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.Flags;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/** Per-clan map settings, stored in SimpleClans' clan flags so they follow the clan's storage. */
public final class ClanFlags {

    /** Same key 1.x used, so icons chosen before the upgrade are kept. */
    private static final String ICON = "defaulticon";
    private static final String HIDDEN = "squaremap-hidden";

    private ClanFlags() {
    }

    public static @Nullable String icon(@NotNull Clan clan) {
        String icon = read(clan).getString(ICON);
        return icon == null || icon.isBlank() ? null : icon.toLowerCase(Locale.ROOT);
    }

    public static boolean hidden(@NotNull Clan clan) {
        return read(clan).getBoolean(HIDDEN, false);
    }

    public static void setIcon(@NotNull SimpleClans simpleClans, @NotNull Clan clan, @Nullable String icon) {
        Flags flags = read(clan);
        flags.put(ICON, icon == null ? "" : icon);
        save(simpleClans, clan, flags);
    }

    public static void setHidden(@NotNull SimpleClans simpleClans, @NotNull Clan clan, boolean hidden) {
        Flags flags = read(clan);
        flags.put(HIDDEN, hidden);
        save(simpleClans, clan, flags);
    }

    private static Flags read(Clan clan) {
        try {
            return new Flags(clan.getFlags());
        } catch (RuntimeException ex) {
            // corrupt flag JSON shouldn't take the whole map down
            return new Flags(null);
        }
    }

    private static void save(SimpleClans simpleClans, Clan clan, Flags flags) {
        clan.setFlags(flags.toJSONString());
        simpleClans.getStorageManager().updateClan(clan);
    }
}
