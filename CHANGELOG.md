# Changelog

## 2.0.0 - 2026-09-29

### Added

- `/ignorelist` now shows temporary (`soft`) and permanent (`hard`) ignores, with a separate removal control for each type.
- Ignore lists preserve players' official capitalization while matching command arguments without case sensitivity.
- Added player-specific regional TPS reporting on Folia, optional Floodgate/Geyser support, and expanded inventory inspection tools.
- Added a GitHub Actions build workflow and automated release publishing for version tags.

### Changed

- Updated the plugin's command and configuration documentation for the current Paper and Folia feature set.
- Death messages use configured templates from `message.json` before built-in defaults.

### Breaking changes

- Removed the previous home (`/sethome`, `/home`, `/delhome`), TPA (`/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, `/tpcancel`, `/tpaon`, `/tpaoff`), team, queue, stats, and spawn command systems, along with `/chat` and `/chatsettings`. Review the current command list before upgrading.
- The help command is now `/help` (previously `/helps`); the `/dm` private-message alias is no longer included.
