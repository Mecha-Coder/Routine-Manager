# Routine Manager: build plan

Thirteen milestones, each sized for one Claude Code session. Order follows SPEC D21: after M2, M3 with M11 beside it; then two parallel tracks (M5 → M6 → M7, and M4, M8 → M9 → M10); then M12 and M13.

Checks marked **(owner)** need Android Studio's GUI (Layout Inspector, Profiler). Claude asks the owner to run them and reports the answer.

## How to run a milestone

1. Normally the owner types `/supervise` and the supervisor runs the milestones one by one (see CLAUDE.md › Supervised builds). To run one by hand instead, start a fresh session from the repo root: "Read CLAUDE.md, then do milestone M<n>."
2. Read [design/README.md](../design/README.md), [SPEC.md](SPEC.md), [ARCHITECTURE.md](ARCHITECTURE.md) and this milestone.
3. Build only this milestone's scope. If it won't fit in one session, stop at a working point, split the rest into M<n>b here, and say so.
4. Pass the **quality gate** in [CLAUDE.md](../CLAUDE.md) and every acceptance criterion below. The last step is always "Install on the phone and verify", except in M13, which changes no app code.
5. Update the status table, add notes under the milestone if something was learned or changed, and ask the owner before committing (`M<n>: <summary>`).

## Status

| Milestone | Status |
|---|---|
| M1 Foundation: window, theme, font, icons | Done |
| M2 Data, routine logic, minute clock | Done |
| M3 Dashboard and debug screen | Not started |
| M4 Night and power | Not started |
| M5 List menu, list view mode, portrait | Not started |
| M6 List edit mode | Not started |
| M7 Drag: indent and reorder | Not started |
| M8 Settings and sound | Not started |
| M9 Weekly timetable | Not started |
| M10 Block editor dialog | Not started |
| M11 App icon | Not started |
| M12 Release APK and install guide | Not started |
| M13 User guide (README) and browser demo | Not started |

## Test times used below

From the sample data. Weekdays: Morning 06:00–07:00, Commute 07:00–08:00, Work Session1 08:00–12:00, Lunch 12:00–13:00, Work Session2 13:00–17:30, Commute 17:30–18:30, Dinner & Chores 18:30–20:00, Personal 20:00–22:30, Wind down 22:30–23:00. Weekend: Morning 08:00–09:00, Work Session1 09:00–12:00, Errands 14:00–17:00, Personal 19:00–21:30, Wind down 22:30–23:00.

| Fake time | Expected |
|---|---|
| Mon 05:30 | Bedtime |
| Mon 07:05 | Commute, LEFT 55min, NEXT Work Session1 |
| Mon 09:30 | Work Session1, 4 sub-routines, LEFT 2hr 30min |
| Mon 12:59 | Lunch, LEFT 1min |
| Mon 22:45 | Wind down, LEFT 15min, NEXT `-` |
| Mon 23:30 | Bedtime |
| Sat 12:30 | Free time 12:00–14:00, LEFT 1hr 30min, NEXT Errands, no sub-routines |

---

## M1 Foundation: window, theme, font, icons

**Goal:** turn the wizard's empty project into the right skeleton, visible on the phone.

**Build**
- Update the outdated libraries lint flagged (core-ktx, lifecycle, activity-compose) and add the dependencies listed in ARCHITECTURE §1. Don't change AGP, Gradle or Kotlin unless the build requires it.
- Debug variant: `.debug` app id suffix, name "Routine Manager (debug)" (SPEC D13).
- Manifest: `allowBackup="false"` and remove the backup XML references (D14); `screenOrientation="sensor"` and `configChanges` (ARCHITECTURE §8).
- `MainActivity`: edge-to-edge, hidden system bars with swipe-to-reveal, cutout mode, keep-screen-on (always on for now; the battery rule comes in M4).
- `ui/theme`: all README colour tokens for light and dark, the README type scale, shapes. Google Sans Flex bundled and subset with its OFL licence (ARCHITECTURE §9). All README icons as vector drawables.
- A temporary token sheet screen showing both palettes, the type scale and all icons. M3 replaces it.

