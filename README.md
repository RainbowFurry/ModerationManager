# ⚖️ ModerationManager

> **Minecraft Paper/Spigot Moderations-Plugin** | Version 1.0 | API: Paper 1.21+ | Java 21

---

<div align="center">

> *Ein modernes, umfangreiches Moderations-Plugin mit GUI, Bestrafungs-System, Anti-Spam, Anti-Swear, DDoS-Schutz, Alt-Account-Erkennung, AutoMod und vielem mehr.*

[![Paper](https://img.shields.io/badge/Paper-1.21%2B-FFC107?style=for-the-badge&logo=buymeacoffee&logoColor=white)]()
[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)]()
[![Maven](https://img.shields.io/badge/Build-Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)]()
[![SQLite](https://img.shields.io/badge/Database-SQLite-003B57?style=for-the-badge&logo=sqlite&logoColor=white)]()
[![License](https://img.shields.io/badge/License-MIT-97CA00?style=for-the-badge)]()

</div>

---

## 📑 Inhaltsverzeichnis

- [✨ Features](#-features)
- [💾 Systemvoraussetzungen](#-systemvoraussetzungen)
- [⬇️ Installation](#️-installation)
- [🖥️ GUI Menüs](#️-gui-menüs)
- [⌨️ Befehle](#️-befehle)
- [🔐 Permissions](#-permissions)
- [⚙️ Konfiguration](#️-konfiguration)
  - [🤖 AutoMod (Automatischer Modus)](#-automod-automatischer-modus)
  - [📐 GUI anpassen (Titel / Items / Texte)](#-gui-anpassen-titel--items--texte)
  - [🚩 Straf-Gründe & Dauer-Templates hinzufügen](#-straf-gründe--dauer-templates-hinzufügen)
  - [💬 Alle Text-Outputs anpassen](#-alle-text-outputs-anpassen)
  - [🛡️ Schutzmechanismen im Detail](#️-schutzmechanismen-im-detail)
- [🧩 Architektur](#-architektur)
- [🛠️ Build-Anleitung](#️-build-anleitung)
- [🩺 Troubleshooting](#-troubleshooting)
- [🗺️ Roadmap](#️-roadmap)
- [🤝 Support](#-support)

---

## ✨ Features

| Bereich | Funktionen |
|---|---|
| **Bestrafungen** | Ban / TempBan / Mute / TempMute / Kick / Warn / Unban / Unmute — alles GUI-gesteuert oder via Commands |
| **8 GUI Menüs** | Hauptmenü, Spieler-Selektor, Spieler-Info, Straf-Menü, Historie (blätterbar + Shift-DEL), Alt-Accounts, Chat-Steuerung, Schutz & Raid Kontrolle |
| **🤖 AutoMod** | Zentraler Master-Switch für alle automatischen Strafen, 5 vorkonfigurierte Kategorien (Chat-Bezeichnungen, Links, Spam, Warn-Schwellwerte, VPN/Proxy), je mit einheitlichen Regeln (`at-violations`, `action`, `reason`, `duration-minutes`, `cooldown-minutes`) — **jede Kategorie einzeln an/abschaltbar** |
| **Chat-Schutz** | Anti-Spam (Delay + Minute-Limit), Caps-Lock-Schutz (mit auto-correct), Flood-Schutz, Wiederholungs-Schutz, Slowmode, Chat-Sperre, Chat-Clear |
| **Filter** | Beleidigungs-Schutz (Blacklist + Auto-Bestrafungen via AutoMod), Link-Schutz (Whitelist/Blacklist-Modus + IP-Links blockieren), Whitelist: youtube/twitch/discord/minecraft/curseforge/modrinth/spigot/papermc/rainbowfurry |
| **Netzwerk-Schutz** | DDoS / Rate-Limit Joins (Connections-per-IP, Joins-per-Sec, Total-per-Min, Packet-Limit), Raid-Erkennung (Threshold + Lockdown), IP-Black/Whitelist, Temp-Block |
| **Accounts** | Alt-Account Erkennung via IP, VPN/Proxy-Erkennung via ip-api.com (Schwellwert konfigurierbar), IP-Log pro Spieler, Auto-Ban möglich |
| **OP Abuse Schutz** | OP Änderungs-Log, blockierte OP-Commands (stop/op/deop), OP Whitelist mit Auto-Deop, Command-Monitoring (gamemode/give/fill/summon/ban/pardon/deop/op/stop) |
| **Staff Tools** | Staff-Chat (Toggle-Modus + Alias `/sc`), Vanish (Godmode / Unsichtbar / Keine Drops / Silent Join), Sound-Notifications für Staff |
| **Monitoring** | Alt/VPN-Join-Notifications, IP-Change-Alerts, Raid-Alerts, OP-Change-Alerts, Command-Monitor-Alerts, Punishment-Alerts (Blocked Swear/Link/Spam) |
| **Datenbank** | Persistente SQLite via HikariCP Connection Pooling (Thread-Safe) — Tabellen: Punishments, PlayerProfiles, IPLogs, OPLogs |
| **International** | MiniMessage + Adventure API statt Legacy Chat-Farbcodes (Gradienten, Hover, Gradient-Tags), 100% konfigurierbare Texte & GUIs |

---

## 💾 Systemvoraussetzungen

| Komponente | Mindestversion |
|---|---|
| 🟡 **Server-Software** | Paper 1.21+ (empfohlen) |
| ☕ **Java** | 21 (LTS) |
| 💿 **RAM** | ≥ 1 GB (512 MB Minimum) |
| 🗄️ **Festplatte** | ≥ 20 MB frei (Datenbank + Plugin) |

> ℹ️ Spigot funktioniert ebenfalls, Paper wird aber empfohlen (Performance + Adventure API nativ).

---

## ⬇️ Installation

1. **JAR herunterladen** (oder selbst bauen, siehe [Build-Anleitung](#️-build-anleitung))
2. Die Datei `moderationmanager-1.0.jar` in den `plugins/`-Ordner deines Servers legen
3. **Server starten** — die Config wird automatisch generiert unter `plugins/ModerationManager/config.yml`
4. (Optional) **Konfiguration anpassen** in `config.yml` (siehe [Konfiguration](#️-konfiguration))
5. **Server neu starten** oder `/modreload` ausführen (OP oder `moderation.reload`)

```
server/
├── plugins/
│   └── ModerationManager/
│       ├── config.yml        ← Hauptkonfiguration
│       └── moderation.db     ← SQLite Datenbank (wird automatisch erstellt)
└── paper.jar
```

---

## 🖥️ GUI Menüs

Das Plugin verfügt über **8 voll konfigurierbare** Inventar-Menüs — alle Titel, Materialien, Lore und Layouts können in der `config.yml` unter `gui:` angepasst werden:

| Menü | Öffnen per | Beschreibung |
|---|---|---|
| **Hauptmenü** | `/mod` | Startpunkt mit allen Untermenüs: Spieler Verwaltung, Chat Steuerung, Schutz & Raid Kontrolle, Vanish |
| **Spieler-Selektor** | `/mod` → Spieler Suche | Online-Spieler Köpfe (Pagination), jeder Klick öffnet PlayerInfo (oder anderes Ziel wenn von anderem Menü aufgerufen) |
| **Spieler-Info** | `/playerinfo <Name>`, `/mod <Name>`, Selektor Klick | Profil (UUID, IP, Erster Login, Spielzeit, aktive Strafen), Statistiken, **Schnell-Aktionen** (Shift-Clicks für Warn/Kick/Mute/Ban), Unmute/Unban Buttons, Weiterleitung zu History/Alts/Punish |
| **Straf-Menü** | `/punish <Name>`, `/mod <Name> punish`, Selektor | Schritt-für-Schritt: **Typ** (Warn/Kick/Mute/Ban) → **Dauer** (Presets aus Config) → **Grund** (Presets) → **Bestätigen** (Status-Anzeige, Validierung, disabled-Button wenn unvollständig) |
| **Straf-Historie** | `/history <Name>`, `/mod <Name> history` | Blätterbar (Prev/Next), pro Eintrag mit Status, ID, Typ, Grund, Operator, Datum, Dauer. **SHIFT+KLICK + `moderation.punishments.delete` = Eintrag dauerhaft löschen** |
| **Alt-Accounts** | `/alts <Name>`, `/mod <Name> alts` | Alle Accounts mit gleicher IP (Pagination), anzeigen ob VPN erkannt, IP, Zuletzt-Gesehen. Auch **globale Suche** via Hauptmenü. |
| **Chat-Steuerung** | `/mod` → Chat Steuerung | **4 Toggles**: Clear Chat (Alle 200 Zeilen löschen für alle ohne Perm), Chat Lock/Unlock, Slowmode On/Off, StaffChat Toggle |
| **Schutz & Raid** | `/mod` → Schutz & Raid | Lockdown-Modus an/aus, Items clear, Mobs clear, Lag-Clear (Alles in einem) |

---

## ⌨️ Befehle

| Befehl | Alias | Beschreibung | Permission |
|---|---|---|---|
| `/mod [Spieler] [sub]` | `/mm` | **Hauptmenü** (ohne Args) oder direkter Sprung: `/mod Steve punish` / `history` / `alts` | `moderation.mod` |
| `/modreload` | - | Config + alle Texte neu laden | `moderation.reload` |
| `/playerinfo <Spieler>` | `/pinfo` | Spieler-Profil anzeigen (als Spieler: GUI, als Konsole: Chat-Format. Flag `--chat` für Text-Ausgabe) | `moderation.playerinfo` |
| `/punish <Spieler>` | - | Spieler bestrafen (öffnet Straf-GUI) | `moderation.punish` |
| `/ban <Spieler> <Grund>` | - | **Permanent** bannen (per Command) | `moderation.ban` |
| `/tempban <Spieler> <Zeit> <Grund>` | - | Temporär bannen, Zeit-Format: `1d`, `12h`, `30m`, `1d12h` | `moderation.tempban` |
| `/unban <Spieler> [Grund]` | - | Spieler entbannen | `moderation.unban` |
| `/kick <Spieler> <Grund>` | - | Spieler kicken (nur Online) | `moderation.kick` |
| `/mute <Spieler> <Grund>` | - | **Permanent** muten | `moderation.mute` |
| `/tempmute <Spieler> <Zeit> <Grund>` | - | Temporär muten | `moderation.tempmute` |
| `/unmute <Spieler> [Grund]` | - | Spieler entmuten | `moderation.unmute` |
| `/warn <Spieler> <Grund>` | - | Schnell-Verwarnung. **Achtung**: 5 aktive Warns → Auto-Mute, 10 → Auto-Ban (via AutoMod konfigurierbar!) | `moderation.warn` |
| `/history <Spieler>` | - | Alle Strafen eines Spielers (GUI) | `moderation.history` |
| `/staffchat [Nachricht]` | `/sc` | Staff-Nachricht senden (ohne Args: Toggle-Modus an/aus, dann geht alles automatisch ins Team) | `moderation.staffchat` |
| `/vanish` | `/v` | Ein/Ausblenden (Godmode + keine Drops + Tablist-Ausblendung) | `moderation.vanish` |
| `/clearchat` | `/cc` | Chat für alle ohne Bypass-Perm leeren | `moderation.clearchat` |

### Command Syntax

- **Zeit-Format** (für `/tempban`, `/tempmute`, GUI):
  - `1d` = 1 Tag
  - `12h` = 12 Stunden
  - `30m` = 30 Minuten
  - `1d12h` = 1 Tag + 12 Stunden (kompakt)

---

## 🔐 Permissions

| Permission | Gewährt Zugriff auf | Default |
|---|---|---|
| `moderation.*` | **ALLE** Perms (rekursiv) | OP |
| `moderation.mod` | `/mod` Menü + Navigation | OP |
| `moderation.admin` | Administration (kann erweitert werden) | ❌ |
| `moderation.reload` | `/modreload` Config Reload | OP |
| `moderation.playerinfo` | `/playerinfo` + Spieler-Info GUI | OP |
| `moderation.punish` | `/punish` GUI + Bestrafungen erstellen | OP |
| `moderation.punishments.delete` | **SHIFT+KLICK** in Historie → Strafen löschen | ❌ |
| `moderation.warn` | `/warn` + Schnell-Verwarnung | OP |
| `moderation.kick` | `/kick` + Kick-Button im GUI | OP |
| `moderation.mute` | `/mute` (permanent) | OP |
| `moderation.tempmute` | `/tempmute` + Mute/TempMute im GUI | OP |
| `moderation.ban` | `/ban` (permanent) | OP |
| `moderation.tempban` | `/tempban` + Ban/TempBan im GUI | OP |
| `moderation.unban` | `/unban` + Unban-Button | OP |
| `moderation.unmute` | `/unmute` + Unmute-Button | OP |
| `moderation.history` | `/history` + Historie einsehen | OP |
| `moderation.staffchat` | `/sc` Staff-Chat (siehe auch Toggle-Modus) | OP |
| `moderation.vanish` | `/vanish` unsichtbar schalten | OP |
| `moderation.vanish.see` | Sieht vanished Spieler (auch in Tablist) | OP |
| `moderation.clearchat` | `/clearchat` / `/cc` | OP |
| `moderation.notify` | Erhält **alle** Staff Benachrichtigungen (Alt, VPN, Raid, IP-Change, OP, Command-Monitor, Punish, Swear/Link/Spam-Blocked) | OP |
| `moderation.bypass` | **Universal-Bypass**: Umgeht Anti-Spam / Link-Schutz / Swear-Filter + AutoMod | OP |
| `moderation.bypass.raid` | Darf während Lockdown / Raid-Modus joinen | OP |
| `moderation.bypass.links` | Darf alle Links posten (auch nicht-whitelisted) | OP |
| `moderation.bypass.spam` | Kein Anti-Spam für diesen Spieler (kein Delay, kein Count) | OP |
| `moderation.bypass.swear` | Kein Beleidigungsfilter | OP |
| `moderation.automod.bypass` | Spezieller AutoMod-Bypass (zusätzlich zu `moderation.bypass` + OP) | ❌ |
| `moderation.automod.notify` | Erhält ausführliche AutoMod-Strafe-Benachrichtigungen | OP (via `staff`-Group) |

---

## ⚙️ Konfiguration

Die gesamte Konfiguration liegt in **`plugins/ModerationManager/config.yml`** und ist **100% editierbar** — von einzelnen Chat-Nachrichten bis hin zu jedem einzelnen Item in den GUIs.

Änderungen aktivieren mit:
```
/modreload
```

---

### 🤖 AutoMod (Automatischer Modus)

> **Das zentrale Feature für Zeiten ohne Staff-Mitglieder online!**

Der AutoMod sammelt **alle** automatischen Strafen (Chat-Filter, VPN, Warn-Schwellwerte) an **einem Ort** — statt früher verteilter `violations`, `auto-punishments`, `warn-thresholds`, `vpn-autoban` pro System. EIN Master-Schalter, `auto-mod.enabled: false`, schaltet **alles** ab.

#### Aufbau:
```yaml
auto-mod:
  enabled: true                  # MASTER-SWITCH: alles aus/an
  operator-name: "AutoMod"       # Wer steht im Ban-Screen / Kick / Warn als "Von"?
  bypass-permission: "moderation.automod.bypass"
  notify-staff: true             # Detaillierte Staff-Message bei jeder Strafe

  categories:
    # Jede Kategorie einzeln an/abschaltbar + eigene rules-Liste
    chat-swear:
      enabled: true
      rules:
        - at-violations: 1       # Ab wie viele Verstöße diese Regel greift
          action: warn           # warn | mute | tempmute | ban | tempban | kick
          reason: "Beleidigung"
          duration-minutes: -1   # -1 = permanent (nur ban/mute relevant)
          cooldown-minutes: 0    # 0 = nie wiederholen solange count passt
        - at-violations: 3
          action: mute
          reason: "Mute nach mehrfacher Beleidigung"
          duration-minutes: 30
          cooldown-minutes: 0
        - at-violations: 5
          action: ban
          reason: "Ban nach 5 Beleidigungen"
          duration-minutes: 1440
          cooldown-minutes: 0
```

#### Die 5 vorkonfigurierten Kategorien:

| Kategorie | Trigger | Default-Regeln |
|---|---|---|
| `chat-swear` | Anti-Swear (blockierte Nachricht) | 1× Warn → 3× Mute 30 Min → 5× Ban 1 Tag |
| `chat-links` | Link-Schutz (Whitelist/Blacklist) | 1× Warn → 2× Mute 15 Min → 3× Ban 1 Tag |
| `chat-spam` | Anti-Spam (Delay/Caps/Flood/Repeat) | 2× Warn → 4× Mute 10 Min → 7× Ban 1 Tag |
| `warn-thresholds` | **Aktive Warnungen** zählen (z.B. nach `/warn` von Staff + AutoMod) | 5 Warns → Mute 30 Min (60 Min Cooldown) / 10 Warns → Ban 3 Tage |
| `vpn` | VPN/Proxy-Erkennung (ip-api.com, Schwellwert: 70) | 1× → Ban Permanent |

#### Hinzufügen eigener Kategorien:
Einfach unter `auto-mod.categories:` ergänzen und per API auslösen:
```java
plugin.getAutoModManager().reportViolation(player, "command-spam", "/give spam");
plugin.getAutoModManager().triggerDirect(targetUUID, name, "illegal-items", 1, "Shulker Box Stack");
```

#### API-Methoden in [AutoModManager](file:///c:/Users/Jasmin/IdeaProjects/ModerationManager/src/main/java/net/rainbowfurry/moderationManager/managers/AutoModManager.java):
- `reportViolation(player, "kategorie", extra)` → Counter +1, wendet passende Regel an
- `triggerDirect(uuid, name, "kategorie", level, extra)` → direkte Regelauswahl (VPN, Warn-Thresholds)
- `resetViolations(playerId, "kategorie")` / `resetAllForPlayer(playerId)` / `clearCachesFor(player)`
- `getViolationCount(playerId, "kategorie")`

---

### 📐 GUI anpassen (Titel / Items / Texte)

Unter `gui:` kannst du **jedes Menü bis auf die letzte Ebene** individualisieren. Unter-Sektionen pro Menü:
- `gui.main-menu` (Slots 10, 13, 16, 20, 22, 24, 30, 32, 40)
- `gui.player-selector` (Pagination, Prev/Next, Close/Back, Status Kopf Lore Online/Offline)
- `gui.player-info` (Kopf, Stats, Punish, History, Alts, Schnell-Aktionen, Unmute/Unban, Back)
- `gui.punish-menu` (**Typ-Buttons**: warn/kick/mute/ban, Duration-Item Texte, Reason-Item Texte, Apply-Enabled/Disabled, Reset-Button, Status-Book)
- `gui.history-menu` (Blätterfunktion, Active/Inactive Status, Typ-Material-Mapping, Delete-Hint für Shift+Click)
- `gui.alt-accounts-menu` (Alt-Liste mit IP/VPN)
- `gui.chat-control` (4 Materialien On/Off pro Button, Texte On/Off, Slowmode-Delay in Lore, Auto-Status StaffChat)
- `gui.items` (Generelle Einstellungen: Back-Arrow, Close-Barrier, Reset-Button, Apply-Button, Status-Book, Skull-Namen)

#### Beispiel:
```yaml
gui:
  fill-border-material: "GRAY_STAINED_GLASS_PANE"
  fill-glass-material: "BLACK_STAINED_GLASS_PANE"

  punish-menu:
    title: "<gradient:#f44336:#ff9800><bold>⚔️ Strafen: %name%</bold></gradient>"
    rows: 6
    type-buttons:
      warn:
        material: "YELLOW_BANNER"
        name: "<yellow>⚠️ Warnung"
        lore:
          - "<gray>Verwarnung ohne Einschränkung"
```

Alle Materialien nutzen die **Material-Enum Namen** aus Paper 1.21:
🔗 [Paper Material Javadoc](https://jd.papermc.io/paper/1.21/org/bukkit/Material.html)

---

### 🚩 Straf-Gründe & Dauer-Templates hinzufügen

Unter `punishments.presets.reasons` und `punishments.presets.durations`.

#### Eigene Gründe:
```yaml
punishments:
  presets:
    reasons:
      - "Spam / Chat Missbrauch"
      - "Beleidigung / Beleidigung anderer Spieler"
      - "Werbung (IP / Server)"
      - "Trolling / Team-Grief"
      - "Bug Ausnutzung"
      - "Cheating / Hacking"
      - "Bannumgehung / Alt-Account"
      - "Sonstiges - siehe Notizen"
```

#### Eigene Dauer-Templates:
```yaml
punishments:
  presets:
    durations:
      - label: "5 Min"
        duration-ms: 300000
        material: "GOLD_INGOT"
      - label: "Permanent"
        duration-ms: -1          # -1 = dauerhaft
        material: "BEDROCK"
```

⏱️ Umrechnungshilfe: 1s = `1000`, 1m = `60000`, 1h = `3600000`, 1d = `86400000`

---

### 💬 Alle Text-Outputs anpassen

Unter `messages:` findest du **jede einzelne Chat-Nachricht** die das Plugin ausgibt:

```yaml
messages:
  no-permission: "<red>Du hast keine Rechte dazu!"
  player-only: "<red>Dieses Kommando kann nur als Spieler ausgeführt werden!"
  config-reloaded: "<green>Moderation Config wurde neu geladen!"
  # ...
  alt-detected-notify: "<gradient:#ff6f00:#ff3d00>⚠️ ALT-ACCOUNT</gradient> <yellow>%name% <gray>hat die gleiche IP wie <white>%other%<gray>!"
  vpn-detected-notify: "<gradient:#9d50bb:#6e48aa>⚠️ VPN</gradient> <yellow>%name% <gray>verwendet eine VPN/Proxy IP (<white>%ip%<gray>)"
```

#### Alle unterstützten Platzhalter `%…%`:

| Platzhalter | Beispiele (Vorkommen) |
|---|---|
| `%name%`, `%target%`, `%player%`, `%other%` | Spielername (Staff, Ziel, Alt-Account Vergleich) |
| `%reason%` | Grund einer Strafe |
| `%operator%`, `%op%`, `%by%` | Wer hat ausgeführt? (Punish, Chat Lock, Clear) |
| `%id%` | Strafe ID (Ban #123) |
| `%type%` | Typ (BAN/TEMPMUTE/WARN/KICK) |
| `%duration%`, `%time%`, `%label%` | Dauer (5 Min / 1 Tag / Permanent), Restzeit bei Bans |
| `%unban_date%`, `%unmute_date%`, `%date%`, `%last_seen%` | Datums-Platzhalter |
| `%bans%`, `%mutes%`, `%warns%`, `%kicks%`, `%count%` | Spieler-Statistiken |
| `%message%`, `%command%` | StaffChat-Nachricht / Command-Monitor |
| `%seconds%` | Slowmode Delay / Wartezeit |
| `%page%`, `%current%`, `%max%`, `%total%` | Paginierung (History, AltAccounts, Selector) |
| `%state%`, `%value%`, `%ip%`, `%uuid%` | Status On/Off, Feldwerte, IP, UUID |
| `%anzahl%`, `%history_list%` | Spezielle PlayerInfo Platzhalter |

---

### 🛡️ Schutzmechanismen im Detail

#### 1. Anti-Spam
- `anti-spam.message-delay: 800` (Millis zwischen 2 Nachrichten)
- `anti-spam.messages-per-minute: 6`
- Caps-Lock: Schwellwert in %, Min-Länge, auto-correct
- Repeat-Protection: gleiche Nachricht Max N mal
- Flood-Protection: N gleiche Zeichen am Stück
- → Meldungen + AutoMod `chat-spam` Regel-Triggerung

#### 2. Link-Schutz
- Modus `WHITELIST` (empfohlen) oder `BLACKLIST`
- `block-ip-links: true` (IP:Port → blockiert, z.B. 1.1.1.1:25565)
- Whitelist: youtube, twitch, discord, minecraft, curseforge, modrinth, spigot, paper, rainbowfurry
- → `chat-links` AutoMod

#### 3. Beleidigungen
- Modus `BLOCK` oder `REPLACE` mit `****`
- Standard Blacklist: ~20 Begriffe (DE + EN)
- Checkt zusätzlich Private-Nachrichten Commands: `msg, tell, r, w, whisper, me, say`
- → `chat-swear` AutoMod

#### 4. DDoS / Join Schutz
- `max-connections-per-ip: 3` (gleichzeitig)
- `joins-per-second: 1` / `total-joins-per-minute: 60`
- Optional Packet-Rate-Limit
- Temp-Block Default 30 Min
- Whitelist 127.0.0.1 (für BungeeCord lokal)

#### 5. Raid-Erkennung
- Threshold `15` neue Spieler in `10` Sekunden
- Auto-Action: `LOCKDOWN` (10 Min)
- Ban neuer Spieler während Raid (optional)
- Staff-Alert + Sound

#### 6. OP Abuse Schutz
- OP-Änderungslog + Staff-Alert
- Blockierte OP-Commands: stop, op, deop
- OP-Whitelist: automatisch De-OP wenn Spieler nicht in Liste
- Command-Monitoring: Benachrichtigung bei `gamemode`, `give`, `fill`, `summon`, `ban`, `pardon`, `op`, `deop`, `stop`

#### 7. Alt-Account & VPN
- Auto-Detect: Staff-Alert bei Join mit bereits bekannter IP
- Max `3` Accounts pro IP (Standard)
- VPN Erkennung: ip-api.com API, Schwellwert `70` (0=IP, 70+=VPN/Proxy/Hosting)
- VPN Autoban (Standard: false, auf true via `auto-mod.categories.vpn.enabled`)

---

## 🧩 Architektur

```
ModerationManager (Hauptklasse, Getter für alle Manager)
  ├── commands/CommandManager.java              ← Alle Befehle (Ban/TempBan/Kick/Warn/Mod/Relaod etc.)
  ├── listeners/                                 ← Event Listener
  │   ├── ChatListener.java                     ← Anti-Swear/Links/Spam via ChatManager
  │   ├── ConnectionListener.java               ← Join/Quit, Alt/VPN/Rate-Limit, Playtime Tracking
  │   ├── MenuInventoryListener.java            ← BaseMenu Klick-Handler (GuiManager)
  │   └── VanishListener.java                   ← Pickup/Damage/Join Cancel für vanished
  ├── guis/                                      ← Inventar-Menüs
  │   ├── BaseMenu.java                         ← Abstraktes Basis-Menu (createInventory, refresh, setItem, fillBorder, skull, makeItem)
  │   ├── MainMenu.java                         ← Hauptmenü (Slots 10/13/16/20/22/24/30/32/40)
  │   ├── PlayerSelectorMenu.java               ← Online Spieler Liste (Pagination 28/Slot)
  │   ├── PlayerInfoMenu.java                   ← Spieler-Profil + Schnell-Aktionen + Unmute/Unban
  │   ├── PunishMenu.java                       ← Straf-GUI (Typ→Dauer→Grund→Apply; aus Config!)
  │   ├── HistoryMenu.java                      ← Straf-Historie (blätterbar, Shift+Click Delete)
  │   ├── AltAccountsMenu.java                  ← Alt-Accounts pro IP (Pagination)
  │   ├── ChatControlMenu.java                  ← Clear/Lock/Slowmode/StaffChat Toggles
  │   └── ProtectionControlMenu.java            ← Lockdown / Items-Clear / Mobs / Lag-Clear
  ├── managers/                                  ← Business Logic (ohne Bukkit Events)
  │   ├── ConfigManager.java                    ← 100% Getter (Config + AutoModRule/DurationPreset Records)
  │   ├── PunishmentManager.java                ← Ban/Mute/Warn/Kick/Unban/Unmute, Check für aktive Strafen, (-> AutoMod triggerDirect für Warn-Thresholds)
  │   ├── AutoModManager.java                   ← Automatischer Modus (reportViolation/triggerDirect/Cooldowns/Regel-Auswahl)
  │   ├── AltAccountManager.java                ← IP-Vergleich + ip-api.com VPN-Check (-> AutoMod.triggerDirect)
  │   ├── ChatManager.java                      ← Anti-Spam, Swear, Links, Slowmode, Lock (-> AutoMod.reportViolation)
  │   ├── ProtectionManager.java                ← DDoS, Raid, Lockdown, Rate-Limit
  │   ├── StaffManager.java                     ← StaffChat (Toggle-Modus!), Vanish (Toggle), Benachrichtigungen
  │   ├── GuiManager.java                       ← Open-Menüs Registrierung, Klick-Dispatch
  │   └── DatabaseManager.java                  ← SQLite CRUD (PlayerProfile, Punishments, IPLogs, OPLogs)
  ├── models/                                    ← Daten-Modelle (POJO, Records)
  │   ├── Punishment.java                       ← Type enum (BAN/TEMPBAN/KICK/MUTE/TEMPMUTE/WARN/UNBAN/UNMUTE)
  │   ├── PlayerProfile.java                    ← Stammdaten + Spielzeit + Zähler (bans/warns/kicks/mutes)
  │   ├── IPLog.java                            ← IP-Log Eintrag (mit PlayerUUID)
  │   └── OPLogEntry.java                       ← OP Änderungs-Eintrag
  └── utils/                                     ← Helfer
      ├── MessageUtils.java                     ← MiniMessage + %var% Ersetzung + formatPunishment + kickPlayer + formatDate/Playtime/Duration
      ├── DurationUtils.java                    ← parseDuration("1d12h"), formatDuration, formatRemaining
      └── UUIDUtils.java                        ← UUID von Name via Mojang API (Offile-Player UUID)
```

---

## 🛠️ Build-Anleitung

Das Projekt nutzt **Maven** (Wrapper optional).

```bash
# 1. Repository klonen
git clone https://github.com/<DEIN-USER>/ModerationManager.git
cd ModerationManager

# 2. Maven Build (Dependencies werden automatisch geladen)
mvn clean package -DskipTests

# 3. Fertige JAR
# Ziel: target/moderationmanager-1.0.jar
```

Oder mit Maven Wrapper (im Projekt vorhanden):
```powershell
# Windows (PowerShell):
.\mvnw.cmd clean package -DskipTests
```

#### Enthaltene Libraries (via Maven Shade in JAR hineinkompiliert):
- 🗄️ **sqlite-jdbc 3.46.0** — SQLite Treiber
- ⚡ **HikariCP 5.1.0** — High-Performance Connection Pool
- 🌈 **MiniMessage / Adventure API** — Paper-nativ
- 📊 **bStats 3.0.2** — Optionale Nutzungsstatistik (anonym, auschaltbar)
- 🔍 **slf4j-api + slf4j-simple** — Logging Bridge für HikariCP

---

## 🩺 Troubleshooting

| Problem | Lösung |
|---|---|
| 🔴 **JAR startet nicht / NoClassDefFound** | Sicherstellen, dass Server **Java 21** nutzt und mindestens **Paper 1.21** |
| 🔴 **Chat-Nachrichten zeigen `%reason%` statt Wert** | Prüfe, ob die Platzhalter in Config korrekt sind (`%key%` Syntax); `/modreload` nicht vergessen |
| 🔴 **GUI öffnet sich nicht / Items fehlen** | Server-Log auf **Stack-Trace** prüfen - oft falsches Material in `config.yml` eingetragen (Enum-Namen prüfen, z.B. `RED_BANNER` statt `BANNER`) |
| 🔴 **AutoMod greift nicht (alte Versionen)** | Alt: Legacies `anti-spam.auto-punishments`, `anti-swear.auto-punishments`, `auto-rules.warn-thresholds`, `alt-accounts.vpn-autoban` werden jetzt ALLE von `auto-mod:*` gehandhabt. Entferne diese alten Abschnitte oder aktiviere `auto-mod.enabled: true` und prüfe pro Kategorie das `enabled:` Flag |
| 🟡 **Alt-Account Erkennung funktioniert nicht** | `alt-accounts.enabled: true` + `alt-accounts.auto-detect: true` prüfen |
| 🟡 **bStats Meldung im Log** | Kann in config via `general.bstats: false` abgeschaltet werden |
| 🟡 **SQLite Lock Timeout** | Prüfe Festplattenzugriffsrechte; HikariCP max-pool-size ggf. herabsetzen |
| 🟠 **VPN Erkennung zu aggressiv / zu schwach** | `alt-accounts.vpn-threshold` anpassen (Standard 70). Höher = strikter |
| 🟠 **Shift+Click in History löscht nichts** | Permission `moderation.punishments.delete` vergeben. |
| 🟠 **Auto-Mod Meldungen doppelt** | Alte `notify-staff` in den Schutz-Bereichen (z.B. `alt-accounts.notify-staff-on-alt-join`) + AutoMod `notify-staff` beide aktiv. Deaktiviere in den alten Abschnitten oder überlasse es AutoMod. |

---

## 🗺️ Roadmap / Geplante Features

- [ ] **AutoMod 2.0**: GUI zur Laufzeit-Regel-Verwaltung
- [ ] **/ignore** System (Spieler gegenseitig blockieren)
- [ ] **Mehr AutoMod Kategorien**: Command-Spy, Illegal-Items, Grief-Prävention
- [ ] **MySQL / PostgreSQL Support** (zusätzlich zu SQLite)
- [ ] **PlaceholdersAPI Integration** (`%mm_bans_%player%%`, etc.)
- [ ] **Discord Webhook** (Ban/Mute/Warn Benachrichtigungen + AutoMod Alerts)
- [ ] **Import/Export** von Straf-Historien (JSON/SQL)
- [ ] **Mehrsprachigkeit** (en/de/es automatisch über Spieler Locale)

---

## 🤝 Support

🐛 **Bug gefunden?** Bitte einen Issue öffnen mit:
- Server Version (`/version`)
- Plugin Version
- **Stack-Trace** aus latest.log
- Schritte zum Reproduzieren
- Auszug aus der betroffenen Config-Sektion (falls Konfigurations-Problem)

💬 **Fragen?** Kontaktiere mich gerne:
- Discord: *füge deinen Discord ein*
- E-Mail: *füge deine E-Mail ein*
- SpigotMC: *füge Resource-Seite ein*
- Homepage: [rainbowfurry.com](https://www.rainbowfurry.com)

---

<div align="center">
<br>

> *Ein Projekt von **RainbowFurry Studios***  
> ModerationManager — The Only Moderation Plugin You'll Ever Need.

</div>
