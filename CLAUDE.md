# Reevz Drip

## What this project is

Reevz Drip is a **personal Android app** for planning outfits. It is the sibling of Reevz Mealz
(`../ReevzMealz`) — same owner, same phone, same engineering conventions, different domain.

It solves two problems: deciding what to wear takes time, and deciding it in the morning means
rushing — so the decision moves to the previous night and the app remembers it. And clothes are seen
by people, so the app tracks **which groups of people have seen which outfit, and when**, to avoid
repeating an outfit in front of the same group.

It is a personal app — not a multi-user SaaS product, not something that ships to a store with
accounts and a backend. Treat that as a design constraint, not a temporary phase.

> **Status: all nine phases (0–8) are built and verified on the phone.** Every flow in
> `docs/REQUIREMENTS.md` works end to end. There is no "next phase" — further work is whatever the
> user asks for next, and the same rules apply: name the change, explain it, verify it.

## Read this first

The requirements are **not** in this file. They live in `docs/`, and that is the source of truth:

| File | What it holds |
| --- | --- |
| `docs/REQUIREMENTS.md` | The user's own words, verbatim, plus the structured spec. Authoritative. |
| `docs/ROADMAP.md` | The nine phases. Build only the phase the user names. |
| `docs/DATA_MODEL.md` | Proposed Room schema, queries, photo storage rules. |
| `docs/TECH_STACK.md` | The stack, what the scaffold has, and what each phase must add. |
| `docs/CONVENTIONS.md` | Code patterns inherited from Reevz Mealz. Follow them. |
| `docs/DECISIONS.md` | Decisions made, and open questions still waiting on the user. |

Before implementing anything, re-read `docs/REQUIREMENTS.md` and the relevant phase in
`docs/ROADMAP.md`. Do not work from memory of an earlier conversation. **Check `docs/DECISIONS.md`
for open questions blocking that phase and ask them before writing code**, not after.

## The domain in one screen

Six sections: **Home, Plan, Combinations, Wardrobe, Groups, Settings.**

- A **Garment** is one item of clothing — name (required), type (TOP / PANT / SHOES / ACCESSORY),
  and an *optional* photo. Lives in Wardrobe, shown as e-commerce-style cards.
- A **Combination** is an outfit: nothing more than a *set of garments*. Shown as a photo collage of
  its garments. No structural rules — no "must have one top".
- A **Group** is named people — "colleagues", "college friends".
- A **Plan entry** assigns one combination to one day, carrying one or more groups. A day can hold
  several. **Today and future only.**
- **History is derived, not recorded.** A plan entry whose day has passed *is* a wear record. There
  is no "did you actually wear this" step, and there is no separate history table. This is the
  single most important modelling decision in the app — see D4 in `docs/DECISIONS.md` before
  touching the schema.

The user flow: add garments → group them into a combination → assign that combination to a future
day with the groups who will see it → on the day, Home shows it read-only → afterwards it becomes
history on both the combination and the group.

## Stack

- Kotlin + Jetpack Compose
- Material 3
- Room for local persistence — wired up, schema v1, exported to `app/schemas/`
- Coil 3 for image loading; photos are files on disk, never database blobs
- MVVM
- Android-first
- Local-first / offline-first

## Core principles

- Keep the app simple and fast to use.
- **Do not introduce a backend, authentication, cloud database, analytics, or AI** unless the user
  explicitly asks for it.
- Prefer local storage and offline functionality. An outfit app that needs a network to tell you
  what to wear has failed.
- Prefer Android/Jetpack libraries when they are appropriate.
- Keep the architecture understandable rather than over-engineered.
- Do not build abstractions for hypothetical future requirements. Solve the problem in front of you.

## Working rules

- **Build only the phase the user names.** The roadmap is a plan, not a queue to burn through.
  Finishing a phase means stopping and reporting, not starting the next one.
- Make small, focused changes. Do not modify unrelated files.
- Preserve existing functionality when implementing new features.
- **Before making an architectural change, explain the reason first.**
- **Before adding a dependency, explain why it is needed** and why an existing library or the
  standard library is not enough.
- Run the appropriate build/tests after significant changes.
- **Never claim a feature works without verifying it.** If verification did not run, or failed, say
  so plainly and show the output.
