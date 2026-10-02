# Decisions

Two lists: what is settled, and what is still waiting on the user. When an open question gets
answered, move it up into "Settled" with the answer and the date.

---

## Settled

### D1 — Inherit the Reevz Mealz stack wholesale
**2026-09-25.** Kotlin + Compose + Material 3 + Room + MVVM, offline-first, no DI framework, no
Navigation-Compose, no repository layer. Reason: Mealz is a working app on the same toolchain, on
the same phone, maintained by the same person. A second stack to keep in your head has no upside
here. Details in `TECH_STACK.md`.

### D2 — No backend, no accounts, no sync
**2026-09-25.** Single user, single device, local Room database. Permanent, not a v1 shortcut.

### D3 — Keep the scaffold's honest version catalog
**2026-09-25.** Drip declares `core-ktx 1.19.0` and `lifecycle 2.11.0`, which are the versions that
actually resolve. Mealz declares `1.10.1` / `2.6.1` and resolves to something else through
transitive constraints. Do not copy Mealz's numbers across — its catalog is the known-wrong one.

### D4 — History is derived from plan entries, not recorded separately
**2026-09-25.** A plan entry whose day is in the past *is* a wear record. No "did you actually wear
this?" step exists anywhere in the requirements, and Home is explicitly read-only, so planned and
worn are the same thing. This removes an entire table and an entire user interaction.

Mealz took the opposite road — `planned_meals` and `eaten_meals` are separate, because people
routinely eat something other than what they planned. Clothes get laid out the night before and then
worn, so the same split would be ceremony. Revisit only if outfits turn out to be skipped often.

**Consequence:** past plan entries must be immutable — see D6.

### D5 — Photos are files; the database stores a filename
**2026-09-25.** Bytes go in `filesDir/garments/`, the row holds a generated filename. Never a Room
`BLOB` — it bloats the database, slows every query touching the row, and makes migrations worse.
Never an absolute path — Android relocates app data directories on restore-to-new-device.

### D6 — Six sections: five tabs plus Settings in the top bar ✅ *built, Phase 0*
**2026-09-25.** The requirements list five sections but omit **Plan**, which the same requirements
ask for twice elsewhere — read as an oversight in the list. That makes six, and six does not fit a
bottom bar.

Bottom bar carries **Home, Plan, Outfits, Wardrobe, Groups** (five being Material 3's documented
maximum); **Settings** is an icon in the top bar, because it is the one section touched about twice
a year. Mealz hit the same wall and put its three occasional sections behind a ☰ pause menu; one
occasional section needs only an icon.

### D7 — Past plan entries are locked ✅ *built, Phase 5*
**2026-09-25.** Follows from D4: if history is derived from plan entries, editing a past entry
rewrites history. Today and the future stay editable; the moment a day passes, its entries become
read-only and become history. Same rule as Mealz's plan lock at midnight.

### D8 — Combinations have an optional name ✅ *built, Phase 3*
**2026-09-25.** The requirements never mention naming an outfit, but a history list reading "worn on
3 Oct, 17 Oct" scans better with a label. Optional, so nothing is blocked by leaving it blank —
unnamed combinations are identified by their collage, which is how you actually recognise an outfit.

### D9 — Deleting something that has been worn: archive, don't destroy ✅ *built, Phase 3*
**2026-09-25.** A garment that belongs to a combination gets **archived** — hidden from the Wardrobe
grid and the combination builder, but still rendering inside existing combinations and history. A
garment in no combination is **hard-deleted**, so a typo added a minute ago still disappears
cleanly. The same rule applies to a combination that has been worn and to a group that has seen
something.

The rejected alternative was cascade-delete, which is simpler and quietly corrupts the wear history
the whole app exists to keep.

**Built for garments in Phase 3.** `garments.archived` plus a rerouted delete path; the delete
dialog says which of the two will happen, and an archived garment keeps its photo file because the
outfits it belongs to still have to draw it.

> ⚠️ **Combinations and groups are still hard-deleted**, which is correct *only* because nothing
> can have been *worn* yet. Phase 5 introduces plan entries and ends that — the same trap Phase 1
> set for Phase 3, so the reroute is already on the Phase 5 checklist.

> **There is no un-archive.** Archiving is one-way and has no UI, by design: it is what "delete"
> means for a garment with history, not a hidden state the user manages. If un-archiving is ever
> wanted, it needs a deliberate "removed clothes" surface rather than a toggle bolted onto the
> Wardrobe.

### D10 — A quiet neutral palette, dynamic colour off ✅ *built, Phase 0*
**2026-09-25.** Near-neutral greys with a dark-slate accent, defined in `ui/theme/Color.kt`.
Requirement 10 asks for minimalistic, and every screen here is wall-to-wall photographs of clothes —
a saturated palette competes with the content, and a wallpaper-derived Material You scheme would
recolour the app unpredictably behind those photos. There is no `dynamicColor` flag, so it cannot be
half-enabled by accident. Mealz reached the same conclusion from the opposite direction: it has no
imagery to protect, so it could afford a loud arcade palette.

