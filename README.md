# ⚖️ ModerationManager

> **Minecraft Paper/Spigot Moderation Plugin** | Version 1.0 | API: Paper 1.21+ | Java 21

---

<div align="center">

> *A modern, comprehensive moderation plugin featuring GUI menus, a full punishment system, anti-spam, anti-swear, DDoS protection, alt-account detection, AutoMod, and much more.*

[![Paper](https://img.shields.io/badge/Paper-1.21%2B-FFC107?style=for-the-badge&logo=buymeacoffee&logoColor=white)]()
[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)]()
[![Maven](https://img.shields.io/badge/Build-Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)]()
[![SQLite](https://img.shields.io/badge/Database-SQLite-003B57?style=for-the-badge&logo=sqlite&logoColor=white)]()
[![License](https://img.shields.io/badge/License-MIT-97CA00?style=for-the-badge)]()

</div>

---

## 📑 Table of Contents

- [✨ Features](#-features)
- [💾 System Requirements](#-system-requirements)
- [⬇️ Installation](#️-installation)
- [🖥️ GUI Menus](#️-gui-menus)
- [⌨️ Commands](#️-commands)
- [🔐 Permissions](#-permissions)
- [⚙️ Configuration](#️-configuration)
  - [🤖 AutoMod (Automatic Mode)](#-automod-automatic-mode)
  - [📐 Customize GUIs (Titles / Items / Text)](#-customize-guis-titles--items--text)
  - [🚩 Add Punishment Reasons & Duration Templates](#-add-punishment-reasons--duration-templates)
  - [💬 Customize All Text Outputs](#-customize-all-text-outputs)
  - [🛡️ Protection Mechanisms in Detail](#️-protection-mechanisms-in-detail)
- [🧩 Architecture](#-architecture)
- [🛠️ Build Instructions](#️-build-instructions)
- [🖼️ Gallery](#-gallery)
- [🩺 Troubleshooting](#-troubleshooting)
- [🗺️ Roadmap](#️-roadmap)
- [🤝 Support](#-support)

---

## ✨ Features

| Category | Features |
|---|---|
| **Punishments** | Ban / TempBan / Mute / TempMute / Kick / Warn / Unban / Unmute — all GUI-driven or via commands |
| **8 GUI Menus** | Main menu, player selector, player info, punishment menu, history (paginated + Shift-DEL), alt-accounts, chat control, protection & raid control |
| **🤖 AutoMod** | Central master switch for all automatic punishments, 5 pre-configured categories (chat swear words, links, spam, warn thresholds, VPN/proxy), each with unified rules (`at-violations`, `action`, `reason`, `duration-minutes`, `cooldown-minutes`) — **every category can be toggled individually** |
| **Chat Protection** | Anti-Spam (message delay + per-minute limit), Caps-Lock protection (with auto-correct), flood protection, repeat protection, slowmode, chat lock, chat clear |
| **Filters** | Anti-Swear (blacklist + auto-punishments via AutoMod), link protection (Whitelist/Blacklist mode + IP link blocking), built-in whitelist: youtube/twitch/discord/minecraft/curseforge/modrinth/spigot/papermc/rainbowfurry |
| **Network Protection** | DDoS / Rate-Limit joins (connections-per-IP, joins-per-sec, total-per-min, packet limit), raid detection (threshold + lockdown), IP black/whitelist, temporary block |
| **Accounts** | Alt-account detection via IP, VPN/Proxy detection via ip-api.com (configurable threshold), per-player IP log, auto-ban possible |
| **OP Abuse Protection** | OP change log, blocked OP commands (stop/op/deop), OP whitelist with auto-deop, command monitoring (gamemode/give/fill/summon/ban/pardon/deop/op/stop) |
| **Staff Tools** | Staff-Chat (Toggle mode + alias `/sc`), Vanish (godmode / invisible / no drops / silent join), sound notifications for staff |
| **Monitoring** | Alt/VPN join notifications, IP-change alerts, raid alerts, OP change alerts, command monitor alerts, punishment alerts (blocked swear/link/spam) |
| **Database** | Persistent SQLite via HikariCP connection pooling (thread-safe) — tables: Punishments, PlayerProfiles, IPLogs, OPLogs |
| **International** | MiniMessage + Adventure API instead of legacy chat color codes (gradients, hover, etc.), 100% configurable texts & GUIs |

---

## 💾 System Requirements

| Component | Minimum Version |
|---|---|
| 🟡 **Server Software** | Paper 1.21+ (recommended) |
| ☕ **Java** | 21 (LTS) |
| 💿 **RAM** | ≥ 1 GB (512 MB minimum) |
| 🗄️ **Disk Space** | ≥ 20 MB free (database + plugin) |

> ℹ️ Spigot works too, but Paper is recommended (performance + native Adventure API support).

---

## ⬇️ Installation

1. **Download the JAR** (or build it yourself, see [Build Instructions](#️-build-instructions))
2. Place `moderationmanager-1.0.jar` into your server's `plugins/` folder
3. **Start the server** — the configuration will be generated automatically at `plugins/ModerationManager/config.yml`
4. (Optional) **Customize the configuration** in `config.yml` (see [Configuration](#️-configuration))
5. **Restart the server** or run `/modreload` (requires OP or `moderation.reload`)

```
server/
├── plugins/
│   └── ModerationManager/
│       ├── config.yml        ← Main configuration
│       └── moderation.db     ← SQLite database (auto-created)
└── paper.jar
```

---

## 🖥️ GUI Menus

The plugin features **8 fully customizable** inventory menus — all titles, materials, lore text, and layouts can be customized in `config.yml` under the `gui:` section:

| Menu | How to Open | Description |
|---|---|---|
| **Main Menu** | `/mod` | Entry point with all submenus: player management, chat control, protection & raid control, vanish |
| **Player Selector** | `/mod` → Player Search | Online player heads (pagination), each click opens PlayerInfo (or a custom target if invoked from another menu) |
| **Player Info** | `/playerinfo <Name>`, `/mod <Name>`, selector click | Profile (UUID, IP, first join, playtime, active punishments), statistics, **Quick Actions** (Shift-Clicks for warn/kick/mute/ban), unmute/unban buttons, navigation to History/Alts/Punish |
| **Punishment Menu** | `/punish <Name>`, `/mod <Name> punish`, selector | Step-by-step: **Type** (Warn/Kick/Mute/Ban) → **Duration** (presets from config) → **Reason** (presets) → **Confirm** (status display, validation, disabled button if incomplete) |
| **Punishment History** | `/history <Name>`, `/mod <Name> history` | Paginated (prev/next), each entry with status, ID, type, reason, operator, date, duration. **SHIFT+CLICK + `moderation.punishments.delete` = permanently delete entry** |
| **Alt Accounts** | `/alts <Name>`, `/mod <Name> alts` | All accounts sharing the same IP (pagination), shows VPN detection, IP, last-seen. Also supports **global search** via the main menu. |
| **Chat Control** | `/mod` → Chat Control | **4 Toggles**: Clear Chat (200 lines for all players without bypass-perm), Chat Lock/Unlock, Slowmode On/Off, StaffChat Toggle |
| **Protection & Raid** | `/mod` → Protection & Raid | Lockdown mode on/off, items clear, mobs clear, lag-clear (all-in-one) |

---

## ⌨️ Commands

| Command | Alias | Description | Permission |
|---|---|---|---|
| `/mod [Player] [sub]` | `/mm` | **Main Menu** (no args) or direct jump: `/mod Steve punish` / `history` / `alts` | `moderation.mod` |
| `/modreload` | - | Reload config + all texts | `moderation.reload` |
| `/playerinfo <Player>` | `/pinfo` | Show player profile (as player: GUI, as console: chat format. Use `--chat` flag for text output) | `moderation.playerinfo` |
| `/punish <Player>` | - | Punish a player (opens punishment GUI) | `moderation.punish` |
| `/ban <Player> <Reason>` | - | **Permanent** ban (via command) | `moderation.ban` |
| `/tempban <Player> <Time> <Reason>` | - | Temporary ban, time format: `1d`, `12h`, `30m`, `1d12h` | `moderation.tempban` |
| `/unban <Player> [Reason]` | - | Unban player | `moderation.unban` |
| `/kick <Player> <Reason>` | - | Kick player (online only) | `moderation.kick` |
| `/mute <Player> <Reason>` | - | **Permanent** mute | `moderation.mute` |
| `/tempmute <Player> <Time> <Reason>` | - | Temporary mute | `moderation.tempmute` |
| `/unmute <Player> [Reason]` | - | Unmute player | `moderation.unmute` |
| `/warn <Player> <Reason>` | - | Quick warning. **Warning**: 5 active warns → auto-mute, 10 → auto-ban (configurable via AutoMod!) | `moderation.warn` |
| `/history <Player>` | - | All punishments of a player (GUI) | `moderation.history` |
| `/staffchat [Message]` | `/sc` | Send staff message (no args: toggle mode on/off, then everything goes automatically to the team) | `moderation.staffchat` |
| `/vanish` | `/v` | Toggle vanish (godmode + no drops + hidden from tablist) | `moderation.vanish` |
| `/clearchat` | `/cc` | Clear chat for all players without bypass-perm | `moderation.clearchat` |

### Command Syntax

- **Time format** (for `/tempban`, `/tempmute`, GUI):
  - `1d` = 1 day
  - `12h` = 12 hours
  - `30m` = 30 minutes
  - `1d12h` = 1 day + 12 hours (compact)

---

## 🔐 Permissions

| Permission | Grants Access To | Default |
|---|---|---|
| `moderation.*` | **ALL** permissions (recursive) | OP |
| `moderation.mod` | `/mod` menu + navigation | OP |
| `moderation.admin` | Administration (can be extended) | ❌ |
| `moderation.reload` | `/modreload` config reload | OP |
| `moderation.playerinfo` | `/playerinfo` + player info GUI | OP |
| `moderation.punish` | `/punish` GUI + create punishments | OP |
| `moderation.punishments.delete` | **SHIFT+CLICK** in history → delete punishments | ❌ |
| `moderation.warn` | `/warn` + quick warnings | OP |
| `moderation.kick` | `/kick` + kick button in GUI | OP |
| `moderation.mute` | `/mute` (permanent) | OP |
| `moderation.tempmute` | `/tempmute` + Mute/TempMute in GUI | OP |
| `moderation.ban` | `/ban` (permanent) | OP |
| `moderation.tempban` | `/tempban` + Ban/TempBan in GUI | OP |
| `moderation.unban` | `/unban` + unban button | OP |
| `moderation.unmute` | `/unmute` + unmute button | OP |
| `moderation.history` | `/history` + view history | OP |
| `moderation.staffchat` | `/sc` staff chat (see also toggle mode) | OP |
| `moderation.vanish` | `/vanish` toggle invisibility | OP |
| `moderation.vanish.see` | See vanished players (also in tablist) | OP |
| `moderation.clearchat` | `/clearchat` / `/cc` | OP |
| `moderation.notify` | Receives **all** staff notifications (Alt, VPN, Raid, IP-Change, OP, Command-Monitor, Punish, Swear/Link/Spam-Blocked) | OP |
| `moderation.bypass` | **Universal bypass**: skips Anti-Spam / Link-Protection / Swear-Filter + AutoMod | OP |
| `moderation.bypass.raid` | Can join during lockdown / raid mode | OP |
| `moderation.bypass.links` | Can post all links (even non-whitelisted) | OP |
| `moderation.bypass.spam` | No anti-spam for this player (no delay, no count) | OP |
| `moderation.bypass.swear` | No swear filter | OP |
| `moderation.automod.bypass` | Special AutoMod bypass (in addition to `moderation.bypass` + OP) | ❌ |
| `moderation.automod.notify` | Receives detailed AutoMod punishment notifications | OP (via `staff` group) |

---

## ⚙️ Configuration

All configuration is located in **`plugins/ModerationManager/config.yml`** and is **100% editable** — from individual chat messages to every single item in the GUIs.

Activate changes with:
```
/modreload
```

---

### 🤖 AutoMod (Automatic Mode)

> **The central feature for times when no staff members are online!**

AutoMod consolidates **all** automatic punishments (chat filters, VPN, warn thresholds) in **one place** — instead of previously scattered `violations`, `auto-punishments`, `warn-thresholds`, `vpn-autoban` per system. A single master switch, `auto-mod.enabled: false`, disables **everything**.

#### Structure:
```yaml
auto-mod:
  enabled: true                  # MASTER SWITCH: disable/enable all
  operator-name: "AutoMod"       # Who appears in ban screen / kick / warn as "Operator"?
  bypass-permission: "moderation.automod.bypass"
  notify-staff: true             # Detailed staff message on every punishment

  categories:
    # Each category can be toggled individually + has its own rules list
    chat-swear:
      enabled: true
      rules:
        - at-violations: 1       # Violation count when this rule triggers
          action: warn           # warn | mute | tempmute | ban | tempban | kick
          reason: "Swear word detected"
          duration-minutes: -1   # -1 = permanent (only ban/mute relevant)
          cooldown-minutes: 0    # 0 = never repeat as long as count matches
        - at-violations: 3
          action: mute
          reason: "Mute after multiple swear words"
          duration-minutes: 30
          cooldown-minutes: 0
        - at-violations: 5
          action: ban
          reason: "Ban after 5 swear violations"
          duration-minutes: 1440
          cooldown-minutes: 0
```

#### The 5 pre-configured categories:

| Category | Trigger | Default Rules |
|---|---|---|
| `chat-swear` | Anti-Swear (blocked message) | 1× Warn → 3× Mute 30 min → 5× Ban 1 day |
| `chat-links` | Link protection (Whitelist/Blacklist) | 1× Warn → 2× Mute 15 min → 3× Ban 1 day |
| `chat-spam` | Anti-Spam (Delay/Caps/Flood/Repeat) | 2× Warn → 4× Mute 10 min → 7× Ban 1 day |
| `warn-thresholds` | **Active warnings** count (e.g. after `/warn` by staff + AutoMod) | 5 warns → Mute 30 min (60 min cooldown) / 10 warns → Ban 3 days |
| `vpn` | VPN/Proxy detection (ip-api.com, threshold: 70) | 1× → Ban permanent |

#### Adding custom categories:
Simply add entries under `auto-mod.categories:` and trigger via API:
```java
plugin.getAutoModManager().reportViolation(player, "command-spam", "/give spam");
plugin.getAutoModManager().triggerDirect(targetUUID, name, "illegal-items", 1, "Shulker Box Stack");
```

#### API methods in [AutoModManager](file:///c:/Users/Jasmin/IdeaProjects/ModerationManager/src/main/java/net/rainbowfurry/moderationManager/managers/AutoModManager.java):
- `reportViolation(player, "category", extra)` → counter +1, applies matching rule
- `triggerDirect(uuid, name, "category", level, extra)` → direct rule selection (VPN, warn thresholds)
- `resetViolations(playerId, "category")` / `resetAllForPlayer(playerId)` / `clearCachesFor(player)`
- `getViolationCount(playerId, "category")`

---

### 📐 Customize GUIs (Titles / Items / Text)

Under `gui:` you can customize **each menu down to the last level**. Sub-sections per menu:
- `gui.main-menu` (Slots 10, 13, 16, 20, 22, 24, 30, 32, 40)
- `gui.player-selector` (Pagination, Prev/Next, Close/Back, Status Head Lore Online/Offline)
- `gui.player-info` (Head, Stats, Punish, History, Alts, Quick Actions, Unmute/Unban, Back)
- `gui.punish-menu` (**Type Buttons**: warn/kick/mute/ban, Duration item texts, Reason item texts, Apply Enabled/Disabled, Reset Button, Status Book)
- `gui.history-menu` (Pagination, Active/Inactive status, Type material mapping, Delete Hint for Shift+Click)
- `gui.alt-accounts-menu` (Alt list with IP/VPN)
- `gui.chat-control` (4 Materials On/Off per button, Texts On/Off, Slowmode delay in lore, Auto-Status StaffChat)
- `gui.items` (General settings: Back Arrow, Close Barrier, Reset Button, Apply Button, Status Book, Skull Names)

#### Example:
```yaml
gui:
  fill-border-material: "GRAY_STAINED_GLASS_PANE"
  fill-glass-material: "BLACK_STAINED_GLASS_PANE"

  punish-menu:
    title: "<gradient:#f44336:#ff9800><bold>⚔️ Punish: %name%</bold></gradient>"
    rows: 6
    type-buttons:
      warn:
        material: "YELLOW_BANNER"
        name: "<yellow>⚠️ Warning"
        lore:
          - "<gray>Warning without restriction"
          - "<gray>Registered in history"
```

All materials use the **Material enum names** from Paper 1.21:
🔗 [Paper Material Javadoc](https://jd.papermc.io/paper/1.21/org/bukkit/Material.html)

---

### 🚩 Add Punishment Reasons & Duration Templates

Under `punishments.presets.reasons` and `punishments.presets.durations`.

#### Custom Reasons:
```yaml
punishments:
  presets:
    reasons:
      - "Spam / Chat abuse"
      - "Insult / insulting other players"
      - "Advertising (IP / Server)"
      - "Trolling / Team grief"
      - "Exploit abuse"
      - "Cheating / Hacking"
      - "Ban evasion / Alt account"
      - "Other - see notes"
```

#### Custom Duration Templates:
```yaml
punishments:
  presets:
    durations:
      - label: "5 Min"
        duration-ms: 300000
        material: "GOLD_INGOT"
      - label: "Permanent"
        duration-ms: -1          # -1 = permanent
        material: "BEDROCK"
```

⏱️ Conversion reference: 1s = `1000`, 1m = `60000`, 1h = `3600000`, 1d = `86400000`

---

### 💬 Customize All Text Outputs

Under `messages:` you'll find **every single chat message** the plugin outputs:

```yaml
messages:
  no-permission: "<red>You don't have permission to do that!"
  player-only: "<red>This command can only be executed as a player!"
  config-reloaded: "<green>Moderation config has been reloaded!"
  # ...
  alt-detected-notify: "<gradient:#ff6f00:#ff3d00>⚠️ ALT-ACCOUNT</gradient> <yellow>%name% <gray>shares IP with <white>%other%<gray>!"
  vpn-detected-notify: "<gradient:#9d50bb:#6e48aa>⚠️ VPN</gradient> <yellow>%name% <gray>is using VPN/Proxy IP (<white>%ip%<gray>)"
```

#### All supported placeholders `%…%`:

| Placeholder | Examples (occurrences) |
|---|---|
| `%name%`, `%target%`, `%player%`, `%other%` | Player names (staff, target, alt-account comparison) |
| `%reason%` | Reason for a punishment |
| `%operator%`, `%op%`, `%by%` | Who executed it? (Punish, chat lock, clear) |
| `%id%` | Punishment ID (Ban #123) |
| `%type%` | Type (BAN/TEMPMUTE/WARN/KICK) |
| `%duration%`, `%time%`, `%label%` | Duration (5 Min / 1 Day / Permanent), remaining time for bans |
| `%unban_date%`, `%unmute_date%`, `%date%`, `%last_seen%` | Date placeholders |
| `%bans%`, `%mutes%`, `%warns%`, `%kicks%`, `%count%` | Player statistics |
| `%message%`, `%command%` | StaffChat message / command monitor |
| `%seconds%` | Slowmode delay / wait time |
| `%page%`, `%current%`, `%max%`, `%total%` | Pagination (History, AltAccounts, Selector) |
| `%state%`, `%value%`, `%ip%`, `%uuid%` | Status On/Off, field values, IP, UUID |
| `%anzahl%`, `%history_list%` | Special PlayerInfo placeholders |

---

### 🛡️ Protection Mechanisms in Detail

#### 1. Anti-Spam
- `anti-spam.message-delay: 800` (millis between 2 messages)
- `anti-spam.messages-per-minute: 6`
- Caps-Lock: threshold in %, min length, auto-correct
- Repeat Protection: same message max N times
- Flood Protection: N same characters in a row
- → Notifications + AutoMod `chat-spam` rule triggering

#### 2. Link Protection
- Mode `WHITELIST` (recommended) or `BLACKLIST`
- `block-ip-links: true` (IP:Port → blocked, e.g. 1.1.1.1:25565)
- Whitelist: youtube, twitch, discord, minecraft, curseforge, modrinth, spigot, paper, rainbowfurry
- → `chat-links` AutoMod

#### 3. Anti-Swear
- Mode `BLOCK` or `REPLACE` with `****`
- Built-in blacklist: ~20 terms (DE + EN)
- Also checks private message commands: `msg, tell, r, w, whisper, me, say`
- → `chat-swear` AutoMod

#### 4. DDoS / Join Protection
- `max-connections-per-ip: 3` (simultaneous)
- `joins-per-second: 1` / `total-joins-per-minute: 60`
- Optional packet rate limit
- Temp-block default 30 min
- Whitelist 127.0.0.1 (for local BungeeCord)

#### 5. Raid Detection
- Threshold `15` new players in `10` seconds
- Auto-Action: `LOCKDOWN` (10 min)
- Ban new players during raid (optional)
- Staff alert + sound

#### 6. OP Abuse Protection
- OP change log + staff alert
- Blocked OP commands: stop, op, deop
- OP whitelist: auto de-op if player not in list
- Command monitoring: notifications for `gamemode`, `give`, `fill`, `summon`, `ban`, `pardon`, `op`, `deop`, `stop`

#### 7. Alt-Account & VPN
- Auto-detect: staff alert on join with already known IP
- Max `3` accounts per IP (default)
- VPN detection: ip-api.com API, threshold `70` (0=residential, 70+=VPN/Proxy/Hosting)
- VPN autoban (default: disabled, enable via `auto-mod.categories.vpn.enabled`)

---

## 🧩 Architecture

```
ModerationManager (Main class, getter for all managers)
  ├── commands/CommandManager.java              ← All commands (Ban/TempBan/Kick/Warn/Mod/Reload etc.)
  ├── listeners/                                 ← Event listeners
  │   ├── ChatListener.java                     ← Anti-Swear/Links/Spam via ChatManager
  │   ├── ConnectionListener.java               ← Join/Quit, Alt/VPN/Rate-Limit, Playtime tracking
  │   ├── MenuInventoryListener.java            ← BaseMenu click handler (GuiManager)
  │   └── VanishListener.java                   ← Pickup/Damage/Join cancel for vanished
  ├── guis/                                      ← Inventory menus
  │   ├── BaseMenu.java                         ← Abstract base menu (createInventory, refresh, setItem, fillBorder, skull, makeItem)
  │   ├── MainMenu.java                         ← Main menu (Slots 10/13/16/20/22/24/30/32/40)
  │   ├── PlayerSelectorMenu.java               ← Online player list (pagination 28/slot)
  │   ├── PlayerInfoMenu.java                   ← Player profile + Quick actions + Unmute/Unban
  │   ├── PunishMenu.java                       ← Punishment GUI (Type→Duration→Reason→Apply; from Config!)
  │   ├── HistoryMenu.java                      ← Punishment history (paginated, Shift+Click Delete)
  │   ├── AltAccountsMenu.java                  ← Alt accounts per IP (pagination)
  │   ├── ChatControlMenu.java                  ← Clear/Lock/Slowmode/StaffChat toggles
  │   └── ProtectionControlMenu.java            ← Lockdown / Items-Clear / Mobs / Lag-Clear
  ├── managers/                                  ← Business logic (no Bukkit events)
  │   ├── ConfigManager.java                    ← 100% getters (Config + AutoModRule/DurationPreset records)
  │   ├── PunishmentManager.java                ← Ban/Mute/Warn/Kick/Unban/Unmute, active punishments check (-> AutoMod triggerDirect for warn thresholds)
  │   ├── AutoModManager.java                   ← Automatic mode (reportViolation/triggerDirect/cooldowns/rule matching)
  │   ├── AltAccountManager.java                ← IP comparison + ip-api.com VPN check (-> AutoMod.triggerDirect)
  │   ├── ChatManager.java                      ← Anti-Spam, Swear, Links, Slowmode, Lock (-> AutoMod.reportViolation)
  │   ├── ProtectionManager.java                ← DDoS, Raid, Lockdown, Rate-Limit
  │   ├── StaffManager.java                     ← StaffChat (Toggle mode!), Vanish (Toggle), Notifications
  │   ├── GuiManager.java                       ← Open menus registration, click dispatch
  │   └── DatabaseManager.java                  ← SQLite CRUD (PlayerProfile, Punishments, IPLogs, OPLogs)
  ├── models/                                    ← Data models (POJO, records)
  │   ├── Punishment.java                       ← Type enum (BAN/TEMPBAN/KICK/MUTE/TEMPMUTE/WARN/UNBAN/UNMUTE)
  │   ├── PlayerProfile.java                    ← Base data + Playtime + Counters (bans/warns/kicks/mutes)
  │   ├── IPLog.java                            ← IP log entry (with PlayerUUID)
  │   └── OPLogEntry.java                       ← OP change entry
  └── utils/                                     ← Helpers
      ├── MessageUtils.java                     ← MiniMessage + %var% replacement + formatPunishment + kickPlayer + formatDate/Playtime/Duration
      ├── DurationUtils.java                    ← parseDuration("1d12h"), formatDuration, formatRemaining
      └── UUIDUtils.java                        ← UUID from name via Mojang API (Offline Player UUID)
```

---

## 🛠️ Build Instructions

This project uses **Maven** (wrapper optional).

```bash
# 1. Clone repository
git clone https://github.com/<YOUR-USER>/ModerationManager.git
cd ModerationManager

# 2. Maven Build (dependencies are downloaded automatically)
mvn clean package -DskipTests

# 3. Finished JAR
# Output: target/moderationmanager-1.0.jar
```

Or with Maven Wrapper (included in the project):
```powershell
# Windows (PowerShell):
.\mvnw.cmd clean package -DskipTests
```

#### Included Libraries (compiled into JAR via Maven Shade):
- 🗄️ **sqlite-jdbc 3.46.0** — SQLite driver
- ⚡ **HikariCP 5.1.0** — High-performance connection pool
- 🌈 **MiniMessage / Adventure API** — Paper-native
- 📊 **bStats 3.0.2** — Optional usage statistics (anonymous, toggleable)
- 🔍 **slf4j-api + slf4j-simple** — Logging bridge for HikariCP

---

## 🖼️ Gallery

<p align="center">
<img width="704" height="505" alt="image" src="https://github.com/user-attachments/assets/201cf023-4a01-4498-9cd4-fba8f67a3fc5" />
<img width="693" height="518" alt="image" src="https://github.com/user-attachments/assets/0ece47db-abda-4039-a593-db3c174cfb22" />
<img width="749" height="524" alt="image" src="https://github.com/user-attachments/assets/5eca0ce7-2ffd-4e23-acf6-fe1586b4cdc8" />
</p>

---

## 🩺 Troubleshooting

| Problem | Solution |
|---|---|
| 🔴 **JAR doesn't start / NoClassDefFound** | Make sure the server uses **Java 21** and at least **Paper 1.21** |
| 🔴 **Chat messages show `%reason%` instead of value** | Verify placeholders in config are correct (`%key%` syntax); don't forget `/modreload` |
| 🔴 **GUI doesn't open / Items missing** | Check server log for **stack trace** — often incorrect Material set in `config.yml` (check enum names, e.g. `RED_BANNER` instead of `BANNER`) |
| 🔴 **AutoMod doesn't trigger (legacy versions)** | Old scattered `anti-spam.auto-punishments`, `anti-swear.auto-punishments`, `auto-rules.warn-thresholds`, `alt-accounts.vpn-autoban` are now ALL handled by `auto-mod:*`. Remove these old sections or enable `auto-mod.enabled: true` and check the `enabled:` flag per category |
| 🟡 **Alt-account detection doesn't work** | Check `alt-accounts.enabled: true` + `alt-accounts.auto-detect: true` |
| 🟡 **bStats message in log** | Can be disabled in config via `general.bstats: false` |
| 🟡 **SQLite Lock Timeout** | Check disk access permissions; consider lowering HikariCP max-pool-size |
| 🟠 **VPN detection too aggressive / too weak** | Adjust `alt-accounts.vpn-threshold` (default 70). Higher = stricter |
| 🟠 **Shift+Click in History deletes nothing** | Grant `moderation.punishments.delete` permission. |
| 🟠 **Auto-Mod notifications appear twice** | Old `notify-staff` in protection sections (e.g. `alt-accounts.notify-staff-on-alt-join`) + AutoMod `notify-staff` both enabled. Disable in old sections or leave it to AutoMod. |

---

## 🗺️ Roadmap / Planned Features

- [ ] **AutoMod 2.0**: GUI for runtime rule management
- [ ] **/ignore** system (players can block each other)
- [ ] **More AutoMod categories**: Command-Spy, Illegal Items, Grief Prevention
- [ ] **MySQL / PostgreSQL support** (in addition to SQLite)
- [ ] **PlaceholdersAPI integration** (`%mm_bans_%player%%`, etc.)
- [ ] **Discord Webhook** (Ban/Mute/Warn notifications + AutoMod alerts)
- [ ] **Import/Export** of punishment histories (JSON/SQL)
- [ ] **Multilingual support** (en/de/es automatic via player locale)

---

## 🤝 Support

🐛 **Found a bug?** Please open an issue including:
- Server version (`/version`)
- Plugin version
- **Stack trace** from latest.log
- Steps to reproduce
- Excerpt from the affected config section (if configuration issue)

💬 **Questions?** Feel free to contact me:
- Discord: *add your Discord handle here*
- E-Mail: *add your email here*
- SpigotMC: *add resource page here*
- Homepage: [rainbowfurry.com](https://www.rainbowfurry.com)

---

<div align="center">
<br>

> *A project by **RainbowFurry Studios***  
> ModerationManager — The Only Moderation Plugin You'll Ever Need.

</div>
