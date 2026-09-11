# 04 — Design System: Cassette Futurism

The look is **the future as imagined around 1979–1985**: amber and phosphor green on near-black,
bezelled panels, split-flap and nixie-style readouts, mechanical toggles, a faint CRT scanline drift.
Chosen over synthwave and atomic-age alternatives because it is the only retrofuturist idiom in which
**dense numeric data looks native** — this app is mostly numbers, and a terminal aesthetic makes that
a feature.

Two rules govern everything below:

1. **The theme is a token layer, not a palette of hex codes sprinkled through the UI.** Composables
   reference semantic tokens only.
2. **Every effect that makes it look retro must be switchable off** without the app becoming ugly or
   unusable. See §6.

## 1. Colour tokens

Dark-only by design (a CRT does not have a light mode). Values are the *reference* tokens; screens
use the *semantic* tokens beneath.

| Reference token | Hex | Role |
|---|---|---|
| `void` | `#07090A` | App background — near-black with a green cast, not pure black |
| `panel` | `#11161A` | Card and panel fill |
| `bezel` | `#1D262B` | Raised panel edge, dividers, inactive controls |
| `phosphorAmber` | `#FFB000` | Primary. Readouts, active controls, the station's lit state |
| `phosphorGreen` | `#33FF66` | Energy, surplus, positive deltas |
| `alert` | `#FF4E2B` | Overdrawn, deadlines inside 14 days, destructive actions |
| `muted` | `#8496A0` | Secondary text, axis labels, disabled states |
| `cream` | `#E8E2D4` | Primary text — warm off-white, never `#FFFFFF` |

**Measured contrast (WCAG 2.1, ratio against each surface):**

| | on `void` | on `panel` | on `bezel` |
|---|---|---|---|
| `cream` | 15.45 | 14.10 | 11.92 |
| `phosphorGreen` | 14.86 | 13.56 | 11.46 |
| `phosphorAmber` | 10.89 | 9.94 | 8.40 |
| `alert` | 6.06 | 5.53 | 4.67 |
| `muted` | 6.51 | 5.94 | 5.02 |

Every pairing clears AA (4.5:1) for normal text; all but `alert`/`muted` on `bezel` clear AAA.
`muted` is deliberately lighter than the classic terminal grey for exactly this reason — the
first candidate (`#6E7F86`) failed AA on `bezel` at 3.70 and was rejected. **Any new colour must be
contrast-checked before it enters the token list**, and the table above is asserted by a unit test in
`:core:designsystem` so a "small tweak" cannot silently break accessibility.

**Semantic tokens** (what screens actually use): `surface`, `surfaceRaised`, `surfaceEdge`,
`onSurface`, `onSurfaceMuted`, `accent`, `energy`, `warning`, `danger`, `gaugeTrack`, `gaugeFill`,
`hazardStripe`, `scanline`.

**Colour is never the only signal.** Overdrawn also gets hazard striping and a label; positive deltas
also get a `+` and an upward glyph; the station's lit modules are also brighter *and* animated. This
is a requirement, not a nicety — it is what makes the app usable for colour-blind users, and it is
checked in the accessibility pass of every feature's Definition of Done.

## 2. Typography

Two families, with a strict split:

- **Monospace — all numerals, readouts, amounts, dates, counters, table columns.** Tabular figures
  are mandatory; a readout that reflows as digits change destroys the instrument illusion.
- **Grotesque — body copy, labels, descriptions, buttons.** Long German strings in a monospace face
  become unreadable at small sizes, which is why this is not a single-font design.