### D11 — Coil for image loading ✅ *built, Phase 2*
**2026-09-25.** Compose has no built-in async image loader; hand-decoding and downsampling bitmaps
for a scrolling card grid is not a reasonable alternative, and Coil also honours the EXIF rotation
that camera JPEGs carry. This is the first dependency Drip will have that Mealz does not.

### D12 — Both the Photo Picker and camera capture ✅ *built, Phase 2*
**2026-09-25.** Both shipped together. The system Photo Picker needs no runtime permission at any
API level. Camera capture goes through a `FileProvider` over a cache scratch directory, and
crucially the manifest declares **no `CAMERA` permission** — `ACTION_IMAGE_CAPTURE` needs none
unless the app asks for it, and declaring it would force a runtime prompt for a capability only the
system camera app ever uses. Do not add it.

### D13 — Warn at planning time when a group has already seen an outfit ✅ *built, Phase 5*
**2026-09-25.** Non-blocking: *"Colleagues saw this on 3 Oct"*, with the save still allowed. Not
literally requested — the requirements ask only for history to look up — but "never repeat an outfit
in front of them" is the stated goal, and the app already knows the answer at the moment the
assignment is made. Turns a lookup you have to remember into one you cannot miss.

### D14 — Epoch day for storage, epoch millis for calendar UI ✅ *built, Phase 0*
**2026-09-25.** Days are stored as an `Int` epoch day, so `day < today` is an honest comparison and
the history queries are trivial. Calendar grid maths works in millis, where `java.util.Calendar` is
good. Conversion happens only at the persistence boundary (`util/Dates.kt`).

`java.time` would be shorter but needs API 26 against this app's minSdk 24. Mealz stores millis and
does the same maths with `Calendar`. Revisit core library desugaring when Phase 5 makes the date
logic heavier.

### D15 — Navigation state must survive configuration changes ✅ *built, Phase 0*
**2026-09-25.** `rememberSaveable`, not `remember`, for the selected section and for the editor
form. Found on-device: the phone flips light/dark on its own at sunset, which recreates the
activity, and with a plain `remember` that silently bounced the user back to Home and discarded a
half-typed garment name. Applies to any future screen state worth keeping.


### D16 — Photos are imported on save, not on pick ✅ *built, Phase 2*
**2026-09-25.** The editor holds a picked image as a `content://` Uri and only writes it into
`filesDir` when the user actually saves. Importing at pick time would be simpler to write, but
every cancelled edit would leave an orphaned file in the photo directory with nothing referencing
it and no way for the user to find or remove it.

The mirror of this rule is the write order on save: **the row first, then delete the old file.** A
file with no row is a harmless orphan; a row pointing at a file that is gone is a visible hole in
the wardrobe.

### D17 — Render the placeholder under the photo, not in a Coil slot ✅ *built, Phase 2*
**2026-09-25.** `GarmentImage` draws the placeholder icon and then an `AsyncImage` over it.
`AsyncImage` with no `error` painter draws nothing when a file is missing, so the placeholder shows
through — no state to observe and no branch to get wrong.

Found the hard way: the first version used `SubcomposeAsyncImage` with `loading` and `error` slots,
and **its `contentScale` is inherited by the slot content**, which blew the 28dp placeholder icon
up to fill the entire card. The replacement also drops subcomposition from every cell of a
scrolling grid, which is where you least want it.

### D18 — A 1080px long edge, re-encoded, with EXIF baked in ✅ *built, Phase 2*
**2026-09-25.** Imports are re-encoded rather than copied. A 12 MP camera JPEG is 4–12 MB and a
couple of hundred garments at that size is multiple gigabytes on a device with no cloud backup;
measured on the phone, a 4000x3000 original went from 216 KB to 11.5 KB with no visible loss at the
size a garment is ever shown.

Re-encoding through `Bitmap.compress` **drops EXIF**, so the orientation has to be baked into the
pixels at import or every portrait photo comes back sideways with no tag left to explain why. All
eight EXIF orientations are handled, mirrored ones included — a missed mirror case produces a
garment that looks almost right, which is worse than one that looks obviously wrong.

Downscaling is two-step on purpose: `inSampleSize` to get near the target without ever allocating
the full-size bitmap, then a precise matrix scale from the small one.


### D19 — Collage order is derived from garment type, not stored ✅ *built, Phase 3*
**2026-09-27.** `combination_items` has **no `position` column**, though an earlier draft of
`DATA_MODEL.md` specified one. `orderedForCollage` sorts by type — top, pant, shoes, accessory —
then name, then id.