**Acceptance criteria**
- [x] Quality gate passes; the outdated-library lint warnings are gone.
- [x] Every README colour token exists in both palettes with the README's values.
- [x] Numbers render with tabular figures (digits line up in a column).
- [x] Font file size recorded in the notes below.

**Install on the phone and verify**
- [x] Installs as "Routine Manager (debug)". With the owner's OK, uninstall the wizard's starter app `com.jsk.routinemanager` (it holds no data).
- [x] With auto-rotate **off**, turning the phone rotates the app: portrait, landscape, reverse landscape.
- [x] Status and navigation bars are hidden; an edge swipe shows them briefly.
- [x] Screen stays on for more than 3 minutes (the phone's timeout is 2).
- [x] The notch covers nothing in either landscape direction.
- [x] Text is visibly Google Sans Flex (compare with the prototype open in Chrome).

**Notes**
- **Font file:** `res/font/google_sans_flex.ttf` = **107,480 bytes (105 KB)**. Source: google/fonts `ofl/googlesansflex/GoogleSansFlex[ROND,opsz,slnt,wdth,wght].ttf` (3.9 MB). fonttools 4.60.2 in a scratch venv:
  - `fonttools varLib.instancer <src> opsz=drop wdth=drop ROND=drop slnt=drop wght=400:700` pins opsz 18, wdth 100, ROND 0, slnt 0 (the font's defaults). Google Fonts serves the prototype a file with only a `wght` axis and these same pinned values, so the app matches the prototype.
  - `pyftsubset --unicodes=U+0020-007E,U+00A0-00FF,U+2013,U+2018,U+2019,U+201C,U+201D,U+2026 --layout-features+=tnum --name-IDs=0,1,2,3,4,5,6,13,14`. **pyftsubset drops `tnum` by default**, so it must be added. Besides ARCHITECTURE's list (– “ ” ×), the subset keeps ‘ ’ (typed apostrophes) and … (Compose's ellipsis character). Other characters fall back to the system font.
  - Licence: `assets/google_sans_flex_OFL.txt` (`res/font` can't hold a .txt), and in the font's name IDs 13/14. The OFL declares no Reserved Font Name, so the subset keeps its name.
- **Tabular figures:** the font's default digits are proportional (`1` = 735 units, `0` = 1290); with `tnum` all are 1290. Every `RmType` style and every Material style sets `tnum`. The token sheet shows `tnum` and `pnum` columns side by side on the phone.
- **Icons:** 24 vector drawables built from the SVGs in google/material-design-icons (`symbols/web/<name>/materialsymbolsrounded/<name>[_fill1|_wght600]_24px.svg`). The path data is copied unchanged into a 960 × 960 viewport inside a `translateY=960` group, because the SVG viewBox is `0 -960 960 960`. The repo's ready-made Android XMLs use long absolute paths (settings: 1796 characters) that trigger lint `VectorPath` (> 800); the SVG paths don't. `autoMirrored` is kept where Google sets it.
- **Kotlin 2.4.21 (SPEC D23):** AGP 9 has a runtime dependency on KGP 2.2.10. Per the AGP 9.0 release notes, a newer one goes on the root `buildscript` classpath: `classpath(libs.kotlin.gradle.plugin)`, using the catalog's `kotlin` version. Proof:
  - `./gradlew buildEnvironment` shows `kotlin-gradle-plugin:2.2.10 -> 2.4.21`, `compose-compiler-gradle-plugin:2.4.21` and `kotlin-serialization:2.4.21`.
  - The build uses `kotlin-compiler-embeddable-2.4.21`, and compiled classes carry Kotlin metadata `mv=[2,4,0]`.
- **Library versions:** core-ktx 1.19.1, lifecycle 2.11.0 (runtime-ktx, runtime-compose, viewmodel-compose), activity-compose 1.13.0, Compose BOM **2026.09.00** (UI 1.12.1, Material3 1.4.0), datastore 1.3.0, kotlinx-serialization-json **1.12.0** (built with Kotlin 2.4.10), kotlinx-coroutines-test 1.11.0, test ext junit 1.3.0, espresso 3.7.0. kotlin-stdlib resolves to 2.4.21.
- **For M2:** the app's runtime gets kotlinx-coroutines-core 1.9.0 (through AndroidX), while unit tests run on 1.11.0 (pulled in by coroutines-test).
- **Lint:** 17 warnings → 2, 0 errors. Both remaining warnings are accepted:
  - Gradle 9.8.2 (`AndroidGradlePluginVersion`): Gradle stays at 9.6.0.
  - `DataExtractionRules` on `allowBackup`: SPEC D14 allows phone-to-phone transfer, so no rules file is added.

  `DiscouragedApi` on `screenOrientation="sensor"` is suppressed in the manifest, with the reason: Android 16 ignores it only when the smallest width is 600 dp or more, and this phone is 384 dp.
- **Removed wizard leftovers:** example unit and instrumented tests (the instrumented one asserted the release package name), `colors.xml`, the two backup XMLs, the activity's redundant label.
- **Phone facts:** on the stand the display is `ROTATION_270`, even with auto-rotate off (`accelerometer_rotation=0`, `user_rotation=0`), because the activity requests `SCREEN_ORIENTATION_SENSOR`. The notch is on the **right** edge (x 1536–1600 px = 34 dp) and the navigation bar's slot is on the left. After an edge swipe both bars report `visible=true`; 5 s later both report `visible=false`. They stay hidden after Home and back.
- **Screen-on checks need a simulated unplug:** the phone has the developer option "Stay awake" on, so while plugged in `dumpsys power` shows `mStayOn=true` and the screen never times out, whatever the app does. With `dumpsys battery unplug` (`mStayOn=false`, timeout 120 s), the app stayed `Awake` through 201 s of no touch. M4's battery-rule test must also unplug, and must always end with `dumpsys battery reset`.
- **Gate (25 Oct 2026):** clean `assembleDebug lintDebug testDebugUnitTest` passes (1 test, 0 failures; lint 0 errors, the 2 accepted warnings). No crashes or ANRs. Memory, 1 hour sampled every 5 minutes (16:49–17:49): 101,182 KB at minute 5, 100,413 KB at minute 60 (−0.8 MB), flat. Owner on the phone: rotation follows the phone in all three positions with auto-rotate off; nothing under the notch in any position; font accepted as Google Sans Flex after a same-size side-by-side with the prototype (the phone draws it slightly thinner than Chrome on a Mac).
- **Profiler check skipped (owner decision):** Live Telemetry attached on 25 Oct, but Android Studio's UI froze and no graph could be read. Its log shows the profiler going through the duplicate adb entry ("… (2)") and re-selecting the device about once a second; that this caused the freeze is a guess. The owner accepted the 1-hour meminfo run as the memory evidence for M1. Before a Profiler check, drop any duplicate adb entry first. While the profiler is attached, its agent stays loaded in the app (PSS rose from about 100 MB to 119 MB), so never take meminfo samples during or after a profiler session without restarting the app.
- **adb duplicate keeps coming back:** the phone reappeared as "… (2)" twice during this gate and broke a plain `adb shell` loop. Long-running adb loops should use `adb -s "<serial without (2)>"`.

---

## M2 Data, routine logic, minute clock

**Goal:** saved data and the routine maths work and are tested; a placeholder shows them updating each minute.

**Build**
- `data/`: model, JSON serializer, `DataRepository` (instant state, 500 ms debounced save, flush on stop, corrupt-file copy), `SampleData` ported from `sample()`.
- `domain/`: `RoutineResolver`, `TimeFormat`, `ToneGain`.
- `time/`: `AppClock`, `MinuteTicker`, time/time-zone/date change receiver registered while visible.
- `MainViewModel` producing `DashboardUi`; the placeholder screen shows clock text and routine state as plain text.

**Acceptance criteria**
- [x] Unit tests for routine resolution: start inclusive, end exclusive; a block ending at 24:00; an empty day (Bedtime); back-to-back blocks; Free time between blocks; NEXT is `-` after the last block; weekday index Mon = 0.
- [x] Unit tests for formats: LEFT `4hr 30min`, `2hr`, `33min` with ceiling rounding; clock `12:05` `AM` at 00:05 and `PM` at 12:00; leading zero (`07:05`).
- [x] Unit tests: JSON round trip keeps everything; sample data has 9 blocks × 5 weekdays and 5 × 2 weekend days, and copies of one weekday row share a `groupId`.
- [x] Unit test: the ticker emits at the minute boundary, using a fake clock.

**Install on the phone and verify**
- [x] The placeholder clock changes exactly on the minute (compare with the phone's status-bar clock).
- [x] Force-stop and relaunch: data is still there; `adb shell run-as com.jsk.routinemanager.debug cat files/datastore/app_data.json` prints the JSON.
- [x] Changing the phone's time zone updates the display at once.

**Notes**
- **Unit tests:** 48 (47 new): `RoutineResolverTest` 11, `TimeFormatTest` 6, `ToneGainTest` 1, `AppDataJsonTest` 5, `SampleDataTest` 5, `DataRepositoryTest` 6, `MinuteTickerTest` 6, `DashboardUiTest` 7 (every row of the test-times table above, against `SampleData`). Lint: 0 errors, the 2 accepted M1 warnings.
- **`DataRepository.data` is `StateFlow<AppData?>`**, null until the file has been read (a few ms after launch), instead of ARCHITECTURE's `StateFlow<AppData>`. Reading doesn't block the main thread, and no consumer ever sees a stand-in empty `AppData`: an empty day resolves to Bedtime, which would dim the screen (M4) or become the tone's baseline (M8) for the first frame. Consumers use `filterNotNull()`.
- **First run:** DataStore only returns its default value when the file is missing; it doesn't write it. The repository checks whether `app_data.json` exists and, if not, writes `SampleData.create()` at once. Otherwise each launch before the first edit would make a new sample with new ids.
- **Debounced save** follows ARCHITECTURE §4: the 500 ms timer restarts on every change, so a burst (typing) is written once, 500 ms after the last change; `flush()` in `onStop` writes anything pending.
- **Unreadable file:** `keepCorruptCopyAndStartOver` copies it to `app_data.corrupt-<epochMillis>.json`, logs, and returns sample data, which DataStore writes over the bad file. A JVM test runs a real DataStore on a temp file.
- **Ticker** (`time/MinuteTicker.kt`): emits now, then at each minute boundary of `AppClock`. A time change restarts the wait (`flatMapLatest`), so the next tick follows the new time. A second value in the same minute is dropped, so a time sync can't cause an extra redraw. `AppClock.speed` is in place for M3's fake clock: the wait is divided by it, and 0 (paused) waits for a change.
- **Time-change receiver** (`Context.timeChanges()`): a `callbackFlow`, so it is registered only while the dashboard state is collected. `dumpsys activity broadcasts` shows it with `TIMEZONE_CHANGED`, `TIME_SET` and `DATE_CHANGED` while the app is visible, and no receiver after Home.
- **`TimeFormat`** has only the formats M2 uses (clock, LEFT, day, date). The timetable range (`6:00am-11:30am`) and the overlap label (`7 AM`) come with M9 and M10, which test them.
- **`TokenSheet.kt` is kept but no longer shown.** Deleting it made lint report the 24 icons as `UnusedResources` (it was their only reference). M3 replaces it; until every icon is used by a real screen, deleting it brings those warnings back. Decision (supervisor, M2): keep it unreferenced, and delete it in the milestone where the last icon gets a real use.
- **Phone, 25 Oct (debug build, real clock):**
  - First run wrote `files/datastore/app_data.json` (11,887 bytes): 55 blocks, 9/9/9/9/9/5/5 per day, sample lists and settings.
  - Force-stop and relaunch: same md5 and modification time, same routine key on screen, so the file was read, not replaced.
  - On the minute: screenshots taken on the phone itself. Captured between 18:58:59.70 and 18:59:00.08 it shows `06:58`; between 18:59:00.20 and 00.61 it shows `06:59` (LEFT 2min → 1min). At 19:00:00.15 Free time had changed to Personal (LEFT 2hr 30min, NEXT Wind down).
  - Back from Home it showed the current minute at once (19:09:31: LEFT 2hr 21min, progress 6 %) and did not write the file.
- **For M3's idle redraw check:** over 3 idle minutes `dumpsys gfxinfo … framestats` lists exactly 3 frames, 60.0 s apart, and "Slow UI thread" / "Frame deadline missed" are 3. "Total frames rendered", however, says **6**: this phone counts each drawn frame twice. Count framestats rows, or expect about 6.
- **Coroutines:** the app runs kotlinx-coroutines 1.9.0 (M1 note); only APIs present in 1.9 are used. `flatMapLatest` needs `@OptIn(ExperimentalCoroutinesApi::class)`.
- `./gradlew assembleRelease` builds with R8 and kotlinx.serialization (unsigned APK, 2.4 MB).
- **Gate (25 Oct 2026):** clean `assembleDebug lintDebug testDebugUnitTest` passes (48 tests, 0 failures, 0 skipped; lint 0 errors, the 2 accepted M1 warnings). No crashes or ANRs. Saved data survived a reinstall. Idle redraw: 3 framestats frames in 3 minutes (one per minute). Memory, 30 minutes (19:17–19:47): 83,900 KB at minute 5, 82,993 KB at minute 30, flat. Owner: in split screen, picking Tokyo moved the clock 1 hour at once, and turning automatic time zone back on returned it to Kuala Lumpur at once.
- **Owner decisions in M2:** saves happen 0.5 s after the last change (SPEC §3.5). The memory check now runs only at M2, M4, M7, M8 and M12, for 30 minutes (CLAUDE.md quality gate).

---

## M3 Dashboard and debug screen

**Goal:** the landscape dashboard's left panel matches the design on the phone; the fake clock makes every state reachable.

**Build**
- `ClockPanel` per README › Screens › 1 with SPEC §3.4's layout rules: clock, day/date, Routine header with LEFT, progress bar with end-dot, start/end times, sub-routines, CURRENT/NEXT, Free time and Bedtime states.
- Landscape split 60/40 with the divider; the right panel shows only the "Lists" header and Settings gear for now.
- Debug screen (debug builds only, SPEC §3.6), opened by long-pressing the Settings gear.

**Acceptance criteria**
- [ ] Each row of the test-times table looks the same as the prototype at that fake time (prototype in Chrome next to the phone).
- [ ] Idle redraw check (CLAUDE.md): over 3 idle minutes the app renders about 3 frames, one per minute.
- [ ] **(owner)** Layout Inspector: the clock panel recomposes once per minute; the right panel doesn't recompose on the minute.
- [ ] `./gradlew assembleRelease` builds (unsigned until M12), proving release compiles without the debug screen.

**Install on the phone and verify**
- [ ] Screenshots at each test time saved to `temp/screenshot/` with `adb exec-out screencap -p`; nothing clipped at 853 × 384 dp, in both landscape directions.
- [ ] Fake clock at 600× runs through a whole day without a crash.

---

## M4 Night and power

**Goal:** the screen-on and Bedtime rules from SPEC §3.2 and §3.3.

**Build**
- `domain/ScreenRules.kt` (keep on? dim?) and `screen/ScreenPolicy.kt`: battery rule, Bedtime dim with forced dark palette, tap-to-brighten for 30 s that swallows the first tap, restore when Bedtime ends.

**Acceptance criteria**
- [ ] Unit tests: keep-on for each combination of charging / not charging and level 14 / 15; dim only in Bedtime, not in Free time.
- [ ] Dark palette complete on the dashboard.

**Install on the phone and verify** (debug fake clock)
- [ ] Jump to Mon 23:30: screen dims and turns dark.
- [ ] Tap: brightens, and the tap does nothing else. 30 s after the last touch it dims again.
- [ ] From Mon 05:58 at 60×: at 06:00 brightness and the chosen theme return.
- [ ] Battery rule, using `adb shell dumpsys battery unplug` and `adb shell dumpsys battery set level 10`: the screen times out after 2 minutes. After `adb shell dumpsys battery reset`, it stays on again. Always run the reset.
- [ ] Tune `BEDTIME_BRIGHTNESS` with the owner in a dark room; record the value in the notes.
- [ ] 30-minute memory check.

---

## M5 List menu, list view mode, portrait

**Goal:** the right panel's lists, in view mode, in both orientations.

**Build**
- `ListMenu`: four rows with icons and badges (README › Screens › 1, right panel).
- List detail header: back, centred title, Edit button (edit mode itself is M6).
- To-Do / Self / Room view mode: numbering, child indent, To-Do done toggle (line-through, done colour, stays in place).
- Grocery view mode: categories, collapse with the rotating chevron (collapsed state saved), item selection with radio and bold text, category count badges.
- Empty state text with the `edit` icon.
- Portrait layout: the list panel at full width. Navigation state and system Back.

**Acceptance criteria**
- [ ] Unit tests: numbering (`1)`, `2)`, `2.1)`, child counter restarts under each parent); To-Do badge counts not-done items at all levels; Grocery badge counts selected items in all categories.

**Install on the phone and verify**
- [ ] Ticking a to-do and selecting a grocery item update the badges and survive a force-stop.
- [ ] A collapsed category stays collapsed after a force-stop.
- [ ] Rotating to portrait with a list open keeps it open; rotating back too.
- [ ] Back goes from a list to the menu.

---

## M6 List edit mode

**Goal:** editing every list, without drag (M7).

**Build**
- Edit/Done toggle (Done = 40 dp filled circle with `check`).
- Outline lists: rows with the drag handle (inert until M7), number, underlined field, ✕. Enter inserts after the item and its children at the same depth and focuses it. Backspace on an empty field deletes it (or the SPEC §3.7 fallback). Deleting a parent promotes its children. "+ Add task" / "+ Add item".
- Grocery: category name field and delete; item rows (radio, field, ✕); "+ Add item" per category; "+ Add category" pill.
- Cleanup when leaving edit mode or the list (README › Saving edits).

**Acceptance criteria**
- [ ] Unit tests for `OutlineOps` (insert after subtree, delete with promote, normalize so no child is orphaned, cleanup of empty items) and `GroceryOps` cleanup (trim, drop empty items, drop empty nameless categories, rename the rest to "Untitled").
- [ ] Typing doesn't lag; saves are debounced (at most about 2 disk writes per second while typing).

**Install on the phone and verify**
- [ ] With Samsung Keyboard: Enter and Backspace-on-empty behave as specified. Record the Backspace result in the notes.
- [ ] With the keyboard open in landscape, the focused field stays visible.
- [ ] Rotating while editing keeps edit mode, the text and the focus.

---

## M7 Drag: indent and reorder

**Goal:** the drag-handle gestures from README › Drag handle gestures, in To-Do, Self and Room.

**Build**
- `domain/DragMath.kt`: gesture decision, indent range with 0.25× rubber band, snap to 24 dp levels, drop index and depth.
- Indent mode (140 ms snap), reorder mode (280 ms hold, 12 ms haptic, ghost card with "+N sub-tasks", rows slide apart 200 ms, 170 ms drop), vertical-first cancel so the list scrolls.

**Acceptance criteria**
- [ ] Unit tests: max depth = `min(prevDepth + 1, 1)`; no indent for the first item; indenting a parent keeps its children at depth 1; drop depth = `min(originalDepth, prevDepth + 1, 1)`; a parent moves with its children.
- [ ] **(owner)** Layout Inspector: rows don't recompose on every frame during a drag.

**Install on the phone and verify**
- [ ] All three gestures work in a list long enough to scroll, in To-Do and in Self.
- [ ] A vertical swipe that starts on a handle scrolls the list.
- [ ] Order and depth survive a force-stop.
- [ ] 30-minute memory check.

---

## M8 Settings and sound

**Goal:** the Settings screen and the block-change tone.

**Build**
- Settings per README › Screens › 4: two columns wrapping at 260 dp; Theme segmented control; "Weekly timetable" row (opens the M9 screen; a placeholder until then); tone radio rows that select and preview; volume slider with live gain and % label; Preview / Stop pill.
- Convert the three WAVs to OGG (ARCHITECTURE §7). `TonePlayer` and `BlockChime` per SPEC §3.1.

**Acceptance criteria**
- [ ] Unit tests: gain curve; chime decisions (start = baseline, no sound; change to a real block plays after 350 ms; change to Free time or Bedtime is silent; a second change within 350 ms plays only for the last one).
- [ ] The three sounds add less than 0.5 MB to the APK.

**Install on the phone and verify**
- [ ] Each tone previews; Stop stops it; the slider changes loudness while playing.
- [ ] Fake clock across Mon 07:59 → 08:00: the tone plays about 350 ms after the change. Crossing into Free time or Bedtime is silent.
- [ ] With a podcast or music playing, it gets quieter during the tone and recovers after.
- [ ] Light/Dark switch is correct on every screen built so far.
- [ ] 30-minute memory check that includes at least one real block change.

---

## M9 Weekly timetable

**Goal:** the timetable screen and the create gesture (README › Screens › 5).

**Build**
- Day chips with today's accent chip; 46 dp hour gutter; 7 columns; 44 dp per hour; today's column fill; now line with dot; auto-scroll so now − 1.5 h is at the top.
- Blocks with title and `6:00am-11:30am` range; the current block in accent.
- Long-press 300 ms on empty time (10 ms haptic), drag with 15-minute snap, bounded by neighbouring blocks, dashed ghost with range label; 15 minutes or less becomes 60 (bounded). Release opens the editor (placeholder until M10). Tapping a block opens it too.

**Acceptance criteria**
- [ ] Unit tests: create snapping and bounds, the ≤ 15 min → 60 min rule, range format (including `12:00am` for a 24:00 end).

**Install on the phone and verify**
- [ ] Create gesture works; a plain vertical swipe still scrolls.
- [ ] The now line moves each minute; today's column follows the fake clock's day.
- [ ] Portrait timetable is usable.

---

## M10 Block editor dialog

**Goal:** creating, editing and deleting routine blocks (README › Screens › 6).

**Build**
- Dialog: title field, 7 day toggles, two wheel pickers (hour, minute, am/pm), sub-routines (max 5, Enter/Backspace, "+ Add sub-routine" hidden at 5), error line, Delete (edit only) and Save.
- Rules: End `12:00 am` = midnight; moving Start to or past End pushes End to Start + 60 min (max midnight); validation order and messages; multi-day groups (editing any copy edits the group, deselecting a day removes that copy, Delete removes the group).

**Acceptance criteria**
- [ ] Unit tests: validation order and exact messages, including `Overlaps “Commute” (7 AM – 8 AM) on Mon, Tue.`; saving a group keeps each day's block id; midnight handling; Start pushing End.

**Install on the phone and verify**
- [ ] Create, edit, delete a block; overlap error shown.
- [ ] With the keyboard open in landscape the dialog scrolls and Save is reachable.
- [ ] Editing today's current block updates the dashboard at once; creating a block that covers now plays the tone (SPEC §3.1).

---

## M11 App icon

**Goal:** a launcher icon for Routine Manager.

**Build**
- Show the owner 2–3 concepts as images first; build the chosen one.
- Adaptive icon: foreground, background and monochrome layers (for One UI themed icons), as vectors. In keeping with the app's black/white + accent look. The debug app's icon must be easy to tell apart from the release icon.

**Acceptance criteria**
- [ ] No lint icon warnings.

**Install on the phone and verify**
- [ ] Icon looks right on the home screen, in the app drawer, in recent apps and with themed icons on.

---

## M12 Release APK and install guide

**Goal:** the signed release app on the phone, and the project's final check (SPEC §5).

**Build**
- The owner creates the release keystore with a command Claude provides; it lives outside the repo. `keystore.properties` (gitignored) points to it; the release `signingConfig` reads it. Add the keystore and properties file to `.gitignore`.
- `versionCode 1`, `versionName "1.0"`. `./gradlew assembleRelease` with R8.
- Confirm the release APK has no debug-screen classes.
- `docs/INSTALL.md`: how to build and install (`adb install`), how to update later (`adb install -r`, never uninstall: that deletes all data), how to back up the keystore and its passwords in two separate places and why (SPEC §3.5), and the recommended phone settings (Motion smoothness Standard, Protect battery on; Auto Blocker off only if an install is blocked).

**Acceptance criteria**
- [ ] Quality gate passes for the release build too (`lintRelease`).
- [ ] Release memory baseline recorded in the notes.
- [ ] The owner confirms the keystore is backed up.

**Install on the phone and verify**
- [ ] Release app installs and starts with the sample data.
- [ ] Keep the debug app installed: M13 needs its fake clock for screenshots.
- [ ] **24-hour run** per SPEC §5. The owner runs it and reports.

---

## M13 User guide (README) and browser demo

**Goal:** a README on GitHub that walks through every feature with screenshots, plus a link to try the prototype in a browser without installing anything.

**GitHub only, not in the app.** This milestone adds no app code: no help screen, no in-app guide, nothing installed on the phone. The phone is used only to take screenshots of the app that already exists.

**Build**
- `README.md` at the repo root (GitHub shows it on the repo's front page), written for someone who has never seen the app:
  - What the app is and who it's for.
  - **Try it in your browser:** link to the prototype (below), with a note that it needs internet (React, Babel and fonts load from CDNs) and has a TEST bar for orientation and time travel that the real app doesn't have.
  - **Feature walkthrough**, one step per feature, each with an image: dashboard (current block, Free time, Bedtime), list menu and badges, To-Do / Self / Room in view and edit mode, drag to indent and reorder, Grocery in view and edit mode, Settings (theme, tones, volume), weekly timetable and the create gesture, block editor (validation, multi-day groups), portrait layout, night dimming.
  - **Important details:** the SPEC decisions a user notices: the tone plays only while the app is on screen; the screen stays on; the low-battery cut-off; Bedtime dimming and tap-to-brighten; rotation ignores the rotation lock; no backup, data lives only on the phone.
  - **How the prototype differs from the app** (SPEC §2 changes, and the TEST bar).
  - Install: link to `docs/INSTALL.md`.
- Screenshots: taken on the phone with `adb exec-out screencap -p`, using the debug app's fake clock and the sample data, saved as `docs/screenshots/<nn>-<name>.png`. For gestures a still image can't show (drag, timetable create), use a before/after pair, or a short GIF made from `adb shell screenrecord` with ffmpeg if that is clearer (under 2 MB each).
- Browser demo: the prototype stays at `design/prototype/` with `support.js` beside it. The owner turns on GitHub Pages (repo Settings → Pages → Deploy from a branch → `main`, folder `/`). The README links to `https://mecha-coder.github.io/Routine-Manager/design/prototype/Routine%20Dashboard.dc.html` and also explains how to download the two files and open the HTML in Chrome.

**Acceptance criteria**
- [ ] Every user-visible feature in README › Screens and SPEC §2 has a walkthrough step with an image.
- [ ] Screenshots show only sample data.
- [ ] Every image and link in README.md works when viewed on github.com.

**Verify on GitHub** (nothing is installed on the phone in this milestone)
- [ ] Each walkthrough step matches what the release app already does on the phone.
- [ ] The owner opens the README on github.com and runs the browser demo from the Pages link on another device.
