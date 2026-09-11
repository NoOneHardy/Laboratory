# ORBIT-7

> Every franc you don't spend becomes power for a station that would otherwise go dark.

ORBIT-7 is a gamified expense reducer for Android. It is not a bookkeeping app: its only purpose is
to make **spending less** — on daily purchases and on silently renewing contracts — something you
actually want to do.

Saved money converts to **Energy Points (EP)**. EP powers and expands a derelict orbital station.
The station is the progress bar, and it is rendered in a cassette-futurist CRT aesthetic: amber and
phosphor green on near-black, bezelled panels, split-flap readouts.

| | |
|---|---|
| **Platform** | Android, native |
| **Stack** | Kotlin, Jetpack Compose, Room, Hilt, WorkManager, Glance |
| **Package** | `ch.no1hardy.orbit7` |
| **Backend** | None. Offline-first, no network permission, data never leaves the device |
| **Locale** | de-CH (default) and English, CHF default currency |
| **Distribution** | Personal use and portfolio, sideloaded APK |
| **Status** | 📐 **Concept phase** — documentation only, no code yet |

## Documents

| # | Document | What it settles |
|---|---|---|
| 01 | [Concept](docs/01-concept.md) | Problem, solution, core loop, scope and non-goals |
| 02 | [Game design](docs/02-game-design.md) | The EP economy, weekly settlement, station, Contract Hunter |
| 03 | [Screens](docs/03-screens.md) | Every screen, state and surface, with test tags |
| 04 | [Design system](docs/04-design-system.md) | Cassette-futurist tokens, type, motion, accessibility |
| 05 | [Architecture](docs/05-architecture.md) | Modules, data model, the four hard rules |
| 06 | [Test strategy](docs/06-test-strategy.md) | Seven test tiers and CI — written before the code |
| 07 | [Roadmap](docs/07-roadmap.md) | Phases P0–P6, risks, open questions |

## Reading order

If you read only two documents, read **02 (game design)** — it is what makes the app different from
every other expense tracker — and **06 (test strategy)**, which is deliberately written before a
single line of production code exists.

## Next step

Phase **P0** in the [roadmap](docs/07-roadmap.md): the walking skeleton. Module structure, version
catalog, CI workflow, and one green test in every tier — before any feature is built.
