# Handoff: Routine Dashboard (personal time & chores app, Android)

## Overview
A single-user Android 16 phone app that stays on display all day (desk/stand). It shows:
- **Landscape:** left 60% = big clock + the current routine time-block (progress, sub-routines, current/next); right 40% = list collection (To-Do, Grocery, Self, Room).
- **Portrait:** list collection only, full width.
- **Settings:** theme (light/dark), block-change sound (3 tones + volume + preview), weekly routine timetable editor.

Routines repeat weekly (Mon–Sun). When a new block starts, a calm 10-second sound plays.

## About the design files
The files in `prototype/` are **design references made in HTML**. They show the intended look and behaviour; they are not production code. Rebuild the design natively.

**Recommended stack (no existing codebase):** Kotlin + Jetpack Compose (Material 3 as the foundation, fully restyled to the tokens below), Room or DataStore for persistence, `MediaPlayer`/`SoundPool` for tones. Single activity, `minSdk 31`, `targetSdk 36`.

To view the prototype, open `prototype/Routine Dashboard.dc.html` in Chrome (keep `support.js` next to it). The **TEST bar** at the top (orientation toggle, time-travel slider, speed, reset) is a prototype-only tool. **Do not build it**, though a hidden debug screen with the same controls is useful.

## Fidelity
**High fidelity.** Colours, type, spacing, radii and interactions are final. Values are in CSS px and map 1:1 to **dp** (font sizes map to **sp**). The prototype screen is **900 × 416 dp landscape** or **416 × 900 dp portrait**. Real devices vary, so the layouts must flex: the left panel takes 60% of the width, and lists scroll.

---

## Design tokens

### Colour: semantic tokens with final rendered values
The prototype gets dark mode by filtering the light UI. **In the app, implement two explicit palettes using these final values:**

| Token | Light | Dark | Used for |
|---|---|---|---|
| `bg` | `#E6E6E6` | `#000000` | Screen background (light is deliberately off-white to save OLED power; dark is true black) |
| `surface` | `#DBDBDB` | `#242424` | List-menu rows, settings rows |
| `surfacePressed` | `#D4D4D4` | `#2F2F2F` | Hover/pressed rows |
| `badgeIdle` | `#D1D1D1` | `#323232` | Zero-count badge fill |
| `divider` | `#CFCFCF` | `#353535` | Vertical divider between panels, progress track |
| `outlineSoft` | `#C8C8C8` | `#3D3D3D` | Input underlines |
| `outline` | `#AAAAAA` | `#595959` | Text-field / wheel / day-toggle borders |
| `outlineStrong` | `#808080` | `#838383` | Theme segmented control, "Add category" pill |
| `textPrimary` | `#0F0F0F` | `#EDEDED` | Primary text, icons, primary buttons (fill) |
| `textSecondary` | `#565656` | `#B2B2B2` | Labels, numbering, AM/PM, date |
| `textDone` | `#7E7E7E` | `#858585` | Struck-through to-dos |
| `textHint` | `#979797` | `#6B6B6B` | Unselected wheel values |
| `onPrimary` | `#E6E6E6` | `#000000` | Text/icon on primary (black/white) buttons |
| `accent` | `#2952E6` (cobalt) | `#4DE0A6` (mint) | Highlights only (see below) |
| `onAccent` | `#E6E6E6` | `#111111` | Text on accent fills |
| `scrim` | `rgba(0,0,0,.32)` | `rgba(255,255,255,.62)`* | Dialog scrim |
| `todayColumn` | `#E1E1E1` | `#0F0F0F` | Today's column in the timetable |
| `gridLine` | `#D7D7D7` | `#2A2A2A` | Timetable hour lines and column borders |

\*The dark scrim renders as a dark grey veil over black. Use `rgba(0,0,0,.6)` if the white veil looks wrong natively; the goal is that the dialog is clearly separated.

**Accent is used only on:** the progress-bar fill, the dot before "CURRENT", count badges when > 0, today's day chip in the timetable, the "now" line + dot in the timetable, the current block's fill in the timetable, and error icons. Buttons stay black/white. **Never use accent for body text.**

