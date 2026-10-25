# Routine Manager: architecture

How the app is built to meet [design/README.md](../design/README.md) and [SPEC.md](SPEC.md) with the least code, no background work, one redraw per minute and flat memory.

## 1. Overview

- One Gradle module (`:app`), one activity (`MainActivity`), Jetpack Compose UI.
- No dependency-injection framework. `RoutineApp : Application` builds a small `AppContainer` that holds the few singletons (repository, clock, tone player).
- No navigation library. The current screen is a small sealed type held in a ViewModel; system Back is handled with `BackHandler`.
- Nothing runs while the app is hidden: no services, alarms, WorkManager jobs, manifest broadcast receivers or wake locks.

### Dependencies

Add only these (ask before adding anything else):

| Library | Why |
|---|---|
| `androidx.datastore:datastore` (core, not `-preferences`) | Stores the one JSON document |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` + Kotlin serialization Gradle plugin | JSON encode/decode of the data model |
| `androidx.lifecycle:lifecycle-runtime-compose` | `collectAsStateWithLifecycle`, so flows stop when the app is hidden |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | ViewModels in Compose |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` (tests only) | Testing the minute ticker and debounced saves |

Deliberately not used: Room, Hilt/Koin, Navigation, WorkManager, AlarmManager, Accompanist, `material-icons-extended`, image loaders.

Versions: AGP 9.4.1 (built-in Kotlin), Kotlin 2.4.21 (SPEC D23), Gradle 9.6.0, Compose BOM 2026.09.00. The serialization plugin version must match the Kotlin version.

## 2. Package layout

```
com.jsk.routinemanager
├── RoutineApp.kt              Application; builds AppContainer
├── MainActivity.kt            window setup, ScreenPolicy, setContent
├── data/
│   ├── Model.kt               AppData, Settings, OutlineItem, GroceryCategory, GroceryItem, Block (@Serializable)
│   ├── AppDataSerializer.kt   DataStore Serializer (JSON)
│   ├── DataRepository.kt      StateFlow<AppData?>, update { }, debounced save, flush()
│   └── SampleData.kt          port of the prototype's sample()
├── domain/                    pure Kotlin: no Android imports, all unit-tested
│   ├── RoutineResolver.kt     resolve(blocks, day, minuteOfDay) -> RoutineState
│   ├── OutlineOps.kt          subtree end, normalize, indent, insert after, delete with promote, move subtree, cleanup
│   ├── GroceryOps.kt          toggle, add, delete, cleanup (trim, drop empty, "Untitled")
│   ├── Numbering.kt           "1)", "2.1)" labels
│   ├── DragMath.kt            indent range + rubber band, drop index and depth
│   ├── BlockRules.kt          validation (ordered messages), save/delete group, wheel <-> minutes, create-gesture snapping
│   ├── TimeFormat.kt          "07:05" + "AM", "4hr 30min", "6:00am-11:30am", "7 AM"
│   ├── ScreenRules.kt         keep-screen-on and dim decisions
│   └── ToneGain.kt            (volume/100)² × 0.9
├── time/
│   ├── AppClock.kt            interface + SystemAppClock
│   └── MinuteTicker.kt        Flow<LocalDateTime>: now, then every minute boundary
├── sound/
│   ├── TonePlayer.kt          MediaPlayer + audio focus: play, stop, setVolume
│   └── BlockChime.kt          watches the routine key, 350 ms rule, baseline on start
├── screen/
│   └── ScreenPolicy.kt        hidden bars, keep-screen-on, Bedtime dim, tap-to-brighten
└── ui/
    ├── theme/                 RmColors (light/dark tokens), RmType (Google Sans Flex), shapes, icons
    ├── components/            shared pieces: 48 dp icon button, badge, radio dot, underlined field, segmented control
    ├── home/                  HomeScreen (landscape split / portrait), ClockPanel, ListMenu
    ├── lists/                 ListDetail, OutlineList (view, edit, drag), GroceryList
    ├── settings/              SettingsScreen
    ├── timetable/             TimetableScreen, grid, create gesture
    └── editor/                BlockEditorDialog, WheelPicker

src/debug/java/com/jsk/routinemanager/debug/    DebugScreen, FakeAppClock, DebugEntry (real)
src/release/java/com/jsk/routinemanager/debug/  DebugEntry (does nothing), so release has no debug code
```

