I'm building "Routine Manager", a routine + chore app for my personal use only
(no Play Store, sideloaded onto my Samsung phone).

Phone: Samsung Galaxy A07 (SM-A075F), Android 16 (API 36), ~6 GB RAM,
720×1600 px at 300 dpi, about 853 × 384 dp in landscape. That is smaller
than the prototype's 900 × 416 dp, so the layouts must flex.

Project layout (run Claude from the repo root):
- design/: design handoff and prototype
- routine-manager/: the Android Studio project (Gradle root; run ./gradlew
  from there). Package: com.jsk.routinemanager.

Context:
- Design handoff: @design/README.md. It is a high-fidelity spec (tokens,
  screens, behaviour, data model). Treat it as the source of truth.
- Interactive prototype: @design/prototype/Routine Dashboard.dc.html
  (logic is in the <script data-dc-script> block). Visual and UX reference,
  not code to port.
- Sounds: design/assets/sounds/*.wav
- Stack: native Kotlin + Jetpack Compose, minSdk 31, compileSdk and
  targetSdk 37 (the wizard's default; the README says 36, keep 37),
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
   leaves open: whether my screen is LCD or AMOLED, and so whether we need
   burn-in protection (the README defaults to a light theme and has no
   pixel-shift), Room vs DataStore, exact alarms vs a
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
