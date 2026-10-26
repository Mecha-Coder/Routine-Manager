# Routine Manager: build plan

Thirteen milestones, each sized for one Claude Code session. Order follows SPEC D21: after M2, M3 with M11 beside it; then two parallel tracks (M5 → M6 → M7, and M4, M8 → M9 → M10); then M12 and M13.

There are no Android Studio GUI checks (Layout Inspector, Profiler): the owner dropped them in M3. Memory and redraws are measured over adb (CLAUDE.md).

## How to run a milestone

1. Normally the owner types `/supervise` and the supervisor runs the milestones one by one (see CLAUDE.md › Supervised builds). To run one by hand instead, start a fresh session from the repo root: "Read CLAUDE.md, then do milestone M<n>."
2. Read [design/README.md](../design/README.md), [SPEC.md](SPEC.md), [ARCHITECTURE.md](ARCHITECTURE.md) and this milestone.
3. Build only this milestone's scope. If it won't fit in one session, stop at a working point, split the rest into M<n>b here, and say so.
4. Pass the **quality gate** in [CLAUDE.md](../CLAUDE.md) and every acceptance criterion below. The last step is always "Install on the phone and verify", except in M13, which changes no app code.
5. Update the status table, add notes under the milestone if something was learned or changed, and commit (`M<n>: <summary>`) and push; the owner doesn't need to be asked first.

## Status

| Milestone | Status |
|---|---|
| M1 Foundation: window, theme, font, icons | Done |
| M2 Data, routine logic, minute clock | Done |
| M3 Dashboard and debug screen | Done |
| M4 Night and power | Done |
| M5 List menu, list view mode, portrait | Done |
| M6 List edit mode | Done |
| M7 Drag: indent and reorder | Not started |
| M8 Settings and sound | Done |
| M8b Two replacement tones | Done |
| M9 Weekly timetable | Done |
| M10 Block editor dialog | Not started |
| M11 App icon | Done |
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
- [x] Each row of the test-times table looks the same as the prototype at that fake time (prototype in Chrome next to the phone).
- [x] Idle redraw check (CLAUDE.md): over 3 idle minutes the app renders about 3 frames, one per minute.
- [ ] **(owner)** Layout Inspector: the clock panel recomposes once per minute; the right panel doesn't recompose on the minute. **Skipped (owner decision, M3)**: covered by the idle redraw check (1 frame per minute) and the code check (only `ClockPanel` collects `dashboard`).
- [x] `./gradlew assembleRelease` builds (unsigned until M12), proving release compiles without the debug screen.

**Install on the phone and verify**
- [x] Screenshots at each test time saved to `temp/screenshot/` with `adb exec-out screencap -p`; nothing clipped at 853 × 384 dp, in both landscape directions.
- [x] Fake clock at 600× runs through a whole day without a crash.

**Notes**
- **Text boxes are now as tall as in the prototype's CSS (`ui/theme/Type.kt`).** The first build put the dashboard 45 dp, then 4–5 dp, lower than the prototype. Two causes, both measured on the phone:
  - Compose's default line-height mode (`Mode.Fixed`) pads a single line back up to the font's height when the set line height is smaller: the 0.84 em clock was about 130 dp tall instead of 87.
  - Compose always turns on Android's fallback line spacing. On this phone that gives any text containing letters a **1.466 em** line (ascent −1.077, descent 0.389 em), although every glyph comes from Google Sans Flex, whose own line is **1.252 em** (hhea 1932 / −572 at 2000 units/em, no line gap; Chrome uses this). Digits-only text keeps 1.252. Found with a temporary probe (`BoringLayout.isBoring` with and without fallback spacing; `elegantTextHeight` made no difference), since removed.
  - Fix: every `RmType` style has an explicit line height (1.252 em, CSS `normal`, unless the README or prototype gives one: clock 0.84, CURRENT/NEXT 1.15, Bedtime 1.2, sub-routines 1.3, body 1.35) with `LineHeightStyle(Center, Trim.Both, Mode.Tight)`, which pins each line to exactly that height. Known gap: digits-only text in a style taller than 1.252 em (sub-routines, list body) gets a 1.252 em box. Material's own text styles are unchanged.
  - Result, uiautomator bounds on the phone vs element boxes in Chrome at the same minute (dp, top edge): clock 28.3 / 28, Routine 131.7 / 131.4, start time 180.8 / 180.4, body 226.1 / 225.4, Lists 28.3 / 28.5. Horizontal positions match after scaling the panel to 60 % of 853 dp.
