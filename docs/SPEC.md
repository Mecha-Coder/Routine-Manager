# Routine Manager: spec decisions

This file records the decisions from the Session 1 Q&A (25 Oct 2026) and every place where the app differs from, or adds to, the design handoff.

The handoff, [design/README.md](../design/README.md), is the base spec. Read it first. This file does not repeat it; it only lists changes, additions and answers to questions the README leaves open.

**Precedence:** this file > design/README.md > the prototype. If a session finds a conflict or gap that is not covered here, stop and ask the owner, then record the answer in this file.

## 1. Target device

Measured over adb on 25 Oct 2026. The app targets this one phone.

| Property | Value | Why it matters |
|---|---|---|
| Model | Samsung Galaxy A07, SM-A075F | Only target device |
| OS | Android 16, API 36 | |
| RAM | ~5.5 GB usable (`MemTotal 5748604 kB`) | |
| Screen | 720 × 1600 px at 300 dpi = **853 × 384 dp** in landscape | Smaller than the design canvas (D17) |
| Panel | PLS LCD (Samsung spec sheet, confirmed by the owner) | No burn-in (D1) |
| Refresh rate | 60 Hz ("Standard"); 90 Hz supported | Keep Standard |
| Display cutout | Notch 64 px = **34 dp** deep: top edge in portrait, left or right edge in landscape | Padding rule (D18) |
| Navigation | 3-button bar | System bars hidden (D3) |
| Auto-rotate | Off | Orientation rule (D2) |
| Font scale | 1.0 | Layout budget assumes 1.0 |
| Time zone | Asia/Kuala_Lumpur (no daylight saving) | Daylight-saving transitions are not tested |

## 2. Decisions

"README" column: **Changes** = overrides the README; **Adds** = something the README doesn't cover; **Confirms** = README option chosen or confirmed.

