# Routine Manager

Personal Android app for one user, sideloaded on a Samsung Galaxy A07. The phone sits on a stand as an always-on dashboard: landscape shows a clock and the current routine block beside four lists (To-Do, Grocery, Self, Room); portrait shows the lists only. Kotlin + Jetpack Compose, local data only, no network.

## Supervised builds

Milestones normally run through the supervisor: the owner types `/supervise` (runbook: `.claude/skills/supervise/SKILL.md`). The supervisor briefs one `milestone-builder` subagent per milestone (rules: `.claude/agents/milestone-builder.md`). A guard script (`.claude/hooks/builder-guard.sh`) blocks builders from committing, pushing, uninstalling, changing phone settings and editing the decision docs. Builders can't talk to the owner; they report to the supervisor, which relays. The supervisor's state lives in `temp/orchestration/log.md`.

## After release (read before changing the app later)

All milestones M1–M13 are built. The phone now holds the owner's **real data** in the release app, and there is no backup (SPEC D14).

- **Release app `com.jsk.routinemanager`: never uninstall it** (not with adb, not on the phone) and never clear its data. Uninstalling deletes everything.
- **Test changes on the debug app first** (`com.jsk.routinemanager.debug`, separate data). If the owner has removed it, `./gradlew installDebug` brings it back.
- **Update the release app** only by installing over it: `./gradlew assembleRelease`, then `adb install -r app/build/outputs/apk/release/app-release.apk` (from `routine-manager/`). Full steps and the `INSTALL_FAILED_UPDATE_INCOMPATIBLE` case: `docs/INSTALL.md` §5.
- **Signing key:** `routine-manager-release.jks` in the project folder (git-ignored). Its password is in `routine-manager/keystore.properties` (git-ignored): **never print, cat or paste it.** Without that file, `assembleRelease` stops on purpose. Backup: SPEC §3.5 and `docs/INSTALL.md` §2.
- **Bring the app back to the front:** `adb shell am start -n com.jsk.routinemanager/com.jsk.routinemanager.MainActivity`. An app locker on the phone (`com.gamemalt.applocker`) sometimes takes the focus.
- **Lessons from the build** (details in the BUILD_PLAN notes):
  - R8's `packageScope` crashed the release app at start (M12); don't add it back.
  - A round launcher icon is not possible on this phone: the launcher forces its rounded square (M11, D24).
  - Parallel builders: create worktrees with `git worktree add -b <branch> .claude/worktrees/<name> main`. The Agent tool's worktree isolation deletes the worktree after a hand-back with no tracked changes.
  - The builder guard also blocks some harmless commands: `rm -rf` anywhere, `which keytool`, `git remote -v`, and commands that mention `design/` together with `tee`, `mv` or `rm`.

## Before starting a milestone

1. Read `design/README.md` (the base spec) and everything in `docs/`: `SPEC.md` (decisions that override the README), `ARCHITECTURE.md`, `BUILD_PLAN.md`.
2. Do only the milestone you were asked for. Read its acceptance criteria before writing code.
3. Precedence: `docs/SPEC.md` > `design/README.md` > the prototype. If something isn't covered, ask the owner, then record the answer in SPEC.md.
4. The prototype (`design/prototype/Routine Dashboard.dc.html`, logic in the `<script data-dc-script>` block) is the reference for exact behaviour and maths. Read the relevant function before building that feature; port the behaviour, not the code structure. SPEC §3.8 lists what not to copy.

## Layout

- `design/`: handoff README, prototype, source sounds. Read-only.
- `docs/`: SPEC, ARCHITECTURE, BUILD_PLAN (INSTALL from M12; `screenshots/` for the user guide from M13).
- `README.md` (from M13): the user guide shown on GitHub.
- `routine-manager/`: the Android Studio project and Gradle root. Package and release app id `com.jsk.routinemanager`; debug app id `com.jsk.routinemanager.debug`.
- `temp/`: scratch files. Verification screenshots go in `temp/screenshot/`, which git ignores.
- GitHub: `origin` = https://github.com/Mecha-Coder/Routine-Manager (public).

## Git identity

This is a personal project. Commit and push only as **Mecha-Coder** (set in this repo's local git config, with GitHub's no-reply email). Never use any other GitHub account or a work email. Pushes always authenticate as Mecha-Coder: this repo's local git config has a credential helper that runs `gh auth token --user Mecha-Coder`, so it works whichever `gh` account is active. Don't switch the active `gh` account; the owner's other projects use a different one.

## Environment

