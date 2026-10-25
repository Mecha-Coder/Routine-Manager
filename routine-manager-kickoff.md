# Routine Manager: Starting an Android App with Claude Code

*Conversation, October 25, 2026*

---

## User

I want to build an Android productivity app.

Now I want to start coding with Claude Code. I already imported the prototype into my local machine project folder. The project folder only has the prototype .html and assets.

How do I start? Is there a skill to use? I plan to just prompt like this; is it enough?

```
I want to build a routine + chore productivity app for my personal use, so I can manage my life better.

App Name: Routine Manager (you design an appropriate logo for this app)

The app prototype and other build requirement already in : /@design

Currently we are on a blank project. See what we need to setup, install dependency before we can start building and test in the MacBook.

Initialize a claude.md to setup this project. See what relevent artifacts to setup first to ensure the other session build the app with relevent context like - build spec, architecture, build plan, UI/UX reference. In the build plan see if this is a multi-session build. If yes, ensure to split the task accordingly, so it can fit conformable in a session.

Please grill me question so you have all the context to build successfully and align with my vision of how the app should be before you start building.

This app I plan to leave it on Fullscreen for hours (like a dashboard). So ensure to build it efficiently (don't consume lot if memory and battery)

After we tested locally and ensure it works (pass our quality gate), turn it into an installer and guide me on how to install in my Samsung android phone (version 16)
```

What would a professional normally do when starting a fresh Android app project (given I already included the prototype)

---

## Claude

Your prompt is a good start, and it already asks for the right things: a spec before code, questions first, and a multi-session plan. A professional would change three things. They'd split it into phases instead of one mega-prompt, pick the tech stack before anything else, and install on the real phone from day one rather than at the end.

### What a professional does first

**1. Pick the stack.** This is the biggest gap in your prompt. Your prototype is HTML, so there are two realistic paths:

- **Native Kotlin + Jetpack Compose (recommended for you).** For a screen left on for hours, this gives the lowest memory and battery use. It also gives you direct access to "keep screen on", alarms and notifications. The HTML becomes a visual reference, not code to port.
- **Wrap the HTML in a WebView (Capacitor or similar).** This is the fastest route to "it runs", but a WebView running all day uses noticeably more RAM and battery. That's the opposite of what you want.

Say which one you want in the prompt, or tell Claude to recommend one with tradeoffs before planning. Otherwise it may quietly choose for you.

**2. Set up the environment yourself (about an hour, no AI needed):**

- Install **Android Studio** on the Mac. It bundles the SDK, JDK, emulator and `adb`.
- Use Android Studio's *New Project → Empty Activity (Compose)* to create the skeleton. The wizard pins Gradle, Kotlin and Compose versions that work together. Hand-written Gradle files are the most common cause of day-one build failures.
- Move the prototype into a subfolder like `design/`.
- Run `git init` and commit. That gives you a rollback point before Claude touches anything.
- On the phone, turn on Developer options, then Wireless debugging, and pair it so `adb devices` shows it. Samsung's **Auto Blocker** (Settings → Security and privacy) can block USB commands and sideloading, so turn it off if installs fail.

**3. Test on your Samsung from the first milestone.** The emulator tells you nothing real about battery use or how Samsung treats a long-running app. You don't need a separate "installer" for daily testing: Android Studio's Run button or `adb install` pushes the app straight to the phone. A signed release APK only matters at the end.

### About skills

There's no built-in Android skill. The Claude Code features that matter here are:

- **Plan mode.** Use it for the first session so Claude asks questions and writes docs without writing code.
- **CLAUDE.md**, the file every new session reads first. `/init` normally generates it by scanning existing code. Your project is nearly empty, so it's better to have Claude write it after the spec exists.
- **A fresh session per milestone.** Use `/clear` or start a new session so context stays clean, and have each one begin with "read CLAUDE.md and BUILD_PLAN.md, do milestone N."

Once you've repeated a workflow a few times, such as "build, install on phone, check memory", you can turn it into your own skill.

### Problems in your current prompt

- **It's too much for one session.** Setup, spec, questions, build and installer all in one prompt means the questions get rushed. Session 1 should end at reviewed documents.
- **The path is wrong.** `/routine-dashboard` means the root of your Mac's disk. Use `./design/` or @-mention the file.
- **"Quality gate" isn't defined.** Claude will invent one. Spell it out (example below).
- **OLED burn-in is missing.** A static dashboard on a Samsung AMOLED screen for hours will burn in. Ask for a dark theme, subtle pixel-shift, and a dim/night mode.
- **It doesn't say how the phone is used.** Mention whether it sits on a stand, in landscape or portrait, and whether it's always charging. These change the design.
- **"Test in the MacBook" is ambiguous.** Say "emulator and my phone over wireless debugging."
- **The logo should wait.** Android app icons need a foreground and background layer. Have it designed in a later milestone, not during setup.

### A better Session 1 prompt

Run this in plan mode:

```
I'm building "Routine Manager", a routine + chore app for my personal use only
(no Play Store, sideloaded onto my Samsung phone, Android 16).

Context:
- UI prototype: @design/Routine Dashboard v2.dc.html (+ assets in design/).
  Treat it as the visual and UX reference, not code to port.
- Stack: native Kotlin + Jetpack Compose, local-only data (Room).
  If you think a different stack is better, argue it before we plan.
- Usage: the phone sits on a stand, screen on for hours as a dashboard,
  usually charging. Efficiency is a hard requirement: low memory, minimal
  wakeups, no per-second recomposition unless needed, dark theme, and
  burn-in protection for the AMOLED screen.
- I've already created the empty Compose project in Android Studio and
  paired my phone over wireless debugging.

This session: NO app code. Do this:
1. Read the prototype thoroughly.
2. Grill me with questions, in rounds of 5–8, until you understand the
   features, data model, reminders/notifications, edge cases and what
   "done" means. Push back where my answers conflict with each other.
3. Then write:
   - docs/SPEC.md: features, screens, data model, out of scope
   - docs/ARCHITECTURE.md: modules, state management, persistence,
     background work, power strategy
   - docs/UI_REFERENCE.md: screen-by-screen mapping from the prototype,
     design tokens (colors, type, spacing)
   - docs/BUILD_PLAN.md: milestones sized for one session each, each with
     acceptance criteria and "install on phone and verify" as its last step
   - CLAUDE.md: build/run/install commands, conventions, the quality gate,
     and "read docs/ before starting a milestone"

Quality gate (each milestone must pass):
- ./gradlew assembleDebug, lint and unit tests pass
- Installed and working on my phone, not just the emulator
- No crashes or ANRs; memory stays flat over a 1-hour run
  (check with Android Studio's profiler)
```

After that, review the docs yourself and edit anything that doesn't match your vision. That's the highest-leverage hour of the whole project. Then commit. Each later session is one milestone.

The last milestone covers the release build: a signed APK, Samsung battery settings (set the app to "Never sleeping" so One UI doesn't kill it), and sideloading. **Back up the signing keystore** somewhere safe. If you lose it, you can't update the installed app without uninstalling it and losing your data.
