# 01 — Product Concept

## The problem

Two kinds of money leak, and both are invisible for different reasons.

**Daily spend** is invisible because it is small and frequent. CHF 6.50 for a coffee, CHF 18 for
lunch, CHF 12 for a delivery fee. Nobody notices a single one; together they are the largest
controllable line in most budgets.

**Regular spend** is invisible because it is automatic. A streaming subscription, a gym membership
used twice since February, a phone plan two generations out of date, an insurance policy that
auto-renews every 1 January if not cancelled by 30 September. These are the *biggest* wins available
— cancelling one CHF 45/month contract beats skipping a lot of coffees — and almost nobody hunts
them, because the work is boring and the deadline is never today.

Existing budgeting apps address neither well. They are built for *recording* money, not *reducing*
it, and recording is homework. The engagement curve is famous: enthusiastic setup, diligent week one,
a forgotten week two, uninstall.

## The solution

ORBIT-7 inverts the reward. You are not tracking expenses — you are generating power.

> **Every franc you don't spend becomes Energy Points (EP). EP powers ORBIT-7, a derelict orbital
> station you are bringing back to life, one module at a time.**

The station is the point. It is a picture, not a number: modules that are dark become lit, sections
that were sealed open up, the reactor ring fills. Progress is visible at a glance and permanent —
accumulated EP is never taken away.

Both leaks map onto the fiction:

| Real world | In ORBIT-7 |
|---|---|
| Staying under a weekly budget | Surplus converts to power at week close |
| Overspending | Power is vented (at half rate — losing hurts less than winning pays) |
| A recurring contract | A **parasitic drain** tapped into your grid, bleeding power every month |
| Cancelling or renegotiating one | **Salvage**: a large one-off EP payout, and the drain is gone for good |

## Target user

One person, their own money, in Switzerland. Phone-first. Wants to spend less, does not want to
become an accountant, will not reconcile a bank statement, will not maintain a spreadsheet. Has a
handful of subscriptions they suspect are wasteful but has never sat down to audit.

This is a single-user app by design. There are no accounts, no sharing, no household mode.

## Core loop

```
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│   log a spend            see the headroom          week closes   │
│   (≤ 3 taps)     ──────►  left this week   ──────►  surplus →EP  │
│        ▲                                                │        │
│        │                                                ▼        │
│   a new module                                   spend EP on     │
│   gives the next   ◄──────────────────────────   a station       │
│   target                                          module         │
└──────────────────────────────────────────────────────────────────┘
```

Daily: log spend, glance at the widget.
Weekly: settlement — power gained, streak, best and worst category.
Monthly: the Contract Hunter deadline check, and the baseline suggestion ("your median grocery spend
is CHF 180 — target CHF 162?").

The full economy is specified in [02 — Game design](02-game-design.md).

## What "fully functioning" means for v1

Included:

- Fast manual expense entry with categories (3 taps, numpad-first).
- Expense log with edit, delete and undo.
- Monthly budget per category, prorated to weeks.
- A rolling baseline computed from the user's own history after 28 days, with target suggestions.
- The EP economy: append-only ledger, weekly settlement, streaks.
- The station: modules to unlock and upgrade with EP.
- **Contract Hunter**: recurring costs with renewal dates, notice periods, cancellation countdowns,
  deadline reminders and salvage payouts.
- A daily reminder notification with a quick-add action and a "nothing spent today" action.
- A Glance home-screen widget: station power and remaining weekly budget.
- German (de-CH) and English, CHF formatted Swiss-style (`CHF 1'234.50`).
- JSON export and import for backup, and a wipe-all-data action.
- Accessibility: font scaling, a reduce-effects mode, WCAG AA contrast, 48dp targets.

## Non-goals

Explicitly out of scope, so the boundary is not argued about later:

| Not building | Why |
|---|---|
| A backend, accounts, sync | Offline-first is the whole privacy story and halves the project |
| Social features, leagues, friends | Needs a backend and other users to be fun; solo loop must work first |
| Bank API / Open Banking | Not meaningfully available for a private individual in CH |
| CSV / camt.053 import | Deferred. Reconsider after the manual loop proves itself |
| Receipt OCR | Impressive demo, poor effort-to-value for a daily habit app |
| Notification scraping of payment apps | Needs an invasive permission for a convenience gain |
| iOS, Wear OS, tablet-optimised layouts | Phone-only v1 |
| Ads, in-app purchases, analytics SDKs | Personal app. No third-party code in the money path |
| Income, net worth, investments, tax | This app reduces outflow. That is all it does |

## Privacy stance

ORBIT-7 v1 ships **without the `INTERNET` permission**. Not "we don't send your data" — the app is
technically incapable of it. All data lives in a local Room database; backup is a JSON file the user
exports themselves.

This is a real feature for a money app, and it keeps a personal project entirely clear of nDSG and
GDPR processing obligations. It is also a constraint to defend: adding any analytics, crash reporting
or cloud sync later breaks the claim and must be treated as a v2 product decision, not a dependency
bump.

## Success criteria

The app has worked if, after three months of use:

1. The user still opens it (the habit survived past week two).
2. At least one recurring contract has been cancelled or renegotiated through Contract Hunter.
3. The rolling baseline for at least one variable category is measurably lower than at the start.

Note that (2) and (3) are measurable *inside the app*, from the user's own data, without telemetry.
Reports surfaces both.

---

Next: [02 — Game design](02-game-design.md)
