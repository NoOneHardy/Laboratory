# 05 — Technical Architecture

## 1. Stack

| Concern | Choice | Why |
|---|---|---|
| Language | Kotlin 2.x | Android's default; coroutines/Flow fit the reactive data model |
| UI | Jetpack Compose (Material 3 *artifacts*, not its theming) | The custom design system needs full control over drawing |
| DI | Hilt | Standard, good testing story (`@TestInstallIn`, `HiltAndroidRule`) |
| Persistence | Room | Type-safe SQL, Flow queries, first-class migration testing |
| Preferences | DataStore (Proto) | Typed settings, no SharedPreferences edge cases |
| Background | WorkManager | Weekly settlement and reminders must survive reboot and Doze |
| Widget | Glance | Compose-style widgets, with a real unit-test harness |
| Navigation | Navigation Compose, type-safe routes | Serializable route objects, no string parsing |
| Time | `java.time` via core library desugaring | Injectable `Clock`, correct DST and month arithmetic |
| Build | Gradle KTS + version catalog | Matches `Java/cdn/service/gradle/libs.versions.toml` in this repo |
| Config | minSdk 26, compile/target SDK 36, R8 for release | 26 covers effectively the whole install base and enables desugaring |

**No `INTERNET` permission.** Declared nowhere in any manifest, including debug. Permissions used:
`POST_NOTIFICATIONS` (runtime, 33+), `RECEIVE_BOOT_COMPLETED` (to restore scheduled work). That is all.

## 2. Module structure

```
:app                     Application, MainActivity, navigation graph, Hilt setup
:core:domain             ← pure Kotlin/JVM. No Android dependency, at all.
:core:data               Room, DataStore, repositories, WorkManager workers
:core:designsystem       Theme, tokens, components, test tags, Paparazzi goldens
:core:testing            Fixtures, builders, FakeClock, fake repositories, test rules
:feature:expenses        Quick Add, Log
:feature:budget          Budgets, baseline suggestions
:feature:station         Bridge, Station, settlement summary
:feature:contracts       Drains, contract editor
:feature:reports         Reports
:feature:settings        Settings, export/import, onboarding
:widget                  Glance widget
```

The important line is the second one. **`:core:domain` is a pure JVM module with zero Android
dependencies**, and it contains the entire game economy: settlement, proration, confidence, streaks,
baseline, salvage, module costs, mission generation. Consequences:

- Those rules are tested by plain JUnit tests that run in milliseconds, with no emulator, no
  Robolectric, no Android framework mocking.
- It is *impossible* to accidentally reach for `System.currentTimeMillis()`, a `Context`, or a
  `SharedPreferences` inside the economy, because they do not compile there.

Dependency direction is strictly `:app` → `:feature:*` → `:core:*`, and `:core:data` → `:core:domain`.
No feature module depends on another feature module; shared UI goes to `:core:designsystem`.

## 3. Presentation pattern

Single `Activity`, Compose-only, MVVM with unidirectional data flow:

```
Repository (Flow)  →  ViewModel  →  StateFlow<UiState>  →  @Composable screen
                          ↑                                       │
                          └────────────── UiEvent ────────────────┘
```

- `UiState` is a single immutable data class per screen, including loading/empty/error as state, not
  as separate branches scattered through the composable.
- Screens are split into a stateful `…Route` (collects the ViewModel) and a stateless `…Screen`
  (takes `UiState` + lambdas). The stateless one is what screenshot tests and Compose behaviour tests
  drive — that split is what makes UI testing cheap, so it is mandatory.
- One-shot effects (navigation, toasts, undo snackbars) go through a `Channel`, never through state.

## 4. Data model

All amounts are `Long` minor units (Rappen). All dates are `LocalDate`; all timestamps are `Instant`.

### `categories`
| Column | Type | Notes |
|---|---|---|
| `id` | Long PK | |
| `key` | String? | Non-null for seeded system categories, used for localized names |
| `customName` | String? | Non-null for user-created categories |
| `iconKey` | String | |
| `colorToken` | String | References a design-system token, never a raw hex |
| `sortOrder` | Int | |
| `archivedAt` | Instant? | Categories are archived, never hard-deleted |

### `expenses`
| Column | Type | Notes |
|---|---|---|
| `id` | Long PK | |
| `amountMinor` | Long | > 0 |
| `currency` | String | ISO-4217, `CHF` default |
| `categoryId` | Long FK | indexed |
| `occurredOn` | LocalDate | **indexed** — every settlement and report query filters on it |
| `note` | String? | |
| `createdAt` / `updatedAt` | Instant | |
| `source` | Enum | `MANUAL` today; the column exists so an importer never needs a migration |

