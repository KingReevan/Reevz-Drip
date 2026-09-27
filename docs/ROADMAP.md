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
| 4 | Groups | Group CRUD | next |
| 5 | Plan | Calendar, assign outfits + groups | |
| 6 | Home | Today, read-only | |
| 7 | History | Combination detail, group detail | |
| 8 | Settings & polish | Theme toggle, empty states, a11y pass | |

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

## Phase 4 — Groups ← next

The smallest phase. Straight CRUD.

- [ ] `PeopleGroup` entity (unique name), schema bump + `@AutoMigration`
- [ ] `GroupDao`, `GroupsViewModel`, `GroupsScreen`, editor sheet
- [ ] Archive rather than delete a group that has seen something (D9)
- [ ] Detail view — the shell that Phase 7 fills with history

## Phase 5 — Plan

The most logic-heavy phase. Most of the bugs in this app will live here.

- [ ] `PlanEntry` + `PlanEntryGroup` entities, schema bump + `@AutoMigration`
- [ ] **Reroute combination and group deletion to archive-if-worn (D9).** Phase 3 and 4 hard-delete
      a combination and a group, which is safe only while nothing can have been *worn*. Plan entries
      end that, exactly as combinations ended it for garments in Phase 3 — so this lands in the same
      phase, not after.
- [ ] Calendar day picker — port Mealz's `DayPicker` (week strip / month grid toggle)
- [ ] **Past days are not plannable.** Pure, tested predicate — the Mealz `PlanLock` pattern
- [ ] Assign a combination to a day; more than one per day
- [ ] **Assigning requires at least one group.** Save is blocked until one is chosen
- [ ] Edit and remove assignments — today and future only
- [ ] Repeat warning (D13): non-blocking, naming the group and the date it last saw this

Everything date-shaped goes in a pure function with a unit test. "Is this day still plannable",
"which days does this month strip cover", "has this group seen this" — all testable without a
device, and all places where a subtle off-by-one is invisible until it matters.

**Done when** an outfit and its groups can be assigned to a future day, and the past is genuinely
unreachable.

## Phase 6 — Home

Small, and the whole point.

- [ ] `HomeViewModel` — today's plan entries with their combinations and groups
- [ ] `HomeScreen` — read-only. Outfits for today, groups who will see each. Nothing else
- [ ] Empty state for a day with nothing planned
- [ ] Rolls over at midnight without needing a restart

**Done when** opening the app in the morning answers "what am I wearing" with no taps.

## Phase 7 — History

Now that plan entries can be in the past, the payoff.

- [ ] Combination detail — every past day it was worn, and which groups saw it, newest first
- [ ] Group detail — every outfit that group has seen, with dates
- [ ] Both read-only, both derived from plan entries (`day < today`), no new tables
- [ ] Empty states for never-worn outfits and groups that have seen nothing

**Done when** tapping an outfit answers "who has already seen this".

## Phase 8 — Settings & polish

- [ ] `AppSettings` single-row entity + theme toggle (System / Light / Dark), Mealz pattern
- [ ] Empty states everywhere, checked with a genuinely empty database
- [ ] Accessibility pass — 48dp targets, content descriptions on garment photos
- [ ] Light/dark check on every screen, photos included
- [ ] Consider R8 (`optimization { enable = true }`) before any real release

---

## Not in the roadmap

From `REQUIREMENTS.md` → "Out of scope". Notably: no laundry state, no weather, no suggestions, no
"did you actually wear it" confirmation, no notifications. An evening "plan tomorrow" reminder is
the most natural future addition given the problem statement — Mealz has a working
`AlarmManager` + `BootReceiver` pattern to copy — but it was not asked for.