| ID | Topic | Decision | README |
|---|---|---|---|
| D1 | Screen type | LCD. No burn-in protection, no pixel-shift. Colours stay as designed, but the README's power reason for them (off-white / true black save OLED power) does not apply: on LCD only brightness affects power. | Changes (reason only) |
| D2 | Orientation | Follow the phone's sensor **even when auto-rotate is off** (`screenOrientation="sensor"`): portrait, landscape and reverse landscape. No upside-down portrait. | Adds |
| D3 | System bars | Status and navigation bars hidden on every screen, both orientations. Swiping in from an edge shows them briefly. | Adds |
| D4 | Block-change tone | Plays **only while the app is visible**. No exact alarms, no foreground service, no boot receiver. Rules in §3.1. | Changes (README asked for exact alarms or a foreground ticker) |
| D5 | Persistence | One JSON document stored with DataStore + kotlinx.serialization. Not Room. Rules in §3.5. | Confirms (one of the two README options) |
| D6 | Keep screen on | On whenever the app is visible, plugged in or not, except for the low-battery rule (D9). | Confirms + D9 |
| D7 | Bedtime display | During Bedtime the screen dims and switches to the dark palette. A tap brightens it for 30 s; that first tap does nothing else. Rules in §3.2. | Adds |
| D8 | First run | Fresh install and debug reset load the full prototype sample (`sample()`): routine and all four lists. | Confirms |
| D9 | Low battery | When the battery is **below 15 % and not charging**, stop keeping the screen on. Rules in §3.3. | Adds |
| D10 | Tone volume | Media audio stream (follows the phone's media volume; silent if it is 0). Other audio (podcast, music) is lowered while the tone plays. | Confirms + adds ducking |
| D11 | Notifications | None. No notification permission requested. | Confirms |
| D12 | Debug screen | Hidden debug screen with the prototype's TEST controls, **debug builds only**. Rules in §3.6. | Confirms README suggestion |
| D13 | Build variants | Debug builds install as a separate app, `com.jsk.routinemanager.debug`, named "Routine Manager (debug)". Test builds never touch the real app's data. The real app (`com.jsk.routinemanager`) is first installed in the last milestone. | Adds |
| D14 | Backup | None. `android:allowBackup="false"`. Data exists only on the phone. | Adds |
| D15 | Reboot and lock | No auto-start after reboot, no showing over the lock screen. The owner unlocks and reopens the app. | Adds |
| D16 | SDK levels | `compileSdk` and `targetSdk` **37**, `minSdk` 31. | Changes (README says target 36) |
| D17 | Design canvas | The prototype's screen is **880 × 396 dp**. The README's 900 × 416 includes the prototype's 10 dp phone frame. The phone is 27 dp narrower and 12 dp shorter than the design. | Corrects |
| D18 | Notch | On the side where the notch is, padding = max(design padding, 34 dp). | Adds |
| D19 | Clock and language | Always 12-hour (README format), whatever the phone's 24-hour setting. English only. | Confirms |
| D20 | Project "done" | A 24-hour run on the phone (§5). | Adds |
| D21 | Build order | Dashboard first, then lists, then settings and sound, then the timetable editor, then icon, release and user guide. See [BUILD_PLAN.md](BUILD_PLAN.md). | Plan |
| D22 | User guide | A root `README.md` walks through every feature with phone screenshots and links to the prototype on GitHub Pages so people can try it in a browser (M13). It lives on GitHub only; the app gets no help screen. | Adds |

## 3. Detailed rules

### 3.1 Block-change tone (D4, D10)

- **Trigger:** the resolved routine key changes to a **real block** while the app is visible. The key is the current block's id, the Free-time gap, or Bedtime (see README › Behaviour and system › Routine resolution). This is the prototype's `tick()` rule.
- Any cause counts: time passing, a clock or time-zone change, a timetable edit (creating a block that covers "now" plays the tone), or a debug fake-clock jump.
- Changes to Free time or Bedtime never play a tone.
- Wait 350 ms after the change, then play only if the key is still the same.
- **Becoming visible is not a change.** On launch, on return from another app, or when the screen comes back on, the current key becomes the baseline and nothing plays. Block starts missed while the app was hidden are never replayed.
- Plays the selected tone file once, start to end (10.4 s). A tone or preview already playing is stopped first.
- Gain = `(volume/100)² × 0.9` (README). Moving the volume slider changes a playing tone live.
- While the tone plays, other apps' audio is ducked (lowered), then restored.

### 3.2 Bedtime display (D7)

- **Bedtime** is the README's routine state: no current block and no Free-time gap, i.e. before the day's first block or after its last. A day with no blocks is Bedtime all day, so the screen is dim all day. The owner accepted this.
- While Bedtime:
  - The app overrides the window brightness to a fixed low value (constant `BEDTIME_BRIGHTNESS`; start at 0.05 and tune on the phone in M4). It is not a setting.
  - The **dark palette** is used, whatever the Theme setting says.
  - The **first touch while dim is swallowed**: it restores normal brightness and does nothing else (no to-do is ticked, no button is pressed).
  - For **30 s after the last touch** the screen stays bright and touches work normally. Then it dims again.
  - Brightening does not switch back to the light theme.
  - The Theme control in Settings still changes the saved theme; the change becomes visible when Bedtime ends.
- When Bedtime ends (a block or Free time becomes current at a minute tick), normal brightness and the chosen theme return immediately.
- Dimming is a window setting, so it applies to every screen: home, lists, settings, timetable, editor dialog.

### 3.3 Keep screen on (D6, D9)

- The screen is kept on when the app is visible **and** (charging **or** battery ≥ 15 %).
- Charging = plugged into AC, USB or a wireless charger.
- Checked when the app becomes visible and at every minute tick. No battery broadcast receiver.
- When the rule turns the flag off, the phone's normal screen timeout applies (2 minutes on this phone). Plugging in turns it back on at the next tick.

### 3.4 Layout on 853 × 384 dp (D3, D17, D18)

- With the bars hidden, the app has the full 853 × 384 dp.
- Landscape: left panel 60 % (512 dp; design 528), right panel 40 %.
- The left panel's height budget is tight: its stacked content needs roughly 380 dp at design sizes (estimate from the prototype's CSS). Rule: the body area (sub-routines plus CURRENT/NEXT, fixed 132 dp in the README) **takes the remaining height** instead of a fixed 132 dp. Sub-routine rows spread evenly inside it. All font sizes and other spacing keep their design values. Confirm with screenshots on the phone in M3.
- Notch (D18): the left panel's 42 dp left padding already clears 34 dp; the right panel's ~24 dp right padding does not, so it becomes 34 dp when the notch is on the right.
- Font scale 1.0 is assumed. Larger system font sizes may clip the left panel; not supported.