Indices: `(occurredOn)`, `(categoryId, occurredOn)`.

### `budgets` — versioned
| Column | Type | Notes |
|---|---|---|
| `id` | Long PK | |
| `categoryId` | Long FK | |
| `amountMinorPerMonth` | Long | |
| `validFrom` | LocalDate | |
| `validTo` | LocalDate? | null = currently active |

Invariant: for a given `categoryId`, validity ranges never overlap and exactly one row has
`validTo = null`. Asserted by a DAO test.

### `contracts`
`id`, `name`, `categoryId`, `amountMinor`, `cadence` (MONTHLY/QUARTERLY/YEARLY), `nextChargeOn`,
`noticePeriodDays`, `earliestCancellationOn`, `status` (ACTIVE/CANCELLED/RENEGOTIATED),
`previousAmountMinor?`, `statusChangedOn?`, `createdAt`.

### `zero_spend_marks`
`date` (PK), `markedAt`, `source` (APP / NOTIFICATION). One row per marked day.

### `energy_ledger`
`id`, `delta` (Int, signed), `reason` (SETTLEMENT/OVERDRAW/SALVAGE/MISSION/MODULE_PURCHASE/CORRECTION),
`referenceId?`, `occurredAt`. **Append-only** — the DAO exposes no update or delete.

### `weekly_settlements`
`weekStartDate` (PK — this is what makes settlement idempotent), `baseEp`, `ventedEp`,
`confidenceFactor`, `streakFactor`, `awardedEp`, `signalDays`, `settledAt`. Immutable once written.

### `station_modules`
`moduleKey` (PK), `level`, `unlockedAt`, `lastUpgradedAt`.

### Derived, not stored
EP balance (sum of the ledger), weekly budgets (prorated on demand), baselines (computed from
expenses). A cached balance projection may exist for rendering but must be rebuildable from the
ledger, and a test asserts that the rebuild matches.

## 5. The four hard rules

These are the rules that make the app testable and the economy trustworthy. Each is enforced by
tooling, not by good intentions:

1. **`Clock` is injected everywhere.** No `LocalDate.now()`, `Instant.now()`, `System.currentTimeMillis()`
   or `Calendar.getInstance()` in production code — a custom detekt rule fails the build on them.
   Without this, nothing about weeks, streaks, proration or deadlines can be tested deterministically.
2. **Money is `Long` minor units end to end.** Formatting happens only in `MoneyFormatter` at the UI
   edge. A detekt rule forbids `Double`/`Float` in `:core:domain` and `:core:data`.
3. **EP changes only by appending to the ledger.** The ledger DAO has no update/delete; a repository
   test asserts the balance always equals the ledger sum.
4. **Every schema change ships a migration and a migration test.** `room.schemaLocation` is set and
   the exported schema JSONs are committed **from the first commit**, so there is never a "we'll start
   versioning later" moment. CI fails if a schema JSON changes without a new migration test.

## 6. Background work

| Worker | Trigger | Behaviour |
|---|---|---|
| `WeeklySettlementWorker` | Periodic, Monday early morning + a catch-up check on app start | Settles every unsettled complete week since the last settlement, oldest first. Idempotent per week (PK on `weekStartDate`). Must handle the user not opening the app for a month |
| `DailyReminderWorker` | Daily at the user's chosen time | Skipped if the app was already used that day. Posts the notification with both actions |
| `ContractDeadlineWorker` | Daily | Emits notifications at 30/14/3 days before each active contract's earliest cancellation date, once each |

All three re-register on `BOOT_COMPLETED` and on app start. The settlement catch-up path — "the user
was away for five weeks" — is a deliberate test case, not an afterthought.

## 7. Export / import

A single JSON document containing every table plus a schema version and export timestamp. Import is
**replace-all**, behind an explicit confirmation, and validates the schema version before touching the
database. Round-tripping (export → wipe → import → identical state) is an end-to-end test.

## 8. Error handling

The app is offline and single-user, so the realistic failure modes are narrow: a corrupt import file,
a database migration failure, an expense saved while a settlement is running. Each has a defined
behaviour (reject with a readable reason; fall back to a destructive-migration-free failure with the
user's data intact and an export offered; settlement takes the week's snapshot at start). No silent
catch blocks — a custom lint rule flags empty `catch`.

---

Previous: [04 — Design system](04-design-system.md) · Next: [06 — Test strategy](06-test-strategy.md)
