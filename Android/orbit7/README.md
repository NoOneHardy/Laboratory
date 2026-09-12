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
| **Status** | 🛠 **Implemented** — every phase of the roadmap is built; see [08 — Implementation notes](docs/08-implementation-notes.md) for what is verified and what is not |

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
| 08 | [Implementation notes](docs/08-implementation-notes.md) | What was built, where it departs from 01–07, and what is verified |

## Reading order

If you read only two documents, read **02 (game design)** — it is what makes the app different from
every other expense tracker — and **06 (test strategy)**, which is deliberately written before a
single line of production code exists.

## Building it

```bash
cd Android/orbit7
./gradlew :core:domain:test     # the economy: 121 tests, no emulator, a few seconds
./gradlew check                 # static analysis, unit tests, screenshots, coverage gates
./gradlew assembleDebug         # the APK
./gradlew connectedDebugAndroidTest   # the end-to-end journeys, on an emulator
```

The debug build installs a second launcher icon, **ORBIT-7 debug**, carrying the component gallery
and the time-travel screen that simulates a year of play in seconds.

## Where the code lives

```
:app            Application, MainActivity, navigation, notifications, debug surfaces
:core:domain    ← the whole economy, in pure Kotlin. No Android dependency, at all
:core:data      Room, DataStore, repositories, the three workers, JSON backup
:core:designsystem  Tokens, Orbit7Theme, components, test tags, MoneyFormatter
:core:testing   FakeClock, builders, fakes, the canonical fixture history
:feature:*      expenses · budget · station · contracts · reports · settings
:widget         The Glance widget
:tooling:detekt-rules   The four custom rules that enforce the hard rules
```

## Next step

The first compile on a machine with the Android SDK, then recording the Paparazzi goldens and
committing the exported Room schema — see
[08 — Implementation notes §3](docs/08-implementation-notes.md#3-verification-status).
