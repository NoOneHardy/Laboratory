# 06 — Test Strategy

This document exists **before any production code**. That is the point: the architecture in
[05](05-architecture.md) was chosen to make these tests cheap, not the other way round. The four hard
rules there (injected `Clock`, integer money, append-only ledger, migration-per-schema-change) are
testability decisions first.

The economy is where a bug is invisible and expensive — a wrong proration or a non-idempotent
settlement silently pays the wrong amount for months. So the heaviest testing sits in a pure-JVM
module that runs in milliseconds, and the UI is covered by behaviour and screenshot tests rather than
by slow emulator tests.

## 1. The tiers

| Tier | Scope | Tooling | Where it runs | When |
|---|---|---|---|---|
| **T0** Static | Formatting, lint, complexity, custom rules | Spotless/ktlint, detekt (+ custom rules), Android Lint | JVM | every push |
| **T1** Domain | The entire game economy | JUnit 5, kotest assertions, **kotest property testing** | JVM, `:core:domain` | every push |
| **T2** Data | DAOs, **migrations**, repositories, workers | Robolectric, Room `MigrationTestHelper`, `TestListenableWorkerBuilder` | JVM | every push |
| **T3** ViewModel | Every `UiState` transition | Turbine, `MainDispatcherRule`, fakes | JVM | every push |
| **T4** UI behaviour | Compose semantics per screen | `createAndroidComposeRule` + Robolectric | JVM | every push |
| **T5** Screenshot | Design system + every screen | **Paparazzi**, goldens committed | JVM | every push |
| **T6** End-to-end | Full journeys, widget, notifications | Compose UI test on emulator, Hilt test app, Glance test harness | Emulator API 34 | PR + main |

T1–T5 run on the JVM with no emulator, which is what keeps the every-push job under a few minutes.
Only T6 pays the emulator cost, and only on PRs and `main`.

## 2. T0 — Static analysis