Dark-mode dialog: add a 1 dp `#353535` border; no shadow. Light-mode dialog: shadow `0 12 40 rgba(0,0,0,.18)`.

### Typography
- **Family:** Google Sans Flex (Google Fonts, variable 400–700). Fallbacks: Google Sans, then Noto Sans. Use tabular figures (`fontFeatureSettings = "tnum"`) on all numbers.
- **Scale used:**
  - Clock 104 / 500 / letter-spacing −4, line-height 0.84
  - Day / date 21 (day 600; date 400 secondary)
  - Clock AM/PM 20 / 500 secondary
  - Screen titles 22 / 500 ("Lists", "Settings", "Weekly routine"); list detail title 20 / 500
  - Dialog title 20 / 500
  - "Routine" heading 20 / 600; block start/end time 20 / 600 + 12 / 600 AM/PM
  - Current/Next value 19 / 700; Left value 17 / 600
  - Sub-routines 17 / 400, line-height 1.3
  - List-menu row label 17 / 500; grocery category 17 / 600
  - Body / list item 16 / 400 (line-height 1.35); grocery item selected 700
  - Buttons 14–15 / 600
  - Labels (LEFT, CURRENT, NEXT) 11 / 600, uppercase, tracking 0.08em, secondary
  - Section headers in Settings 13 / 600 secondary
  - Timetable block 10.5 / 600 title + 10.5 / 400 range at 72% opacity; hour labels 10 / 500

### Shape, spacing and elevation
- **Radii:** list-menu group 24 (rows inside 6, so the group reads as one card with 2 dp gaps); settings rows 24; dialog 28; text fields and wheels 12; drag ghost 14; timetable blocks 8; pills and badges fully rounded.
- **Icon buttons:** 48 dp touch target (40 dp for row delete buttons, 36 dp for the dialog close button).
- **Row heights:** list menu 60; settings rows 64 (tone and volume rows 50); list items min 44; grocery header 48.
- **Padding:** left panel 28 / 30 / 22 / 42 (t/r/b/l); list content horizontal 30 left / 26 right; settings 32 horizontal.
- **Elevation:** only the drag ghost (`0 12 30 rgba(0,0,0,.18)`) and the light-mode dialog. Everything else is flat.

### Icons
Material Symbols **Rounded** (weight 400, FILL 0 unless noted): `schedule`(FILL 1), `bedtime`(FILL 1), `settings`, `arrow_back`, `edit`, `check`(wght 600), `close`, `delete`, `add`, `drag_indicator`, `expand_more`, `format_list_bulleted`(FILL 1), `shopping_cart`(FILL 1), `person`(FILL 1), `home`(FILL 1), `contrast`, `light_mode`(FILL 1), `dark_mode`(FILL 1), `calendar_view_week`, `chevron_right`, `volume_down`(FILL 1), `play_arrow`/`stop`(FILL 1), `error`(FILL 1).

---

## Screens

### 1. Home, landscape (900 × 416)
**Left panel (60%)**, separated by a 1 dp `divider` line inset 28 dp top and bottom:
1. **Top row:** clock `HH:MM` (12-hour, leading zero, e.g. `07:05`) with AM/PM top-aligned to its right. Right-aligned stack: day (`Mon`), then date (`19 Oct`, no year) with a 12 dp gap.
2. **Routine header** (16 dp below): `schedule` icon + "Routine" on the left; on the right, `LEFT` label + remaining time (`4hr 30min`, `2hr`, `33min`). It updates every minute and is computed as `ceil(end − now)`.
3. **Progress bar** (8 dp below): 10 dp tall. The filled segment (accent, radius 5) and the remaining track (`divider`, radius 5) are separate pills with a 4 dp gap. The track has a 4 dp `textPrimary` end-dot inset 3 dp from its right end.
4. **Start / end times** under the bar's ends (`07:00 AM` … `11:30 AM`).
5. **Body** (20 dp below, fixed 132 dp tall), two columns:
   - Left: up to **5 sub-routines**, spread evenly top to bottom, each with a 5 dp bullet; ellipsize on overflow.
   - Right, right-aligned: `● CURRENT` (accent dot 7 dp) + block title; `NEXT` + next block title (or `-` when nothing follows today). Max width 190, ellipsized.