- Do not silently change project-wide configuration (Gradle files, version catalog,
  `gradle.properties`, manifest, theme). If a config change is genuinely required, call it out and
  explain it.
- When a requirement is ambiguous, ask — do not invent a reading and build on it. Record the answer
  in `docs/DECISIONS.md`.

### Feature workflow

1. Inspect the existing implementation.
2. Explain the proposed approach briefly.
3. Make the smallest reasonable change.
4. Build/test the project.
5. Report what changed and whether verification succeeded.
6. Update `docs/` if the change settled an open question or altered the plan.

## Build and verify

Windows / PowerShell (primary shell here):

```
.\gradlew.bat assembleDebug          # compile the app
.\gradlew.bat testDebugUnitTest      # host unit tests
.\gradlew.bat lint                   # Android lint
```

Instrumented tests (`connectedDebugAndroidTest`) need a running emulator or device; don't assume one
is attached.

Put logic worth testing in **pure Kotlin functions outside the ViewModel** so it can be covered by
host unit tests. Reevz Mealz does this consistently — date maths, money formatting, grouping, lock
rules — and has 15 unit test files as a result. Follow that.

## Git

**This project is not a Git repository yet.** Do not run `git init` unless the user asks. Once it
is one:

- Keep commits small and logically focused.
- Do not commit generated build artifacts, `local.properties`, IDE-specific files, or secrets.
- Do not rewrite Git history unless explicitly requested.
- **Do not push to GitHub unless explicitly requested.**
- Room schemas under `app/schemas/` **are** committed — they are the migration baseline.
- User photos and the database are never committed.

## UI guidelines

- Design primarily for a **phone**. Target device is a **Nothing Phone (2a)**.
- Optimize for quick **one-handed** interaction — common actions reachable with a thumb.
- Prioritize clarity and ease of use over visual complexity.
- Avoid unnecessary screens and navigation. Fewer taps to plan an outfit is the goal.
- Use Jetpack Compose and Material 3.
- Support **light and dark themes**. Route all new UI through the app theme and use colour *roles*,
  never hardcoded colours.
- Use accessible touch targets (48dp minimum) and readable typography.
- **Minimalistic** — the user asked for this explicitly, and it applies everywhere.
- **This is a visual domain.** Unlike a meal log, clothes are recognised by sight, not by name.
  Favour image-first layouts — grids of garment thumbnails over lists of text rows — wherever the
  screen is about choosing something to wear.
- **The photos are the content.** The theme stays out of their way: a quiet palette, not a loud one.
- **Home is the most minimal screen in the app.** Today's outfits and the groups who will see them.
  Nothing else — no stats, no prompts, no navigation-away. It is read-only by design.
- Every screen needs a real empty state. A brand-new install has no garments, no combinations and no
  groups, and that is the first thing the user will ever see.

## Data guidelines

- Wardrobe data is stored **locally**. No cloud, no sync.
- Once persistent data exists, database schema changes **must** use proper Room migrations. Do not
  rely on `fallbackToDestructiveMigration`.
- Room schemas are exported to `app/schemas/` and **are committed** — they are the baseline every
  future migration is written against. Schema changes mean bumping `@Database(version = ...)` and
  adding a real `Migration`.
- Additive schema changes (new table, new nullable column) should use `@AutoMigration`, which Room
  generates from the exported schema diff — far safer than hand-writing `CREATE TABLE` SQL that
  must match Room's expected hash exactly. Non-additive changes still need a manual `Migration`.
- Room migrations can only be tested with instrumented tests (`MigrationTestHelper`), so they need
  a device. Say plainly when a migration has not been exercised on one.
- **`connectedAndroidTest` uninstalls the app when it finishes, which deletes the database and every
  garment photo.** On the real phone that is the user's only copy of their wardrobe. Back it up
  first, binary-safe, and restore it afterwards. Verify with `PRAGMA integrity_check`, row counts,
  and a file count for the photo directory — not by eye.
- **Garment photos are user data too.** They live in `filesDir/garments/`; the database stores a
  *filename*, never a path and never the bytes. The rules, all implemented in Phase 2 and all worth
  keeping:
  - Import re-encodes to `MAX_PHOTO_EDGE` (1080) and bakes EXIF rotation into the pixels, because
    `Bitmap.compress` drops EXIF and the photo would otherwise come back sideways.
  - Import happens **on save**, never on pick, so a cancelled editor leaves nothing behind.
  - Deleting a garment deletes its file; replacing or removing a photo deletes the old one. The
    **row is written first**, then the old file removed — a file with no row is a harmless orphan,
    a row with no file is a visible hole.
  - A missing file degrades to the placeholder. `GarmentImage` is the single place this is handled;
    do not re-implement it per screen.
