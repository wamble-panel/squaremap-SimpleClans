# Clan icon vouchers

Ready-made CrazyVouchers vouchers for the 16 banner icons, plus a bundle that unlocks all of them.

| File | Item | Grants |
|---|---|---|
| `clan_icon_banner_<color>.yml` (×16) | the banner of that colour | `simpleclans.map.icon.banner_<color>` |
| `clan_icon_banner_bundle.yml` | Loom | `simpleclans.map.icon.banners` (all 16) |

Each voucher:

- grants its permission through LuckPerms (`lp user {player} permission set …`)
- can't be redeemed twice: it's blacklisted for anyone who already has that permission
- uses two-step confirmation, the challenge-complete sound and fireworks in the banner's colours
- shows its name in the banner's own colour, with small-caps lore

## Install

1. Copy the `.yml` files into `plugins/CrazyVouchers/vouchers/`.
2. Run `/crazyvouchers reload`.
3. Give one out: `/crazyvouchers give clan_icon_banner_red 1 <player>`.

Permissions are granted to whoever redeems the voucher. The icon can only be picked by a **clan leader** (`/clanmap icon`), so a member who redeems one needs to be (or become) a leader to use it.

## LuckPerms context

The commands grant the permission globally. On a network where squaremap only runs on one server, you can limit it the same way your claim-colour vouchers do by adding `server=<name>`:

```yaml
commands:
- lp user {player} permission set simpleclans.map.icon.banner_red true server=earth
```