Nothing in the requirements asks to reorder an outfit, so a position column would be state built
for a hypothetical. It also has to be renumbered on every edit, and a gap or duplicate in those
numbers shows up only as a subtly shuffled collage. Type order is better anyway: every outfit reads
top-to-bottom the same way, and in the 3-tile layout the largest tile is the top, which is what
identifies an outfit fastest. The name and id tiebreakers make the order total, so a collage cannot
flicker between reads.

### D20 — The builder offers archived garments an outfit already holds ✅ *built, Phase 3*
**2026-09-27.** The picker normally shows only the live wardrobe. When *editing* an outfit that
contains an archived garment, that garment is appended to the picker too, labelled "Removed".

Found on the phone: without it the builder read *"3 selected"* above two ticked tiles, because the
archived garment was in the selection but not on screen — the count contradicted the display and
there was no way to take the garment out. Building a *new* outfit still only ever sees the live
wardrobe.


### D21 — `archived` columns are added with the table, not when first used ✅ *Phase 3 and 4*
**2026-09-27.** `groups.archived` shipped in the migration that created `groups`, even though
nothing can set it until plan entries exist in Phase 5. D9 has already decided the rule, so this is
a scheduled requirement rather than speculation, and riding the table's own migration costs nothing
where a later addition would need a whole schema bump for one boolean.

Same play as `garments.photoName`, added unused in Phase 1 and picked up by Phase 2 with no
migration at all. The limit is the one CLAUDE.md draws: a column a *decided* requirement needs, not
a column something might one day want.

### D22 — Duplicate group names are caught in the UI, not by the index ✅ *built, Phase 4*
**2026-09-27.** `groups.name` has a unique index, but SQLite compares bytes, so it would happily
store "Colleagues" beside "COLLEAGUES". `isDuplicateGroupName` does a case-insensitive,
whitespace-trimmed check as the user types, and the index is the backstop that turns a logic bug
into a loud failure rather than the first line of defence.

Checked while typing rather than on save because the failure is silent: two groups that look
identical in every list the app shows are indistinguishable, and you would only find out you picked
the wrong one after wearing the outfit.


### D23 — Today is plannable here, unlike in Reevz Mealz ✅ *built, Phase 5*
**2026-09-27.** `planLock` has two states, OPEN (today and later) and PASSED. Mealz has three, and
locks a day the moment it *begins* — because its Today screen owns what actually happened, and
letting the plan be rewritten afterwards would make its plan-versus-actual comparison meaningless.

Drip has no such split (D4), and requirement 5 is explicit: *"I can also plan for the current
day."* Porting Mealz's rule unchanged would have quietly broken the day the user most needs to
edit — the morning you realise the plan is wrong. Worth recording because the Mealz file is the
obvious thing to copy and copying it would have been wrong.

### D24 — Plan reads the clock once, Home must not ✅ *Phase 5, and a note for Phase 6*
**2026-09-27.** `PlanViewModel` reads `todayEpochDay()` once at construction. Re-reading it per
recomposition would make the lock flicker across midnight while a screen is open, and a plan that
silently becomes uneditable mid-edit is worse than one a few hours stale.

**Home has the opposite requirement** — it must roll over at midnight without a restart — so Phase
6 needs its own answer rather than reusing this one.


### D25 — Home rolls over at midnight; two mechanisms, both needed ✅ *built, Phase 6*
**2026-09-27.** `HomeViewModel.todayFlow()` emits today's epoch day, sleeps exactly until the next
local midnight (`millisUntilNextMidnight`), then emits again. This is the **opposite** of
`PlanViewModel`, which reads the clock once (D24) — and the difference is deliberate, because the
two screens fail in opposite directions. A plan that changes its lock mid-edit is a bug; a Home
screen that needs the app killed and reopened at midnight fails the only job it has.

Both mechanisms are required, because midnight passes in two different ways:

- **App left open overnight** — the sleep fires and re-emits. Waiting for the instant costs one
  wakeup; polling every minute would cost 1,440 to catch the same moment.
- **Backgrounded and reopened the next day** — `WhileSubscribed` stops the flow shortly after the
  UI stops collecting and restarts it on return, re-reading the clock. This is also the
  belt-and-braces half: if Doze defers the sleep while the screen is off, the restart on resume
  corrects it before anything is shown.

The delay is computed from `startOfDayMillis(today + 1)` rather than by adding 24 hours, so a 23-
or 25-hour DST day still lands on midnight, and it is clamped to at least 1ms so the loop cannot
spin.


### D26 — History is two queries, not a table ✅ *built, Phase 7*
**2026-09-27.** Both history views are `SELECT ... WHERE day < :today` over `plan_entries`. No new
tables, no schema bump — the database stayed at v4 through the whole phase. That is D4 paying off:
because a past plan entry *is* the wear record, "what have I worn" and "what have they seen" are
two orderings of data that already exists.

