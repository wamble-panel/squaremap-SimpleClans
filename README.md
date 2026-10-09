# squaremap-SimpleClans

Shows [SimpleClans](https://github.com/RoinujNosde/SimpleClans) on [squaremap](https://github.com/jpenilla/squaremap):

- **Clan Homes**: a banner at every clan home, in the colour the clan's leader picks. Hovering shows the clan's tag in its Minecraft colours and who's online. Clicking shows leaders, members, KDR, allies, rivals, wars and territory.
- **Clan Territory**: the clan's protected land (WorldGuard, GriefPrevention and the other plugins SimpleClans hooks into), coloured by clan tag.
- **Recent Kills**: where PvP kills happened, labelled war / rival / ally / neutral / civilian, cleared after a few minutes.

## Requirements

| | Version |
|---|---|
| Server | Paper 1.21 or newer (or a fork such as Purpur) |
| Java | 21 |
| squaremap | 1.3 or newer |
| SimpleClans | 2.18 or newer |

## Install

1. Download the jar from [Releases](../../releases), or from the latest [Build run](../../actions/workflows/build.yml) (under *Artifacts*).
2. Put it in `plugins/` next to squaremap and SimpleClans, then restart.
3. Edit `plugins/squaremap-SimpleClans/config.yml` if you like, then run `/clanmap reload`.

Upgrading from 1.x? Your old config is kept as `config-v1.yml`, and icons in `images/clanhome/` keep working.

## Commands

| Command | Who | |
|---|---|---|
| `/clanmap icon` | clan leaders | Open the icon picker |
| `/clanmap icon <name>` | clan leaders | Pick an icon directly (`reset` for the default) |
| `/clanmap icons` | everyone | List icons; click one to pick it |
| `/clanmap hide` / `show` | clan leaders with `simpleclans.map.hide` | Hide the clan from the map |
| `/clanmap reload` | `simpleclans.map.reload` | Reload the config |

`/cmap` works too.

## Permissions

| Permission | Default | |
|---|---|---|
| `simpleclans.map.seticon` | everyone | Use `/clanmap icon` (clan leaders only) |
| `simpleclans.map.list` | everyone | Use `/clanmap icons` |
| `simpleclans.map.icon.banner_<color>` | nobody | Use that banner, e.g. `simpleclans.map.icon.banner_blue` |
| `simpleclans.map.icon.banners` | nobody | Use all 16 banners |
| `simpleclans.map.icon.<name>` | nobody | Use a custom icon |
| `simpleclans.map.icon.bypass` | op | Use any icon |
| `simpleclans.map.hide` | op | Use `/clanmap hide` and `show` |
| `simpleclans.map.reload` | op | Use `/clanmap reload` |

Every clan can always use the default icon (`banner_red` unless you change `default-icon`).

## Icons

The plugin comes with a pixel-art banner in each of Minecraft's 16 dye colours:

![The 16 banner icons](docs/banners.png)

`banner_white`, `banner_light_gray`, `banner_gray`, `banner_black`, `banner_brown`, `banner_red`, `banner_orange`, `banner_yellow`, `banner_lime`, `banner_green`, `banner_cyan`, `banner_light_blue`, `banner_blue`, `banner_purple`, `banner_magenta`, `banner_pink`

Clan leaders choose one with `/clanmap icon`. That opens a chest menu where each icon appears as the matching Minecraft banner. Locked icons are greyed out, and the menu also has a reset button and a show/hide toggle. Each icon needs its own permission, so you can sell them as ranks, rewards or perks.

To add your own, put PNG files in `plugins/squaremap-SimpleClans/icons/`. The file name is the icon name, so `castle.png` becomes `castle`. Run `/clanmap reload` and grant `simpleclans.map.icon.castle`. 32×32 pixel art looks best. Custom icons show as a painting in the menu; choose another item under `menu.items` in the config. A PNG named like a built-in banner (e.g. `banner_red.png`) replaces it. To change the kill icon, put a `kill.png` in the plugin folder.

## Configuration

Every option is explained in [`config.yml`](src/main/resources/config.yml). The main ones:

- `tooltip.rows`: what the click popup shows, in order.
- `layers.*.update-interval-seconds`: how often each layer redraws. Homes, tags, membership and diplomacy changes also show straight away.
- `layers.lands.territory-owners`: count only leaders' claims (`leaders`) or every member's (`members`).
- `layers.kills.types`: which kinds of kill appear.
- `messages`: every chat message, in [MiniMessage](https://docs.advntr.dev/minimessage/format) format.

## Building

```sh
./gradlew build
```

The jar ends up in `build/libs/`. GitHub Actions runs the same build on every push. Pushing a `v*` tag (e.g. `v2.0.0`) publishes a release with the jar attached.

Based on [Dynmap-SimpleClans](https://github.com/RoinujNosde/Dynmap-SimpleClans) by phaed, RoinujNosde and Minat0_.