- Do not delete or reset user data as part of a normal feature implementation.
- Treat user-entered wardrobe data and photos as valuable and non-recoverable.

## Current project state

Single Gradle module `:app`, package `com.reevan.reevzdrip`. Room persists locally, fully offline.
Target device is a **Nothing Phone (2a)**; `minSdk 24` / `targetSdk 37` covers it.

**Built: all of it.** Five bottom tabs — Home, Plan, Outfits, Wardrobe, Groups — plus Settings
behind a top-bar icon. No placeholders remain.

**Wardrobe** shows every garment as a two-column card grid — photo slot, name, type — newest first,
filterable by type with a chip row whose active chip toggles off. Adding and editing happen in a
bottom sheet: a name (required), an optional photo, and a TOP / PANT / SHOES / ACCESSORY chip —
the order the user described. Delete asks first, by name.

**Combinations** are outfits: a set of garments, nothing more. The list is a grid of collage cards;
the builder is a full-screen 3-column wardrobe grid you tap to select from, with an optional name;
the detail view shows the collage large, the contents by type, and the wear-history section Phase 7
fills in. Collage order is **derived from garment type**, not stored — see D19.

**Groups** are named sets of people — the ones who will see an outfit. A plain list rather than a
card grid: a group is a word, not something to look at. Names are unique, checked
case-insensitively as you type (D22). The detail view holds the "what they've seen" history that
Phase 7 fills in.

**Plan** is a day picker (week strip or month grid, days with something planned carry a dot) over
the chosen day's assignments. Assigning opens a full screen where you pick one outfit and one or
more groups — both are required, and Save stays disabled until both are there. A day can hold
several outfits, but not the same one twice: the duplicate is greyed out as "Already on this day".
Choosing an outfit and groups fires the **repeat warning** (D13) — non-blocking, one line per
group, in the right tense for a past sighting versus a future clash.

**Today and every later day are editable; past days are read-only** (D7, D23). Past days stay
*visible*, because a past day is the record of what you wore.

**Settings** holds the theme toggle (System / Light / Dark), stored in a single-row `app_settings`
table (D27). `PreferencesViewModel` is shared with `MainActivity`, so a change repaints
immediately. New settings belong here as another panel.

**History** is two read-only queries, not a table (D26). An outfit's detail lists every **past**
day it was worn and who saw it; a group's detail lists every past outfit they have seen, with
dates. Both are `day < today` over `plan_entries` — which is D4 paying off, since a past plan entry
already *is* the wear record. Neither filters `archived`: a retired group must still be named on the
day it saw something.

**Home** is today's outfits and who will see each, read-only, and **nothing else** — no date
header, no edit affordance, no link through to Plan. The requirement is unusually specific about
what is absent, so resist adding to it. It rolls over at midnight without a restart (D25).

**Photos** come from the system Photo Picker or the camera. They are re-encoded on import to a
1080px long edge with EXIF rotation baked into the pixels, stored as `filesDir/garments/<uuid>.jpg`
with only the filename on the row. The import runs **on save, not on pick**, so abandoning the
editor cannot orphan a file.

**The launcher icon** is a t-shirt, off-white on SlateInk — the app's own two colours. It is
*generated*: `tools/icon/generate_launcher_icon.py` owns the outline and emits both the three
vector layers and the ten legacy `mipmap-*dpi` bitmaps that `minSdk 24` still needs, so the two
representations cannot drift. Change the shape there and re-run it; hand-editing the path leaves
the bitmaps stale (D29). The phone has themed icons on, so the `<monochrome>` layer is the one
actually on the home screen.

