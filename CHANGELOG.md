# Changelog

## 2.0.0 — squaremap rebuild

Rewritten from scratch for squaremap, Paper 1.21+ and Java 21.

### Map
* Tooltips use squaremap's own look. Clan tags appear as dark nameplates (like squaremap's player nameplates) in their real Minecraft colours with the in-game text shadow.
* Every colour format renders: `§x§R§R§G§G§B§B`, `&x&R&R…`, `&#RRGGBB`, `<#RRGGBB>`, `{#RRGGBB}`, named colours, bold/italic/underline/strikethrough. No more raw `&x`/`§8` codes in tooltips.
* Hover shows the tag, name and online count. Clicking shows a popup with the configured rows: leaders (with heads), members, KDR, allies, rivals, wars, territory, description. Empty rows are skipped.
* Territory draws every claim separately, coloured by clan tag. GriefPrevention claims, which SimpleClans reports as two corners, now render, and the full block area is covered.
* Territory includes land owned by leaders (or all members) on every world, not only the claim the home is in.
* Markers refresh on a timer, plus instantly on home, tag, membership, ally, rival and war changes, so tooltips stay current.
* Kill markers say what kind of kill it was (war, rival, ally, neutral, civilian), can be filtered by type, and are capped.
* New pixel-art icons for homes and kills.
* Worlds loaded after startup get layers too.

### Commands
* `/clanmap` (alias `/cmap`) uses Paper's Brigadier API for real tab completion.
* `/clanmap icon <name|reset>`, `/clanmap icons` (clickable list), `/clanmap hide` / `show`, `/clanmap reload`.
* Clan icon choices are now saved to SimpleClans storage, so they survive restarts.
* Messages use MiniMessage.

### Build
* Gradle (Kotlin DSL) replaces Maven, and Java replaces the Kotlin/Java mix. No shaded libraries.
* GitHub Actions builds every push and attaches the jar to `v*` tag releases.

### Upgrading from 1.x
* The config layout changed. The old `config.yml` is renamed to `config-v1.yml` on first start.
* Icons in `images/clanhome/` still load. New icons go in `icons/`.

---

Entries below are from the original Dynmap-SimpleClans project.

## [2.0.2](https://github.com/RoinujNosde/Dynmap-SimpleClans/compare/v2.0.1...v2.0.2) (2022-06-16)


### Bug Fixes

* don't send messages during tab complete ([ffebda7](https://github.com/RoinujNosde/Dynmap-SimpleClans/commit/ffebda7ae3d625b3045829fb71dc7a65a823e2e2))

## [2.0.1](https://github.com/RoinujNosde/Dynmap-SimpleClans/compare/v2.0.0...v2.0.1) (2022-06-14)


### Bug Fixes

* dynmap not found on Spigot ([35aae4d](https://github.com/RoinujNosde/Dynmap-SimpleClans/commit/35aae4d964b9fe2a16223f229b6565b4b80b0f98))

## [2.0.0](https://github.com/RoinujNosde/Dynmap-SimpleClans/compare/v1.4.2...v2.0.0) (2022-06-10)


### Features

* add time placeholder for KillsLayer ([2d041ee](https://github.com/RoinujNosde/Dynmap-SimpleClans/commit/2d041ee0354808a09f74c3155af1afca185a57b5))
* HEXColor#of accept char or string ([ee6cd60](https://github.com/RoinujNosde/Dynmap-SimpleClans/commit/ee6cd6008d7e8e05eba3e5af331929c2655de8e3))
* LandsLayer implementation ([859b5d1](https://github.com/RoinujNosde/Dynmap-SimpleClans/commit/859b5d127efc3cdf1b287ce65d625235b0979c6b))


### Bug Fixes

* consider throwing exception if markerApi is null ([58492e2](https://github.com/RoinujNosde/Dynmap-SimpleClans/commit/58492e2f646f524c2a5405470bc0c4fb2f205c01))
* delete markers when home was cleared ([c191463](https://github.com/RoinujNosde/Dynmap-SimpleClans/commit/c1914635d9cfb59af5ccd9ebe91496c27835a506))
* delete old icons, NPE on parsing getFlags ([3d8c471](https://github.com/RoinujNosde/Dynmap-SimpleClans/commit/3d8c47152f8d61c16dcf77b7516facb39bf928b4))
* delete unused plugin ([d9a56ef](https://github.com/RoinujNosde/Dynmap-SimpleClans/commit/d9a56efb61be94053d6b12199804a3b9c262363a))
* don't create tasks every reloading ([9ad29b4](https://github.com/RoinujNosde/Dynmap-SimpleClans/commit/9ad29b4b076fbe79c8f484a2222da21f60cb714e))
* excessive configuration call ([5848d15](https://github.com/RoinujNosde/Dynmap-SimpleClans/commit/5848d1567975d19f6fbac89d4b9a87e14dd746c9))
* not relative path on Dynmap-SimpleClans.iml ([4407155](https://github.com/RoinujNosde/Dynmap-SimpleClans/commit/44071555f2dbcb970658aabf7999854fb6051c2f))
* reloading even after exception throwing ([0283fae](https://github.com/RoinujNosde/Dynmap-SimpleClans/commit/0283fae2b27c9f5d85824e61a0a4b20a31a389b0))
* remove meaningless debug about coordinates' finding ([20d15e4](https://github.com/RoinujNosde/Dynmap-SimpleClans/commit/20d15e4826f1258f83d8a2da318389c7f0dbabd6))
* reversed hiding logic on KillsLayer ([0c998e7](https://github.com/RoinujNosde/Dynmap-SimpleClans/commit/0c998e78584cf120e27101e290928103ac974eb8))
* update HomeLayer on tag modifying ([35972b5](https://github.com/RoinujNosde/Dynmap-SimpleClans/commit/35972b5cfd041973611cf78d69fb901763e7fb8d))
* update Layers priority ([d2a82cf](https://github.com/RoinujNosde/Dynmap-SimpleClans/commit/d2a82cfb0b0ef1cc2afe7f2cc3ce92a310f21b9b))
* use correct dependency name for DynMap ([cc1de92](https://github.com/RoinujNosde/Dynmap-SimpleClans/commit/cc1de92e6847fc5a51045b6f99a0602a0dc00c89))
* use Kotlin recommended style for creating constants ([0bee70d](https://github.com/RoinujNosde/Dynmap-SimpleClans/commit/0bee70dce7adfbeaadc8b5a069e6168bb61eee0a))


### Documentation

* Add javadoc to DynmapSimpleClans#getLang ([5b063b3](https://github.com/RoinujNosde/Dynmap-SimpleClans/commit/5b063b3fd81d734ceb11b761286a0f683ba60c64))
* even more about IconStorage constructor ([b0f4e3d](https://github.com/RoinujNosde/Dynmap-SimpleClans/commit/b0f4e3dc2e42492b986073ffcf373f7bc066f518))


### Code Refactoring

* move disablePlugin() to try...catch block ([806ba20](https://github.com/RoinujNosde/Dynmap-SimpleClans/commit/806ba20a666a85a99c18541e521c6ca62f05d320))
