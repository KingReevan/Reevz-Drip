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

### D7 — Past plan entries are locked 🔜 *Phase 5*
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

### D13 — Warn at planning time when a group has already seen an outfit 🔜 *Phase 5*
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

---

## Open — needs the user

*Nothing outstanding.* Phase-specific questions will be raised when their phase starts.
