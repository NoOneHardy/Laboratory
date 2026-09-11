# 02 — Game Design & Economy

This document is the specification of the game. Every number here is a tunable constant that lives in
one place in code (`GameBalance` in `:core:domain`), and every rule here has a corresponding test in
[06 — Test strategy](06-test-strategy.md).

## 1. Fiction

ORBIT-7 is a research station in a decaying orbit, powered down after its supply contract lapsed. The
player is the only engineer aboard. There is no resupply — the sole power source is money that was
*not* spent planetside.

Three forces act on the grid:

- **Surplus** — money under budget. Converts to power at week close.
- **Overdraw** — money over budget. Vents power, at half rate.
- **Drains** — recurring contracts, physically drawn as parasitic taps clamped onto the station's
  power bus, each bleeding a fixed amount per month. Cutting one is *salvage*.

The tone is dry, competent and slightly wry — a ship's computer that respects you. Never scolding.
See §8 for the anti-patterns this rules out.

## 2. Currency and ledger

The single currency is **EP (Energy Points)**.

- Default conversion: **1 CHF saved = 1 EP** (`GameBalance.epPerMajorUnit`, tunable).
- EP is an integer. There are no fractional points.

**Hard rule: EP is only ever changed by appending a row to the energy ledger.**

```kotlin
data class EnergyLedgerEntry(
    val id: Long,
    val delta: Int,             // signed; negative for spending EP or venting
    val reason: EnergyReason,   // SETTLEMENT | OVERDRAW | SALVAGE | MISSION | MODULE_PURCHASE | CORRECTION
    val referenceId: String?,   // week id, contract id, module key …
    val occurredAt: Instant,
)
```

Balance is `ledger.sumOf { it.delta }`. There is no mutable balance field that can drift out of sync
(a cached projection may exist for rendering, but it is derived and rebuildable). This one decision
buys: an auditable history the player can read, trivially replayable economy tests, and no class of
"my points disappeared" bug.

**Hard rule: money is stored as `Long` minor units (Rappen).** Never `Double`, at any layer. Rounding
happens once, explicitly, at conversion to EP, using half-up.

## 3. Weekly settlement — the heart of the loop

The settlement period is a week, **Monday 00:00 to Sunday 23:59:59.999 in the user's timezone**. It
runs shortly after week close via WorkManager, and is **idempotent per week**: a week already settled
can never be settled twice, no matter how often the worker fires.

### 3.1 Formula

```
for each category c:
    weeklyBudget_c = proratedWeeklyBudget(c, week)      // see §3.2
    spent_c        = Σ expenses in c within the week
    surplus_c      = max(0, weeklyBudget_c − spent_c)
    overdraw_c     = max(0, spent_c − weeklyBudget_c)

base       = Σ surplus_c  × epPerMajorUnit                       // gross power generated
vented     = Σ overdraw_c × epPerMajorUnit × overdrawRate        // overdrawRate = 0.5
confidence = 0.4 + 0.6 × (signalDays / 7)                        // ∈ [0.4, 1.0]   see §3.3
streak     = 1.0 + min(0.05 × consecutiveGoodWeeks, 0.50)        // ∈ [1.0, 1.5]   see §3.4

epAwarded  = roundHalfUp(base × confidence × streak) − roundHalfUp(vented)
```

`epAwarded` may be negative in a bad week. The ledger accepts it, but **the running balance is
floored at zero** — the player can end a week with nothing gained, never with a debt. Progress
already spent on modules is never reclaimed.

### 3.2 Prorating a monthly budget to a week

Budgets are set monthly (that is how people think about rent, groceries, going out). A week can span
two months, so the weekly figure is computed by exact day-proration, **not** by dividing by 4:

```
weeklyBudget_c = Σ over the 7 days d in the week:
                     monthlyBudget_c(d) / daysInMonth(d)
```

using the budget version valid on day `d` (§4.2). Rounding to minor units happens once on the sum.
This is fiddly enough to get wrong, and is therefore one of the first things the test suite pins
down — including a week straddling a month boundary, a leap-February, and a DST changeover.

### 3.3 The confidence factor (why abandoning the app doesn't pay)

The app cannot verify spending, and by design it does **not** demand a daily confirmation tap. That
creates an obvious exploit: a week in which nothing is logged looks identical to a perfect week.

The fix is economic rather than procedural:

```
signalDays = number of days in the week with at least one of:
               • a logged expense
               • an explicit zero-spend mark   (today card, or the notification's action)
confidence = 0.4 + 0.6 × (signalDays / 7)
```

- Full engagement (7 signal days) → ×1.0, the full payout.
- Total silence (0 signal days) → ×0.4. Something, but never the full amount.

Consequences, stated plainly:

- Not using the app is always worse than using it, so the exploit is not worth running.
- A quiet week is never worth *zero*, so a bad week at work doesn't wipe the player out.
- No blocking dialog, no mandatory ritual, no guilt. The nudge is a notification that can be ignored.

Marking a zero-spend day is a one-tap affordance on the Bridge's today card and an action on the
daily notification — offered, never required.

### 3.4 Streaks

A **good week** is one where total spend ≤ total budget across all categories.

- Each consecutive good week adds +5% to the multiplier, capped at +50% (10 weeks).
- A bad week resets the multiplier to 1.0 — it does **not** remove EP already earned.
- The current streak is shown on the Bridge; the reset is reported neutrally ("Grid resync. Streak
  bonus reset to ×1.00"), never as a failure message.

### 3.5 Settlement summary

The next time the app is opened after a settlement, a summary is presented: power generated, the
confidence and streak multipliers as they actually applied, best and worst category, streak state,
and — if applicable — the first module now affordable. This is the app's weekly "payoff moment" and
gets the heaviest motion design in the product ([04](04-design-system.md)).

### 3.6 Worked example

Budgets: Groceries CHF 400/mo, Eating out CHF 200/mo, Transport CHF 100/mo. A 30-day month, so the
weekly prorations are 400×7/30 = 93.33, 200×7/30 = 46.67, 100×7/30 = 23.33 → CHF 163.33 total.

The week's spend: Groceries 78.20, Eating out 61.00, Transport 12.40.

```
surplus   = (93.33−78.20) + 0 + (23.33−12.40) = 15.13 + 10.93 = 26.06  → base   = 26 EP
overdraw  = (61.00−46.67) = 14.33                                      → vented = 14.33 × 0.5 ≈ 7 EP
signalDays = 6                    → confidence = 0.4 + 0.6 × 6/7 = 0.914
consecutive good weeks = 3        → streak     = 1.15
epAwarded = round(26 × 0.914 × 1.15) − 7 = round(27.33) − 7 = 27 − 7 = 20 EP
```

Note the two separate judgements: one category was overdrawn and vented power, but the week as a whole
came in under budget (CHF 163.33 budgeted vs. CHF 151.60 spent), so it counts as a good week and the
streak advances to 4. The example is deliberately fiddly; it is reproduced verbatim as a test case.

## 4. Budgets and the baseline

### 4.1 Budgets

Set per category, per month, in CHF. Onboarding seeds a default Swiss category set with suggested
starting values (see [03 — Screens](03-screens.md), onboarding). Categories can be added, renamed,
recoloured and archived; they are never hard-deleted while expenses reference them.

### 4.2 Budgets are versioned

A budget row carries `validFrom` and nullable `validTo`. Changing a budget closes the current row and
opens a new one. Historical settlements therefore stay reproducible: a settlement from March still
evaluates against March's budget, even if the budget was raised in May.

This is the kind of detail that is very cheap now and very expensive to retrofit.

### 4.3 The rolling baseline

After **28 days** of data in a category, the app computes the **rolling median** of the last 12
complete weeks of weekly spend in that category (median, not mean — one holiday grocery run should
not move the baseline).

The baseline appears as a ghost marker on every budget and report chart, and drives a suggestion:

> *Your baseline for Groceries is CHF 180/week. Target CHF 162 (−10%)?* — [Accept] [Adjust] [Dismiss]

- The suggested cut (`GameBalance.suggestedCutRatio`, default 10%) is never applied automatically.
- A suggestion for a category is not repeated within 28 days of being dismissed.
- If the current budget is already below the baseline, no suggestion is made — the user is already
  winning there.

## 5. The station

### 5.1 Modules

EP is spent on station modules. Each has 3–5 levels; each level lights more of the station graphic
and, for some modules, unlocks a feature.

| Module | Levels | Effect |
|---|---|---|
| **Reactor Core** | 5 | Required first. Raises the EP storage cap; each level makes the whole station visibly brighter |
| **Life Support** | 4 | Cosmetic + the station stops reading "critical" |
| **Hydroponics** | 4 | Cosmetic; the greenhouse ring animates as it grows |
| **Comms Array** | 3 | Unlocks **missions** (§7) |
| **Observation Deck** | 3 | Unlocks the **Reports** screen and its charts |
| **Hangar** | 4 | Unlocks cosmetic ship variants docked outside |
| **Cryo Lab** | 3 | Endgame cosmetic; long-run EP sink |

### 5.2 Cost curve

Level cost follows `cost(n) = baseCost × growth^(n−1)`, with `growth = 1.8`, rounded to the nearest
10 EP. Base costs are tabulated so the whole curve can be rebalanced in one place:

| Module | Base cost | L1 | L2 | L3 | L4 | L5 |
|---|---|---|---|---|---|---|
| Reactor Core | 60 | 60 | 110 | 190 | 350 | 630 |
| Life Support | 80 | 80 | 140 | 260 | 470 | — |
| Hydroponics | 90 | 90 | 160 | 290 | 520 | — |
| Comms Array | 140 | 140 | 250 | 450 | — | — |
| Observation Deck | 160 | 160 | 290 | 520 | — | — |
| Hangar | 200 | 200 | 360 | 650 | 1170 | — |
| Cryo Lab | 400 | 400 | 720 | 1300 | — | — |

Design intent for pacing: a moderately disciplined user earning ~60–120 EP per week should light the
first module within the first week, have a meaningful choice to make by week three, and still have
something to save toward after six months.

### 5.3 Spending EP

Unlocking is instant and irreversible; there is no refund and no downgrade. Modules can be bought in
any order once the Reactor Core prerequisite is met, so the player always has a choice between a
cheap immediate win and a saved-up big one.

## 6. Contract Hunter — the regular-expenses module

This is where the real money is, and the module is designed to make an annoying admin task feel like
hunting.

### 6.1 What a contract holds

| Field | Notes |
|---|---|
| Name, category | e.g. "Fitness Wankdorf", Health |
| Amount + cadence | MONTHLY / QUARTERLY / YEARLY → normalised to a monthly equivalent |
| Next charge date | Drives the forecast |
| Notice period | In days or months (CH contracts are usually 1 or 3 months) |
| Earliest cancellation date | Derived: `nextRenewal − noticePeriod`; editable when a contract has odd terms |
| Status | ACTIVE / CANCELLED / RENEGOTIATED |
| Previous amount | Set on renegotiation, so the saving is provable |

### 6.2 Salvage payouts

```
cancel:      salvage = 6 × monthlyEquivalent × epPerMajorUnit
renegotiate: salvage = 6 × (previousMonthly − newMonthly) × epPerMajorUnit
```

Six months of the saving, paid once, immediately. It is deliberately large — larger than a good week
of daily discipline — because the underlying real-world win is larger, and because the action happens
once and must feel worth the phone call.

The compounding is real, not just points: the contract's monthly cost leaves the fixed-cost forecast
and the category baseline, so every future week genuinely has more headroom.

Guardrails: salvage is paid once per contract per state transition and is recorded in the ledger with
the contract id as reference. Status cannot be toggled back and forth to farm payouts — a cancelled
contract that comes back is entered as a **new contract**, which pays nothing. That rule is simpler
than any reversal logic and leaves nothing to exploit.

### 6.3 Deadline pressure

Each active contract shows a countdown to its **last possible cancellation date**. Notifications fire
at **30 / 14 / 3 days** before it. Inside 14 days the entry is rendered with hazard striping — the
one place the design system is allowed to shout.

### 6.4 The drains view

Contracts are listed as taps on the power bus, each labelled with its monthly bleed. The header shows
total monthly fixed cost and its EP equivalent: *"Your grid loses 214 EP/month to 7 drains."* Sorting
defaults to bleed rate, descending — the biggest target first.

## 7. Missions (v1.2, gated behind Comms Array)

Lightweight weekly challenges generated from the user's own data, three offered at a time, pick one:

- *"Keep Eating out under CHF 25 this week"* — from the category baseline, set ~25% below it.
- *"Three zero-spend days"* — from the previous week's count.
- *"No delivery charges for 7 days"* — from a merchant/note pattern, if one recurs.

Payout: 15–40 EP by difficulty. Missions are opt-in, never auto-assigned, and a failed mission costs
nothing. Generation rules are pure functions in `:core:domain` and unit-testable against a fixed
history fixture.

## 8. Balancing, and the ethics line

**Balancing.** All constants live in `GameBalance`. A debug-only *time travel* screen (debug build
type only) can advance the injected `Clock`, generate synthetic expense history, and run settlements
in a loop, so a full year of play can be simulated in seconds before shipping any change to the
numbers.

**Ethics.** This app is meant to be pleasant on a bad month. The following are ruled out for the life
of the project:

- ❌ No decay of earned EP, and no reclaiming of built modules. The past is safe.
- ❌ No guilt, shame or scolding copy. A bad week is reported as telemetry, not as a verdict.
- ❌ No streak-loss panic mechanics, no "you'll lose everything in 3 hours" notifications.
- ❌ No more than one scheduled notification per day (plus contract deadlines, which are real).
- ❌ No engagement-farming: no daily login bonus, no spin-the-wheel, no artificial scarcity.
- ❌ No mechanic that makes *not eating* or *not heating* the optimal play. Budgets are set by the
  user and the app never pushes a target below a floor the user set.

The app is a motivator for a goal the user already has. The moment it becomes a slot machine, it has
failed.

---

Previous: [01 — Concept](01-concept.md) · Next: [03 — Screens](03-screens.md)
