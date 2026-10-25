I'm building "Routine Manager", a routine + chore app for my personal use only
(no Play Store, sideloaded onto my Samsung phone, Android 16).

Context:
- Design handoff: @design/README.md. It is a high-fidelity spec (tokens,
  screens, behaviour, data model). Treat it as the source of truth.
- Interactive prototype: @design/prototype/Routine Dashboard.dc.html
  (logic is in the <script data-dc-script> block). Visual and UX reference,
  not code to port.
- Sounds: design/assets/sounds/*.wav
- Stack: native Kotlin + Jetpack Compose, minSdk 31, targetSdk 36,
  local-only data. If you think a different stack is better, argue it
  before we plan.
- Usage: the phone sits on a stand, screen on for hours as a dashboard,
  usually charging. Efficiency is a hard requirement: low memory, minimal
  wakeups, redraw once per minute, no per-second recomposition.
- The empty Compose project is created and builds. My phone is paired over
  wireless debugging (`adb devices` shows it).

This session: NO app code. Do this:
1. Read the README and the prototype thoroughly.
2. Grill me with questions, in rounds of 5–8, about anything the README
   leaves open: AMOLED burn-in protection (the README defaults to a light
   theme and has no pixel-shift), Room vs DataStore, exact alarms vs a
   foreground ticker, notifications, edge cases, and what "done" means.
   Push back where my answers conflict with each other or with the README.
3. Then write:
   - docs/SPEC.md: decisions from our Q&A and anything that changes or adds
     to the README. Link to the README; don't copy it.
   - docs/ARCHITECTURE.md: modules, state management, persistence,
     background work, power strategy.
   - docs/BUILD_PLAN.md: milestones sized for one session each, each with
     acceptance criteria and "install on phone and verify" as its last step.
     The app icon/logo is its own later milestone. The signed release APK
     and install guide is the last milestone.
   - CLAUDE.md: build/run/install commands, conventions, the quality gate,
     and "read design/README.md and docs/ before starting a milestone".

Quality gate (each milestone must pass):
- ./gradlew assembleDebug, lint and unit tests pass
- Installed and working on my phone, not just the emulator
- No crashes or ANRs; memory stays flat over a 1-hour run
  (check with Android Studio's profiler)
