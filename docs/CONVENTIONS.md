# Code conventions

Lifted from **Reevz Mealz**, which has ~70 source files following these patterns consistently. New
code in Reevz Drip should be indistinguishable in style. Examples below are real Mealz code with the
domain left intact — translate the nouns, keep the shape.

## Package layout

```
com.reevan.reevzdrip/
├── MainActivity.kt          one ComponentActivity, sets content to the app shell
├── data/                    entities, DAOs, the @Database — flat, no sub-packages
├── ui/
│   ├── AppSection.kt        the section enum (this is the navigation model)
│   ├── ReevzDripApp.kt      app shell: nav bar, header, section dispatch
│   ├── common/              composables shared by two or more screens
│   ├── theme/               Color / Shape / Theme / Type
│   └── <feature>/           one package per section: Screen, ViewModel, + its own helpers
├── notify/                  alarms and receivers (only if a phase needs them)
└── util/                    pure functions: dates, formatting, text case
```

One package per feature, holding `XScreen.kt` + `XViewModel.kt`, plus any pure helper that feature
owns. A helper used by two features moves to `ui/common/` or `util/`.

## ViewModel

Every ViewModel follows this exact shape:

```kotlin
data class FoodsUiState(
    val foods: List<Food> = emptyList(),
    val loaded: Boolean = false,
)

class FoodsViewModel(private val dao: FoodDao) : ViewModel() {

    val uiState: StateFlow<FoodsUiState> =
        dao.observeAll()
            .map { FoodsUiState(foods = it, loaded = true) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = FoodsUiState(),
            )

    fun save(...) { viewModelScope.launch { ... } }
    fun delete(food: Food) { viewModelScope.launch { dao.delete(food) } }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                FoodsViewModel(AppDatabase.getInstance(application).foodDao())
            }
        }
    }
}
```

Rules that matter here:

- **One `data class XUiState`** per ViewModel, all fields defaulted, exposed as a single
  `StateFlow`. Screens never collect two flows and reconcile them.
- A **`loaded` flag** distinguishes "no rows yet" from "not read yet", so an empty-state message
  never flashes before the first emission.
- **`SharingStarted.WhileSubscribed(5_000)`** — survives a configuration change without re-querying.
- Constructor takes **DAOs, not a Context**. The `Factory` in the companion object is the only place
  that knows about `Application`.
- Mutations are `fun` + `viewModelScope.launch`, returning `Unit`. The UI re-renders from the Flow;
  it does not read a return value.

## DAO

```kotlin
@Dao
interface FoodDao {

    /** All foods, alphabetical so the list stays predictable as it grows. */
    @Query("SELECT * FROM foods ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Food>>

    @Insert  suspend fun insert(food: Food): Long
    @Update  suspend fun update(food: Food)
    @Delete  suspend fun delete(food: Food)
}
```

- Reads return **`Flow`** and are named `observeX`. Writes are **`suspend`**.
- `ORDER BY` lives in the query, not in the ViewModel. Text sorts use `COLLATE NOCASE`.
- One DAO per entity, plus purpose-built read-only DAOs (Mealz has `SpendDao` for totals) when a
  query spans tables.

## Entity + the `xOf()` builder

This is the most transferable pattern in the codebase and worth copying deliberately.

```kotlin
@Entity(tableName = "foods")
data class Food(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val source: MealPlace,
    /** Price in paise, or null when [source] is [MealPlace.HOME]. */
    val pricePaise: Int? = null,
    val place: String? = null,
)

/** Builds a [Food] from raw form input, applying the rules every entry point must obey. */
fun foodOf(id: Long = 0L, name: String, source: MealPlace, ...): Food = Food(
    id = id,
    name = capitalizeWords(name.trim()),
    pricePaise = if (source == MealPlace.HOME) null else pricePaise,
    ...
)
```

The point: **normalisation lives in one free function, not in each screen.** Mealz has three
separate entry points that create a `Food`, and each would otherwise have to remember to trim,
capitalise, and null out the fields that don't apply. The `xOf()` function is also a pure function,
so the rules are unit-testable without a database.

Other entity rules:

- **Money is stored as an `Int` of paise**, never a `Float`/`Double`. Formatting happens in
  `util/Money.kt`. If Drip records garment prices, do the same.
- **"Not recorded" is `null`**, never `0` or `""`. One representation per meaning.
- Enums are stored directly; Room handles them.

## Composables

- `XScreen(viewModel: XViewModel = viewModel(factory = XViewModel.Factory))`, state read with
  `collectAsStateWithLifecycle()`.
- Screens are stateless below the top level — child composables take data + lambdas, never a
  ViewModel. That keeps them previewable.
- Editors are **bottom sheets** (`ModalBottomSheet`), one per entity: `FoodEditorSheet`,
  `BoughtItemEditorSheet`. Not full screens, not dialogs. Fewer taps, keyboard stays close to the
  thumb.
- Colours come from `MaterialTheme.colorScheme` **roles**. No `Color(0xFF...)` outside
  `ui/theme/Color.kt`.

## Pure functions and tests

Anything with logic in it gets extracted to a pure function under `util/` or next to its feature,
and gets a unit test. Mealz's 15 test files are almost all of this kind — `PlanLockTest`,
`MoneyTest`, `SpendPeriodTest`, `MonthGroupingTest`, `TextCaseTest`.

The rule of thumb: **if you would have to launch the app to check whether it is right, it belongs in
a pure function with a test instead.** Date-boundary maths especially — "is this day still
editable", "which week does this fall in" — is where the bugs are, and it is trivially testable once
it is not tangled into a ViewModel.

## Database

```kotlin
@Database(entities = [...], version = N, exportSchema = true, autoMigrations = [...])
abstract class AppDatabase : RoomDatabase() {
    abstract fun garmentDao(): GarmentDao

    companion object {
        private const val NAME = "reevz-drip.db"
        @Volatile private var instance: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, NAME,
                ).build().also { instance = it }
            }
    }
}
```

- Double-checked `@Volatile` singleton, `applicationContext` only.
- **A KDoc block above `@Database` lists the full schema history**, one line per version. Mealz is at
  v10 and every bump is recorded. Start this at v1.
- `@AutoMigration` for additive changes, hand-written `Migration` otherwise.
- **Never `fallbackToDestructiveMigration()`.**

## Comment style

Mealz comments the *why*, at length, and it is the reason the codebase is navigable. Match it:

- KDoc on every entity, DAO query, and non-obvious composable parameter.
- When a decision could reasonably have gone the other way, say why it went this way — Mealz's
  `Food.place` carries a paragraph on why it is free text instead of a shops table, and
  `AppSection` explains why six tabs became four plus a pause menu.
- Constraints that will bite later get stated where they will be read: the "all four slots must be
  visible without scrolling" requirement is written into the code that would break it.
- Don't comment the *what*. `// increment the counter` is noise.