```
app/src/main/java/com/reevan/reevzdrip/
├── MainActivity.kt              single ComponentActivity, hosts ReevzDripApp
├── data/
│   ├── Garment.kt               @Entity + GarmentType enum + garmentOf() builder
│   ├── GarmentDao.kt            observeAll(Flow) w/ outfit counts, archive, insert/update/delete
│   ├── Combination.kt           @Entity + CombinationItem + relation POJO + orderedForCollage()
│   ├── CombinationDao.kt        @Relation/@Junction reads, transactional create/replace
│   ├── PeopleGroup.kt           @Entity + peopleGroupOf() + isDuplicateGroupName()
│   ├── PlanEntry.kt             @Entity + PlanEntryGroup + details POJO + Sighting
│   ├── PlanDao.kt               day/usage/sighting queries, transactional assign + reassign
│   ├── GroupDao.kt              observeAll(Flow), archive, insert/update/delete
│   ├── PhotoSizing.kt           pure: sample size, scaling, EXIF orientation (tested)
│   ├── PhotoStore.kt            import / delete / camera target — all the Android-side IO
│   ├── AppSettings.kt           single-row @Entity + ThemeMode enum
│   ├── AppSettingsDao.kt        observe / get / upsert
│   └── DripDatabase.kt          @Database v5, singleton, exportSchema, foreign_keys=ON
├── ui/
│   ├── AppSection.kt            the six sections — this is the navigation model
│   ├── ReevzDripApp.kt          shell: top bar, nav bar, section dispatch, back handling
│   ├── combinations/
│   │   ├── CombinationsScreen.kt      list + builder/detail dispatch, own BackHandler
│   │   ├── CombinationsViewModel.kt   outfits + the wardrobe the builder picks from
│   │   ├── CombinationCard.kt         collage card
│   │   ├── CombinationBuilderScreen.kt  full-screen picker
│   │   └── CombinationDetailScreen.kt   collage, contents, wear history
│   ├── common/
│   │   ├── CombinationCollage.kt  the collage layouts + collagePlan()
│   │   ├── GarmentImage.kt        the ONE place a garment photo is drawn
│   │   └── EmptyState.kt          the "nothing here yet" state
│   ├── home/
│   │   ├── HomeScreen.kt          today's outfits, read-only
│   │   └── HomeViewModel.kt       HomeUiState + the midnight-rollover flow
│   ├── groups/
│   │   ├── GroupsScreen.kt        list + detail dispatch, own BackHandler
│   │   ├── GroupsViewModel.kt     GroupsUiState + save/delete
│   │   ├── GroupEditorSheet.kt    one-field sheet, live duplicate check
│   │   └── GroupDetailScreen.kt   name, rename/delete, what they've seen
│   ├── settings/
│   │   ├── SettingsScreen.kt      panels; today just the theme
│   │   └── PreferencesViewModel.kt  shared with MainActivity
│   ├── plan/
│   │   ├── PlanScreen.kt          picker + the day's assignments, own BackHandler
│   │   ├── PlanViewModel.kt       PlanUiState + assign/remove/warningsFor
│   │   ├── DayPicker.kt           week strip / month grid, ported from Mealz
│   │   ├── AssignOutfitScreen.kt  pick one outfit + groups, repeat warning
│   │   ├── PlanLock.kt            pure: which days may be written to (differs from Mealz)
│   │   └── RepeatWarning.kt       pure: "they've seen this before"
│   ├── theme/                   Color / Theme / Type — quiet neutral, no dynamic colour
│   └── wardrobe/
│       ├── WardrobeScreen.kt    card grid, type filter, FAB, delete dialog
│       ├── WardrobeViewModel.kt WardrobeUiState + save/delete/setFilter
│       ├── GarmentCard.kt       the card
│       └── GarmentEditorSheet.kt  add/edit sheet, incl. picker + camera launchers
└── util/
    ├── Dates.kt                 epoch-day ↔ millis conversion, calendar helpers
    ├── Today.kt                 todayFlow() — re-emits at midnight (Home + both histories)
    └── TextCase.kt              capitalizeWords, ported from Mealz
```

**The schema is complete** for the requirements as written: `garments`, `combinations` +
`combination_items`, `groups`, `plan_entries` + `plan_entry_groups`, `app_settings`. Database at
v5, five exported schemas in `app/schemas/`, every migration an `@AutoMigration` exercised against
live data on the phone.

**R8 is still off** (`optimization { enable = false }`), deliberately — see D28. Turn it on when
there is a reason to ship a release build, as its own change with its own verification.

### Deleting things: the rule, and the debt it carries