Candidate pairings (final choice is an [open question](07-roadmap.md#open-questions) — both are
licensed for embedding, but the exact files must be checked before they are committed):

| | Mono | Grotesque | Character |
|---|---|---|---|
| A | **JetBrains Mono** (OFL) | **Inter** (OFL) | Clean, modern, very legible; the safe pick |
| B | **IBM Plex Mono** (OFL) | **Space Grotesk** (OFL) | More period-correct; Plex descends from IBM terminal faces |

**Scale** (all sizes respond to the system font-scale setting — nothing is fixed in `dp`):

| Style | Family | Size | Use |
|---|---|---|---|
| `readoutXL` | mono | 48sp | EP balance, Quick Add amount |
| `readoutL` | mono | 32sp | Screen-level figures |
| `readoutM` | mono | 20sp | Row amounts, countdowns |
| `label` | mono | 12sp, +0.08em tracking, uppercase | Panel captions, axis labels |
| `titleL` / `titleM` | grotesque | 22 / 16sp | Screen and section titles |
| `body` / `bodyS` | grotesque | 15 / 13sp | Copy |

Layouts must survive a 200% font scale. Panel captions truncate; **numbers never do** — a truncated
amount is a bug, and the screenshot matrix in [06](06-test-strategy.md) has a largest-font-scale
configuration precisely to catch it.

## 3. Surfaces and shapes

The base object is the **panel**: a `panel`-filled rectangle with a 2dp `bezel` border, a 1dp inner
highlight along the top edge and a soft inner shadow below it, giving the sense of a recessed screen
in a machined faceplate. Corner radius 4dp — hardware, not app cards. Optional rivet dots in the
corners for larger panels.

Other primitives:

- **Readout window** — a darker inset area inside a panel, where numbers live.
- **Hazard stripes** — 45° amber/void diagonals. Reserved for genuine urgency (overdrawn week,
  deadline inside 14 days, destructive confirmations). Scarcity is what makes them work.
- **Bus lines** — thin connector lines between panels on the Bridge and Drains screens, implying
  wiring. Purely decorative, and among the first things `reduceEffects` removes.
- **Toggle** — a physical switch with a throw, not a Material pill.
- **Numpad key** — a chunky keycap with a pressed state that actually sinks.

Elevation is expressed through bezel and inset shadow, never Material tonal elevation. Spacing grid:
4dp base, panels padded 16dp, screen gutters 16dp.

## 4. Motion

Motion carries the whole "this is a machine" illusion, and is therefore specified, not improvised:

| Moment | Motion | Duration |
|---|---|---|
| Cold start | CRT boot sweep: a scanline wipes down, the UI snaps in with a brief bloom | ~600ms, tap to skip |
| Any number change | **Digit roll** — each digit flips split-flap style, staggered 30ms left→right | 250–400ms |
| Toggle | Switch throws with a 2-step ease, plus a light haptic click | 120ms |
| Save an expense | Panel edge pulses green once; the week gauge animates to its new value | 300ms |
| Week settlement | Energy transfer: particles travel the bus lines into the reactor ring, the EP readout rolls up, station brightness steps | ~2.5s, skippable |
| Module power-up | The module lights, a ring expands, the station graphic re-renders one step brighter | ~1.8s, skippable |
| Ambient | Slow scanline drift (24s loop) and a barely-visible vignette flicker | continuous |

Constraints: ambient motion runs **only while the screen is in the foreground and the battery is not
in saver mode**; every celebratory animation is skippable with a tap; no animation blocks input; the
two long animations (settlement, power-up) never run twice for the same event.

## 5. Sound and haptics

Off by default. When enabled: soft relay clicks on numpad and toggle, a low hum swell on settlement.
Haptics use standard Android feedback constants so they follow system settings. No sound is ever
required to understand state.

## 6. Accessibility — a first-class constraint

A heavy aesthetic like this fails accessibility by default, so the mitigations are part of the design,
not a later audit:

- **`reduceEffects` setting** — kills scanlines, flicker, bloom, particle animation and bus-line
  decoration; digit rolls become instant; the boot sweep is skipped. The app remains fully usable and
  still looks deliberate, because the look rests on colour, type and panel construction, not on the
  effects.
- **Auto-enabled** when the system's animation scale is 0 or reduce-motion is on. Flicker and scanline
  effects are a genuine photosensitivity risk; this is not optional.
- Font scaling to 200% supported on every screen.
- Minimum touch target 48dp, everywhere, including numpad keys and chips.
- Content descriptions on the station graphic and every gauge, phrased as values ("Reactor Core,
  level 2 of 5, next level 190 energy points"), not as pictures.
- Talkback traversal order defined per screen; decorative elements marked as such.
- No information conveyed by colour alone (§1).

## 7. Implementation contract

- A custom `Orbit7Theme` exposing `Orbit7Colors`, `Orbit7Typography`, `Orbit7Shapes` and
  `Orbit7Motion` through `CompositionLocal`s. **Material 3 defaults are not used** — pulling in
  `MaterialTheme` colours would quietly reintroduce Material blues via component defaults.
- All components live in `:core:designsystem`: `Panel`, `ReadoutWindow`, `DigitRollText`, `Gauge`,
  `HazardBanner`, `SwitchToggle`, `NumpadKey`, `CategoryChip`, `StationView`, `ModuleTile`,
  `BusLine`, `ScanlineOverlay`.
- Every component ships with: a `@Preview`, a committed **Paparazzi golden**, and an entry in a
  debug-build **component gallery** screen used for manual review on a real device.
- `testTag` constants for every interactive element live beside the components, so feature tests and
  design tests refer to the same identifiers.
- Motion constants (durations, easing, stagger) live in `Orbit7Motion` — no hardcoded durations in
  feature code, so `reduceEffects` has exactly one place to intervene.

## 8. Localization and formatting

- de-CH is the default locale, English the fallback; switching is per-app (`localeConfig`, Android 13+)
  with an in-app override for older versions.
- **No hardcoded strings, from the first commit.** A lint rule fails the build on string literals in
  composables.
- Currency: `CHF 1'234.50` — Swiss apostrophe grouping and a period decimal separator. Formatting is
  centralised in one `MoneyFormatter`, takes `Long` minor units, and is unit-tested for both locales.
- Dates: `Mo, 14.04.` style short forms in de-CH. Week starts Monday by default, configurable.
- German strings run roughly 30% longer than English — all layouts are reviewed against the German
  strings, and the screenshot matrix renders de-CH.

---

Previous: [03 — Screens](03-screens.md) · Next: [05 — Architecture](05-architecture.md)