- **Bedtime state** (no block now, and either no earlier block today or no later one): the whole panel is centred. Small "Routine" label, then `bedtime` icon + **"Bedtime, see you tomorrow"** (22 / 600).
- **Gap between two blocks:** show a pseudo-block titled **"Free time"** from the previous block's end to the next block's start, with no sub-routines and no sound.

**Right panel (40%): List menu**
- Header: "Lists" (22 / 500) and a `settings` icon button on the right.
- One grouped card of 4 rows (`surface`, 2 dp gaps, outer radius 24): icon 24 + label + badge on the right.
  - **To-Do** badge = number of not-done items (all levels).
  - **Grocery** badge = number of selected items across all categories.
  - Self and Room have no badge.
  - Badge: 26 dp pill, 13 / 600; accent + `onAccent` when > 0, `badgeIdle` + secondary text when 0.
- Tapping a row replaces **only the right panel** with that list.

### 2. Home, portrait (416 × 900)
The list panel only, full width; no clock or routine. Same list behaviour.

### 3. List detail (right panel)
- Header grid `1fr auto 1fr`: back (`arrow_back`) on the left, list name centred, and on the right either **Edit** (`edit` icon button) or, in edit mode, **Done** (a 40 dp filled `textPrimary` circle with a `check` in `onPrimary`).
- Leaving the list (back) also exits edit mode and runs the cleanup described under "Saving edits".

#### To-Do, Self, Room: numbered outline lists
- **Automatic numbering:** `1)`, `2)`, children `2.1)`, `2.2)`. Maximum depth is **1** (one level of children). The child counter restarts under each parent. Numbers are secondary colour, followed by an 8 dp gap; children are indented 24 dp.
- **View mode:**
  - **To-Do:** tapping a row toggles done (line-through + `textDone`). Done items **stay in place**, are still numbered, and tapping again undoes it.
  - **Self / Room:** plain items; tapping does nothing.
- **Edit mode** (rows are 44 dp): `drag_indicator` handle (28 dp wide, colour `outlineStrong`), number, an underlined text field (1 dp `outlineSoft`, focused → `textPrimary`), and a `close` delete button.
  - **Enter** inserts a new item after this item (and after its children) at the same depth, and focuses it.
  - **Backspace** in an empty field deletes it.
  - Deleting a parent promotes its children up one level.
  - "+ Add task" / "+ Add item" text button appends a top-level item.
- **Drag handle gestures** (the core interaction; build carefully):
  1. **Press then move sideways** (more than 8 dp, mostly horizontal, before 280 ms) → **indent mode**. The row follows the finger on the X axis only, with 0.25× rubber-band past the allowed range. On release it snaps (140 ms) to the nearest 24 dp level.
     - It can indent only if there is an item above it: max depth = `min(prevDepth + 1, 1)`.
     - Indenting a parent makes it a child of the item above; its former children stay at depth 1.
  2. **Press and hold 280 ms** (light haptic) → **reorder mode**. A ghost card lifts (`bg` fill, 1 dp `divider` border, radius 14, drag shadow; shows the handle, number, text and "+N sub-tasks"). The original row and its subtree go invisible.
     - The other rows **slide apart to open a gap** where it will land (transform 200 ms `cubic-bezier(.2,.7,.3,1)`). Vertical movement only. The item moves together with its children.
     - On release the ghost animates into the gap (170 ms) and then commits. The dropped depth is `min(originalDepth, prevDepth + 1, 1)`.
  3. **Press then move vertically first** → cancel, so the list can scroll.

#### Grocery: categories with selectable items
- **View mode:**
  - Categories are separated by 14 dp.
  - **Category header** (48 dp, tappable, light hover tint, radius 14): `expand_more` chevron (rotates −90° when collapsed, 180 ms), name (17 / 600, never smaller than items), and a count badge on the right showing the number of selected items in that category. The collapsed state is **persisted**.
  - **Items** are indented 30 dp: a radio (20 dp ring, 2 dp stroke, 10 dp inner dot when selected) + text (16 sp). Tapping toggles selection; selected = filled dot + **bold** text.
  - Selected means **"need to buy"**, and it feeds the Grocery badge.