- **Height budget (SPEC §3.4):** the body takes the remaining height: 136.6 dp on the phone (design 132), so the 5 sub-routine rows are 28.6 dp apart instead of 27.5. Nothing is clipped in any test state.
- **Layout follows the prototype markup** where the README is brief: the clock row stays at the top in Bedtime and the message is centred in the space below it; there are always 5 sub-routine rows (empty ones keep their height), so 2 sub-routines sit at the top; the progress track is never under 10 dp and is clipped on the right near 100 %, as in the prototype at Mon 12:59.
- **Debug screen** (`src/debug/.../debug/`): `FakeAppClock` (the prototype's `simNow()`), `DebugScreen`, and `DebugEntry`, the main code's only way in. `src/release/.../debug/DebugEntry` is a stub (system clock, no screen, long-press not wired). The fake clock's changes are merged into the ticker's change flow, so a jump shows at once. Controls follow the prototype: a day chip keeps the time of day; the slider keeps the seconds; any speed, day or slider change leaves Live; Reset loads `SampleData.create()` (lists, blocks and settings). Its unit tests are in `src/testDebug` (they need the debug source set).
- **Exact test times over adb:** the slider spans the full width (about 1,470 px for 1,439 minutes), so every minute is reachable with one `adb shell input tap`; the time label beside the speed chips confirms it.
- **Gear tap** does nothing until M8 (Settings). In portrait the home screen shows the list panel (header only until M5).
- **For M8:** the prototype's Reset also stops the tone and re-takes the chime baseline (`lastKey = null`). The reset gives every block a new id, so without that the reset would count as a block change and play the tone.
- **Phone fact:** the stand can hold the phone either way round: M3 started at `ROTATION_90` (notch on the left) and ended at `ROTATION_270` (notch on the right, as in M1's note). The gear's position on screen differs between the two (x 1529 px vs 1491 px), so adb taps on it must be read from a UI dump.
- **Unit tests:** 55 (7 new, `FakeAppClockTest`: live = phone clock; day jump keeps the time within the Mon–Sun week; slider keeps day and seconds; runs on at its speed; pause holds; Live returns; the ticker shows a jump at once). Lint: 0 errors, the 2 accepted M1 warnings.
- **Phone, 25 Oct (debug build):**
  - Test times: the 7 rows set with the debug screen (paused) and the same 7 in the prototype (Chrome, TEST bar paused): `temp/screenshot/m3-phone-<state>.png` and `m3-proto-<state>.png`. Each shows the expected block, LEFT, NEXT and sub-routines; Mon 12:59 clips the track on the right exactly as the prototype does.
  - Both landscape directions: the 7 states again with the notch on the right (`ROTATION_270`, owner turned the phone): `m3-phone-r270-<state>.png`, nothing clipped. The gear's right edge is 45.5 dp from the screen edge there (34 dp notch padding + 12 dp button inset, SPEC D18) and 25.3 dp with the notch on the left (14 + 12); the left panel is the same in both (42 dp already clears the notch).
  - Idle redraw, live clock, 20:13:48–20:16:48: 3 framestats rows, 60.1 s and 60.0 s apart ("Total frames rendered" 6: the phone counts each frame twice, M2 note).
  - 600× from Mon 00:00 for 152 s (about 25 clock hours, into Tue 01:24): no crash, no ANR; the date and Bedtime changed at midnight.
  - `assembleRelease` (R8): 2.4 MB unsigned. `dexdump` finds no `FakeAppClock` or `DebugScreen` class in it (both are in the debug APK); only the `DebugEntry` stub.
- **Gate (25 Oct 2026):** clean `assembleDebug lintDebug testDebugUnitTest assembleRelease` passes (55 tests, 0 failures, 0 skipped; lint 0 errors, the 2 accepted warnings). Installed, no crashes or ANRs. Idle redraw 20:23–20:26: 3 framestats frames ("Total frames rendered" 6). Only `ClockPanel` collects `dashboard`. No memory check at M3 (CLAUDE.md quality gate).

---

## M4 Night and power

**Goal:** the screen-on and Bedtime rules from SPEC §3.2 and §3.3.

**Build**
- `domain/ScreenRules.kt` (keep on? dim?) and `screen/ScreenPolicy.kt`: battery rule, Bedtime dim with forced dark palette, tap-to-brighten for 30 s that swallows the first tap, restore when Bedtime ends.

**Acceptance criteria**
- [x] Unit tests: keep-on for each combination of charging / not charging and level 14 / 15; dim only in Bedtime, not in Free time.
- [x] Dark palette complete on the dashboard.

**Install on the phone and verify** (debug fake clock)
- [x] Jump to Mon 23:30: screen dims and turns dark.
- [x] Tap: brightens, and the tap does nothing else. 30 s after the last touch it dims again.
- [x] From Mon 05:58 at 60×: at 06:00 brightness and the chosen theme return.
- [x] Battery rule, using `adb shell dumpsys battery unplug` and `adb shell dumpsys battery set level 10`: the screen times out after 2 minutes. After `adb shell dumpsys battery reset`, it stays on again. Always run the reset. (Screen-off itself interrupted by a touch; evidence accepted by the supervisor, see notes.)
- [x] Tune `BEDTIME_BRIGHTNESS` with the owner in a dark room; record the value in the notes. (0.05, owner.)
- [x] 30-minute memory check.

**Notes**
- **Code:** `domain/ScreenRules.kt` holds the decisions (`keepScreenOn`, `dim`, `darkPalette`); `screen/ScreenPolicy.kt` applies them to the window. `MainActivity` feeds it `MainViewModel.routine` while STARTED, and `dispatchTouchEvent` asks it first.
- **One ticker:** `MainViewModel` now shares the minute ticker (`shareIn`, `WhileSubscribed(replayExpirationMillis = 0)`, so a return from the background never starts from a stale minute). The dashboard, `routine` (ScreenPolicy, every screen and both orientations) and `darkTheme` all use it, so the app still wakes once per minute. For M8: `BlockChime` can use `routine` too. The debug screen still runs its own ticker while it is open (debug only).
- **Keep-screen-on:** checked at every tick from the sticky `ACTION_BATTERY_CHANGED` (null receiver, nothing registered). Charging = `EXTRA_PLUGGED != 0` (AC, USB, wireless), so a phone held at 85 % by "Protect battery" still counts. The flag and the brightness are written only when they change, because every window change re-lays out the window.
- **Tap-to-brighten:** the whole gesture whose DOWN arrives while dim is swallowed (DOWN to UP/CANCEL), so a long press can't fire either. The 30 s wait starts when the finger lifts and a new DOWN cancels it, so the screen never dims under a finger. When Bedtime starts at a tick the screen dims at once, even right after a touch (supervisor, M4: only a touch made while dim counts). Going to the background cancels the timer; back in Bedtime, the screen starts dim.
- **For M10:** Compose `Dialog` is its own window, so its touches don't pass through `Activity.dispatchTouchEvent`. The editor dialog must route its touches through `ScreenPolicy.swallows`, or the first tap on a dim dialog would act. The dim itself (an activity window attribute) should still apply while the dialog is open; check it on the phone in M10.
- **Panel facts (dumpsys display):** brightness floor 0.011764706 (3/255). A window value below it is clamped: 0.002 and 0.01 both gave `Display Brightness=0.011764706`; 0.02 and 0.05 applied as set. Default 0.4; the system's pre-timeout dim is 0.05. In a ~10–80 lux room the owner's auto brightness was 0.0157, so the starting value 0.05 is brighter than the phone runs in dim light.
- **`BEDTIME_BRIGHTNESS` = 0.05, confirmed by the owner (SPEC §3.2).** During M4 the debug screen briefly had a "Bedtime brightness" chip row (0.012 to 0.08) that changed the value live; the owner chose to remove it, so it and its DebugEntry hooks are gone. To re-tune, change the constant: anything below the panel floor 0.0118 looks the same as 0.0118.
- **Unit tests:** 64 (9 new, `ScreenRulesTest`). Lint: 0 errors, the 2 accepted M1 warnings. `assembleRelease` builds.
- **Phone, 25 Oct (debug build with the tuning chips, since removed; phone really unplugged, 75–78 %; notch on the right):**
  - Mon 23:30 (paused): window `sbrt=0.05`, `mBrightnessReason=override(…MainActivity)`, `Display Brightness=0.05`. Dashboard and debug screen dark: sampled pixels bg `#000000`, divider `#353535`, clock and gear `#EDEDED`, date and "Routine" `#B2B2B2` (`temp/screenshot/m4-home-bedtime-dark.png`, `m4-debug-bedtime.png`).
  - While dim, a tap on the debug slider only brightened (`mBrightnessReason=automatic`, the time label unchanged); the next tap moved the slider. A 1 s long press on the gear while dim only brightened, and the debug screen did not open.
  - Re-dim: 28–31 s after a single tap; with a second touch 20 s later, 30–32 s after that touch (poll every ~2.5 s). In a fine poll: exactly 30.0 s after the last touch.
  - From Mon 05:20 at 60× (so the 30 s re-dim falls before 06:00): dim at +30.0 s; brightness back at +34.9 s, when the clock reached 06:00, with the light palette (`m4-debug-0600.png`).
  - Bars swiped in at Bedtime: white icons on black, readable (`m4-bars-bedtime.png`). On the light palette the system puts a grey scrim behind them (`m4-bars-day.png`). The icon colour follows the phone's dark mode (on now); recheck in M8 with the phone in light mode.
  - Idle redraw, live clock, 21:06:20–21:09:20: 3 framestats frames, 60.01 s and 60.00 s apart ("Total frames rendered" 6, M2 note).
  - Battery rule: `unplug` + `set level 10` at 21:11:03 → `FLAG_KEEP_SCREEN_ON` cleared at the 21:12:00 tick; `reset` at 21:12:04 → back at the 21:13:00 tick, and the screen was still `Awake` at 21:15:34, 3.5 min after the last touch. Second run: level 10 at 21:18:22, flag cleared at the 21:19:00 tick; the system started its pre-timeout dim at 21:20:40 (`policy=DIM`). **The timeout counts from the tick that clears the flag, not from the last touch:** the window wake lock is ON_AFTER_RELEASE, so its release counts as user activity. A real touch at 21:20:50 (someone using the phone) woke it about 10 s before screen-off, so the screen-off itself was not observed; the supervisor accepted this evidence without a rerun. `dumpsys battery reset` ran at 21:21:43.
  - No crashes, no ANRs.
- **Gate (25 Oct 2026, on `main` with M4, M5 and M11 merged):** clean `assembleDebug lintDebug testDebugUnitTest assembleRelease` passes (80 tests, 0 failures; lint 0 errors, the 2 accepted warnings). Fake Mon 11:12 PM: `Display Brightness=0.05` with our activity's override, dark palette on the dashboard and the list menu; back on Live the brightness returns to `automatic`. No crashes or ANRs. Memory, 30 minutes (22:07–22:37): 95,085 KB at minute 5, 92,440 KB at minute 30, flat. Idle redraw untouched (22:11–22:14): 3 framestats frames ("Total frames rendered" 6). An earlier count that started 11 s after leaving the debug screen and Bedtime caught a one-off burst (252 frames, mostly in its first seconds); the cause wasn't found. The M12 24-hour run should show whether anything like it repeats.

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
- [x] Unit tests: numbering (`1)`, `2)`, `2.1)`, child counter restarts under each parent); To-Do badge counts not-done items at all levels; Grocery badge counts selected items in all categories.

**Install on the phone and verify**
- [x] Ticking a to-do and selecting a grocery item update the badges and survive a force-stop.
- [x] A collapsed category stays collapsed after a force-stop.
- [x] Rotating to portrait with a list open keeps it open; rotating back too.
- [x] Back goes from a list to the menu.

**Notes**
- **Unit tests:** 71 (16 new): `NumberingTest` 6, `OutlineOpsTest` 4, `GroceryOpsTest` 5, `MenuBadgesTest` 1. Lint: 0 errors, the 2 accepted M1 warnings.
- **Where things are:**
  - `domain/`:
    - `Numbering.labels`.
    - `OutlineOps.openCount` and `toggleDone`. M6 adds the edit operations here.
    - `GroceryOps.selectedCount` (all categories, or one), `toggle` and `toggleCollapsed`.
  - `MainViewModel` holds:
    - the open list (`openList`, with `showList` and `showMenu`): kept through rotation, not saved;
    - `badges` (`MenuBadges`);
    - the three view-mode actions.
  - `HomeScreen` now takes the ViewModel. Its private `ListPanel` shows either `ListMenu` or `ui/lists/ListDetail` (`OutlineList`, `GroceryList`), and holds the `BackHandler` while a list is open.
  - Shared pieces: `CountBadge`, `RadioDot` (also for M8's tone rows), `Modifier.bleed` (the prototype's negative horizontal margin, which M6's edit rows also use), `RmIconButton(iconSize)` (the Edit icon is 22 dp), `RmType.emptyState` (15 / 400) and `RmShapes.categoryHeader` (14).
- **The Edit button does nothing until M6**, as the gear did until M8.
- **Pressed tints:** no ripples, following the prototype.
  - List menu rows: `surfacePressed` (README).
  - Grocery category header: `surface`. The prototype's light hover tint `#f5f5f5`, drawn through its `brightness(.9)` filter, comes out as #DCDCDC, which is the README's `surface` (#DBDBDB). The tint reaches 10 dp past the text on both sides (`margin: 0 -10px` in the prototype).
  - To-do rows and grocery items: no tint, as in the prototype. The strike-through or the dot is the feedback. They are `toggleable` (`Role.Checkbox`), so `uiautomator dump` shows `checked=`.
- **Notch (SPEC §3.4):** list content uses max(design padding, 34 dp) on the notch side, like the header: the menu card's 22 dp and the list's 26 dp both become 34 dp.
- **Empty state** ("Nothing here. Tap ✎ to add.") can't be seen on the phone until M6, because nothing in view mode removes items. Grocery follows the prototype: the line shows when no category has items, under any categories that are left.
- **For M6/M7:** `uiautomator dump` reports rows shorter than 48 dp as up to 48 dp tall, with the top up to 2 dp higher (accessibility touch bounds). Take positions from the text nodes.
- **Phone, 25 Oct (debug build, landscape, notch on the right):**
  - Compared with the prototype in Chrome (`temp/screenshot/m5-proto-*.png` against `m5-phone-*.png`), in dp from the list pane's left edge:
    - To-Do numbers at x 32.2 and 56.2, text at 56.2 and 95.1 (prototype 32 / 56 / 56 / 95.1);
    - the first line of text at y 85.3 (85.2);
    - the title at y 27.7 (27.5);
    - Grocery headers at x 19.9, y 74.1 and 220.3 (20, 74, 220); chevron at x 30.1, name at x 59.9, item text at x 94.1 (30 / 60 / 94).
  - Ticking "Clean room" (a parent) and "Itinerary" (a child) took the To-Do badge from 4 to 2. Selecting "Egg" took Grocery from 1 to 2. Collapsing "Food" kept its badge at 1.
  - Force-stop and relaunch: `app_data.json` held the ticks, the selection and `"collapsed": true`. Both badges still showed 2, and Food was still collapsed.
  - Back works both ways: the Back key goes from a list to the menu and the app stays on top; so does the header's back arrow.
  - A screen recording of a held tap on the Food header shows the `surface` tint while it is pressed. After release, the chevron turns through intermediate frames for about 150–180 ms.
  - Idle redraw with To-Do open, 21:57–22:00: 3 framestats frames, 60.0 s apart ("Total frames rendered" 6, since the phone counts each frame twice). The list panel adds no frames.
  - Rotation. The supervisor rotated the display over adb, with `cmd window fixed-to-user-rotation enabled` and `cmd window user-rotation lock <n>`; the app still gets a normal configuration change. Afterwards the supervisor restored the defaults (auto-rotate off, the app following the sensor).
    - To ROTATION_0 with To-Do open: To-Do stayed open with its ticks, at the full 384 dp width. The header sits at 34 dp because of the top notch (the design has 16). Rows have the design's 30 / 26 dp side padding, and the title is centred.
    - In portrait: the menu, Self and Grocery all work. A tap on a Self item left `app_data.json` unchanged.
    - Back to ROTATION_270 with Grocery open: Grocery stayed open in the right panel, with the clock panel beside it.
    - Screenshots: `m5-phone-portrait-*.png` and `m5-phone-r270-grocery-after-rotation.png`.
  - No crashes or ANRs during the checks. Afterwards the sample list state was restored (To-Do 4, Grocery 1, nothing collapsed), and the app was left on the live dashboard.

---

## M6 List edit mode

**Goal:** editing every list, without drag (M7).

**Build**
- Edit/Done toggle (Done = 40 dp filled circle with `check`).
- Outline lists: rows with the drag handle (inert until M7), number, underlined field, ✕. Enter inserts after the item and its children at the same depth and focuses it. Backspace on an empty field deletes it (or the SPEC §3.7 fallback). Deleting a parent promotes its children. "+ Add task" / "+ Add item".
- Grocery: category name field and delete; item rows (radio, field, ✕); "+ Add item" per category; "+ Add category" pill.
- Cleanup when leaving edit mode or the list (README › Saving edits).

**Acceptance criteria**
- [x] Unit tests for `OutlineOps` (insert after subtree, delete with promote, normalize so no child is orphaned, cleanup of empty items) and `GroceryOps` cleanup (trim, drop empty items, drop empty nameless categories, rename the rest to "Untitled").
- [x] Typing doesn't lag; saves are debounced (at most about 2 disk writes per second while typing).

**Install on the phone and verify**
- [x] With Samsung Keyboard: Enter and Backspace-on-empty behave as specified. Record the Backspace result in the notes. **Done with Gboard**, the phone's default keyboard; Samsung Keyboard not tested (supervisor decision, M6; see notes).
- [x] With the keyboard open in landscape, the focused field stays visible.
- [x] Rotating while editing keeps edit mode, the text and the focus.

**Notes**
- **Unit tests:** 99 (19 new): `OutlineOpsTest` +11, `GroceryOpsTest` +8. Lint: 0 errors, the 2 accepted M1 warnings.
- **Where things are:**
  - `domain/OutlineOps`: `subtreeEnd`, `normalize`, `editText`, `insertAfter`, `append`, `delete` (children promoted, then normalised), `cleanup`, `MAX_DEPTH`. `domain/GroceryOps`: `rename`, `deleteCategory`, `addCategory`, `editItem`, `deleteItem`, `addItem` (after an item, or at the end), `cleanup`, `UNTITLED`. All ported from the prototype. As in the prototype, cleanup trims category names only; kept item text is not trimmed.
  - `MainViewModel`:
    - `editing` (survives rotation, not saved);
    - `startEditing` and `finishEditing`, which cleans all four lists, like the prototype's `purge`;
    - `showList` opens a list in view mode, and `showMenu` cleans up when editing;
    - one function per edit. The ones that add an item return its id, so the list can focus it.
  - `ListDetail` now takes the ViewModel instead of one lambda per action (there would be 16). `OutlineEditList` (in `OutlineList.kt`) and `GroceryEditList` (in `GroceryList.kt`) take plain lambdas.
  - Shared pieces: `UnderlinedField`, `AddButton` ("+ Add task", "+ Add item"), `RmIconButton(tint)`, `Modifier.bleed(start, end)` (edit rows reach 30 dp left and 8 dp right, as in the prototype), `RmType.field` (16 / 400 at the input's `normal` line height).
- **No list-editing ViewModel yet** (ARCHITECTURE §3 allows one). The field to focus is a `remember` in the edit list. The activity isn't recreated on rotation (`configChanges`), so the composition, the fields' text state and the focus all survive. M7 can decide where the drag session lives.
- **Text fields:** state-based `BasicTextField` (`TextFieldState`). The field owns its text while shown and pushes every change through `snapshotFlow` to `repository.update`. Typing never waits for the saved data to come back, so the cursor can't jump and no key is lost; the repository's 500 ms debounce does the writing.
- **Keyboard choices (owner, M6; SPEC §3.7):**
  - The action key is Next (→|) on item fields: Enter inserts the new item and focuses it, and the keyboard stays open.
  - It is Done (✓) on the category name, where it only closes the keyboard; the prototype does nothing there.
  - The placeholder uses `textHint`. The prototype renders #696969; the README has no placeholder token.
  - Sentences capitalisation.
  - After Backspace or ✕ deletes the focused item, nothing keeps the focus and the keyboard closes, as in the prototype. ✕ on another row leaves the focus where it was.
- **Backspace on an empty field:** the phone's default keyboard is **Gboard** (`com.google.android.inputmethod.latin`). Samsung Keyboard is installed but not the default.
  - A temporary InputConnection probe (since removed) showed that Gboard sends `KEYCODE_DEL` as a key event on every backspace, empty field or not. `onPreviewKeyEvent` catches it, so Backspace-on-empty works.
  - **Samsung Keyboard was not tested** (supervisor decision: Gboard is the owner's real keyboard). ✕ stays the supported delete path (SPEC §3.7).
  - If a keyboard ever sends nothing for an empty field, Compose's `InterceptPlatformTextInput` can wrap the InputConnection and catch `deleteSurroundingText`.
- **Keyboard in landscape:**
  - Gboard covers the bottom 375 px (200 dp). The list's scroll area ends above it (`imePadding`).
  - Compose sets `IME_FLAG_NO_FULLSCREEN`, so there is no full-screen text box in landscape.
  - Compose's text field keeps only its **cursor line** above the keyboard (its focus rect is the cursor), which left a low field's underline under the keyboard. `UnderlinedField` now also brings the whole field into view while focused, whenever the keyboard's height changes. Checked: the underline sits at the keyboard's top edge, also for newly added fields at the bottom.
- **Phone, 25 Oct (debug build, landscape, notch on the right, Gboard; fake clock Mon 09:05; keys tapped with `adb shell input tap`):**
  - Layout against the prototype in Chrome (`temp/screenshot/m6-proto-*.png` vs `m6-phone-*.png`), dp from the list pane's left edge:
    - To-Do edit: handle icon 6.4 (prototype 6), number 32.5 (32), field 58.7 (58), child number 56.5 (56), child field 97.6 (97.1), "Add task" text 58.7 (58).
    - Grocery edit: name field 29.9 (30), item field 92.3 (92), "Add item" text 88.5 (88).
    - ✕ and `delete` end at the content edge (34 dp notch padding).
    - Rows are 83 px (44.27 dp), as in M5.
  - Enter:
    - on "Clean room": a new focused "2)";
    - on the child "Complete places": a new focused "2.2)";
    - on the parent "Sabah": a new top-level "3)" after its children;
    - in a grocery item: a new item right after it.
  - Backspace on empty: deleting "Sabah" letter by letter, then one more backspace, removed it and promoted its children. Same for an empty grocery item. On an empty category name it does nothing.
  - Saves, from an on-device poll of `app_data.json`'s mtime every 0.1 s:
    - 9 keys in 1.1 s: 1 write, 0.53 s after the last key;
    - 5 keys 0.73 s apart: 5 writes (1.4/s). The debounce caps writes at 2/s.
  - Typing, 20-key burst (`gfxinfo`): 50th / 90th / 99th percentile frame 26 / 32 / 38 ms. That is one slow frame per key (about 8–10 ms recomposition plus about 8 ms draw, debug build). No dropped keys.
  - Cleanup:
    - Done drops an empty to-do.
    - Back while editing: the first Back closes the keyboard (edit mode stays); the second goes to the menu and drops the empty item.
    - Grocery Done: a nameless category with items became "Untitled"; an empty new category was dropped; a named category with no items was kept.
    - Emptying Self with ✕ shows "Nothing here. Tap ✎ to add." (first seen on the phone here; M5 note).
  - Rotation (the supervisor rotated over adb), with To-Do in edit mode, a new item "Dotate" focused and Gboard open:
    - In portrait, edit mode, the text, the focus and the keyboard stayed, and typing went on into the same field.
    - Back in ROTATION_270, the same; the field sat just above the keyboard.
    - Screenshots: `m6-rot-portrait.png`, `m6-rot-back-landscape.png`.
  - After the probe was removed, Enter and Backspace-on-empty were checked again on the final build. No crashes or ANRs (`logcat -b crash`, `ANR in com.jsk`: empty). Sample data was reset, and the app was left on the live dashboard.
- **Phone facts:**
  - An app locker (`com.gamemalt.applocker`) now and then starts an invisible 2×2 px activity (`ServiceLauncherActivity`) on top. It takes the focus: the app pauses, and `uiautomator dump` shows the locker's window. `adb shell am start -n com.jsk.routinemanager.debug/com.jsk.routinemanager.MainActivity` brings the app back without restarting it.
  - The display also rotated by itself for 13 s once (phone moved?); taps sent meanwhile land in the wrong place. Check `mDisplayRotation` before a tap sequence.
- **Known gap:** cleanup runs on Done and Back only. If the process dies in edit mode, empty items already saved stay and show as empty numbered rows (process death is not handled specially, ARCHITECTURE §3).
- **For M7:** the handle is a 28 × 44 dp box with the `drag_indicator` icon (content description "Drag to move or indent"), still inert. `OutlineOps.subtreeEnd`, `normalize` and `MAX_DEPTH` are ready for the drag maths.

---

## M7 Drag: indent and reorder

**Goal:** the drag-handle gestures from README › Drag handle gestures, in To-Do, Self and Room.

**Build**
- `domain/DragMath.kt`: gesture decision, indent range with 0.25× rubber band, snap to 24 dp levels, drop index and depth.
- Indent mode (140 ms snap), reorder mode (280 ms hold, 12 ms haptic, ghost card with "+N sub-tasks", rows slide apart 200 ms, 170 ms drop), vertical-first cancel so the list scrolls.

**Acceptance criteria**
- [x] Unit tests: max depth = `min(prevDepth + 1, 1)`; no indent for the first item; indenting a parent keeps its children at depth 1; drop depth = `min(originalDepth, prevDepth + 1, 1)`; a parent moves with its children.
- [x] Code check: drag positions are read only in `offset { }`, `graphicsLayer { }` or draw lambdas, so rows don't recompose on every frame during a drag. (Replaces a Layout Inspector check; owner decision, M3.) Also measured with a temporary probe build, see notes.

**Install on the phone and verify**
- [x] All three gestures work in a list long enough to scroll, in To-Do and in Self.
- [x] A vertical swipe that starts on a handle scrolls the list.
- [x] Order and depth survive a force-stop.
- [ ] 30-minute memory check. (Supervisor.)

**Notes**
- **Unit tests:** 127 (20 new): `DragMathTest` 14, `OutlineOpsTest` +6. Lint: 0 errors, the 2 accepted M1 warnings. `assembleRelease` builds.
- **Where things are:**
  - `domain/DragMath`: `decide` (sideways more than 8 dp and more sideways than vertical → indent; vertical more than 8 dp first → scroll; else wait), `maxIndentDepth`, `indentOffset` (0.25× rubber band), `indentDepth` (snap to the nearest 24 dp level), `dropIndex`, `dropDepth`, `gapTop`, `shift`. Constants `HOLD_MILLIS` 280, `DECIDE_DP` 8, `LEVEL_DP` 24. All ported from the prototype's `dDown` / `dMove` / `dCalc` / `dGapTop` / `dUp`.
  - `domain/OutlineOps`: `setDepth` (the indent commit: only the item changes, then normalise) and `moveSubtree` (the item and its children move together; the children shift by the same depth delta).
  - `ui/lists/OutlineDrag.kt`:
    - `OutlineDragState`: the session, remembered in `ListDetail`. It is not a ViewModel: it holds layout coordinates and animations, and rotation keeps the composition (`configChanges`). Settles the M6 note.
    - `DragHandle`, `Modifier.outlineDragRow`, `OutlineDragGhost`.
  - `ListDetail` is now a Box: the list column, and the ghost over the whole panel, as in the prototype (its ghost lives in the pane, so it can pass over the header).
  - `MainViewModel.setOutlineDepth` / `moveOutlineItem`.
  - Theme: `RmElevation.dragGhost` and `RmType.dragGhostCount` (12 / 400).
- **Behaviour as in the prototype:**
  - Touching a handle clears the focus (the prototype's `blur()`), so the keyboard closes.
  - A tap on a handle does nothing.
  - Indent: only the dragged row moves; the release snaps in 140 ms (CSS `ease`); then only that item's depth changes.
  - Reorder:
    - 12 ms vibration at pick-up.
    - Ghost: 42 dp, `bg`, 1 dp `divider` border, radius 14, 1.03 scale with the drag shadow; it covers the row from the bled left edge to the bled right edge.
    - The item and its children are hidden; the other rows slide 200 ms with `cubic-bezier(.2,.7,.3,1)`.
    - The release animates the ghost 170 ms into the gap (and sideways to the new depth), with the scale and shadow going back to none.
    - Dropping back at the same place saves nothing.
  - A cancelled gesture counts as a release (the prototype's `pointercancel` = `dUp`).
  - Only deviation: the ghost of an item with no text shows the list's placeholder ("New item" in Self and Room); the prototype always says "New task".
- **No auto-scroll while reordering** (owner, M7: as in the prototype). Moving an item beyond the visible rows takes two drags with a scroll between. A possible follow-up if the owner finds that tedious: scroll while the ghost is near the list's top or bottom edge.
- **Positions:**
  - Everything is measured in the panel. The rows' positions come from `onPlaced` placed before each row's `graphicsLayer`, so they never include the drag's own offsets, and they are measured live on every move, not captured once as the prototype does. A layout change during the drag (keyboard closing, rotation) is followed.
  - Finger positions are converted from the handle with `localPositionOf`, so they stay right while the handle's row is translated (indent).
- **No flicker on commit:** the edit list shows the drag's result (`OutlineDragState.shown`) until the saved data comes back with it. The offsets are cleared in the same frame as the new order shows. A screen recording of a drop shows the ghost in the gap in one frame and the new order in place in the next.
- **Code check:**
  - Read every frame:
    - the indent offset, the slides and the hidden rows: in each row's `graphicsLayer { }`;
    - the ghost's x / y: in `offset { }`;
    - its scale: in `graphicsLayer { }`;
    - its shadow: in the `dropShadow { }` block (draw phase);
    - its width: in a `layout { }` lambda.
  - Read in composition: only the session's start and end (the ghost appears and goes) and the committed result.
  - **Probe build** (temporary `Log.d` in the list body, the row body and the ghost, plus the measured tops on each move; removed, and the clean build reinstalled):
    - Reorder with 41 move events: the ghost composed once at pick-up; 0 list or row recompositions during the moves. At the commit, the list once plus the 4 rows whose number changed; the other 5 skipped.
    - The measured tops stayed the same on all 41 moves while rows slid.
    - Indent over 0.8 s: 0 recompositions during the swipe; at the commit, the list once plus the 6 rows whose numbers changed.
- **Haptic:**
  - `VibrationEffect.createOneShot(12, DEFAULT_AMPLITUDE)`, which needs `android.permission.VIBRATE` (install-time, no prompt). M9 adds the same permission and its own helper; the supervisor merges them.
  - `dumpsys vibrator_manager` showed `Step=12ms`, usage TOUCH, scale LOW (0.71: the phone's touch-vibration intensity).
- **Shadow:** `0 12 30 rgba(0,0,0,.18)` is `Shadow(radius = 15.dp, offset 12 dp, alpha .18)`. Compose passes the radius straight to Android's `BlurMaskFilter` (sigma 0.577 × radius + 0.5, checked in the ui-graphics 1.12.1 bytecode); Chrome blurs a CSS box shadow with sigma 0.289 × blur + 0.5.
- **Rotation during a drag** (the supervisor rotated over adb while an injected pointer was held down):
  - Android does not cancel the touch: the drag goes on in the new orientation.
  - Portrait → landscape: positions were followed, and the drop after a move landed correctly. The first build kept the ghost's portrait width (720 px, past the screen edge) until release. Now the ghost takes the dragged row's current width when it is measured; the list is measured first.
  - Checked landscape → portrait: the ghost was re-sized to the portrait row (`m7-rotate2-after.png`).
- **Phone, 26 Oct (debug build; landscape, notch on the right, unless noted):**
  - **Setup:** debug fake clock Mon 09:05 paused, app volume 0 (the tones that played were silent). To-Do with 4 extra items (9 rows, scrolls) and Self with 4 extra (7 rows, scrolls). Gestures sent with `input swipe`, `input draganddrop` and `input motionevent DOWN/MOVE/UP`; recorded with `screenrecord` and read frame by frame.
  - **Vertical swipe on a handle:** scrolls the list both ways in To-Do and in Self; `app_data.json` unchanged.
  - **Indent:**
    - "Sabah" (a parent) → 1.1); its children stay at depth 1 (1.2, 1.3).
    - Outdent back: it re-adopts them.
    - The first item only rubber-bands and stays at depth 0.
    - Frames show the whole row following the finger, then moving at a quarter rate past the range: −68 px measured against −67.75 expected.
  - **Reorder:**
    - Sabah with its 2 children below "Find future room": the ghost reads "2) Sabah +2 sub-tasks", and "Find" slides up 3 rows (`m7-reorder-parent-held.png`). Measured in frames: slide about 200 ms, drop about 170 ms.
    - A child dragged to the top becomes a parent.
    - A parent dropped between another parent and its child stays a parent and adopts the child (as in the prototype).
    - With the list scrolled to the bottom, reorder and indent both land correctly.
    - In Self, the ghost reads "+2 sub-items".
    - Portrait drag works too (full-width ghost).
    - Dark palette (real Bedtime): ghost in `bg` black with the `divider` border (`m7-dark-held.png`).
    - Prototype reference: `m7-proto-lifted.png`, `m7-proto-moved.png`.
  - **Force-stop and relaunch:** To-Do and Self order and depths kept, in `app_data.json` and on screen.
  - **End state:** debug Reset (sample data; light, Soft chime, 60 %), fake clock Live, list menu shown. No crashes or ANRs (crash buffer, logcat, dropbox).
- **Phone facts:** adb dropped once during an install ("device offline", no mDNS service); the owner re-enabled Wireless debugging. Afterwards the phone was listed only as `adb-R8YY710D4SN-23fgJR (2)._adb-tls-connect._tcp`.

---

## M8 Settings and sound

**Goal:** the Settings screen and the block-change tone.

**Build**
- Settings per README › Screens › 4: two columns wrapping at 260 dp; Theme segmented control; "Weekly timetable" row (opens the M9 screen; a placeholder until then); tone radio rows that select and preview; volume slider with live gain and % label; Preview / Stop pill.
- Convert the three WAVs to OGG (ARCHITECTURE §7). `TonePlayer` and `BlockChime` per SPEC §3.1.

**Acceptance criteria**
- [x] Unit tests: gain curve; chime decisions (start = baseline, no sound; change to a real block plays after 350 ms; change to Free time or Bedtime is silent; a second change within 350 ms plays only for the last one).
- [x] The three sounds add less than 0.5 MB to the APK.

**Install on the phone and verify**
- [x] Each tone previews; Stop stops it; the slider changes loudness while playing. (adb and owner; loudness: see notes.)
- [x] Fake clock across Mon 07:59 → 08:00: the tone plays about 350 ms after the change. Crossing into Free time or Bedtime is silent.
- [x] With a podcast or music playing, it gets quieter during the tone and recovers after. (Owner.)
- [x] Light/Dark switch is correct on every screen built so far.
- [x] 30-minute memory check that includes at least one real block change. (Supervisor.)

**Notes**
- **Where things are:**
  - `sound/TonePlayer.kt` (in `AppContainer`). Each play gets a new MediaPlayer, prepared with `prepareAsync`, with AudioAttributes(USAGE_MEDIA, CONTENT_TYPE_SONIFICATION) and focus `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`. The player is released and focus abandoned when the tone ends, is stopped, or loses focus (`AUDIOFOCUS_LOSS` or `LOSS_TRANSIENT`). `playing` drives Preview / Stop.
  - `sound/BlockChime.kt` (in `AppContainer`, pure Kotlin) is the prototype's `tick()` rule. `MainActivity` feeds it from the same routine collector as `ScreenPolicy`, which runs only while the app is visible.
  - `ui/settings/SettingsScreen.kt`. `ui/components/SegmentedControl.kt`.
  - `ui/timetable/TimetableScreen.kt` is a placeholder for M9 (header and one line). `Screen.Settings` and `Screen.Timetable` are in `MainViewModel`; Back goes Timetable → Settings → Home.
  - New theme entries: RmType `settingsLabel`, `settingsLabelSelected`, `settingsDescription` and `control` (prototype 16/500, 16/700, 13/400, 14/500), and RmShapes `settingsCardRow` (4 dp).
- **Sounds (louder than the WAVs, SPEC §3.1 Loudness):**
  - Encoded from copies of `design/assets/sounds/*.wav` (design/ unchanged) with the Mac's ffmpeg 8.1.2: `ffmpeg -i <name>.wav -af "volume=<gain>" -c:a libvorbis -q:a 4 -map_metadata -1 <name>.ogg`. Plain gain only, so each tone keeps its character:
    - chime: +11.3 dB
    - pad: +11.4 dB
    - bowl: +3.8 dB
  - Measured with `ebur128=peak=true` (integrated / true peak / LRA / peak-to-loudness), WAV → final OGG:
    - singing_bowl: −22.3 LUFS / −5.0 dBTP / 21.4 LU / 17.3 dB → **−18.4 LUFS / −1.2 dBTP / 21.4 LU / 17.2 dB**
    - soft_chime: −25.3 / −14.1 / 3.7 / 11.2 → **−14.0 / −2.8 / 3.7 / 11.2**
    - warm_pad: −27.0 / −12.6 / 4.8 / 14.4 → **−15.6 / −1.1 / 4.8 / 14.5**
  - Two-pass `loudnorm` to −14 LUFS was not used: for the bowl (peak −5.0 dBFS, LRA 21.4 LU) and the pad, reaching −14 LUFS under −1 dBTP needs loudnorm's dynamic mode, which would also reshape their envelopes (supervisor, M8).
  - The chime stops at −14 LUFS (SPEC's "chime ≈ −14 LUFS, about +11 dB"); plain gain all the way to −1 dBTP would make it −12.2 LUFS.
  - **Rejected by ear: a louder bowl with a limiter on the strike.** The filter was `volume=7dB,alimiter=limit=0.8414:attack=5:release=50:level=false:latency=true`: −15.5 LUFS / −1.3 dBTP / 21.4 LU / 14.2 dB. The measurements looked clean:
    - The limiter acted only in the first 0.3 s (up to −3.3 dB). After that, peaks matched the original + 7 dB within ±0.2 dB, so the fade was not lifted.
    - The strike's energy above 1.5 kHz rose only from −41.5 to −41.1 dB relative to the strike.

    The owner still heard the strike as wrong, so the bowl is plain gain to −1.2 dBTP. It can't be louder without touching the strike.
  - Sizes: singing_bowl 52,519, soft_chime 50,341, warm_pad 57,759 = 160,619 bytes (157 KB). Each is mono, 44.1 kHz, 10.40 s, with no tags. Stored uncompressed in the APK, so they add exactly that. Release APK (unsigned): 2,920,686 bytes.
- **Default tone is Soft chime (SPEC D25, owner, M8):** after hearing the louder files, the owner found the Singing bowl and Warm pad too soft to notice a block change. `Settings.tone` now defaults to `CHIME`, so fresh installs, `SampleData` (and with it the debug Reset), and a saved file without a tone all get Soft chime. A saved choice is kept, because the file always holds the tone (`encodeDefaults`). Bowl and pad stay selectable with their current files; replacing them is M8b. The phone's debug app was set to Soft chime at 60 % without playing a sound: volume 0 first, select, Stop, then 60.
- **Decisions (supervisor, M8):**
  - The tone or a preview stops when the app is hidden (`onStop`), per SPEC D4 and §3.1 and CLAUDE.md's "nothing runs while the app is hidden". `onStop` also re-takes the chime baseline.
  - The slider's unfilled track uses `outline`; the README has no slider token.
- **Debug Reset** stops the tone and re-takes the baseline before it loads the sample data (M3 note).
- **Volume slider is not Material's Slider.** M3 1.4's slot-based `Slider` (custom thumb and track) in a `weight(1f)` row laid itself out 38 px wider than its slot (uiautomator: 516 px against the 478 px slot). Taps and drags in the last 19 px at each end did nothing, and the value saturated before the drawn ends. It is now a `Canvas` with `detectTapGestures` and `detectHorizontalDragGestures`, value = round((x − r) / (w − 2r) × 100) clamped to 0..100. It is drawn like Chrome's range input: an 8 dp track with a 1 dp `outline` border, `textPrimary` fill and a 16 dp thumb. A vertical swipe that starts on it still scrolls. On the phone, taps gave exactly 0/25/50/75/100 % at the expected x, both ends included.
- **Bug found on the phone:** inside `MediaPlayer().apply { }`, `stop()` resolved to `MediaPlayer.stop()`, not `TonePlayer.stop()`, so in the first build a tone that ended kept its player and the audio focus, and the button stayed on Stop. The player is now built without `apply`, and a comment explains why.
- **System bars:** a version that set `isAppearanceLightStatusBars` / `NavigationBars` from the app's palette was tried and removed. The flags were applied (`dumpsys window`: `mLastAppearance=LIGHT_STATUS_BARS LIGHT_NAVIGATION_BARS`), but bars swiped in while hidden still showed white icons on the system's grey scrim. So this phone styles transient bars itself, whatever the app asks. The M4 recheck "with the phone in light mode" needs a phone setting change and was not done. Bars on the light palette: grey scrim with white icons (`m8-bars-light.png`); on the dark palette: white icons (`m8-bars-dark.png`).
- **For M10:** creating a block that covers now plays the tone with no extra code, because the routine flow re-emits when the blocks change.
- **Unit tests:** 88 (8 new: 7 in `BlockChimeTest`, plus `SampleDataTest.newInstallsAndResetsUseSoftChime…`). Lint: 0 errors, the 2 accepted M1 warnings. `assembleRelease` builds.
- **Phone, 25 Oct (debug build, landscape, notch on the right, phone unplugged and in dark mode):**
  - Settings matches the prototype (`temp/screenshot/m8-proto-settings.png` against `m8-settings-light.png`). Measured in dp: back button centre 38.1 / 40.3 (prototype 38 / 40); title x 69.9, y 26.1 (70 / 26.5); "Appearance" x 37.9, y 72.5 (38 / 72); first tone name y 103.5 (103.3). The second column starts at 441.5 dp, because the phone is 853 dp wide and has 34 dp of notch padding.
  - Preview, read from `dumpsys audio` and `dumpsys media.audio_flinger`:
    - Focus `GAIN_TRANSIENT_MAY_DUCK` with USAGE_MEDIA / CONTENT_TYPE_SONIFICATION; the player started 87 ms after the focus request.
    - Track gain 0.324 at 60 % (−9.8 dB), 0.225 at 50 %. Dragging the slider while it played changed it at once to 0.900 at 100 % and 0.056 at 25 %.
    - At the end of the file: `playback complete`, `release` and `abandonAudioFocus` within 18 ms, and the button returned to "Preview sound".
    - Stop 2 s in: released 14 ms after the tap, focus abandoned, no active track.
    - Home during a preview: released and focus abandoned 2 s later; back on Settings the button read "Preview sound".
    - Each tone row selected its tone (saved as `pad`, `chime`, `bowl`) and started its preview, with one active track each time.
  - Chime, with the fake clock paused:
    - A jump from Mon 07:59 to 08:00 requested audio focus 384 ms after the touch on the slider; 378 ms on the final build. That is the 350 ms wait plus touch handling.
    - Sat 11:59 → 12:00 (Free time) and Mon 22:59 → 23:01 (Bedtime): no focus request. At Bedtime the screen dimmed to 0.05.
  - At 60× across Mon 08:00: one request, about 0.49 s after starting the clock, and none at 08:01–08:04.
  - At 600× from Mon 05:58 to 23:51: 9 requests, one per weekday block start (06:00, 07:00, 08:00, 12:00, 13:00, 17:30, 18:30, 20:00, 22:30), spaced 6.0 s per clock hour. Nothing at 23:00.
  - Debug Reset: the current block's id changed (8da4d3fc → 0a91b7ce) and no tone played.
  - Hidden at 600× for 10 s, then back: no request while hidden and none on return. The next one came at the next block start.
  - Light / Dark: home, To-Do, Grocery, Settings, the timetable placeholder and the debug screen in both palettes (`m8-<screen>-light.png` / `-dark.png`). The setting is saved at once (`"theme": "dark"` in `app_data.json`).
  - Idle redraw on the live dashboard (real Bedtime, dim), 23:20–23:23: 4 framestats frames ("Total frames rendered" 8). Two of them came 60.06 s and 60.00 s after the one before; the extra frame is the re-dim 30 s after the last touch.
  - No crashes or ANRs (crash buffer, logcat and dropbox), also after the final install on 26 Oct. Battery simulation was not used. The debug app was left on the live dashboard with light theme, Soft chime, 60 %.
- **Owner, 25 Oct:** all three tones are audible; the slider changes loudness while a tone plays; Stop is instant; a podcast is ducked during the tone and recovers. Settings in portrait (checked by the supervisor over adb): the sections stack in one column and nothing is cut off. **At 100 % with the phone's media volume high, the tone was not loud enough**, so the files were made louder (Sounds, above). Owner, 26 Oct: all three louder tones are loud enough; the limited bowl's strike sounded wrong, so it was replaced by the plain-gain bowl. After that, the bowl and the pad were still too soft for a block change, so the default became Soft chime (SPEC D25) and the two tones get replaced in M8b.
- **Gate (26 Oct 2026, on `main` with M6 and M8 merged):** clean `assembleDebug lintDebug testDebugUnitTest assembleRelease` passes (107 tests, 0 failures; lint 0 errors, the 2 accepted warnings). Installed; Settings and To-Do edit mode checked on the phone; no crashes or ANRs. Memory, 30 minutes (00:16–00:46) with the debug clock started at Mon 07:54 at 1× so the 08:00 Work Session1 start played the tone (audio focus 00:21:33, released 00:21:43; app volume 0 at night): 98,366 KB at minute 5, 96,158 KB at minute 30, flat. Afterwards: debug clock Live, Soft chime at 60 %.

---

## M8b Two replacement tones

**Goal:** replace Singing bowl and Warm pad, which are too soft to notice a block change, with two crisper sounds the owner picks (SPEC D25). Soft chime stays and is the default.

**Build**
- Make 4–6 candidate sounds, each about 1–3 s, clearly audible on the phone speaker at 60 %: synthesised (as the handoff's tones were, from the prototype's synth) or taken from sources whose licence allows use in the app (for example AOSP's Apache-2.0 notification sounds). Record each source and licence.
- Let the owner listen and pick two, with names and one-line descriptions for the Settings rows.
- Replace the two OGGs and their Settings labels; keep the saved `tone` keys working (an old saved key maps to its replacement). Loudness as SPEC §3.1 (plain gain, true peak ≤ −1 dBTP), similar to Soft chime.

**Acceptance criteria**
- [x] The owner picked both sounds by ear; sources and licences are in the notes.
- [x] The sounds still add less than 0.5 MB in total. (70 KB.)
- [x] A saved `bowl` or `pad` setting still loads and plays its replacement. (Unit test for both; on the phone with `bowl`, see notes.)

**Install on the phone and verify**
- [x] Each tone previews from Settings; the block-change tone plays the selected one.

**Notes**
- **Owner's pick (26 Oct, by ear on the phone, from six candidates):**
  - **Marimba** ("Wooden notes, rising") replaces Singing bowl: row 1, and a saved `bowl` loads as Marimba.
  - **Steel drum** ("Bright two-note ping") replaces Warm pad: row 3, and a saved `pad` loads as Steel drum.
  - Soft chime stays in row 2 and is still the default.
  - Not picked: Doorbell, Kalimba, Electric piano, Harp.
- **Why the bowl and the pad were missed:** a phone speaker loses most of the sound below about 700 Hz. Loudness after a 700 Hz high-pass (`highpass=f=700,highpass=f=700,ebur128`), a rough speaker model: Soft chime −16.1 LUFS, Singing bowl −36.9, Warm pad −38.9. So every candidate keeps its notes at 700–1320 Hz.
- **Source and licence:** synthesised for this project by `make_candidates.py` (Python 3.9 standard library, ffmpeg 8.1.2). No third-party audio, so there is no licence to record. The script, all six candidates (OGG and WAV) and `candidates.txt` are in `temp/orchestration/m8b-candidates/`, which git ignores. A re-run gives identical decoded audio.
- **Synth (the prototype's `note()` idea):**
  - Each note is a sum of exponentially decaying sines with a raised-cosine attack, plus a few ms of low-passed noise for the mallet.
  - A small mono Freeverb-style room (6 combs, 2 allpasses) is mixed in at −13 dB relative to the dry signal.
  - A 150 ms fade at the end.
  - Marimba: G5, C6, E6 at 0, 0.15 and 0.30 s (velocity 0.8, 0.85, 1.0). Partials 1× (τ = 0.55 s × √(659 Hz / f)), 3.93× (0.30, τ 90 ms) and 10× (0.05, τ 20 ms); 1.5 ms attack.
  - Steel drum: G5 at 0 s, C6 at 0.18 s (0.85, 1.0). Partials 1× (τ 0.65 s), 2× (0.35, τ 0.45 s, 12 ms attack) with a +1.5 Hz twin (0.25) for shimmer, 3× (0.22, τ 0.22 s) and 4× (0.07, τ 0.10 s).
- **Levelling (SPEC §3.1):** plain gain only, to −14 LUFS, or less if the OGG's true peak would pass −1 dBTP. Encoded with libvorbis `-q:a 4`, mono, 44.1 kHz, `-map_metadata -1`.

  | File | Length | Integrated | True peak | Above 700 Hz | Bytes |
  |---|---|---|---|---|---|
  | `marimba.ogg` | 2.2 s | −14.0 LUFS | −1.0 dBTP (−1.04 at 4× oversampling) | −15.5 LUFS | 9,817 |
  | `soft_chime.ogg` (unchanged) | 10.4 s | −14.0 LUFS | −2.8 dBTP | −16.1 LUFS | 50,341 |
  | `steel_drum.ogg` | 2.2 s | −14.0 LUFS | −1.4 dBTP | −15.6 LUFS | 11,540 |

  Loudness range reads 0.0 LU for the 2.2 s files, because LRA needs at least 3 s of audio.
- **Size:** the three sounds are 71,698 bytes, down from 160,619, and stored uncompressed in the APK. Release APK (unsigned): 3,294,598 bytes.
- **Files:** `res/raw/singing_bowl.ogg` and `warm_pad.ogg` are removed; `design/` keeps the original WAVs.
- **Saved keys:**
  - `Tone` holds its saved key (`marimba`, `chime`, `steel_drum`). The enum order is the Settings row order.
  - `ToneSerializer` in `data/Model.kt` writes the key, and also reads `bowl` as Marimba and `pad` as Steel drum. The next save writes the new key.
  - It is a custom serializer because `@JsonNames` is still `@ExperimentalSerializationApi` in kotlinx.serialization 1.12.0 (checked in the jar).
  - An unknown key still makes the file unreadable (corruption handler), as with the generated enum serializer before.
- **Unit tests:** 109. Two are new in `AppDataJsonTest`:
  - `everyToneIsSavedUnderItsOwnKeyAndReadBack`
  - `aSavedBowlOrPadLoadsAsItsReplacementInsteadOfResettingAllData`
  
  Lint: 0 errors, the 2 accepted warnings. `assembleRelease` builds.
- **Phone, 26 Oct (debug build, landscape, real Bedtime):** all checks were silent, with the app volume at 0 before any sound. Which file played was read from `dumpsys media.extractor`: its source is the APK at the OGG's offset and length (9,817 = marimba, 11,540 = steel drum, 50,341 = chime).
  - **Old key:** with the app stopped, the saved `"tone":"chime"` was changed to `"bowl"` with `run-as sed` (supervisor's OK). On launch the file loaded, with the lists kept and no corrupt copy. Settings showed Marimba selected (`m8b-settings-bowl-key.png`), and the next save wrote `"tone":"marimba"`.
  - **Previews:** each row played its own file, with focus `USAGE_MEDIA/CONTENT_TYPE_SONIFICATION`:
    - Marimba: 2,200,000 µs; focus 01:05:55.367 → abandoned 01:05:57.860.
    - Steel drum: 2,200,000 µs; focus 01:06:10.631 → abandoned 01:06:13.115.
    - Soft chime: 10,400,000 µs; track gain `-inf` at volume 0. Stop released it 2.45 s in.
  - **Block-change tone,** debug clock paused:
    - Steel drum selected, Mon 01:06 (Bedtime) → 09:01 (Work Session1): focus 419 ms after the touch down, file 11,540.
    - Marimba selected, 09:01 → 12:31 (Lunch): focus 398 ms after the touch down, file 9,817, released 2.45 s later.
    - Back to Live (Bedtime): no focus request.
  - **No crashes or ANRs:** crash buffer, logcat and dropbox. Left on the live dashboard with Soft chime, 60 %, light theme (`m8b-settings-final.png`, `m8b-home-final.png`). Battery simulation was not used.

---

## M9 Weekly timetable

**Goal:** the timetable screen and the create gesture (README › Screens › 5).

**Build**
- Day chips with today's accent chip; 46 dp hour gutter; 7 columns; 44 dp per hour; today's column fill; now line with dot; auto-scroll so now − 1.5 h is at the top.
- Blocks with title and `6:00am-11:30am` range; the current block in accent.
- Long-press 300 ms on empty time (10 ms haptic), drag with 15-minute snap, bounded by neighbouring blocks, dashed ghost with range label; 15 minutes or less becomes 60 (bounded). Release opens the editor (placeholder until M10). Tapping a block opens it too.

**Acceptance criteria**
- [x] Unit tests: create snapping and bounds, the ≤ 15 min → 60 min rule, range format (including `12:00am` for a 24:00 end).

**Install on the phone and verify**
- [x] Create gesture works; a plain vertical swipe still scrolls.
- [x] The now line moves each minute; today's column follows the fake clock's day.
- [x] Portrait timetable is usable.

**Notes**
- **Where things are:**
  - `domain/BlockRules.kt` holds the create gesture's rules, ported from the prototype's `colDown`/`colMove`/`colUp`: `createAnchor`, `dragRange` and `releaseRange`, with `TimeRange` and `CreateAnchor`. M10 adds validation and group saving to the same file.
  - `TimeFormat` gains `blockRange` (`6:00am-11:30am`), `hour` (`7 AM`), `time` (`7:00 AM`, the prototype's `lbl`) and `ghostRange` (`7:00 AM – 8:00 AM`).
  - `ui/timetable/TimetableScreen.kt` replaces M8's placeholder.
  - `ui/editor/BlockEditorPlaceholder.kt` holds `EditorRequest` (`New(day, range)` or `Edit(blockId)`) and a placeholder card that shows what the editor would open with. M10 replaces the card. The request is a `remember` in the timetable: it survives rotation (no recreation) and is dropped when the timetable closes, as the prototype's `closeRoutine` clears the sheet.
  - `MainViewModel` gains `timetableBlocks`, `now` and `currentBlockId`, all from the one shared minute ticker. `now` uses `WhileSubscribed(replayExpirationMillis = 0)`, so a timetable opened later never scrolls to a stale minute.
- **One minute redraw:** only the now line reads the minute, in its `offset { }` lambda, so a tick moves it without recomposing the grid. `today` is a `derivedStateOf` (only the chips and the two affected columns recompose at midnight). The current block's id changes only at block changes. Each day's block list keeps its instance until the blocks change (`groupBy` in a `remember`), so unchanged columns skip.
- **Create gesture** (`detectCreate`, a `pointerInput` on each day column):
  - The finger must stay within 8 dp for 300 ms.
  - Moving first leaves the events to `verticalScroll`, so a swipe scrolls; a consumed event also cancels.
  - After the long press every move is consumed, so the grid can't scroll while drawing.
  - The ghost is a state in its column, set only when the snapped range changes.
  - A press on a block returns at once (`createAnchor` is null), so the block's own tap opens it. A hold then drag on a block scrolls, as in the prototype (`touch-action: pan-y`).
- **Haptic:** a 10 ms `VibrationEffect.createOneShot` with `USAGE_TOUCH`, which needs `android.permission.VIBRATE` (granted at install, nothing shown to the user). The README asks for exactly 10 ms. `USAGE_TOUCH` follows the phone's touch-vibration setting. The helper is private to the timetable; M7's 12 ms pick-up can share it once both are merged. (Supervisor accepted, M9.)
- **Editor placeholder** is drawn in the activity's window, not as a Compose `Dialog`, so at Bedtime `ScreenPolicy` still swallows the first touch on it (M4 note). Its scrim takes every touch.
- **Decisions where the README is silent (supervisor accepted, M9):**
  - Ghost label `7:00 AM – 8:00 AM`, the prototype's `lbl(s) – lbl(e)`. The README says only "range label".
  - Hour labels use `outlineStrong`: the prototype's #8e8e8e is the grey the README maps to `outlineStrong` for the drag handle and the "Add category" border.
  - The line under the day chips (prototype #ebebeb) uses `gridLine`.
  - The ghost's fill is `textPrimary` at 5 % opacity (prototype rgba(0,0,0,.05)), with a 1.5 dp dashed `textPrimary` outline.
- **Ghost dashes:** Chrome draws the prototype's `1.5px dashed` as about 3 dp dashes with 2 dp gaps (measured at 4× zoom), so the ghost uses 3 / 2 dp. A first try at 3 × stroke for both (4.5 / 4.5 dp) looked sparser than the prototype on the phone.
- **Bug found on the phone:** the first build opened at 12 AM instead of now − 1.5 h. The grid had no height until the blocks loaded, so the scroll on open was cut to 0. The scroll content now has its full 1056 dp height from the first frame.
- **Unit tests:** 123 (16 new: `BlockRulesTest` 12, `TimeFormatTest` +4). Lint: 0 errors, the 2 accepted M1 warnings. `assembleRelease` builds: 3,399,958 bytes, 65.6 KB more than d93abe0.
- **Phone, 26 Oct (debug build, landscape, notch on the right; fake clock Mon 09:30 paused unless noted; gestures sent with `input motionevent` DOWN, a 0.6 s wait, MOVE steps, UP):**
  - **Layout against the prototype in Chrome at the same fake time** (`temp/screenshot/m9-proto-timetable.png` vs `m9-phone-light.png`), dp, phone / prototype:
    - title y 22.4 / 22.5
    - chip text y 70.4 / 70
    - 8 AM label centre y 108.8 / 108.0, right edge 48.0 / 48.0
    - block text x 67.2 / 67.0, y 113.1 / 112.0

    Columns are 109 dp against 115, because the phone is narrower and pads 34 dp for the notch, so the ghost label wraps onto two lines.
  - **Create (Sat, free 12:00–14:00):**
    - Hold then drag down: ghost `12:15 PM – 1:30 PM`, then the placeholder "Sat, 12:15 PM – 1:30 PM".
    - Hold without a drag: 12:30–1:30 PM. At 13:30 the hour is cut at Errands: 1:30–2:00 PM.
    - Dragging past Errands stops at 2:00 PM. Dragging up past Work Session1 stops at 12:00 PM (12:00–1:15 PM).
    - `dumpsys vibrator_manager`: a one-shot `Step=10ms`, usage TOUCH, from the app.
    - One run showed no placeholder in the UI dump. Repeated runs (6 + 4) all worked, and the missing one is most likely a failed `uiautomator dump`.
  - **What must not create:**
    - A vertical move before 300 ms scrolls and creates nothing; a sideways one does nothing; a 3 dp wobble still creates.
    - Swipes on empty time and on blocks scroll, with no editor. A hold then drag on a block scrolls 185 px (200 minus the touch slop).
    - A tap on a block opens "Edit block" with its group's days (Work Session1: Mon–Fri, 8:00 AM – 12:00 PM).
    - ✕ and Back close the placeholder and stay on the timetable.
  - **Now line, fake clock at 1×:** it moved at each fake minute boundary (dot centre 337 → 339 → 340 → 342 → 343 px; one minute is 1.375 px).
  - **Today follows the fake clock:**
    - After a jump to Wed 10:04, the Wed chip is in the accent, Wed's Work Session1 is current and the now line is on Wednesday (`m9-phone-wed.png`).
    - With the timetable left open across Wed 23:59 → Thu at 1×, the chip moved to Thu.
  - **Dark and Bedtime:**
    - Dark theme: blocks in `textPrimary` with black text, the current block in mint, a light dashed ghost, the placeholder in dark (`m9-phone-dark*.png`).
    - At Mon 11:28 PM: `Display Brightness=0.05` set by our activity, dark palette, no block in the accent, and the grid opens at its bottom (`m9-phone-bedtime.png`).
    - While dim, the first tap on a block only brightened; the second opened "Edit block: Wind down".
  - **Idle redraw** with the timetable open at 1×:
    - 3 minutes: "Total frames rendered" 6, i.e. 3 frames (the phone counts each frame twice).
    - 3 min 51 s with the dot sampled every 15 s: 4 dot moves and "Total frames rendered" 8, i.e. 4 frames.
    - In both runs framestats lists the drawn frames 60.0 s apart, plus one row flagged 8 (skipped: nothing drawn) about 20 ms after the last drawn frame, and one fewer drawn frame than ticks. The cause wasn't found.
  - **Rotation (the supervisor rotated over adb to ROTATION_0 and back, then restored the defaults):**
    - An open "New block" placeholder survived to portrait; an open "Edit block" placeholder survived back to landscape, with the timetable and its scroll.
    - Portrait matches the prototype's layout (`m9-proto-portrait.png`). Columns are about 45 dp, so titles shorten to "W…" and the ghost's label wraps and is clipped, as the prototype's would.
    - In portrait, create (Sat 12:15 PM – 1:30 PM), swipes both ways and a tap on a block (Edit "Personal") all work.
  - **Afterwards:** no crashes or ANRs (`logcat -b crash`, `ANR in com.jsk`). The debug clock was set back to Live and the app left on the dashboard, light theme. No memory check at M9 (CLAUDE.md).
- **Gate (26 Oct 2026, on `main` after M8b):** clean `assembleDebug lintDebug testDebugUnitTest assembleRelease` passes (125 tests, 0 failures; lint 0 errors, the 2 accepted warnings). Installed; the timetable opens from Settings at night with the now line and the dark palette; no crashes or ANRs. No memory check at M9.

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
- [x] No lint icon warnings.

**Install on the phone and verify**
- [x] Icon looks right in the app drawer and in recent apps, with the phone in dark and in light mode (home screen uses the same renderer). Themed icons: not checked (see notes).

**Notes**
- **Design (SPEC D24):** concept C, "07:05" in Google Sans Flex (weight 600, tabular figures) above the dashboard's progress bar, chosen from three concepts (A progress dial, B lists + bar, C clock + bar). The icon is the inverse of the phone's theme: light phone → dark icon (bg #000000, mark #EDEDED, mint #4DE0A6, track #595959); dark phone → light icon (bg #E6E6E6, mark #0F0F0F, cobalt #2952E6, track #AAAAAA). Debug = the same icon plus a `bug_report` badge (Material Symbols Rounded, filled): a 10 dp accent disc at (71, 71) with a 14 dp bug in the palette's onAccent colour; the disc covers the track, so the debug icon has no track or end-dot.
- **Theme-following:** night-qualified resources: `values(-night)/ic_launcher_background.xml` (colour) and `drawable(-night)/ic_launcher_foreground.xml`; the debug foregrounds are swapped by name in `src/debug/res`. On this phone the launcher swaps the icon at once when dark mode is switched (checked both ways over adb on 25 Oct).
- **Shape:** the launcher's own rounded square, full-bleed. A round disc on a clear background was tried: this phone's launcher masks every adaptive icon with its squircle and fills clear areas inside the mask with black, which showed as black corners over light surfaces (recent apps). The owner chose the rounded square. Icons show at about 90 % brightness in dark mode. The phone has a Galaxy Themes icon pack applied (`com.MPC113_Icons4a.appiconpack`).
- **Geometry:** digits from the font's weight-600 outlines (fonttools, `tnum`, 2000 UPM) converted to dp paths with 2 decimals, zero-length curve segments removed; ink 50 dp wide, centred at x 54, baseline y 52, tracking −77 font units. Bar: 8 dp pills centred at y 64.5, fill x 29–59, 4 dp gap, track x 63–79, end-dot r 1.6 at x 75. The badge reaches 34 dp from the centre (1 dp outside the 66 dp safe zone, inside the 72 dp visible area).
- **Monochrome layer:** the same shapes in white, track at alpha 0.38, no end-dot; in debug the badge is a disc with the bug cut out (even-odd fill). Themed icons were not checked: turning them on is a One UI wallpaper setting that interacts with the installed icon pack, and the owner did not ask for it.
- **Old wizard files:** the 10 webp bitmaps and the wizard's background drawable are deleted; with minSdk 31 the vector icon is always used (`aapt2`: `mipmap-anydpi-v21/ic_launcher.xml` at every density). Lint `VectorPath` reports only from 3000 path characters; the longest path is 916.
- **Generator:** the icon XML is generated by a Python script (`gen_icon.py`) kept outside the repo, in the supervisor's `temp/orchestration/icon-generator/` (git-ignored), with the glyph outlines and the bug SVG.
- **Release icon:** approved by the owner from the preview; checked on the phone in M12.
- **Gate (25 Oct 2026):** clean `assembleDebug lintDebug testDebugUnitTest assembleRelease` passes in the M11 worktree (55 tests, 0 failures; lint 0 errors, the 2 accepted warnings). No crashes or ANRs on the phone. No memory check at M11.

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