### 3.5 Persistence (D5, D14)

- One document holds everything the README lists under Persistence: the four lists, grocery collapsed state, blocks and settings. Shape = README › Data model, plus a `schemaVersion` field. The group id field is named `groupId` (the prototype's code calls it `gid`).
- A change shows on screen at once and reaches disk within 0.5 s. Pending changes are also written when the app goes to the background.
- Not saved: open list, edit mode, fake clock. Orientation is not saved (it follows the sensor).
- If the saved file cannot be read, a copy of it is kept next to it (`app_data.corrupt-<time>.json`) and the app starts from sample data.
- **No backup (D14).** Uninstalling the release app deletes all data. Losing the release signing key means an update needs an uninstall, which also deletes all data. M12 covers storing the key safely.

### 3.6 Debug screen (D12)

- Exists only in debug builds. The release APK contains none of its code.
- Opened by **long-pressing the Settings gear** on the Lists header.
- Controls, taken from the prototype's TEST bar: Live; jump to day Mon–Sun; minute-of-day slider; speed Pause / 1× / 60× / 600×; Reset data to sample. (The orientation toggle is not needed; the phone rotates.)
- The fake clock drives everything time-based: clock, routine, progress, LEFT, tone, Bedtime dimming, the timetable's today column and now line.

### 3.7 Other behaviour the README leaves open

- **Rotation** keeps all UI state: open list, edit mode, focused field, an open editor dialog with unsaved values.
- **Midnight:** the date and the day's routine change at the minute tick at 00:00 local time.
- **Time or time-zone change** on the phone: the display updates at once, not at the next minute.
- **Enter** in a list or sub-routine field is the keyboard's action key (fields are single-line).
- **Backspace on an empty field** may not be reported by Samsung Keyboard. If it can't be detected reliably, the ✕ button is the supported way to delete an item, and M6 records what was found.
- **Haptics** as in the README (12 ms reorder pick-up, 10 ms timetable create).

### 3.8 Notes on reading the prototype

- Use the README's colour values, not the hex codes in the prototype. The prototype draws light mode through a `brightness(.9)` filter and dark mode through an SVG filter; the README lists the final rendered colours.
- Self and Room use the same numbered outline UI as To-Do (the prototype's `viewPlain`/`editPlain` blocks are switched off).
- Not to build: the view-mode swipe-to-indent handlers (`swDown`/`swMove`/`swUp`), `sinkTodo_unused`, the `<select>`-based time options (`START_OPTS`/`END_OPTS`), the computed but unrendered Bedtime "at 6 AM" text (`nextAt`), and the device frame/scaling (`fit()`).

## 4. Out of scope for version 1

- Notifications, alarms, or any work while the app is not visible.
- Backup, export/import, sync, accounts, any network use.
- Home-screen widgets, lock-screen display, auto-start after reboot.
- Burn-in protection and pixel shift.
- Languages other than English; a 24-hour clock option.
- Tablets, foldables, and system font scales above 1.0.

## 5. Definition of done

- **Each milestone:** the quality gate in [CLAUDE.md](../CLAUDE.md) plus the milestone's acceptance criteria in [BUILD_PLAN.md](BUILD_PLAN.md).
- **The project** is done when all of these hold:
  1. Every README feature, as changed by this file, works on the phone in the signed release app.
  2. The release signing key is stored as described in M12.
  3. A **24-hour run** of the release app, plugged in, on the stand, shows: no crash or ANR; memory flat after warm-up; the tone at every block start; dim + dark at Bedtime and back to normal at the first block; the date changes at midnight.
  4. The user guide (`README.md`) with screenshots and the browser demo link is on GitHub (M13).