- **Edit mode:**
  - Category name field (17 / 600) + `delete` (removes the whole category).
  - Items: radio + text field + `close`.
  - "+ Add item" under each category; Enter and Backspace work as in the To-Do list.
  - "+ Add category": a 44 dp outlined pill (1 dp `outlineStrong`).
- Empty state: "Nothing here. Tap ✎ to add." in secondary text.

#### Saving edits (when leaving edit mode or the list)
- Remove items with empty text and normalise depths, so no child is left without a parent.
- Grocery: trim names, drop empty items, drop categories with no name and no items, and rename any remaining nameless category to **"Untitled"**.

### 4. Settings (full screen, over everything)
Header: back + "Settings". Content in two columns that wrap at a 260 dp minimum width.
- **Appearance:** row (`surface`, 64 dp, radius 24) with `contrast` icon, "Theme", and a segmented control (40 dp, 1 dp `outlineStrong`, Light / Dark with icons; selected = `textPrimary` fill).
- **Routine:** row with `calendar_view_week` icon, "Weekly timetable" and a `chevron_right` that opens the timetable.
- **Block-change sound:** grouped card of 3 radio rows:
  - "Singing bowl" (Deep tone, slow fade)
  - "Soft chime" (Gentle bell notes)
  - "Warm pad" (Airy chord swell)

  Tapping a row selects it **and plays a preview**. Under the rows: a volume row (`volume_down` icon, 0–100 slider, `60%` label). Below the card: a "Preview sound" pill button (`textPrimary` fill) that toggles to "Stop" while playing.

### 5. Weekly routine timetable (full screen)
- Header: back (returns to Settings) + "Weekly routine".
- Day header row: Mon…Sun; today is an accent pill chip with `onAccent` text.
- Scrollable grid: a 46 dp hour-label gutter (`12 AM`…`11 PM`, 10 sp), 7 equal columns, and 44 dp per hour. Today's column uses the `todayColumn` fill. A 2 dp accent **now line** with an 8 dp dot spans today's column.
- On open, auto-scroll so that `now − 1.5 h` is at the top.
- **Blocks:** `textPrimary` fill with `onPrimary` text, radius 8, 3 dp side insets. Each shows its title and the full range in the format **`6:00am-11:30am`**. **The current block** uses an accent fill with `onAccent` text.
- **Create:** on touch, **long-press 300 ms** on empty time (haptic), then drag. It snaps to 15 minutes and can't cross neighbouring blocks. A dashed outline ghost shows the range label. On release, if the range is 15 minutes or less it defaults to 60 minutes (bounded). Then the editor dialog opens.
- **Tap a block** to edit or delete it.

### 6. Block editor dialog
Centred modal: max width 600, radius 28, scrim, padding 16 / 20. Title "New block" / "Edit block"; a 36 dp outlined circular `close` button on the right (no Cancel button).
- **Left column:**
  - **Title** field (42 dp, 1 dp `outline`, radius 12; focus = 2 dp `textPrimary`), placeholder "e.g. Work Session1".
  - **Days:** 7 circular 34 dp toggles M T W T F S S, multi-select (selected = `textPrimary` fill).
  - **Start | End:** two **scroll-wheel pickers** side by side. Each has three drums (hour `12,1…11`; minute `00 15 30 45`; `am/pm`) with a `:` between hour and minute.
    - Drums are 36 dp wide; items 24 dp tall with 3 visible; snap scrolling; top and bottom fade.
    - A 24 dp `surface` selection band sits behind the centre row. The selected value is `textPrimary` 700; others are `textHint` 500. Tapping a value selects it.
    - End `12:00 am` means **midnight (end of day)**.
    - Moving Start to or past End pushes End to Start + 60 min (max midnight).
