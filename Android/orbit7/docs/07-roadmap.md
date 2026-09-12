# 07 — Roadmap, Risks and Open Questions

> **Status:** P0–P6 are implemented. What that means precisely — including what has been run and
> what has not — is recorded in [08 — Implementation notes](08-implementation-notes.md).

Seven phases. Each ends with **green CI** and something demoable on a real device. The order is
deliberate: the test infrastructure exists before the features, and the design system exists before
the screens that use it.

---

## P0 — Foundation (the walking skeleton)

*No features. This phase builds the thing that keeps every later phase honest.*

- Gradle project at `Android/orbit7/`, KTS, version catalog in `gradle/libs.versions.toml`.
- All modules from [05 §2](05-architecture.md#2-module-structure) created and wired, with the
  dependency direction enforced.
- Hilt, Compose, Room (schema export **on**, v1 schema committed), DataStore, WorkManager, Glance.
- Spotless, detekt (including the four custom rules), Android Lint at `abortOnError`.
- Paparazzi and Kover configured.
- `:core:testing` with `FakeClock`, `MainDispatcherRule` and the first builders.
- **One green test in every tier T0–T6**, against a placeholder screen — so the emulator job, the
  screenshot job and the coverage gate are all proven to work before there is anything to break.
- `.github/workflows/android.yml`, both jobs, green.

**Done when:** a push to the branch runs the full pipeline green, and `./gradlew check` passes locally.

## P1 — Design system

- Colour, typography, shape and motion tokens; `Orbit7Theme` with its `CompositionLocal`s.
- Components: `Panel`, `ReadoutWindow`, `DigitRollText`, `Gauge`, `HazardBanner`, `SwitchToggle`,
  `NumpadKey`, `CategoryChip`, `BusLine`, `ScanlineOverlay`, `ModuleTile`, `StationView` (v1 art).
- The `reduceEffects` setting and its plumbing through `Orbit7Motion`.
- `testTag` constants; debug component-gallery screen.
- Paparazzi goldens for every component × 3 configs; the WCAG contrast unit test.
- `MoneyFormatter` with de-CH and en formatting, unit-tested.

**Done when:** the gallery screen shows every component on a device, and the goldens are committed.

## P2 — Expense core

- Room entities, DAOs, migrations, repositories for categories, expenses and versioned budgets.
- Quick Add (numpad, chips, 3-tap save, undo), Log (grouping, edit, swipe-delete + undo, filters),
  Budgets (editor, month/week toggle).
- Baseline computation and suggestion cards.
- T1 proration and baseline tests, T2 DAO + migration tests, T3/T4/T5 for all three screens.

**Done when:** a week of real spending can be entered and reviewed comfortably on a phone.

## P3 — Game core

- Energy ledger (append-only), balance projection and its rebuild test.
- `SettlementCalculator` in `:core:domain` — proration, confidence, streaks, venting.
- `WeeklySettlementWorker` with the catch-up path.
- Bridge screen (station viewport, power readout, week gauge, today card, zero-spend mark).
- Station screen, modules and the cost curve; settlement summary screen.
- The full property-test set from [06 §3](06-test-strategy.md#3-t1--domain-tests-the-core-of-the-suite),
  including the anti-exploit property. First E2E journey.

**Done when:** a simulated year of play in the debug time-travel screen produces a sane progression
curve, and the loop is fun on a real device.

## P4 — Contract Hunter

- Contract entity, editor, notice-period and deadline maths.
- Drains screen with bleed sorting, countdowns and hazard states.
- Cancel / renegotiate flows with salvage payouts and the salvaged section.
- `ContractDeadlineWorker` and its notifications.
- T1 notice-period and salvage tests, T2 worker tests, E2E journey 2.

**Done when:** one real contract has been entered, its deadline reminder has fired, and cancelling it
paid out correctly.

## P5 — Surfaces

- Onboarding (3 steps, Swiss default categories and budgets).
- `DailyReminderWorker` and the notification with both actions, including the no-launch zero-spend path.
- Glance widget, both sizes.
- JSON export/import with the replace-all confirmation, and wipe-all-data.
- E2E journeys 3–5.

**Done when:** the app can be installed fresh, set up in under two minutes, and lives on the home
screen.

## P6 — Polish and release

- Full localization pass, German copy reviewed by a human, not just translated.
- Accessibility audit: TalkBack traversal on every screen, 200% font scale, `reduceEffects` end to end.
- Baseline profile, R8, startup and frame-timing check.
- Signed release APK, install instructions in the README.
- The manual checklist from [06 §12](06-test-strategy.md#12-what-is-tested-by-hand).

**Done when:** the release APK is installed on the daily-driver phone and the concept documents match
what was actually built.

---

## After v1

Not scheduled, listed so they are not accidentally designed out:

- **v1.2 Missions** ([02 §7](02-game-design.md#7-missions-v12-gated-behind-comms-array)) — the
  generator is pure domain logic and can land any time after P3.
- CSV / camt.053 import — the `source` column on `expenses` already exists for it.
- Richer station art, seasonal module skins.
- A second device via export/import (deliberately manual — still no backend).

## Risks and mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| **The economy is badly balanced** — progress too fast (boring) or too slow (pointless) | Kills the whole concept | Every constant in `GameBalance`; debug time-travel screen simulates a year in seconds; P3 is not "done" until a simulated year looks right |
| **The retro effects hurt battery or accessibility** | Uninstall, or excludes users | `reduceEffects` built in P1, not retrofitted; auto-enabled from system settings; ambient motion foreground-only; battery measured in the manual checklist |
| **Kotlin and Compose are new territory** | Slow start, poor idiom | P0 and P1 are deliberately feature-free; the economy is written in pure Kotlin with no framework to fight; heavy test coverage makes refactoring safe once the idiom improves |
| **Scope creep from Contract Hunter** | P4 swallows the project | It is scheduled last of the core features, after the loop already works; its data model is small and fully specified in [05 §4](05-architecture.md#4-data-model) |
| **The daily habit doesn't stick** | The app is unused, regardless of quality | 3-tap entry, widget and notification all land in P5; the success criteria in [01](01-concept.md#success-criteria) are measured in-app so the answer is knowable |
| **Screenshot tests become noisy and get disabled** | The design silently rots | Goldens rendered from fixed `UiState` fixtures with pinned animation frames; diffs uploaded as CI artifacts so reviewing a change is one click |
| **Offline-only means one lost phone loses everything** | Data loss | Export/import in P5; the README states plainly that backup is manual, because that is the price of no network permission |

## Open questions

Not blocking P0, but needed before the phase in brackets:

1. **Font pairing** [P1] — JetBrains Mono + Inter, or IBM Plex Mono + Space Grotesk? Both are OFL; the
   exact font files must be licence-checked before being committed.
2. **Default category set** [P2] — is the proposed Swiss list (Groceries, Eating out, Transport,
   Household, Health, Leisure, Subscriptions, Other) the right starting taxonomy, and what are
   realistic default budgets to suggest?
3. **CHF → EP rate** [P3] — 1:1 is intuitive but makes four-digit EP numbers common. An alternative is
   1 CHF = 10 EP for a more granular feel, or 2 CHF = 1 EP for smaller numbers. Worth deciding while
   simulating in P3.
4. **Station art direction** [P1/P3] — vector cross-section drawn in Compose (fully themeable,
   animatable, scales with tokens) versus layered raster art (prettier, heavier, harder to re-colour).
   The first is assumed; the second is a possible P6 upgrade.

---

Previous: [06 — Test strategy](06-test-strategy.md) · Next: [08 — Implementation notes](08-implementation-notes.md)