`today` is passed in as a parameter rather than read from SQLite's clock, so the rule is testable
and the app owns its own definition of now — and so the same query can be re-run at midnight from
`util/todayFlow` to roll a today-entry into the past while the screen is open.

Neither view filters `archived`. A retired group still has to be named on the day it saw something,
and a retired outfit still has to appear in the group that saw it — which is the whole reason
archiving exists instead of deleting (D9).


### D27 — One settings row, not a key/value store ✅ *built, Phase 8*
**2026-09-27.** `app_settings` is a single row at `id = 1` with a typed column per preference.
A key/value table would trade compile-time field names for runtime string keys and nullable reads,
in exchange for a flexibility nothing here needs — there is one app with one settings screen.
New preferences are new columns, each an additive migration Room generates.

The row does not exist until something is changed, and readers substitute the defaults, so a fresh
install needs no seeding step. Setters re-read the stored row before writing rather than writing
back the substituted default, which is what stops a future setter clobbering a column it does not
know about.

`PreferencesViewModel` is shared between `MainActivity` and the Settings screen — `viewModel()`
resolves to the activity's store — so changing the theme repaints straight away instead of on next
launch.

### D28 — R8 stays off until there is a release to ship ✅ *decided, Phase 8*
**2026-09-27.** `optimization { enable = false }` is unchanged. Enabling it means building and
testing a release variant, verifying keep rules against a minified build for Room and Coil, and
having a signing config — real work whose only payoff is a build nobody currently runs, since the
app is installed as a debug APK on one phone.

Revisit when there is a reason to produce a release build, and treat it as its own change with its
own on-device verification rather than a tick on a polish list.

### D29 — The launcher icon is generated, not drawn ✅ *built, 2026-10-02*
**2026-10-02.** A t-shirt, off-white on SlateInk. Both colours come straight from
`ui/theme/Color.kt`, so the icon is made of the app's own palette rather than a third scheme
nobody chose.

The awkward part is that one icon needs **thirteen files**. Adaptive icons are three vector
layers, but `minSdk 24` predates them, so API 24–25 also fall back to ten flat
`mipmap-*dpi` bitmaps. Drawing the tee twice — once as path data, once in an image editor —
is two representations that drift apart the first time anyone nudges a sleeve.

So `tools/icon/generate_launcher_icon.py` owns the outline, and emits both: the
`pathData` for the vectors and the rasterised bitmaps, from the same sampled curve. Re-running
it reproduces the committed `.webp` files byte for byte, which is the point — that is what
makes it the source rather than a thing that once produced them. **Editing the path by hand
leaves the ten bitmaps showing the old tee**, and nothing in the build will say so.

It also pins the shape inside the safe zone mechanically. A launcher may mask an adaptive icon
to any shape inside the central 72dp circle; the script reports the furthest point from centre
(27.1 of 36) instead of leaving that to the eye.

The `<monochrome>` layer is its own drawable, not the foreground re-used. That matters more
here than it looks: the target Nothing Phone (2a) has themed icons **on**, so the monochrome
layer is the one actually on the home screen — the SlateInk background is only seen with
themed icons off. Sibling app Reevz Mealz separates the layer the same way.

### D30 — "All" is a chip, not a hidden toggle ✅ *built, 2026-10-02*
**2026-10-02.** The Wardrobe type filter originally had four chips and no "All". Clearing it meant
tapping the active chip a second time, and the old comment in `WardrobeScreen.kt` presented that as
a saving: no separate "All" to maintain.

Reported by the user: *"after I click on a filter, I am not able to go back to that 'all' view."*
The toggle was working — verified on the phone, two taps on the same chip did clear it. The
gesture was simply invisible. Two things made it worse together:

- Nothing on screen suggests a selected chip is tappable *again*, and the thing it would do has no
  name anywhere in the UI.
- The palette is deliberately quiet (D-less, but see `ui/theme/Color.kt`), so a selected
  `FilterChip` differs from an unselected one by a few points of container luminance. On the dark
  theme the active chip is genuinely hard to pick out, so even "re-tap the selected one" is not
  answerable by looking.

A chip costs one line. The rule worth keeping: **the way back to the default state gets a visible
control, not a gesture** — doubly so when the design language is low-contrast by choice, because
a quiet palette spends its contrast budget on photographs and has none left over to signal state.

Re-selecting the active chip still clears the filter, since a chip that looks like a toggle should
behave like one. `setFilter(null)` already handled the "All" case, so the ViewModel was unchanged.

---

## Open — needs the user

*Nothing outstanding.* Phase-specific questions will be raised when their phase starts.