- **Right column (fixed 206 dp tall so the footer never moves):**
  - "Sub-routines": up to **5** underlined fields, each with a `close` button. Enter adds a field below and Backspace on an empty field removes it.
  - "+ Add sub-routine" is **hidden when there are 5** and reappears below 5.
- **Error line** (when present): `error` icon in accent + message.
- **Footer:** "Delete" (edit only; `delete` icon text button) on the left, **Save** (`textPrimary` pill) on the right.
- **Validation, in this order:**
  1. `Give the block a title.`
  2. `End time must be after the start time.`
  3. `Pick at least one day.`
  4. Overlap with any other block on a selected day: `Overlaps “Commute” (7 AM – 8 AM) on Mon, Tue.`
- **Multi-day groups:** a block saved for several days is one **group** (`groupId`) that shares title, times and sub-routines.
  - Editing any copy edits the group.
  - Deselecting a day removes that day's copy.
  - Delete removes the whole group.

---

## Behaviour and system
- **Routine resolution** (per local weekday, Mon = 0): sort the day's blocks.
  - Current block = `start ≤ now < end`.
  - Next = the first block with `start ≥ current.end`, otherwise `-`.
  - If between two blocks → "Free time" (see above).
  - Otherwise → Bedtime.
  - Blocks never cross midnight (end ≤ 24:00).
- **Block-change sound:**
  - When the resolved current block changes to a **real block** (not Free time, not Bedtime), wait 350 ms and play the selected tone once, **10 s long**.
  - Gain = `(volume/100)² × 0.9`. Moving the volume slider while a tone plays adjusts it live.
  - Use the media audio stream. Use **exact alarms** (`AlarmManager.setExactAndAllowWhileIdle` for each upcoming block start, re-armed daily) or a foreground ticker so the tone fires on time even if the activity is paused.
- **Always-on display:** keep the screen on while the home screen is visible (`FLAG_KEEP_SCREEN_ON`). Redraw **once per minute**, aligned to the minute boundary, not every second. The light theme's off-white background and the dark theme's true black are both deliberate power savers.
- **Orientation:** follow the device rotation; landscape layout vs portrait layout as described.
- **Persistence:** everything (lists, collapsed state, blocks, settings) saves locally on every change and restores on launch. No account and no network.
- **Haptics:** 12 ms on reorder pick-up and 10 ms on timetable create.

## Data model (as used in the prototype)
```ts
Settings { theme: 'light'|'dark'; tone: 'bowl'|'chime'|'pad'; volume: 0..100 }  // default light, bowl, 60
OutlineItem { id; text; depth: 0|1; done: boolean }        // todo, self, room (done only used by todo)
GroceryCategory { id; name; collapsed?: boolean; items: { id; text; on: boolean }[] }
Block { id; groupId; day: 0..6 /*Mon..Sun*/; start: minutes 0..1425; end: minutes 15..1440; title; subs: string[] /*≤5*/ }
Data { todo: OutlineItem[]; grocery: GroceryCategory[]; self: OutlineItem[]; room: OutlineItem[]; blocks: Block[] }
```
Times snap to 15 minutes. Outline lists are stored flat, in order, with depth. A child always follows its parent.

**Sample data** (for the first run or a debug reset) is in `sample()` inside the prototype script. It contains a weekday routine (Morning, Commute, Work Session1, Lunch, Work Session2, Commute, Dinner & Chores, Personal, Wind down) and a weekend routine (Morning, Work Session1, Errands, Personal, Wind down).

## Assets
- `assets/sounds/singing_bowl.wav`, `soft_chime.wav`, `warm_pad.wav`: 10.4 s, 44.1 kHz mono, rendered from the prototype's synth at full gain. Convert to OGG for the APK and apply the volume curve at playback.
- Fonts: Google Sans Flex (Google Fonts). Icons: Material Symbols Rounded. No images.

## Files
- `prototype/Routine Dashboard.dc.html`: the full interactive prototype. Template markup (inline styles) is at the top; all logic is in the `<script data-dc-script>` block at the bottom (routine resolution, drag/indent maths, wheel pickers, validation, sound synthesis).
- `prototype/support.js`: the runtime needed to open the prototype in a browser. It is not part of the app.
