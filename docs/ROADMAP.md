# Roadmap

Nine phases. **The user names the phase; the assistant builds that phase and stops.** Finishing
Phase 3 is not permission to start Phase 4.

Each phase is a vertical slice — schema + DAO + ViewModel + screen, working end to end on the phone
— and ends with a build, the relevant tests, and an honest report of what was and was not verified.

The order follows the user's own flow: you cannot make a combination before there are garments, and
you cannot plan before there are combinations and groups. Home comes late because it is the payoff
of Plan, and history comes last because it needs plan entries with dates in the past to show
anything at all.

| # | Phase | Delivers | Status |
| --- | --- | --- | --- |
| 0 | Foundation | Build config, Room, theme, app shell | ✅ **done** 2026-09-25 |
| 1 | Wardrobe | Garment CRUD, card grid, no photos | ✅ **done** 2026-09-25 |
| 2 | Photos | Capture/pick, storage, thumbnails | ✅ **done** 2026-09-25 |
| 3 | Combinations | Build/view/edit outfits, collage cards | ✅ **done** 2026-09-27 |
| 4 | Groups | Group CRUD | ✅ **done** 2026-09-27 |
| 5 | Plan | Calendar, assign outfits + groups | ✅ **done** 2026-09-27 |
| 6 | Home | Today, read-only | ✅ **done** 2026-09-27 |
| 7 | History | Combination detail, group detail | ✅ **done** 2026-09-27 |
| 8 | Settings & polish | Theme toggle, empty states, a11y pass | ✅ **done** 2026-09-27 |

All eight open questions were answered on 2026-09-25 and are recorded as D6–D15 in
`DECISIONS.md`.

---

## Phase 0 — Foundation ✅

Stack work, no product surface. Detail in `TECH_STACK.md` → "Gap list".

- [x] KSP + Room in the version catalog, root build file, app build file
- [x] `android.disallowKotlinSourceSets=false` in `gradle.properties` — **load-bearing**
- [x] Schema export to `app/schemas/` — `1.json` is committed as the migration baseline
- [x] `lifecycle-viewmodel-compose` + `lifecycle-runtime-compose`
- [x] `compileOptions` Java 11 → 17
- [x] `DripDatabase` at v1 with `garments` only, singleton in the Mealz style, `PRAGMA
      foreign_keys=ON` via an `onOpen` callback
- [x] `ui/AppSection.kt` — six sections, five tabs plus Settings in the top bar (D6)
- [x] `ui/ReevzDripApp.kt` — shell: nav bar, top bar, section dispatch, back handling
- [x] Quiet neutral theme (D10) in `ui/theme/`, dynamic colour off, day/night window background
- [x] `util/Dates.kt` — epoch-day conversion (D14) + `util/TextCase.kt` ported from Mealz
- [x] Eleven vector icons drawn in `res/drawable/`

**Done:** `assembleDebug` passes, all six sections are reachable on the phone, back handling
behaves, and both themes render.

## Phase 1 — Wardrobe ✅

The core noun. Everything else builds on it.

- [x] `Garment` entity + `GarmentType` enum + `garmentOf()` builder (pure, 6 unit tests)
- [x] `GarmentDao` — `observeAll()` as `Flow`, insert/update/delete
- [x] `WardrobeViewModel` with a single `WardrobeUiState`
- [x] `WardrobeScreen` — two-column card grid. Card = photo slot + name + type
- [x] `GarmentEditorSheet` — the "simple form": name + type chips. Save disabled on a blank name
- [x] Delete with a named confirmation dialog
- [x] Filter by type, the active chip toggling it off
- [x] `rememberSaveable` throughout, so a sunset theme flip keeps your place (D15)

**Deliberately no photos.** Splitting them out keeps Phase 1 to one uncomplicated slice and gets a
usable screen on the phone fast — and requirement 11 makes photos optional anyway, so a
text-and-placeholder Wardrobe is a coherent product, not a stub.