## 3. State management

Data flows one way: repository and clock → ViewModels → UI state → composables; user actions → ViewModel → domain function → `repository.update`.

- **Saved data:** `DataRepository.data: StateFlow<AppData>` is the single source of truth.
- **Time:** `MinuteTicker` gives a `StateFlow<LocalDateTime>` that changes once per minute.
- **ViewModels** (survive rotation):
  - `MainViewModel`: current screen (Home / Settings / Timetable / Debug), open list, edit mode, and the dashboard state = `combine(blocks, minute)` → `DashboardUi` (clock text, date, `RoutineState`, progress fraction, LEFT text), plus list-menu badges.
  - Add a separate ViewModel only for a screen with its own draft state: list editing (focus target, drag session) and the block editor (dialog draft). Don't split further.
- UI models are immutable data classes. Compose's strong skipping (default in Kotlin 2.x) keeps unchanged rows from recomposing.
- Process death is not handled specially: unsaved UI state (open list, open dialog) may be lost; saved data is not.

### Recomposition rules (efficiency requirement)

1. Only composables that show time read the minute state: the clock panel, and the timetable's now line and today column. The list panel must not read it. Check with Layout Inspector's recomposition counts (M3).
2. Values that change every frame (drag offsets, rubber band, ghost position, wheel scroll) are read only inside `Modifier.offset { }`, `graphicsLayer { }` or draw lambdas, so a drag never recomposes the rows.
3. No infinite animations, no frame loops, no `LaunchedEffect` that polls.

## 4. Persistence

- `DataStore<AppData>` with a JSON `Serializer` (`Json { ignoreUnknownKeys = true; encodeDefaults = true }`). File: `files/datastore/app_data.json`.
- First start with no file: write `SampleData.create()` (SPEC D8).
- `DataRepository` keeps the latest value in a `MutableStateFlow<AppData?>` the UI reads. It is `null` until the file has been read; screens wait for it, because a stand-in empty `AppData` would resolve to Bedtime on the first frame (dimming the screen in M4, becoming the tone baseline in M8). `update(transform)` sets it immediately, then restarts a 500 ms debounce that calls `dataStore.updateData { latest }`. `flush()` writes immediately; `MainActivity.onStop` calls it.
- DataStore writes to a temporary file and renames it, so a crash during a write doesn't corrupt the saved file.
- Unreadable file: a `ReplaceFileCorruptionHandler` copies it to `app_data.corrupt-<epochMillis>.json`, logs an error and starts from sample data.
- `schemaVersion = 1`. If the model changes after release, bump it and migrate in the serializer.
- Manifest: `android:allowBackup="false"`; remove the `backup_rules.xml` and `data_extraction_rules.xml` references (SPEC D14).

## 5. Time and the minute redraw

