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

## [0.3.6] — 2026-08-31

### Added

- Added the nonlethal `Внезапный удар` micro-prank: it removes exactly half a heart when safe to do so and knocks the player in a random horizontal direction.
- Chaos-managed Places visits now restore the selected destination's native arrival templates before teleporting players, resetting the starting area for the next visit.
- Added client-side Xaero's Minimap suppression inside every `places:*` dimension using Xaero's `no_minimap` effect when it is available.

### Changed

- Every Chaos-triggered Places transfer now takes at least two eligible players when two or more are online. Solo transfer remains possible only when one eligible player is available.
- The minimum-two rule applies to scheduled slips, beds, ender pearls, dark doors, deep-cave triggers and the operator Places test path through the shared transfer pipeline.
- `Лавовые гейзеры` no longer ignite the player; the event now launches the player upward and grants a short Slow Falling effect.
- `Огненный шторм` was replaced by `Пепельная буря`, using smoke, darkness, slowness, nausea and wind-like knockback instead of directly setting the player on fire.

### Fixed

- Fixed Xaero's Minimap remaining visible after entering Places in client setups where the previous server-message approach was ineffective.
- Consolidated the previously unmerged 0.3.6 work into `main` so the version number and feature set no longer diverge between public and private builds.

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

### Fixed

- Fixed the event configuration book applying its background blur twice and obscuring event names.
- Items created by large events are now temporary and are removed from player inventories,
  open containers and the ground when the event ends. Tagged leftovers are also removed when
  a player reconnects, a container is reopened or an unloaded dropped item is loaded again.

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