**Done:** verified on the phone — four garments added, renamed, retyped, filtered and deleted;
survives a cold restart; database inspected directly (`integrity_check` ok, `addedOn` = 20721 =
2026-09-25).

> Four test garments (Blue Oxford Shirt, Black Jeans, White Sneakers, Steel Watch) were left on the
> phone from that verification. Delete them from the Wardrobe whenever you like.

## Phase 2 — Photos ✅

The riskiest phase, and the one with no precedent in Reevz Mealz. Isolated on purpose.

- [x] Coil 3.3.0 added (it also supplies `exifinterface`, which is declared directly since the
      import path uses it rather than leaning on a transitive)
- [x] Photo Picker **and** camera capture, with a `FileProvider` over a cache scratch dir (D12)
- [x] Re-encode on import to a 1080px long edge; EXIF rotation and mirroring baked into the pixels
- [x] `filesDir/garments/`, UUID filenames, `photoName` on the row
- [x] Delete garment → delete file; replace photo → delete old file; remove photo → delete file
- [x] Missing file renders the placeholder, never crashes
- [x] Wardrobe cards and the editor preview show real photos
- [x] Import happens **on save**, not on pick, so abandoning the editor leaves no orphan file

`photoName` **already exists** on the `garments` table from Phase 1, unused — so this phase needs no
schema bump and no migration at all. `GarmentCard.GarmentPhoto()` is the single function to change;
every surface that shows a garment goes through it.

**Done:** verified on the phone with two generated test images.

- A 4000x3000 JPEG imported to exactly **1080x810**, 216 KB → 11.5 KB.
- A 1200x900 JPEG tagged EXIF orientation 6 imported to **810x1080 portrait** with the corner
  marker still on the left — so the rotation was baked in and nothing was mirrored.
- Replace left exactly one file; remove and garment-delete each left zero; a file deleted out from
  under a live row fell back to the placeholder with no crash.

One bug found and fixed during verification: `SubcomposeAsyncImage` passes its `contentScale` down
into the loading/error slots, which blew the 28dp placeholder icon up to fill the card. Replaced
with a plain `AsyncImage` drawn over an always-present placeholder — simpler, and it drops
subcomposition from every grid cell.

## Phase 3 — Combinations ✅

- [x] `Combination` + `CombinationItem` entities, schema **v2** with `@AutoMigration(1, 2)`
- [x] `CombinationDao` with a `@Relation` + `@Junction` POJO, transactional create/replace
- [x] Builder: a full-screen 3-column wardrobe grid, tap to select, name, save
- [x] Collage composable — 1, 2, 3 and 4+ layouts, garments with and without photos, "+N" overflow
- [x] List, edit (add/remove garments), delete
- [x] Detail view — collage, contents by type, and the history section Phase 7 fills
- [x] **`archived` column added and the Wardrobe delete path rerouted (D9).** A garment used by
      any outfit is archived; an unused one is still hard-deleted with its photo
- [x] `GarmentDao.observeAll` now carries each garment's outfit count, so the delete dialog can
      say which of the two things is about to happen
- [x] **No `position` column** — collage order is derived from garment type (D19)

The collage is the piece worth prototyping before committing: it has to look right at 2 garments and
at 7, with some of them missing photos.

**Done:** verified on the phone, including the **first real migration** — the device held a live
v1 database and came through at v2 with every garment intact, `archived` defaulted to 0,
`foreign_key_check` clean and `integrity_check` ok. Run twice, since the post-test restore migrated
a v1 backup again.

Also verified: archiving a garment used in 2 outfits left both outfits and all 5 membership rows
untouched; deleting an outfit cascaded its membership rows and touched no garment.

One bug found and fixed during verification: editing an outfit that contained an archived garment
showed *"3 selected"* above two ticked tiles, because the archived garment was in the selection but
not in the picker — so it could not be seen or deselected. The builder now also offers the garments
an outfit already holds, labelled "Removed".