- **Spotless + ktlint** — formatting is not a review topic.
- **detekt** with the standard rule set, plus **four custom rules that enforce the hard rules from
  [05 §5](05-architecture.md#5-the-four-hard-rules)**:
  1. `NoDirectClockAccess` — forbids `LocalDate.now()`, `Instant.now()`, `System.currentTimeMillis()`,
     `Calendar.getInstance()` outside the DI module that provides the `Clock`.
  2. `NoFloatingPointMoney` — forbids `Double`/`Float` in `:core:domain` and `:core:data`.
  3. `NoHardcodedStringsInCompose` — every user-visible string comes from resources.
  4. `NoEmptyCatch`.
- **Android Lint** at `abortOnError = true`, with accessibility checks promoted to errors
  (`ContentDescription`, `TouchTargetSizeCheck`, `SetTextI18n`).

Writing rule 1 as a lint rule on day one is what makes the whole test strategy hold: a single
`LocalDate.now()` that slips into a settlement path makes the economy untestable and nobody notices
until a test is flaky at midnight.

## 3. T1 — Domain tests (the core of the suite)

Everything in `:core:domain`, tested as pure functions against an injected `FakeClock`.

**Example-based cases, at minimum:**

- **Proration** ([02 §3.2](02-game-design.md#32-prorating-a-monthly-budget-to-a-week)): a week inside
  one month; a week straddling a month boundary; February in a leap year; a month in which the budget
  was changed mid-week (versioned budgets); a week containing a DST changeover (Europe/Zurich, the
  March and October Sundays) — the week must still have exactly 7 days of budget.
- **Settlement**: the [worked example from 02 §3.6](02-game-design.md#36-worked-example), asserted
  digit for digit; an all-surplus week; an all-overdraw week; a week with no expenses and no marks;
  a week with no expenses but 7 zero-spend marks (these two must **not** produce the same result).
- **Confidence factor**: 0…7 signal days; a day with both an expense and a mark counts once; marks
  outside the week are ignored.
- **Streaks**: build-up, cap at ×1.5 after 10 weeks, reset on a bad week, and the assertion that a
  reset never removes banked EP.
- **Baseline**: fewer than 28 days → no baseline; median not mean (a single outlier week must not move
  it); suggestion suppressed when the budget is already under the baseline; suggestion suppressed for
  28 days after a dismissal.
- **Salvage**: monthly, quarterly and yearly cadences normalised correctly; renegotiation pays on the
  delta only; a re-entered contract pays nothing ([02 §6.2](02-game-design.md#62-salvage-payouts)).
- **Notice periods**: `earliestCancellationOn` across month-length boundaries (a 3-month notice from
  31 March), and the 30/14/3-day reminder offsets.
- **Module costs**: the published cost table in [02 §5.2](02-game-design.md#52-cost-curve) is asserted
  against the generated curve, so a change to `growth` cannot silently contradict the documentation.

**Property-based invariants** (kotest property testing, over generated expense/budget histories):

1. EP balance always equals the sum of the ledger.
2. Settlement is idempotent: running it twice for the same week produces one ledger entry and an
   identical balance.
3. The EP balance is never negative.
4. `confidence ∈ [0.4, 1.0]` and `streak ∈ [1.0, 1.5]` for every possible input.
5. **A week with zero logged activity never earns more than the same week fully logged.** This is the
   anti-exploit property for the pure-trust model and is the single most important test in the suite.
6. Settling a run of weeks one at a time equals settling them in a catch-up batch (the "user was away
   for five weeks" path).
7. Money arithmetic never loses a Rappen: the sum of per-category surpluses equals the total surplus.

**Coverage gate: ≥ 90% line coverage on `:core:domain`**, enforced by Kover in CI. The module is pure
logic with no framework noise, so this is an achievable number rather than a vanity one.

## 4. T2 — Data tests

- **DAO tests** on an in-memory Room database — one suite per table in
  [05 §4](05-architecture.md#4-data-model), covering CRUD and Flow emission on change, plus the
  invariant each table carries:
  - `categories` — archival instead of deletion; archived categories still resolve for old expenses.
  - `expenses` — date-range and category queries hit the indices and return correct boundaries
    (an expense on the first and last day of a week is included exactly once).
  - `budgets` — versioning: no overlapping validity ranges, exactly one open row per category, and
    the correct version resolved for an arbitrary past date.
  - `contracts` — status transitions are recorded once; a re-entered contract is a new row.
  - `zero_spend_marks` — one row per date, marking twice is idempotent, `source` preserved.
  - `energy_ledger` — the DAO exposes **no** update or delete path, and the balance query equals the
    sum of deltas.
  - `weekly_settlements` — the primary key on `weekStartDate` makes a second settlement of the same
    week fail rather than duplicate.
  - `station_modules` — level only ever increases; a purchase writes exactly one ledger entry.
- **Migration tests from version 1.** `room.schemaLocation` is set and schema JSONs are committed from
  the first commit. Every migration has a test that opens the previous schema, inserts representative
  rows, migrates, and asserts the data survived. CI fails if a schema JSON changes without an
  accompanying migration test — *no destructive migrations, ever*, since there is no cloud backup to
  restore from.
- **Repository tests** against fake DAOs, covering mapping and the derived-balance rebuild.
- **Worker tests** with `TestListenableWorkerBuilder`: settlement catch-up over five missed weeks;
  the daily reminder suppressed when the app was already used; deadline notifications firing once per
  threshold and not repeating.
- **Export/import** round-trip at the repository level (the full journey is T6).

**Coverage gate: ≥ 70% on `:core:data`.**

## 5. T3 — ViewModel tests

Every ViewModel, every `UiState` transition, using Turbine on the `StateFlow` and fakes from
`:core:testing`:

- initial → loading → loaded, and initial → loading → empty;
- each user event's effect on state (amount entered, category chosen, save, undo, filter applied);
- one-shot effects arrive exactly once (a navigation event must not replay on rotation);
- error paths render as state, never as a crash.

`MainDispatcherRule` swaps in a `TestDispatcher`; no ViewModel test touches a real database or clock.

## 6. T4 — Compose behaviour tests

Driven against the **stateless `…Screen` composables** ([05 §3](05-architecture.md#3-presentation-pattern)),
with Robolectric so they run on the JVM in CI:

- Assertions use the `testTag` constants from `:core:designsystem` — **never** user-visible text,
  which is localized and would make the suite break on a translation change.
- One test per screen minimum, covering: the empty state, the loaded state, and the primary action
  invoking the right callback.
- Named critical cases: Quick Add really does save in three interactions; the zero-spend mark toggles
  and can be undone; a swipe-delete in the Log offers Undo and restores the row; the Station's
  "Power up" is disabled when EP is insufficient and states why.
- Accessibility assertions in the same tests: every interactive node has a content description, and no
  touch target is under 48dp.

## 7. T5 — Screenshot tests (Paparazzi)

This is what protects the retrofuturistic design from silent regression — the thing most likely to
degrade without anyone noticing, because nobody re-reviews a screen that "still works".

**Matrix — every design-system component and every screen, in three configurations:**

| Config | Why |
|---|---|
| Default (Pixel 6, de-CH, 1.0 font scale) | The reference look |
| **Largest font scale (2.0)** | Catches truncated numbers and broken panels — the most common real regression |
| **`reduceEffects = on`** | Proves the app is still coherent with every effect disabled, which is the accessibility promise from [04 §6](04-design-system.md#6-accessibility--a-first-class-constraint) |

Screens are rendered from fixed `UiState` fixtures, so goldens are deterministic. Animations are
pinned to a fixed frame. Goldens are committed to the repo; `verifyPaparazziDebug` runs on every push
and CI uploads the diff images on failure.

Additionally, a **token contrast test** (plain JUnit, not a screenshot) asserts the WCAG ratio table
in [04 §1](04-design-system.md#1-colour-tokens), so a colour tweak that breaks AA fails the build.

## 8. T6 — End-to-end tests (emulator)

Few, slow, high-value. Run on API 34 in CI on PRs and `main` only, with a Hilt test application
injecting an in-memory database and a controllable `FakeClock`.

1. **The full loop**: onboarding → seed categories and budgets → log expenses across a week →
   advance the clock → settlement runs → summary is shown → unlock a station module → the station
   graphic changes.
2. **Contract Hunter**: add a contract with a notice period → deadline notification fires at the
   3-day threshold → cancel → salvage EP appears in the ledger → the monthly fixed-cost total drops.
3. **Export/import round trip**: export → wipe all data → import → every screen shows the original
   state.
4. **Widget**: Glance `runGlanceAppWidgetUnitTest` for rendering states, plus an instrumented check
   that the widget updates after an expense is added.
5. **Notification actions**: the "Nothing spent today" action writes a zero-spend mark without
   launching the app, and that mark raises the confidence factor at settlement.
6. **Process death**: state restoration on Quick Add with a half-entered amount.

## 9. Test infrastructure — `:core:testing`

Shared, and built in P0 before any feature exists:

- **Builders with sensible defaults**: `anExpense(amount = 12.50.chf, on = mondayOf(week))`,
  `aContract(...)`, `aBudget(...)`, `aWeek(...)`, `aStation(...)`.
- **`FakeClock`** — settable and advanceable, the single source of time in every test.
- **Fake repositories** implementing the domain interfaces, backed by in-memory lists with Flow
  emission, used by T3 and T4.
- **`MainDispatcherRule`**, Hilt test rules, and a Robolectric base configuration.
- **Fixture histories**: a canonical "three months of realistic Swiss spending" dataset used by
  baseline tests, report tests and screenshot fixtures, so the numbers on screenshots are believable
  and stable.

## 10. CI

`.github/workflows/android.yml` — the first CI in this repository.

**Job A — `check` (ubuntu-latest, every push and PR):**
```
setup JDK 21 + Gradle cache
./gradlew spotlessCheck detekt lint          # T0
./gradlew testDebugUnitTest                  # T1–T4
./gradlew verifyPaparazziDebug               # T5
./gradlew koverVerify                        # coverage gates
./gradlew assembleDebug
always: upload test reports + Paparazzi diff images as artifacts
```

**Job B — `e2e` (emulator, PR and `main` only):**
```
reactivecircus/android-emulator-runner, API 34, AVD cached
./gradlew connectedDebugAndroidTest          # T6
always: upload reports + failure screenshots
```

Both jobs must be green before merge. Job A is the fast gate; job B is allowed to take longer.

## 11. Definition of Done

A feature is not done until **all** of these hold:

- [ ] Domain rules unit-tested (T1), including at least one property where an invariant exists
- [ ] DAO/repository/worker changes tested (T2), with a migration + migration test if the schema moved
- [ ] Every new `UiState` transition covered (T3)
- [ ] At least one Compose behaviour test per new screen, empty state included (T4)
- [ ] Paparazzi goldens added or updated for all three configurations (T5)
- [ ] Strings present in **both** de-CH and English, no hardcoded literals
- [ ] Accessibility pass: content descriptions, 48dp targets, 200% font scale, `reduceEffects` path
- [ ] Detekt/lint clean, coverage gates met, CI green

## 12. What is tested by hand

Automation cannot judge these, so they are an explicit checklist run on a real device before each
milestone is called finished:

- Does the CRT boot sweep feel like a machine waking up, or like a slow app?
- Is the screen legible in a dark room at minimum brightness, and not blinding?
- Do the haptics on the numpad feel mechanical rather than buzzy?
- Battery cost of the ambient scanline over a 10-minute session.
- Is the settlement animation still enjoyable the tenth time, or does it need to be shorter?
- Does the German copy read naturally, or like a translation?

---

Previous: [05 — Architecture](05-architecture.md) · Next: [07 — Roadmap](07-roadmap.md)
