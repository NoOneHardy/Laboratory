# 08 — Implementation Notes

This document records what was built against the specification in 01–07, the places where the
implementation deliberately departs from those documents, and the state of verification.

## 1. What exists

| Module | Contents |
|---|---|
| `:core:domain` | The whole economy: proration, settlement, confidence, streaks, baseline, salvage, notice periods, module costs, missions — plus the repository ports and the use cases. Pure Kotlin/JVM, zero Android dependencies |
| `:core:data` | Room schema, DAOs, repositories, Proto-style typed settings, JSON backup, the three workers, Hilt wiring |
| `:core:designsystem` | Tokens, `Orbit7Theme`, the twelve components, test tags, `MoneyFormatter`, the WCAG contrast test |
| `:core:testing` | `MainDispatcherRule` and the canonical fixture history, re-exporting the domain's fakes |
| `:feature:expenses` | Quick Add, Log |
| `:feature:budget` | Budgets, baseline suggestions |
| `:feature:station` | Bridge, Station, settlement summary |
| `:feature:contracts` | Drains, contract editor |
| `:feature:reports` | Reports, success-criteria panel |
| `:feature:settings` | Settings, onboarding, export/import/wipe |
| `:widget` | The Glance widget, both sizes |
| `:app` | Application, MainActivity, navigation, notifications, boot receiver, debug activity |
| `:tooling:detekt-rules` | The four custom rules that enforce the hard rules |

CI is `.github/workflows/orbit7-android.yml` **at the repository root** — GitHub only runs workflows
from there — with both jobs from `docs/06` §10 and a path filter so it fires only on `Android/orbit7`.

## 2. Deliberate departures from 01–07

Each of these is a decision, not an oversight.

**Factors are integer basis points, not decimals.** `docs/02` writes the confidence and streak
factors as `0.914` and `1.15`. The implementation carries them as `9143` and `11500` basis points
and does every multiplication in `Long` arithmetic. This keeps hard rule 2 absolute — there is no
floating point anywhere in `:core:domain` or `:core:data`, so the `NoFloatingPointMoney` rule needs
no exceptions — and it makes the worked example reproduce exactly, which it does (see
`SettlementCalculatorTest`).

**Notice periods are stored as months *and* days.** `docs/05` §4 lists a single `noticePeriodDays`
column, but `docs/06` §3 requires "a 3-month notice from 31 March" to land on 31 December. Ninety
days does not. `contracts` therefore carries `noticePeriodMonths` and `noticePeriodDays`, and the
deadline is calendar arithmetic.

**Settings are typed JSON over DataStore, not Proto DataStore.** The guarantees `docs/05` asks for —
a schema, explicit defaults, a versioned document, no untyped string keys — are met by a
`@Serializable` DTO with a `version` field and a `Serializer` that fails loudly on corruption. This
avoids adding protoc to the build for a settings file with fifteen fields. The DTO is versioned, so
switching to Proto later is a migration, not a rewrite.

**`:core:testing` is a pure JVM module, and the domain's fakes live in the domain's own test
fixtures.** `docs/06` §9 puts the fakes in `:core:testing`, but `:core:domain`'s own tests need them
too, and `:core:domain` cannot depend on a module that depends on it. The fakes and builders are
therefore `testFixtures` of `:core:domain`, and `:core:testing` re-exports them (`api
testFixtures(project(":core:domain"))`) alongside `MainDispatcherRule` and the fixture history.
Every module still sees one set of fakes. The Hilt and Robolectric test rules `docs/06` mentions
live in `:app`'s `androidTest` source set, which is the only place they can be installed.

**The settlement anchor.** `docs/02` §3.3 is explicit that a silent week still pays ×0.4, so the
economy cannot start at the first logged expense — a user who sets budgets at onboarding and logs
nothing for two weeks has still stayed under budget. Settlement therefore starts at the earliest of
the first expense, the first zero-spend mark, and the day budgeting began, and the catch-up is
capped at 52 weeks so an import carrying old budgets cannot replay half a decade.

**The debug surfaces are a separate activity.** The component gallery and the time-travel screen
live in `app/src/debug`, behind their own launcher activity, rather than as hidden routes in the
navigation graph. Nothing about them can reach a release build.

## 3. Verification status

| Tier | State |
|---|---|
| **T1 Domain** | **121 tests, green.** Run as written, including the worked example asserted digit for digit and the eight property tests — among them the anti-exploit property, idempotence, catch-up equals piecemeal, and "the balance always equals the ledger sum" |
| T0 Static | `.editorconfig`, Spotless/ktlint and detekt are configured, and the whole tree is ktlint-clean at the pinned 1.5.0 ruleset. The four custom rules have their own tests |
| T2 Data | Written: DAO suites, the v1 migration test, worker tests, the backup round trip |
| T3 ViewModel | Written: Quick Add, Log, Station, Drains, Budgets, Onboarding |
| T4 UI behaviour | Written: Quick Add and Bridge, against the stateless screens, asserting on test tags only |
| T5 Screenshot | Written: the component gallery and the main screens, each in the three configurations. **Goldens are not committed** — they must be recorded on a machine with the Android SDK (`./gradlew recordPaparazziDebug`) |
| T6 End-to-end | Written: the full loop, Contract Hunter, the backup round trip, and the notification zero-spend path |

**The Android modules have not been compiled.** The environment this was written in has no Android
SDK and no route to `dl.google.com`, so AGP, Compose and Room could not be resolved. `:core:domain`
is pure JVM and was compiled and tested for real; everything above it is written but unproven. The
first thing to do on a machine with the SDK is therefore:

```bash
cd Android/orbit7
./gradlew :core:domain:test          # should stay green: 121 tests
./gradlew assembleDebug              # the first real compile of the Android modules
./gradlew recordPaparazziDebug       # record and commit the goldens
./gradlew check                      # the full gate
```

Expect the first compile to surface the ordinary mechanical errors of code that has never seen a
compiler — a missing import, an API that moved between library versions — and expect the version
catalog to need a pass against what actually resolves.

**The Room schema JSON is not committed yet.** `room.schemaLocation` is configured and
`exportSchema = true` is set, but the v1 JSON is generated by the Room compiler, and its identity
hash cannot be hand-written. It appears at `core/data/schemas/…/1.json` on the first build and must
be committed in that first commit that compiles — that is what hard rule 4 asks for, and
`MigrationTest` already fails if a migration is ever added without a matching test.

## 4. Open questions still open

The four from `docs/07` are unchanged, and two now have a default in code:

1. **Font pairing** — the theme uses the platform monospace and sans as stand-ins. Every style
   already carries its final size, tracking and case, so committing the licensed font files is a
   one-file change in `Orbit7Typography`.
2. **Default category set** — the Swiss list is implemented with the suggested monthly budgets in
   `Onboarding.kt` (Groceries 600, Eating out 250, Transport 120, Household 200, Health 100,
   Leisure 180, Subscriptions 80, Other 100). They are starting points the user edits on the same
   screen.
3. **CHF → EP rate** — 1:1, as `GameBalance.epPerMajorUnit`. Decide it while simulating in the
   time-travel screen.
4. **Station art** — vector, drawn in Compose from the tokens, as assumed.

---

Previous: [07 — Roadmap](07-roadmap.md) · Back to [README](../README.md)
