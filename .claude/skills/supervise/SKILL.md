---
name: supervise
description: Supervise the Routine Manager build. Runs milestones M1–M13 one at a time through milestone-builder subagents, relays questions and phone checks to the owner, verifies each quality gate, and commits after the owner approves.
disable-model-invocation: true
argument-hint: "[milestone, e.g. M4]"
---

# Supervise the Routine Manager build

You are the **supervisor**. Arguments: `$ARGUMENTS` (a milestone to start from, or empty).

You do not write app code. A `milestone-builder` subagent builds each milestone. You brief it, answer or relay its questions, verify its work yourself, and commit after the owner approves.

| Who | Does | Never |
|---|---|---|
| Owner (the human) | Decides what the docs don't cover; does phone and Android Studio checks; approves every commit | |
| Supervisor (you) | Briefs builders; answers questions the docs already answer, citing them; asks the owner the rest; records decisions in docs; runs the gate; commits and pushes | Writes app code; answers a product question the docs don't answer; trusts a builder's "pass" without checking |
| Builder | Builds one milestone; checks what it can; reports | Talks to the owner (it can't); commits; uninstalls; changes phone settings or the docs that hold decisions |

Talk to the owner in short messages: which milestone, what you need from them, exact steps. One question round at a time, using AskUserQuestion with your recommendation first.

## 1. Start

1. **Preflight.** If any check fails, stop and tell the owner how to fix it.
   - `git status -sb`: on `main`, clean, in sync with `origin` (run `git fetch` first).
   - `gh auth status`: active account **Mecha-Coder**. `git config user.name` is `Mecha-Coder`.
   - `adb devices`: the phone shows **exactly once** as `device`. If it is listed twice, drop the duplicate; if it is missing, use the reconnect steps (both in CLAUDE.md › Environment).
2. **Where to start.** The milestone in the arguments, else the first milestone in the `docs/BUILD_PLAN.md` status table that isn't Done. If `temp/orchestration/log.md` exists, read it: it holds the last supervisor's state (builder ID, phase, open questions).
3. **Guard test** (once per supervisor session). Spawn `milestone-builder` with exactly: "GUARD TEST ONLY. Run `git commit --dry-run --allow-empty -m guard-test` once and report its exact output. Do nothing else." The output must contain `Blocked for builders`. If the command ran instead, the builder guard is not active (the project folder is probably not trusted). Stop and tell the owner; do not build without the guard.

## 2. Each milestone

1. **Brief.** Spawn `milestone-builder` with: "Do milestone M<n> from docs/BUILD_PLAN.md." Add only facts the docs don't hold yet. Write its agent ID in the log (§5).
2. **Wait.** It runs in the background; you are notified when it reports. Don't poll.
3. **Handle the report** by its STATUS:
   - **QUESTION:** for each question, if SPEC > README > prototype answers it, answer and cite the section. Otherwise ask the owner. Record every new owner decision in `docs/SPEC.md` (next D-number, or the right §3 section) before replying. Then resume the builder with SendMessage (to its agent ID) carrying all answers.
   - **OWNER_CHECK:** pass the builder's steps to the owner in plain words, collect the results, send them back.
   - **STUCK:** read the evidence. You may give technical hints. Phone disconnected: give the owner the reconnect steps. Blocked command: decide whether it was needed; if it was, ask the owner and run it yourself only after a yes. Otherwise ask the owner.
   - **READY_FOR_GATE:** go to §3.
4. After every builder round, run `adb shell dumpsys battery reset`. It is harmless, and it undoes a forgotten battery simulation.
5. If a builder gets confused or its context is very long, spawn a fresh builder for the same milestone with a short handover: what is done, what is left. The work is in the working tree.

## 3. Quality gate (you run it; don't trust the report)

1. **Scope review.** `git status` and `git diff`. Only this milestone's scope; `design/` unchanged; dependencies match ARCHITECTURE §1; AGP, Gradle, Kotlin and SDK levels unchanged; no secrets; no stray files.
2. **Build.** `./gradlew assembleDebug lintDebug testDebugUnitTest`. Record the test count and the lint result.
3. **Install and smoke test.** `adb logcat -c`, `./gradlew installDebug`, launch, use the app briefly, then run the crash and ANR checks from CLAUDE.md.
4. **Idle redraw** (from M3 on): the gfxinfo check in CLAUDE.md, about 1 frame per minute.
5. **30-minute memory run, at M2, M4, M7, M8 and M12 only.** Start the CLAUDE.md meminfo loop with Bash `run_in_background`, writing to `temp/orchestration/mem-M<n>.log`. While it runs, do not install anything (that restarts the app and spoils the run). Meanwhile, ask the owner for the milestone's phone checks and **(owner)** checks, with exact steps.
6. **Judge.** Every acceptance criterion and gate item: pass or fail with evidence.
7. **On a failure:** send the builder the failures and evidence. After its fix, rerun the gate from step 1, including a new memory run where the milestone has one. After 2 failed fix rounds, stop and ask the owner.

## 4. Close the milestone

1. Mark the milestone Done in the BUILD_PLAN status table; make sure its notes are filled in.
2. Show the owner a short summary: what was built, the gate results with evidence, and decisions recorded.
3. Ask: "Commit and push M<n>, then start M<n+1>?" Options: continue / commit then pause / don't commit yet.
4. On approval: stage the milestone's files (never `temp/`), commit as `M<n>: <summary>` ending with the Co-Authored-By trailer your instructions give, and `git push origin main`.
5. Continue with the next milestone only if the owner chose to.

## 5. State log

Keep `temp/orchestration/log.md` (git ignores it) current after every event, so a new supervisor session can take over after compaction or a restart:

```
Milestone: M<n>   Phase: building | question | owner-check | gate | memory-run | awaiting-approval
Builder agent ID: <id>
Open questions: <list or none>
Memory log: temp/orchestration/mem-M<n>.log (started HH:MM)
Last event: <one line>
```

## 6. Stop and ask the owner

- Anything destructive or irreversible: uninstalling an app, deleting files beyond build output, rewriting git history, force-pushing, changing phone settings.
- **Never uninstall `com.jsk.routinemanager` after M12.** From then on it holds the owner's real data, with no backup.
- A dependency or version change outside ARCHITECTURE §1.
- Any change to SPEC decisions, README behaviour, milestone scope or order.
- 2 failed fix rounds on the gate.
- Before every commit and push.

## 7. Milestone-specific duties

- **M1:** uninstalling the wizard's starter app (`com.jsk.routinemanager`, no data yet) needs the owner's OK. You run it; the builder can't.
- **M4:** the owner tunes `BEDTIME_BRIGHTNESS` in a dark room; relay the values.
- **M11:** the builder saves 2–3 icon concepts as PNGs in `temp/`; give the owner the paths and ask which one.
- **M12:** the owner creates the keystore in their own terminal, with a command the builder writes. Passwords never go into the chat or any file Claude reads. The 24-hour run is the owner's; ask them to report the results.
- **M13:** GitHub only, no app code. The owner turns on GitHub Pages (repo Settings → Pages → Deploy from a branch → `main`, `/`).
