# UtilityPlus
> Lightweight chat, vanish, death-message, spawn, and server utility tools for Paper servers.

## Features
- Manage chat visibility, private messages, ignores, and death-message preferences.
- Customize join, leave, death, tab-list, broadcast, and action-bar messages.
- Track server TPS, MSPT, CPU, memory, disk usage, uptime, and player ping.
- Control admin utilities such as vanish, reload, kill confirmation, and shutdown countdowns.
- Support Paper and Folia-friendly task scheduling.

## Requirements
- Paper 1.21.1+
- Java 21+

## Installation
1. Download the latest `UtilityPlus-*.jar`.
2. Drop the jar into your server's `plugins/` folder.
3. Restart the server.
4. Edit `plugins/UtilityPlus/config.yml` if you want custom messages or spawn behavior.
5. Run `/upreload` after config changes when possible.

## Commands
| Command | Description | Permission |
|---|---|---|
| `/ignore <player>` | Temporarily ignore a player. | `utilityplus.chat` |
| `/ignorehard <player>` | Permanently ignore a player. | `utilityplus.chat` |
| `/ignorelist` | List permanently ignored players. | `utilityplus.chat` |
| `/ignoredeathmsgs <player>` | Hide death messages from a player. | `utilityplus.chat` |
| `/togglechat` | Toggle global chat visibility. | `utilityplus.chat` |
| `/toggleprivatemsgs` | Toggle private message visibility. | `utilityplus.chat` |
| `/toggledeathmsgs` | Toggle death message visibility until restart. | `utilityplus.chat` |
| `/toggledeathmsgshard` | Toggle death message visibility permanently. | `utilityplus.chat` |
| `/msg <player> <message>` | Send a private message. | `utilityplus.pm` |
| `/w <player> <message>` | Send a private message. | `utilityplus.pm` |
| `/whisper <player> <message>` | Send a private message. | `utilityplus.pm` |
| `/pm <player> <message>` | Send a private message. | `utilityplus.pm` |
| `/r <message>` | Reply to the last private message. | `utilityplus.pm` |
| `/reply <message>` | Reply to the last private message. | `utilityplus.pm` |
| `/l <message>` | Message the player you last messaged. | `utilityplus.pm` |
| `/last <message>` | Message the player you last messaged. | `utilityplus.pm` |
| `/upreload` | Reload UtilityPlus config and data. | `utilityplus.reload` |
| `/v` | Toggle vanish mode. | `utilityplus.vanish` |
| `/bc <message>` | Broadcast a message to all players. | `utilityplus.broadcast` |
| `/broadcast <message>` | Broadcast a message to all players. | `utilityplus.broadcast` |
| `/kill` | Kill yourself after confirmation. | `utilityplus.kill` |
| `/stopnow <time\|now\|cancel\|time>` | Manage a shutdown countdown. | `server.stop` |
| `/help [page]` | Show the UtilityPlus help link. | `utilityplus.helps` |
| `/tpsmore [show]` | Show detailed TPS and performance info. | `utilityplus.tpsmore` |
| `/tps [show]` | Show detailed TPS and performance info. | `utilityplus.tpsmore` |
| `/ping [player]` | Show your ping or another player's ping. | `utilityplus.ping` |
| `/pingall` | Show all online player pings. | `utilityplus.ping.others` |
| `/uptime` | Show server uptime. | `utilityplus.uptime` |

## Permissions
| Node | Default | Description |
|---|---|---|
| `utilityplus.helps` | `true` | Access the plugin help menu. |
| `utilityplus.chat` | `true` | Manage chat preferences. |
| `utilityplus.pm` | `true` | Send private messages. |
| `utilityplus.anvil.color` | `true` | Use color codes in anvil item names. |
| `utilityplus.reload` | `op` | Reload UtilityPlus config and data. |
| `utilityplus.vanish` | `op` | Toggle vanish mode. |
| `utilityplus.vanish.see` | `op` | See vanished players. |
| `utilityplus.broadcast` | `op` | Broadcast messages. |
| `utilityplus.kill` | `true` | Use the self-kill confirmation command. |
| `server.stop` | `op` | Start, cancel, or inspect shutdown countdowns. |
| `utilityplus.tpsmore` | `op` | View detailed TPS and performance info. |
| `utilityplus.ping` | `true` | View your own ping. |
| `utilityplus.ping.others` | `op` | View other players' pings. |
| `utilityplus.uptime` | `true` | View server uptime. |

## Configuration
`config.yml` is created on first run with sensible defaults.

- `spawn`: controls first-join, death, no-respawn-point, cooldown, and warmup behavior.
- `random-respawn`: enables random death respawns and controls radius, attempts, and target world.
- `unsafe-blocks`: lists blocks that random respawn should avoid.
- `join-message` and `leave-message`: control custom join/quit text, vanilla hiding, and broadcast behavior.
- `death-message`: controls custom death-message formatting and colors.
- `tab-list`: configures header/footer lines and refresh interval.
- `broadcast`: sets the prefix used by `/bc` and `/broadcast`.
- `announcement.action-bar`: controls the repeating action-bar announcement.
- `queue.message`: stores queue text used by queue-style output.

Color formatting uses `&` codes and is rendered through Adventure components.

## Building from source
```bash
git clone
cd UtilityPlus
./gradlew shadowJar
# output: build/libs/UtilityPlus-*.jar
```

## License
MIT — © 2025 deluxeg4
