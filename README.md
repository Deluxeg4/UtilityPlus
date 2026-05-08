# UtilityPlus

UtilityPlus is a comprehensive utility plugin for Minecraft servers, designed to work seamlessly on **Paper** and **Folia**. It provides essential features like spawn management, homes, TPA requests, team systems, and administrative tools with a focus on performance and Folia compatibility.

## 🚀 Features

- **Platform Support:** Fully compatible with Paper and Folia (Regionalized Threading).
- **Spawn System:** Set and teleport to the server spawn.
- **Home System:** Up to 4 home slots per player with a GUI interface.
- **TPA System:** Request to teleport to others or have them teleport to you.
- **Team System:** Create teams, invite members, and use private team chat.
- **Vanish:** Admin invisibility mode.
- **Stats:** Track player kills and deaths with a leaderboard GUI (`/topstats`).
- **Chat Management:** Toggle global, team, or private message preferences.
- **2b2t-Style Controls:** Ignore players, toggle death messages, and view queue lengths.
- **Performance:** Includes custom TPS and CPU monitors (`/tpsmore`).
- **Admin Tools:** Overclock items, see inventories/enderchests, and managed shutdown.

## 📋 Commands & Permissions

| Command | Description | Permission | Default |
|---------|-------------|------------|---------|
| `/spawn` | Teleport to spawn | `utilityplus.spawn` | True |
| `/setspawn` | Set server spawn | `utilityplus.setspawn` | OP |
| `/home [1-4]` | Teleport to or list homes | `utilityplus.home` | True |
| `/sethome <1-4>` | Set a home slot | `utilityplus.sethome` | True |
| `/delhome <1-4>` | Delete a home slot | `utilityplus.delhome` | True |
| `/tpa <player>` | Request teleport to player | `utilityplus.tpa` | True |
| `/tpahere <player>` | Request player teleport to you | `utilityplus.tpa` | True |
| `/tpaccept` | Accept teleport request | `utilityplus.tpa` | True |
| `/tpdeny` | Deny teleport request | `utilityplus.tpa` | True |
| `/team` | Team management | `utilityplus.team` | True |
| `/msg <player> <msg>` | Send a private message | `utilityplus.pm` | True |
| `/r <msg>` | Reply to last message | `utilityplus.pm` | True |
| `/ignore <player>` | Temporarily ignore a player | `utilityplus.chat` | True |
| `/v` | Toggle vanish | `utilityplus.vanish` | OP |
| `/bc <msg>` | Broadcast a message | `utilityplus.broadcast` | OP |
| `/gmc` | Set gamemode to Creative | `utilityplus.gamemode` | OP |
| `/gms` | Set gamemode to Survival | `utilityplus.gamemode` | OP |
| `/kill [player]` | Kill yourself or others | `utilityplus.kill` | True |
| `/overclock` | Overclock held item | `utilityplus.overclock` | OP |
| `/invsee <player>` | See player inventory | `utilityplus.invsee` | OP |
| `/s <player>` | Summon player | `utilityplus.summon` | OP |
| `/topstats` | View leaderboard GUI | `utilityplus.topstats` | True |
| `/tpsmore` | Detailed performance info | `utilityplus.tpsmore` | OP |
| `/upreload` | Reload plugin | `utilityplus.reload` | OP |
| `/stopnow` | Managed server shutdown | `server.stop` | OP |

*Use `/helps` in-game for a full list of commands.*

## 🛠️ Installation

1. Download the latest release from the [Releases](https://github.com/Deluxeg4/UtilityPlus/releases) page.
2. Choose the JAR corresponding to your platform:
   - `UtilityPlus-Paper-x.x.x.jar` for Paper/Spigot.
   - `UtilityPlus-Folia-x.x.x.jar` for Folia.
3. Place the JAR in your server's `plugins` folder.
4. Restart the server.

## 🔨 Building from Source

To build the plugin yourself, ensure you have JDK 21 installed.

```bash
# Clone the repository
git clone https://github.com/Deluxeg4/UtilityPlus.git
cd UtilityPlus

# Build the JARs
./gradlew build
```

The resulting JARs will be located in `build/libs/`.

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details (if applicable).