D9 in `docs/DECISIONS.md`: anything with history is **archived**, not destroyed. A garment used by
an outfit sets `archived = 1`, keeps its photo file, disappears from the Wardrobe and the builder,
and still renders inside its outfits. A garment nothing references is really deleted, photo and all.
`GarmentDao.observeAll` carries each garment's outfit count so the delete dialog can say which one
is about to happen. There is deliberately **no un-archive** — archiving is what delete *means* here.

### The delete rule is now complete

Every deletable thing follows D9: **archived if it has history, really deleted if it has none.**
Garments archive when an outfit uses them, outfits when a day is planned around them, groups when
they are attached to a planned outfit. Each delete dialog says which of the two is about to happen,
and each `observeAll` hides archived rows while every history query still shows them. There is no
un-archive anywhere, by design.

Nothing is left hard-deleting something that could have history. If a future phase adds another
entity that can be referenced, apply the same rule **in the phase that makes the reference
possible** — that is the trap Phase 1 set for Phase 3, and Phases 3 and 4 set for Phase 5.

### The clock is read two different ways, on purpose

`PlanViewModel` reads it **once** at construction, so its lock cannot flicker across midnight
mid-edit (D24). `HomeViewModel` **re-emits at midnight**, because today's outfits must appear when
the day arrives (D25). Neither is the "right" pattern to copy blindly — pick by asking whether the
screen would rather be a few hours stale or change under the user's hands.

`util/Today.kt` holds the shared rolling flow; Home and both history views use it, Plan does not.

## Toolchain notes (non-obvious — read before touching Gradle)

This project is on a very new toolchain, and several things differ from older Android setups:

- **AGP 9.3.3**, Gradle 9.5.0, Kotlin 2.2.10, Compose BOM 2026.02.01.
- **There is no `org.jetbrains.kotlin.android` plugin.** AGP 9 compiles Kotlin itself. Only
  `com.android.application` and `org.jetbrains.kotlin.plugin.compose` are applied. Don't "fix" this
  by adding the Kotlin Android plugin.
- `compileSdk` uses the AGP 9 block form: `compileSdk { version = release(37) }`. `targetSdk 37`,
  `minSdk 24`.
- Release build type uses `optimization { enable = false }` — the AGP 9 replacement for
  `isMinifyEnabled` / `proguardFiles`. R8 is currently off; that needs flipping before any real
  release.
- R8 keep rules live in `app/src/main/keepRules/rules.keep`, not `proguard-rules.pro`.
- Gradle **configuration cache is enabled**. Build logic that isn't configuration-cache-safe will
  fail the build.
- All dependencies and versions go through the version catalog at `gradle/libs.versions.toml` —
  never hardcode a version in `app/build.gradle.kts`.
- `compileOptions` targets **Java 17**, raised from the template's Java 11 in Phase 0 to match Reevz
  Mealz.
- The Gradle daemon toolchain is a newer JDK than `compileOptions` targets. That is intentional and
  fine — the daemon JVM is not the bytecode target. Kotlin follows `compileOptions` on its own, so
  there is no `jvmTarget` / `jvmToolchain` block and none is needed.

### Room and KSP (wired up in Phase 0 — don't undo these)

- `ksp = "2.2.10-2.0.2"` in the catalog. **KSP must track the Kotlin version** — Kotlin 2.2.10 pairs
  with KSP `2.2.10-2.0.2`. A mismatched pair fails at configuration time, so these two move
  together or not at all.
- **`android.disallowKotlinSourceSets=false` in `gradle.properties` is load-bearing. Do not remove
  it.** KSP registers its generated source directories through `kotlin.sourceSets`, which AGP 9's
  built-in Kotlin rejects by default (AGP issue #386221070). Without the flag the build fails with
  *"Using kotlin.sourceSets DSL to add Kotlin sources is not allowed with built-in Kotlin"*. It only
  relaxes that ownership check — it does not disable built-in Kotlin. Drop it once AGP and KSP stop
  conflicting.
- Schemas export to `app/schemas/` via `ksp { arg("room.schemaLocation", ...) }`. `1.json` exists
  and is the baseline every future `@AutoMigration` is generated against. Keep it.
- Lint reports newer versions available for AGP, Kotlin, the Compose BOM and Room. Left alone
  deliberately — bumping the toolchain is its own change, with its own verification, not a drive-by.
