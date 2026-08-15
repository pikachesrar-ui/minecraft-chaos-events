# Changelog

Notable changes to Chaos Events are documented here. The project follows [Semantic Versioning](https://semver.org/) for tagged releases.

## [Unreleased]

### Added

- Public project documentation in English and Russian.
- Contribution guidelines and GitHub issue templates.
- Automatic GitHub releases for version tags.

### Changed

- Replaced NeoForge template metadata with project-specific information.
- Adopted the MIT License for the public project.

## [0.3.4] — 2026-08-15

### Added

- Two players using beds within six blocks of each other now have a 1-in-12 chance to be sent
  into Places together when the second player uses their bed.
- Scheduled Places slips now have a 25% chance to take a second eligible player when at least two
  players are available.
- Chaos-triggered Places slips now choose one of six verified native arrival procedures: Level 0,
  Manila, Red Road, The End, Structure Bridge or Warp Tunnel. Players travelling together use the
  same destination.
- Git-ignored `src/private/resources` assets are included in local builds, allowing private
  screamer images and sounds to stay out of the public repository and GitHub releases.

### Changed

- Scheduled Places slips now occur every 40–90 minutes.
- The 28 non-screamer micro-pranks do not repeat until their cycle is exhausted; the two screamer
  prank types remain eligible throughout the cycle, while immediate identical repeats are avoided.

## [0.3.3] — 2026-08-14

### Changed

- Removed the long-running `Life Drain` and `Withered Air` big events.
- Added a short `Wither Burst` micro-prank that applies Wither I for a random 5–10 seconds.
- Players inside any Places dimension are now explicitly rejected from every spatial-swap stage;
  swaps wait when fewer than two eligible players remain and continue among the remaining players otherwise.
- Reduced automatic Places stays from 5–10 minutes to 3–9 real-time minutes.

## [0.3.2] — 2026-08-14

### Changed

- Places isolation is now per player: large events continue normally for players outside Places.
- Players inside any `places:*` dimension are removed from large-event targeting, announcements,
  boss bars and spatial swaps until they return.
- Places entity simulation remains at normal speed during the global accelerated-time event.
- Native Places door entries now receive the same automatic 5–10 minute return to the exact
  pre-entry position as Chaos Events reality slips.

### Fixed

- Active long-duration large-event effects and temporary game-mode changes are cleared immediately
  when the affected player enters Places.
- Leaving Places through a native exit now cancels only that player's pending automatic return.

## [0.1.0] — development preview

### Added

- Operator-controlled chaos session lifecycle.
- Independent large-event, micro-prank, trivia and spatial-swap engines.
- Non-repeating event and prank selection cycles.
- Boss-bar event timer and administrative test commands.
- Spatial anchors, automatic swaps and the first-diamond swap trigger.
- Optional Weather2 tornado and Oh My, Meteors! integrations.
- English and Russian localization for registered items.
