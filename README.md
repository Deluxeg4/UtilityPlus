# UtilityPlus

UtilityPlus is a Paper and Folia plugin with chat controls, player utilities, server information, and administrative tools.

## Features

- Chat controls for global chat, private messages, player ignores, and death messages.
- Clickable online player names in chat. Hover to see the message hint; click to put `/w <player>` in the chat input.
- A paginated `/ignorelist` for soft and permanent ignores, with labels that can be clicked to remove the matching ignore type. Each page lists nine players.
- Player-specific regional TPS in the tab list on Folia. Each player sees the TPS for the region they are currently in.
- Tab-list placeholders for TPS, online players, ping, and server uptime.
- Administrative tools for vanish, inventory and ender-chest inspection, offline teleport, gamemode, broadcast, and shutdown countdowns.
- Optional Floodgate/Geyser support for Bedrock join notices and coordinate display.

## Requirements

- Java 21 or newer
- Paper 1.21.1 or a compatible Paper fork; Folia is supported
- Floodgate and Geyser are optional and only needed for their Bedrock features

## Install

1. Download the latest `UtilityPlus-<version>.jar` from [Releases](https://github.com/Deluxeg4/UtilityPlus/releases).
2. Put the jar in the server's `plugins` folder.
3. Start or restart the server. UtilityPlus creates `plugins/UtilityPlus/config.yml` on first start.
4. Edit `config.yml` and run `/upreload` to apply configuration changes.

## Build

Use Java 21 and run the Gradle wrapper from the project root:

```powershell
./gradlew.bat shadowJar
```

The plugin jar is written to `build/libs/UtilityPlus-<version>.jar`.

## Commands

### Chat

| Command | Description | Permission |
|---|---|---|
| `/ignore <player>` | Temporarily ignore an online player. | `utilityplus.chat` |
| `/ignorehard <player>` | Permanently ignore an online player. | `utilityplus.chat` |
| `/ignorelist [page]` | View soft and permanent ignores, nine players per page. | `utilityplus.chat` |
| `/ignoredeathmsgs <player>` | Toggle ignoring an online player's death messages. | `utilityplus.chat` |
| `/togglechat` | Toggle global chat visibility. | `utilityplus.chat` |
| `/toggleprivatemsgs` | Toggle private-message visibility. | `utilityplus.chat` |
| `/toggledeathmsgs` | Toggle death-message visibility until restart. | `utilityplus.chat` |
| `/toggledeathmsgshard` | Toggle persistent death-message visibility. | `utilityplus.chat` |
| `/tell <player> <message>` (`/t`) | Send a private message. | `utilityplus.pm` |
| `/msg <player> <message>` | Send a private message. | `utilityplus.pm` |
| `/w <player> <message>` | Send a private message. | `utilityplus.pm` |
| `/whisper <player> <message>` | Send a private message. | `utilityplus.pm` |
| `/pm <player> <message>` | Send a private message. | `utilityplus.pm` |
| `/r <message>` and `/reply <message>` | Reply to the last private message. | `utilityplus.pm` |
| `/l <message>` and `/last <message>` | Message the last player you contacted. | `utilityplus.pm` |

### Player and server tools

| Command | Description | Permission |
|---|---|---|
| `/v` | Toggle vanish. | `utilityplus.vanish` |
| `/gmc [player]`, `/gms [player]`, `/gmsp [player]`, `/gma [player]` | Change your gamemode; changing another player's gamemode also requires `utilityplus.gamemode.others`. | `utilityplus.gamemode` |
| `/invsee <player>` | View or edit a player's inventory. | `utilityplus.invsee` |
| `/enderchestsee <player>` (`/endersee`) | View or edit a player's ender chest. | `utilityplus.enderchestsee` |
| `/offlinetp <player>` | Teleport to an online player's location or an offline player's last saved location. | `utilityplus.offlinetp` |
| `/s <player>` | Summon a player to your location. | `utilityplus.summon` |
| `/kill` | Kill yourself after confirmation. | `utilityplus.kill` |
| `/bc <message>`, `/broadcast <message>` | Broadcast a message. | `utilityplus.broadcast` |
| `/stopnow <time\|now\|cancel\|time>` | Schedule, inspect, cancel, or start a shutdown. | `server.stop` |
| `/overclock <enchant> <level>`, `/overclock 32k`, `/overclock name <name>` | Apply unsafe enchantments or rename the held item. | `utilityplus.overclock` |
| `/ping [player]` | Show your ping or another player's ping. | `utilityplus.ping`; others: `utilityplus.ping.others` |
| `/pingall` | Show online player pings. | `utilityplus.ping.others` |
| `/tpsmore`, `/tps` | Show server performance information. | `utilityplus.tpsmore` |
| `/uptime` | Show how long the server process has been running. | `utilityplus.uptime` |
| `/upreload` | Reload configuration and plugin data. | `utilityplus.reload` |
| `/help [page]` | Open the UtilityPlus help page. | `utilityplus.helps` |

## Permissions

| Permission | Default | Description |
|---|---|---|
| `utilityplus.helps` | Everyone | Access the help menu. |
| `utilityplus.chat` | Everyone | Use chat controls and ignore commands. |
| `utilityplus.pm` | Everyone | Send and reply to private messages. |
| `utilityplus.anvil.color` | Everyone | Use color codes in anvil item names. |
| `utilityplus.kill` | Everyone | Use the self-kill command. |
| `utilityplus.ping` | Everyone | View your own ping. |
| `utilityplus.uptime` | Everyone | View server uptime. |
| `utilityplus.reload` | Operators | Reload UtilityPlus. |
| `utilityplus.vanish` | Operators | Toggle vanish. |
| `utilityplus.vanish.see` | Operators | See vanished players. |
| `utilityplus.broadcast` | Operators | Broadcast messages. |
| `utilityplus.gamemode` | Operators | Change your gamemode. |
| `utilityplus.gamemode.others` | Operators | Change another player's gamemode. |
| `utilityplus.overclock` | Operators | Use overclock item commands. |
| `utilityplus.invsee` | Operators | Inspect inventories. |
| `utilityplus.invsee.unseen` | Operators | Inspect data for players who have never joined. |
| `utilityplus.enderchestsee` | Operators | Inspect ender chests. |
| `utilityplus.offlinetp` | Operators | Teleport to saved player locations. |
| `utilityplus.summon` | Operators | Summon players. |
| `utilityplus.tpsmore` | Operators | View detailed performance information. |
| `utilityplus.ping.others` | Operators | View other players' ping. |
| `server.stop` | Operators | Control the shutdown countdown. |

## Configuration

The configuration file is `plugins/UtilityPlus/config.yml`. Run `/upreload` after editing it.

| Section | Purpose |
|---|---|
| `spawn` | First-join and respawn behavior, teleport cooldown, and warmup. |
| `random-respawn`, `unsafe-blocks` | Random respawn range, attempts, world, and unsafe blocks. |
| `join-message`, `leave-message` | Custom join/leave messages, broadcasting, and vanilla message visibility. |
| `bedrock-warning`, `bedrock-coordinates` | Optional Bedrock notices and coordinate display. |
| `death-message` | Death-message template and colors. |
| `tab-list` | Header/footer text and update interval. `%tps%` and `%server_tps%` use the viewer's current region TPS on Folia. |
| `broadcast` | Prefix used by broadcast commands. |
| `announcement.action-bar` | Repeating action-bar messages and timing. |
| `messages` | Text for ignore commands, `/ignorelist`, bad-command feedback, and player-name hover/click behavior. |

Message values support MiniMessage tags such as `<gold>`, `<dark_aqua>`, and `<bold>`. Existing config files that use legacy `&` color codes are migrated to MiniMessage format at startup. The `messages` section supports `{player}`, `{page}`, and `{pages}` where applicable. Player-name settings include:

```yaml
messages:
  player-name:
    hover: "<gold>Message <dark_aqua>{player}"
    suggest-command: "/w {player}"
```

## License

GPL-3.0. See [LICENSE](LICENSE).