## Phase 4 — Groups ✅

The smallest phase. Straight CRUD.

**Done:** verified on the phone. v2 → v3 migrated a live database with every garment intact and
`integrity_check` ok; groups created, renamed and deleted; the duplicate check rejected
"COLLEGE FRIENDS" against an existing "College Friends"; light and dark both checked. Test groups
were removed afterwards, so the section is empty.

- [x] `PeopleGroup` entity (unique name), schema **v3** with `@AutoMigration(2, 3)`
- [x] `GroupDao`, `GroupsViewModel`, `GroupsScreen` (a plain list, not a card grid), editor sheet
- [x] Case-insensitive duplicate-name check as you type, with the unique index as the backstop
- [x] Detail view — name, rename/delete, and the history section Phase 7 fills
- [x] `groups.archived` column added **now**, unused, so Phase 5 needs no schema bump for it (D21)
- [ ] ~~Archive rather than delete a group that has seen something~~ — **moved to Phase 5.** Nothing
      can have *seen* anything until plan entries exist, so hard delete is correct today and
      `GroupDao.archive` is already written for the reroute

## Phase 5 — Plan ✅

The most logic-heavy phase. Most of the bugs in this app will live here.

- [x] `PlanEntry` + `PlanEntryGroup` entities, schema **v4** with `@AutoMigration(3, 4)`
- [x] **Combination and group deletion rerouted to archive-if-worn (D9)** — the debt from Phases 3
      and 4, paid in the phase that created it. Both dialogs say which of the two will happen
- [x] Calendar day picker ported from Mealz, converted to epoch days
- [x] **Past days are not plannable** — but they *are* viewable, since a past day is the record of
      what you wore. `planLock` is pure and tested, and deliberately differs from Mealz (D23)
- [x] Assign a combination to a day; more than one per day, with the duplicate greyed out
- [x] **Assigning requires at least one group** — Save disabled, and refused in the ViewModel too
- [x] Edit and remove assignments — today and future only
- [x] Repeat warning (D13): non-blocking, one line per group, correct tense for past vs future

Everything date-shaped goes in a pure function with a unit test. "Is this day still plannable",
"which days does this month strip cover", "has this group seen this" — all testable without a
device, and all places where a subtle off-by-one is invisible until it matters.

**Done:** verified on the phone. v3 → v4 migrated a live database with every garment intact,
`foreign_key_check` and `integrity_check` clean; the same again on the post-test restore.

Verified end to end: an outfit assigned to today and to tomorrow with a group; a past day showing
"Yesterday" with **no add button** and past-tense empty text; the repeat warning firing with the
right tense; the duplicate outfit greyed out as "Already on this day"; archiving a planned outfit
and a group that had seen one, both leaving every plan row untouched.

**Not verified through the UI:** the *past-tense* branch of the repeat warning. Creating a past
sighting needs a plan entry on a past day, which the lock correctly makes impossible, and changing
the device clock needs root. It is covered by unit tests instead — noted rather than glossed.

## Phase 6 — Home ✅

Small, and the whole point.

- [x] `HomeViewModel` — today's plan entries with their combinations and groups
- [x] `HomeScreen` — read-only. Outfits for today, groups who will see each. Nothing else: no date
      header, no edit affordance, no link through to Plan
- [x] Empty state for a day with nothing planned
- [x] Rolls over at midnight without needing a restart (D25)

**Done:** verified on the phone. An outfit planned for today appears on Home with its group and
nothing else; the empty state reads correctly with nothing planned; both themes checked; the screen
survives a background/resume and a configuration change without crashing.

**Not verified on-device: the midnight rollover itself.** Observing it needs either a nine-hour
wait or setting the device clock, which needs root — and changing the phone's clock to prove a
point is not a trade worth making on someone's daily driver. What *is* verified: the delay
computation has five unit tests (lands exactly on midnight from any hour, always positive so the
loop cannot spin, crosses a month boundary), and the resume path is standard `WhileSubscribed`
behaviour, exercised by the background/resume test above. Stated here rather than glossed.