- `AppClock.now()` returns the local date-time in the phone's time zone. Release uses the system clock. Debug can swap in `FakeAppClock(simAnchor, realAnchor, speed)`, the prototype's `simNow()`.
- `MinuteTicker`: emit now; then loop: work out the time to the next wall-clock minute from `clock.now()`, convert it to real time (divide by the fake clock's speed; wait while paused), `delay()`, emit. It recomputes every loop, so drift can't build up.
- Collected with `repeatOnLifecycle(Lifecycle.State.STARTED)`, so it does no work while the app is hidden.
- While STARTED, a receiver registered in code (not the manifest) for `ACTION_TIME_CHANGED`, `ACTION_TIMEZONE_CHANGED` and `ACTION_DATE_CHANGED` makes the ticker emit at once. It is unregistered on STOP.

## 6. Routine resolution

- `RoutineResolver.resolve` ports the prototype's `routine()`: blocks for the weekday (Mon = 0), sorted by start. Current = `start ≤ now < end`; Next = first block with `start ≥ current.end`, else `-`; between two blocks → Free time from the previous end to the next start; otherwise Bedtime.
- It takes minute-of-day **with seconds** (a `Double`), as the prototype does, so LEFT = `ceil(end − now)` is right on the first frame after launch as well as on minute ticks.
- `RoutineState.key` is the block id, `gap:<previous block id>` or `bed`. `BlockChime` and `ScreenPolicy` use it.

## 7. Sound

- Tone files: `res/raw/singing_bowl.ogg`, `soft_chime.ogg`, `warm_pad.ogg`, converted from `design/assets/sounds/*.wav` with the Mac's ffmpeg (`-c:a libvorbis -q:a 4`; mono, 44.1 kHz, 10.4 s). The WAVs stay in `design/`.
- `TonePlayer` creates a `MediaPlayer` for each play with `AudioAttributes(USAGE_MEDIA, CONTENT_TYPE_SONIFICATION)`, requests `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` (so other audio is lowered), sets `setVolume(gain, gain)`, and releases the player and audio focus when the sound ends or is stopped. `setVolume` also works while playing. It exposes `playing: StateFlow<Boolean>` for the Preview/Stop button.
- Why `MediaPlayer` and not `SoundPool`: SoundPool keeps each decoded tone in memory (~0.9 MB each). MediaPlayer streams the file and is released after each play, so no audio memory is held between chimes. A 10-second sound doesn't need SoundPool's low start-up delay.
- `BlockChime` runs while STARTED. On start it takes the current key as the baseline. On a change to a real block it waits 350 ms, checks the key is unchanged, and plays the selected tone (SPEC §3.1).

## 8. Screen and power (`ScreenPolicy`)

- **Orientation:** manifest `android:screenOrientation="sensor"`. Android's documentation states the sensor is used even if the user has locked rotation (SPEC D2).
- **Rotation without recreation:** manifest `android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize|keyboardHidden"`. Compose re-lays out; nothing reloads.
- **Full screen:** edge-to-edge (enforced for targetSdk 35+). `WindowInsetsControllerCompat.hide(systemBars())` with `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`. `layoutInDisplayCutoutMode = ALWAYS`; content pads by max(design padding, cutout inset) on the notch side (SPEC D18).
- **Keep screen on:** at start and at each minute tick, read the sticky battery status with `registerReceiver(null, IntentFilter(ACTION_BATTERY_CHANGED))`. This returns the last battery broadcast without registering a receiver. Then set or clear `FLAG_KEEP_SCREEN_ON` per SPEC §3.3.
- **Bedtime dim:** when `RoutineState` is Bedtime, set `window.attributes.screenBrightness = BEDTIME_BRIGHTNESS` and force the dark palette; otherwise `BRIGHTNESS_OVERRIDE_NONE` and the chosen theme. Never use 0.0, which turns some screens fully off.
- **Tap to brighten:** `MainActivity.dispatchTouchEvent`. While dim, swallow the whole gesture that starts with `ACTION_DOWN`, remove the brightness override and start a 30 s timer. Any touch restarts the timer. When it ends and it is still Bedtime, dim again.
- **Decisions are pure functions** in `domain/ScreenRules.kt` (keep on? dim?) so they are unit-tested; `ScreenPolicy` only applies them to the window.

### Power budget

| State | Work done |
|---|---|
| App hidden | None |
| App visible, idle | One coroutine wake-up per minute; Compose draws one frame per minute, only the changed area |
| Touch, drag, scroll | Normal frame rendering until the gesture settles |

Recommended phone settings (written up in M12's install guide): Motion smoothness = Standard (60 Hz); "Protect battery" on, because the phone is plugged in most of the time.

## 9. Theme, fonts, icons

- `RmColors` holds every README colour token; `LightColors` and `DarkColors` hold the final values. Provided through a `CompositionLocal`. `MaterialTheme` gets a colour scheme built from the same values, so stock Material components never bring their own colours. No hard-coded colours outside `ui/theme`.
- **Google Sans Flex** (SIL Open Font License 1.1): download the variable font from Google Fonts. With fonttools in a temporary virtual environment (outside the repo), keep only the weight axis (pin the others to their defaults) and subset to Latin-1 plus the punctuation the UI uses (– “ ” ×; the ✎ in the empty-state text is the `edit` icon, not a character), keeping the `tnum` feature (`pyftsubset --layout-features+=tnum`). Ship `res/font/google_sans_flex.ttf` and its licence as `assets/google_sans_flex_OFL.txt` (`res/font` can't hold a `.txt`). Weights 400/500/600/700 via `FontVariation.Settings(FontVariation.weight(n))`. All numbers use `fontFeatureSettings = "tnum"`. Every `RmType` style sets an explicit line height (1.252 em, the font's own line, unless the README gives one) with `LineHeightStyle(Center, Trim.Both, Mode.Tight)`, because Compose's fallback line spacing and default line-height mode make text taller than in the prototype (M3 notes). Screens use `RmType` styles only.
- **Icons:** the Material Symbols Rounded icons listed in the README, as vector drawables (`res/drawable/ic_<name>.xml`) in the fill/weight variants the README gives. Take the SVGs from Google's `material-design-icons` GitHub repo and copy their path data into VectorDrawable XML. Their viewBox is `0 -960 960 960`, so the path goes inside a `<group android:translateY="960">`. Apache 2.0. No icon font.

## 10. Lists, gestures, timetable

- Lists are short (tens of items), so they are a plain `Column` in `verticalScroll`, not a `LazyColumn`. Every row is measured, so drag maths can use real row positions (as the prototype does), and there is no lazy-row reuse to fight during reorder.
- The drag handle uses `pointerInput` and the README's three-way decision: sideways more than 8 dp before 280 ms → indent; hold 280 ms → reorder; vertical movement first → cancel so the list scrolls. The decision and drop maths live in `domain/DragMath.kt`; the composable only feeds pointer positions in.
- Timetable grid: one `Canvas` draws hour lines and column borders; blocks are positioned `Box`es; total height 24 × 44 dp = 1056 dp in a `verticalScroll`. The long-press create gesture uses a pure snapping/bounds function in `BlockRules.kt`.
- Wheel picker: `LazyColumn` with snap fling behaviour, three visible 24 dp rows, top/bottom fade drawn with a gradient in `drawWithContent`.

## 11. Build variants and signing

- `debug`: `applicationIdSuffix = ".debug"`, app name "Routine Manager (debug)", includes `src/debug` (debug screen, fake clock).
- `release`: R8 on (already configured), signed with a keystore stored outside the repo. Path and passwords come from `routine-manager/keystore.properties`, which is gitignored. Set up in M12.

## 12. Testing

- JVM unit tests (`./gradlew testDebugUnitTest`) for everything in `domain/`, the JSON round trip, the sample data, the ticker's boundary maths and the debounced save. Tests for debug-only code (fake clock, debug screen) go in `src/testDebug`.
- Each test's name or comment says why the rule matters, e.g. `deletingParentPromotesChildrenSoNoChildIsOrphaned`.
- UI behaviour is verified on the phone in every milestone. Instrumented UI tests are optional.

## 13. Known risks

| Risk | Plan |
|---|---|
| Samsung Keyboard doesn't report Backspace on an empty field | Try a key-event check on the field; if unreliable, the ✕ button is the delete path (SPEC §3.7). Record the result in M6. |
| Left panel needs ~380 dp of the 384 dp height | Body area takes the remaining height (SPEC §3.4); screenshot check in M3 |
| Rotating with the keyboard open, or with a drag in progress | Test both on the phone in M6 and M7 |
| Memory creeping up over hours (unreleased MediaPlayer, flows collected twice) | Release players after each play; collect flows with the lifecycle; 30-minute memory check at M2, M4, M7, M8 and M12; 24-hour run at M12 |
| Wireless debugging drops (Samsung turns it off on Wi-Fi change or reboot) | Reconnect steps in CLAUDE.md |
