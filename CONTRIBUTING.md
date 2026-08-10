# Contributing to Chaos Events

Thanks for helping improve Chaos Events. Bug fixes, new event ideas, translations and documentation improvements are welcome.

## Development setup

1. Install a Java 21 JDK.
2. Fork and clone the repository.
3. Create a focused branch from `main`.
4. Run `./gradlew build` before opening a pull request.

Use `./gradlew runClient` for local gameplay testing and `./gradlew runServer` for dedicated-server checks. On Windows, replace `./gradlew` with `.\gradlew.bat`.

## Event design rules

- Every temporary change must be undone when its event ends, the session stops or the server shuts down.
- Micro pranks should be disruptive but should not intentionally kill a player or permanently destroy valuable items.
- Check that players, dimensions and optional mods are available before starting an event that depends on them.
- Keep server work bounded; avoid scanning large world areas or running expensive logic every tick.
- User-facing text should be clear. Add both `en_us` and `ru_ru` translations for registered content.
- Random selection should avoid immediate repeats when practical.

## Pull requests

Keep each pull request focused and explain:

- what changed and why;
- how the behavior was tested;
- whether the change modifies blocks, gamerules, inventories or player positions;
- how temporary state is restored after stopping the event.

Do not commit generated build output, IDE settings, local run directories or third-party mod JARs.

## Bug reports

Use the bug-report template and include the Chaos Events version, NeoForge version, installed optional integrations, steps to reproduce and the relevant log section. Remove access tokens, server addresses and other sensitive information before attaching logs.
