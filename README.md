<p align="center">
  <img src="docs/assets/banner.svg" alt="Chaos Events banner" width="100%">
</p>

<p align="center">
  A server-controlled NeoForge mod that turns a Minecraft multiplayer session into an unpredictable event show.
</p>

<p align="center">
  <a href="https://github.com/pikachesrar-ui/minecraft-chaos-events/actions/workflows/build.yml"><img alt="Build" src="https://img.shields.io/github/actions/workflow/status/pikachesrar-ui/minecraft-chaos-events/build.yml?branch=main&style=flat-square&label=build"></a>
  <img alt="Minecraft 1.21.1" src="https://img.shields.io/badge/Minecraft-1.21.1-62b47a?style=flat-square">
  <img alt="NeoForge 21.1.244+" src="https://img.shields.io/badge/NeoForge-21.1.244%2B-f47b20?style=flat-square">
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-e76f00?style=flat-square">
  <a href="LICENSE"><img alt="MIT License" src="https://img.shields.io/badge/license-MIT-8b5cf6?style=flat-square"></a>
</p>

<p align="center">
  <strong>English</strong> · <a href="README_RU.md">Русский</a>
</p>

## What is Chaos Events?

Chaos Events runs several independent systems while a session is active. Large world events, targeted nonlethal pranks, timed trivia and spatial swaps overlap to create a stream-friendly challenge for friends and small multiplayer servers.

The session is controlled by an operator and does not start automatically. Use `/chaos start` when everyone is ready, pause every timer with `/chaos pause`, and cleanly stop active mechanics with `/chaos stop`.

> [!WARNING]
> This is an early public release. Some events intentionally change weather, gamerules, player positions, mobs and blocks. Back up important worlds before playing.

## Highlights

- **Large chaos events** with a boss-bar timer, randomized breaks and no immediate repeats.
- **Micro pranks** aimed at individual players every 1–3 minutes, designed to be disruptive without being deliberately lethal.
- **Trivia rounds** every 6–12 minutes with a 15-second answer window, rewards and penalties for mistakes.
- **Spatial swaps** that exchange online players between positions and dimensions, including a cooperative return-anchor mechanic.
- **In-game content configuration** through four books that toggle individual events, pranks, trivia questions and swap triggers.
- **Accelerated world event** that runs the world at 200 TPS while keeping players close to normal speed.
- **Optional disaster integrations** for [Weather2](https://www.curseforge.com/minecraft/mc-mods/weather-storms-tornadoes) tornadoes and [Oh My, Meteors!](https://www.curseforge.com/minecraft/mc-mods/oh-my-meteors) meteor showers when those mods are installed.
- English and Russian item localization; server announcements are currently Russian-first.

## Requirements

| Component | Version |
| --- | --- |
| Minecraft | `1.21.1` |
| NeoForge | `21.1.244` or newer compatible `1.21.1` build |
| Java | `21` |
| Side | Client and server |

Optional integrations are detected at runtime. Weather2 and Oh My, Meteors! are not required for Chaos Events to load.

## Installation

1. Install Minecraft `1.21.1` with a compatible NeoForge build.
2. Download the Chaos Events JAR from the [Releases](https://github.com/pikachesrar-ui/minecraft-chaos-events/releases) page or build it from source using the instructions below.
3. Put the JAR in the `mods` folder on the server and on every connecting client.
4. Start the world and run `/chaos start` as an operator.

## Commands

All commands require permission level 2.

| Command | Purpose |
| --- | --- |
| `/chaos start` | Start a new chaos session |
| `/chaos pause` | Freeze active mechanics and all timers |
| `/chaos resume` | Continue a paused session |
| `/chaos stop` | Stop the session and clean up temporary effects |
| `/chaos status` | Show engine state, timers and registered content counts |
| `/chaos skip` | End the current large event and schedule the next break |
| `/chaos book big` | Give the large-events configuration book |
| `/chaos book prank` | Give the micro-pranks configuration book |
| `/chaos book trivia` | Give the trivia configuration book |
| `/chaos book swap` | Give the player-swaps configuration book |
| `/chaos test big` | Force a random large event |
| `/chaos test speed` | Force the accelerated-world event |
| `/chaos test prank` | Apply a random micro prank |
| `/chaos test screamer` | Show a screamer to the selected player |
| `/chaos test trivia` | Start a trivia question |
| `/chaos test swap` | Force a spatial swap; requires at least two players |

## In-game configuration

An operator can obtain one of four configuration books with `/chaos book ...` and open it with right click. Each book presents a paged list with `ON`/`OFF` toggles. **Save** writes the selection to `config/chaosevents-settings.json`.

When a Chaos Events session is already active, saving settings cleanly clears temporary mechanics and restarts the internal chaos session with the newly enabled content. A full Minecraft or server restart is not required.

The swap book can independently disable the scheduled 15–20 minute swap, the one-time first-diamond-ore swap, and the Spatial Swap large event.

## Build from source

Install a Java 21 JDK, then clone the repository and run:

```bash
./gradlew build
```

On Windows PowerShell:

```powershell
.\gradlew.bat build
```

The compiled JAR is written to `build/libs/`. Development clients and servers can be launched with `./gradlew runClient` and `./gradlew runServer`.

Every push and pull request is compiled by GitHub Actions. Tags matching `v*` build the project and publish the resulting JAR as a GitHub release.

## Contributing

Bug reports, event ideas and pull requests are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting a change, and use the repository issue templates so reproduction details are not missed.

## License and attribution

Chaos Events is available under the [MIT License](LICENSE). It is an independent community project and is not affiliated with Mojang Studios, Microsoft, NeoForge or the authors of optional integrations. Minecraft is a trademark of Microsoft Corporation.
