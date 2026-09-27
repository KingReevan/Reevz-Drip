# Requirements

Received 2026-09-25. The **Verbatim** section is the authority; everything after it is
interpretation and can be wrong. When the two disagree, the verbatim text wins and the
interpretation gets fixed.

---

## Verbatim

*The user's own words, unedited.*

> 1. It takes time for me to decide what clothes to wear to office and to gym. If I don't plan it
>    the previous night then I have to rush (this doesn't feel good).
> 2. The app must help me store clothing combinations.
> 3. I want to keep track of all the clothes I have. This will be done in the 'Wardrobe' section of
>    the app.
> 4. I want to be able to plan my outfits for any day of the week/month/year. There must be a
>    separate 'Plan' section in the app that will allow me to do this.
> 5. I must be able to create clothing combinations with the clothes I have in the 'Wardrobe'
>    section
> 6. The clothes are actually seen by people. I want to be able to know which group of people saw me
>    in which outfit. This will ensure that I never repeat an outfit in front of them. This means
>    that I should be able to create a group (example: 'colleagues' or 'college friends'). There
>    should be a section in the app called 'Groups.' I will be able to create groups in this section
>    and it will help me know which groups have seen me in which outfits at what dates. Then I will
>    know whether to repeat an outfit or not.
> 7. The app should be able to store photos. These will be photos of the individual clothing items
>    in the 'wardrobe' section. The combinations section of the app will just reuse these 'wardrobe'
>    photos by just grouping them together (outfit). In the UI, it will look like a photo collage
>    where each individual photo is a clothing item from the wardrobe section.
> 8. The type of clothes the application must support should be: top, pant, shoes and everything
>    else will come under 'accessories' like a watch, chain, hat etc. When I add clothes in the
>    wardrobe section, then app must ask me which type the clothing belongs to.
> 9. All clothes will appear like cards in the wardrobe UI (similar to how e-commerce shopping sites
>    show clothes). The card will show the name of the clothing, the photo (if available) and the
>    type.
> 10. I want the UI to be minimalistic.
> 11. The 'Wardrobe' is a section of the app where I can view all of my clothing. It must support
>     CRUD operations. I must be able to view clothes in a nice UI and I must also be able to add,
>     delete and update a piece of clothing. The adding of clothing is done by just giving a name for
>     the new clothing and optionally providing a photo and providing the type.
> 12. In the 'Combinations' section of the application, I can view all of my existing clothing
>     combinations. If I wanted to create a new combination then I could easily do it. Create a
>     combination just involves 'having a look' at the clothing items in the 'Wardrobe' section and
>     picking the clothes that I think would make a good outfit. Then I 'save' it and it appears as a
>     combination. It is essentially just grouping of the individual clothing items.
> 13. The app must have 5 sections:
>     1. Home
>     2. Wardrobe
>     3. Combinations
>     4. Groups
>     5. Settings
>
> This is how the entire flow is supposed to go:
>
> 1. I buy 3 new clothing items from a store. I add it to the app through the 'Wardrobe' section. I
>    provide the names for each clothing, then I provide a photo (optional) and then I put the type.
>    The UI for this will just look like a simple form.
> 2. Now, I am able to see the 3 individual clothing pieces in the 'Wardrobe' (could be t-shirt,
>    shirt, jeans etc.).
> 3. I then go to the Combinations section of the app and create a New Combination. The new
>    combination will involve the new clothing I just bought and maybe some existing clothing as
>    well. They will be grouped and shown in the Combination section.
> 4. I will save the combination and now I am able to view the outfit in the combination section.
> 5. I will then go to the Plan section of the application and I will choose the outfits for a day of
>    the week. Or month. Or year. It could be any day. I will only be able to plan for days that have
>    not arrived yet (future days, not past). I can also plan for the current day.
> 6. The planning in the plan section will be done by first choosing a day in the calendar UI and
>    then assigning an outfit to that day. I can assign more than one outfit to that day. Every time
>    an outfit is assigned, I have to also assign a 'Group'. The Group represents the people who are
>    going to see me in this outfit. I should be able to assign more than one Group.
> 7. Once I assign an outfit to a day and also assign the group who is going to see me in that outfit
>    for that day, then the entire flow is basically done. Now I just have to wait for that day to
>    arrive.
> 8. When that day arrives, I am able to see the previously planned clothes on the 'Home' section of
>    the app. The Home section is minimal and will only show the outfits to wear for today and in
>    front of which group. Nothing else. This 'Home' screen will be read only. It just takes whatever
>    clothes that got planned for a day in the plan section, and shows it in the Home section when
>    that day arrives.
> 9. When that day is over, I am able to go to the combinations section and click on a particular
>    outfit's card and see the history of when I wore it and who I wore it in front of (group). This
>    will help me not repeat the outfits in front of them.
> 10. I can also go to the 'Groups' section and check all the outfits which each group saw me wear.
>     It will appear as a history of what clothes I've worn in front of them.
>
> The settings section of the app will just contain all the typical stuff like theme toggle and if
> there is any configuration needed then you can just put it all in the settings.

---

## The problem being solved

Deciding what to wear takes time, and deciding it in the morning means rushing. The app moves that
decision to the previous night, and remembers it. The second job is social memory: not repeating an
outfit in front of the same people.

Two things follow from that, and they should steer every design call:

- **The morning path must be trivial.** Open app, see today's outfit, close app. Everything else in
  the app exists to make that one screen correct.
- **History is the payoff, not a feature.** "Have these people seen this already" is the question
  the app is really answering.

## Sections

| Section | What it does |
| --- | --- |
| **Home** | Read-only. Today's planned outfits and the group(s) who will see each. Nothing else. |
| **Wardrobe** | Every garment, as cards. Full CRUD. |
| **Combinations** | Saved outfits — groupings of wardrobe garments, shown as photo collages. Tapping one shows its wear history. |
| **Plan** | Calendar. Assign combinations (1..n) to a day, each with group(s). Today and future only. |
| **Groups** | Named groups of people. Full CRUD. Tapping one shows what it has seen you wear. |
| **Settings** | Theme toggle, plus any configuration that turns up later. |

⚠️ **That is six sections, but requirement 13 lists five** — it omits Plan, which requirements 4
and flow-step 5 both clearly require. Treated as an oversight in the list, not as a decision to fold
Plan into another section. See Q1 in `DECISIONS.md`.

## Entities

### Garment
A single item of clothing.

- `name` — required. The only mandatory field.
- `type` — required, one of **TOP / PANT / SHOES / ACCESSORY**. Accessory is the catch-all: watch,
  chain, hat, belt, anything that is not the other three.
- `photo` — **optional**. A garment with no photo is fully valid and must render cleanly.

Added through a simple form. Full CRUD.

### Combination
A saved outfit: *nothing more than a set of garments*. Requirement 12 is explicit — "essentially
just grouping of the individual clothing items."

- Holds references to garments. It does not copy them, and it does not own photos.
- **No structural rules.** No "must have exactly one top". The user picks whatever set they like.
- Displayed as a **photo collage** of its garments' photos.
- Tapping it opens its wear history: every past day it was planned for, and which groups saw it.

### Group
A named set of people — "colleagues", "college friends". Name is the only field.

Tapping a group shows the reverse view: every outfit that group has seen, with dates.

### Plan entry
An assignment of *one combination* to *one day*, carrying *one or more groups*.

- A day can hold **more than one** combination (office then gym is the motivating case).
- Each assignment **must** carry at least one group — requirement flow-step 6 says "Every time an
  outfit is assigned, I have to also assign a 'Group'." Treated as a hard validation rule.
- **Today and future days only.** Past days cannot be planned.

## History is derived, not recorded

There is no "confirm I actually wore this" step anywhere in the requirements, and Home is explicitly
read-only. So **a plan entry for a past day *is* a wear record.** History is a query over plan
entries with `day < today`, not a separate table.

This is the single biggest modelling decision in the app and it keeps things dramatically simpler.
Its consequence: once a day passes, its plan entries must become **immutable**, because editing them
would rewrite history. See Q2 in `DECISIONS.md`.

*(Reevz Mealz took the other road — it separates planned meals from eaten meals, because people
routinely eat something other than what they planned. Clothes are laid out the night before and
then worn, so the same split would be ceremony here. If it turns out outfits get skipped often,
revisit.)*

## UI

- **Minimalistic.** Requirement 10, and it applies everywhere.
- **Wardrobe** is a card grid, e-commerce style. Each card: photo (or a clean placeholder), name,
  type.
- **Combinations** are collage cards built from their garments' photos.
- **Home** is the most minimal screen in the app. Today's outfits, the groups who will see them,
  nothing else. No navigation-away, no stats, no prompts.
- Photos are the content here. The theme should stay out of their way.

## Out of scope

Not requested. Do not build these without being asked:

- Any "did you actually wear this?" confirmation step.
- Laundry / availability / in-the-wash state.
- Weather, occasion or season awareness.
- Outfit suggestions, scoring, or any automatic recommendation.
- Prices, cost-per-wear, or any spending tracking.
- Background removal or cutouts on garment photos.
- Sharing, export, backup to cloud, or multi-device sync.
- Notifications or reminders. *(The problem statement is about planning the night before, so an
  evening nudge is an obvious future fit — but it was not asked for.)*