## Phase 7 — History ✅

Now that plan entries can be in the past, the payoff.

- [x] Combination detail — every past day it was worn, and which groups saw it, newest first
- [x] Group detail — every outfit that group has seen, with dates and a collage
- [x] Both read-only, both derived from plan entries (`day < today`), **no new tables, no schema
      bump** — the database stayed at v4
- [x] Empty states for never-worn outfits and groups that have seen nothing
- [x] `todayFlow` extracted to `util/Today.kt` and shared with Home, so history rolls a
      today-entry into the past at midnight without reopening the screen (D25)
- [x] `describeAudience` — "Colleagues and Gym" rather than a comma-separated dump

**Done:** verified on the phone with injected fixture data — a plan entry in the **past** cannot be
created through the UI, because the Phase 5 lock correctly forbids it, so the rows were written
straight into the database instead. That exercises the read paths, which is all this phase adds;
the write paths were verified in Phase 5.

With entries seeded at today−10, today−3, **today** and **today+5**:

- "Office Friday" listed exactly the two past days, newest first, and the two-group day read
  "Colleagues and Gym".
- The **today** entry and the **future** entry were correctly absent from both views — the
  `day < today` rule doing its job.
- The Colleagues group listed the same two days with collages.
- A never-worn outfit showed its empty state.

**Not verified on-device: the "group has seen nothing" empty state.** The phone was picked up and
in use partway through, so driving it further would have been fighting the user for the screen. It
is the same `when (!loaded / isEmpty / else)` shape as the outfit empty state, which *was* seen
working. Noted rather than glossed.

## Phase 8 — Settings & polish ✅

- [x] `AppSettings` single-row entity + theme toggle (System / Light / Dark), schema **v5** with
      `@AutoMigration(4, 5)`. Shared `PreferencesViewModel` between `MainActivity` and Settings, so
      a change repaints immediately rather than on next launch
- [x] Empty states everywhere — every one has now been seen rendering on the phone, including the
      "this group has seen nothing" state that Phase 7 left unverified
- [x] Accessibility pass — **one real fix**: the calendar day cell was a 42dp touch target, under
      the 48dp minimum. `minimumInteractiveComponentSize()` reserves the full 48dp for the tap
      without growing the circle. Every other tappable thing is either a Material 3 component
      (which handles its own minimum) or a grid cell far above it
- [x] Light/dark checked on every screen across the phases, photos included
- [x] Dead code removed — `SectionPlaceholder` had no callers left once every section was built
- [ ] **R8 left off, deliberately.** See below.

**Done:** verified on the phone. v4 → v5 migrated a live database with every garment intact and
`integrity_check` clean, twice counting the post-test restore. The theme toggle was exercised both
ways against an opposing system setting — **app dark while the phone was light, then app light
while the phone was dark** — repainting immediately and surviving a cold restart.

### On R8

The roadmap said "consider R8 before any real release", and the considered answer is **not yet**.
Turning `optimization { enable = true }` on means building and testing a *release* variant: Room
and Coil both do reflective work that needs keep rules verified against a minified build, and a
signing config has to exist. That is a real piece of work.

There is currently no release — the app is installed as a debug APK on one phone, and the user
never sees a minified build. Flipping the switch today would buy nothing and risk a class of bug
that only appears in the variant nobody runs. **Do it when there is a reason to ship a release
build, and treat it as its own change with its own on-device verification** — not as a tick on a
polish list.

---

## Not in the roadmap

From `REQUIREMENTS.md` → "Out of scope". Notably: no laundry state, no weather, no suggestions, no
"did you actually wear it" confirmation, no notifications. An evening "plan tomorrow" reminder is
the most natural future addition given the problem statement — Mealz has a working
`AlarmManager` + `BootReceiver` pattern to copy — but it was not asked for.
