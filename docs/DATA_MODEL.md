# Data model

Proposed Room schema for the requirements in `REQUIREMENTS.md`. **Nothing here is built yet.**
Tables land in the phase that needs them — see `ROADMAP.md` — not all at once.

Shape follows Reevz Mealz: `Long` autoGenerate primary keys, `epochDay` (`Int`/`Long`) for calendar
days, enums stored directly, `null` for "not recorded".

## Diagram

```
groups ──┐
         │  (many-to-many)
         └──< plan_entry_groups >──┐
                                   │
garments ──< combination_items >── combinations ──< plan_entries >── (day)
```

## Tables

### `garments`

```kotlin
@Entity(tableName = "garments")
data class Garment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val type: GarmentType,
    /** Filename inside filesDir/garments/, or null when no photo was provided. */
    val photoName: String? = null,
    val addedOn: Int,          // epochDay
    /** Retired from the wardrobe, still part of the outfits it belongs to. D9. */
    @ColumnInfo(defaultValue = "0") val archived: Boolean = false,
)

enum class GarmentType { TOP, PANT, SHOES, ACCESSORY }
```

`photoName` is a **filename, not a path** — an absolute path breaks the moment Android moves the
app's data directory, which it does on restore-to-new-device. The directory is resolved at read
time. Image bytes never go in the database.

Needs a `garmentOf(...)` builder in the Mealz style, owning name trimming and capitalisation, since
garments can be created from both the Wardrobe form and (possibly) the combination builder.

### `combinations`

```kotlin
@Entity(tableName = "combinations")
data class Combination(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** Optional label. Null means the collage identifies it. See Q3. */
    val name: String? = null,
    val createdOn: Int,        // epochDay
)
```

### `combination_items`

The grouping. Composite primary key — a garment appears in a combination at most once.

```kotlin
@Entity(
    tableName = "combination_items",
    primaryKeys = ["combinationId", "garmentId"],
    foreignKeys = [
        ForeignKey(Combination::class, ["id"], ["combinationId"], onDelete = CASCADE),
        ForeignKey(Garment::class,     ["id"], ["garmentId"],     onDelete = RESTRICT),
    ],
    indices = [Index("garmentId")],
)
data class CombinationItem(
    val combinationId: Long,
    val garmentId: Long,
)
```

> **Built without `position`.** This document originally specified a `position` column for collage
> order; the implementation derives the order from garment type instead. See D19 in
> `DECISIONS.md`.

Note the asymmetry, and it is deliberate:

- Deleting a **combination** cascades — its item rows are meaningless without it.
- Deleting a **garment** is `RESTRICT`, so the database refuses to orphan a combination silently.
  The UI handles this case explicitly rather than letting a delete quietly rewrite outfits the user
  has already worn. See Q4.

### `groups`

```kotlin
@Entity(tableName = "groups", indices = [Index(value = ["name"], unique = true)])
data class PeopleGroup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val createdOn: Int,
)
```

Named `PeopleGroup` in Kotlin — `Group` collides with Compose/Android names often enough to be
worth avoiding. Unique name: two groups called "Colleagues" is a data-entry slip, not a use case.

### `plan_entries`

One combination assigned to one day.

```kotlin
@Entity(
    tableName = "plan_entries",
    indices = [
        Index(value = ["day", "combinationId"], unique = true),
        Index("combinationId"),
    ],
    foreignKeys = [
        ForeignKey(Combination::class, ["id"], ["combinationId"], onDelete = RESTRICT),
    ],
)
data class PlanEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val day: Int,              // epochDay
    val combinationId: Long,
)
```

Unique `(day, combinationId)`: assigning the same outfit twice to one day says nothing. Different
outfits on one day are fine and expected — office then gym.

`RESTRICT` on combination for the same reason as garments: a plan entry in the past is a wear
record, and deleting the combination would erase it.

### `plan_entry_groups`

Who will see it. At least one row per plan entry — enforced in the ViewModel, since SQLite cannot
express "at least one child".

```kotlin
@Entity(
    tableName = "plan_entry_groups",
    primaryKeys = ["planEntryId", "groupId"],
    foreignKeys = [
        ForeignKey(PlanEntry::class,   ["id"], ["planEntryId"], onDelete = CASCADE),
        ForeignKey(PeopleGroup::class, ["id"], ["groupId"],     onDelete = RESTRICT),
    ],
    indices = [Index("groupId")],
)
data class PlanEntryGroup(val planEntryId: Long, val groupId: Long)
```

### `app_settings`

Single-row settings table, straight from Mealz: `id = 0`, `themeMode: ThemeMode`.

## Foreign keys need turning on

SQLite ignores foreign key constraints unless they are enabled per connection. Room does this for
you **only** when you ask:

```kotlin
Room.databaseBuilder(...)
    .setDriver(BundledSQLiteDriver())   // or the callback form
    .build()
```

Check that `RESTRICT` actually restricts with a test before relying on it for data integrity — a
silently-ignored constraint is worse than no constraint, because the code around it assumes
protection it does not have.

## The queries that matter

History is **derived from plan entries**, not stored. `today` is passed in as an epochDay parameter
rather than computed in SQL, so it is testable and so the app does not depend on SQLite's clock.

**Today's outfits (Home):**
```sql
SELECT * FROM plan_entries WHERE day = :today
```
…joined out to combinations, their garments, and each entry's groups.

**One combination's wear history (Combinations detail):**
```sql
SELECT * FROM plan_entries
WHERE combinationId = :id AND day < :today
ORDER BY day DESC
```

**One group's history (Groups detail):**
```sql
SELECT pe.* FROM plan_entries pe
JOIN plan_entry_groups peg ON peg.planEntryId = pe.id
WHERE peg.groupId = :groupId AND day < :today
ORDER BY pe.day DESC
```

**"Has this group already seen this outfit?"** — the question the whole app exists to answer, and
the one worth surfacing *at planning time*, in the Plan section, as a warning before the assignment
is saved:
```sql
SELECT day FROM plan_entries pe
JOIN plan_entry_groups peg ON peg.planEntryId = pe.id
WHERE pe.combinationId = :combinationId AND peg.groupId IN (:groupIds)
ORDER BY day DESC LIMIT 1
```

Return types should be Room **relation POJOs** (`@Relation`) rather than hand-joined flat rows —
the shapes here are genuinely nested (entry → combination → garments, entry → groups) and Room
builds them correctly for free.

## Photo storage

- Directory: `context.filesDir / "garments"`.
- Filename: a generated id (e.g. `UUID` + `.jpg`), never the garment name — names are user text and
  change on edit.
- On import: re-encode to a bounded long edge (~1080px) and a sane JPEG quality. A 12 MB camera
  original per garment is unaffordable on a device with no backup.
- Deleting a garment deletes its file. Replacing a photo deletes the old one.
- A row whose file is missing renders the placeholder — **never crashes**.
- The photo directory is *not* transactional with the database. Write the file first, then the row:
  a file with no row is a harmless orphan, a row with no file is a visible bug. A periodic orphan
  sweep can come later if it ever matters.