- `JAVA_HOME` (Android Studio's bundled JDK) and `ANDROID_HOME` are set in `~/.zshrc`. If `java` or `adb` isn't found, VS Code was started before `~/.zshrc` changed: restart VS Code.
- Run Gradle from `routine-manager/`.
- The phone connects over Wireless debugging. If `adb devices` is empty: on the phone, Settings → Developer options → Wireless debugging on, same Wi-Fi as the Mac; `adb mdns services` should list it. Samsung turns Wireless debugging off after a Wi-Fi change or a reboot. If it still fails, ask the owner to re-pair from Android Studio's Device Manager.
- If `adb devices` lists the phone twice (one name ends in `(2)`), every adb command fails with `more than one device/emulator`. Drop the duplicate: `adb disconnect "<the name ending in (2)>"`. This only closes a connection; the phone is untouched.

## Commands

```sh
cd routine-manager
./gradlew assembleDebug lintDebug testDebugUnitTest      # build, lint, unit tests
./gradlew installDebug                                   # install the debug app on the phone
adb shell am start -n com.jsk.routinemanager.debug/com.jsk.routinemanager.MainActivity
adb logcat -b crash -d                                   # crashes
adb logcat -d | grep -E "ANR in com.jsk"                 # ANRs
adb exec-out screencap -p > ../temp/screenshot/<name>.png
adb shell run-as com.jsk.routinemanager.debug cat files/datastore/app_data.json
```

Memory check: **only at M2, M4, M7, M8 and M12** (owner decision, M2; the project ends with one overnight run of the release app, SPEC D20). 30 minutes, sampled every 5 minutes, with the app on screen. Run it in the background (if the phone is listed twice, add `-s <serial without (2)>` to `adb`):

```sh
for i in $(seq 0 6); do date +%H:%M; adb shell dumpsys meminfo com.jsk.routinemanager.debug | grep "TOTAL PSS:"; sleep 300; done
```

"Flat" means no steady upward trend after the first 5 minutes. As a rule of thumb, a rise of more than 5 MB between minute 5 and minute 30 fails and needs investigating.

Idle redraw check (proves "redraw once per minute"): leave the app on the dashboard untouched, then

```sh
adb shell dumpsys gfxinfo com.jsk.routinemanager.debug reset
sleep 180
adb shell dumpsys gfxinfo com.jsk.routinemanager.debug | grep "Total frames rendered"   # expect about 6 on this phone
```

On this phone "Total frames rendered" reads 2 per idle minute, while `dumpsys gfxinfo com.jsk.routinemanager.debug framestats` lists 1 frame per minute (M2: 6 against 3 over 3 minutes, one window). So expect about 6, or about 3 rows in framestats. A per-second redraw would show about 180.

Battery simulation (always reset afterwards):

```sh
adb shell dumpsys battery unplug
adb shell dumpsys battery set level 10
adb shell dumpsys battery reset
```

## Quality gate (every milestone)

1. `./gradlew assembleDebug lintDebug testDebugUnitTest` passes. Lint has no errors and no new warnings.
2. Installed and working on the phone, not the emulator. The milestone's "Install on the phone and verify" steps are done and reported.
3. No crashes or ANRs during verification (the logcat checks above).
4. At M2, M4, M7, M8 and M12 only: memory flat over a 30-minute run (the meminfo loop above).

Report each item as pass or fail with its evidence. A milestone with any skipped item is not done: say what was skipped and why.

There are no Android Studio GUI checks (Profiler, Layout Inspector): the owner dropped them in M3. Memory and redraws are measured with the adb commands above.

## Conventions

- Package layout and responsibilities: `docs/ARCHITECTURE.md` §2. Pure logic goes in `domain/` with no Android imports and gets unit tests.
- Colours, type and shapes come only from `ui/theme`, using the README token names. No hard-coded colours or font sizes in screens.
- Each test's name or comment says why the rule matters, not just what it checks.
- UI text is English and may be written inline; no translations are planned.
- Efficiency is a hard requirement:
  - Nothing runs while the app is hidden: no services, alarms, WorkManager, manifest receivers or wake locks.
  - Only composables that show time read the minute state. One redraw per minute, never per second.
  - Values that change every frame (drag, scroll, animation) are read in `offset { }`, `graphicsLayer { }` or draw lambdas, not during composition.
  - No infinite animations or polling loops.
- Dependencies: only those in ARCHITECTURE §1. Ask before adding any other.
- When unsure how a library API works today (Compose, DataStore, lifecycle), check its current docs (context7) before writing the code.
- Don't change AGP, Gradle, Kotlin, `compileSdk`/`targetSdk` (37) or `minSdk` (31) without asking.
- At the end of a milestone, update its status and notes in `docs/BUILD_PLAN.md`, then commit as `M<n>: <summary>` and push to `origin main`. The owner doesn't need to be asked first (owner decision, M5).
- Never commit keystores, `keystore.properties` or passwords.
