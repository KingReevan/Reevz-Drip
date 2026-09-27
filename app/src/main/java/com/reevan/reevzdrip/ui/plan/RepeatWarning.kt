package com.reevan.reevzdrip.ui.plan

import com.reevan.reevzdrip.data.Sighting

/**
 * One line of "you have worn this in front of these people before".
 *
 * [inPast] decides the tense, which is the difference between a warning you can still act on and
 * one you cannot: a group that *saw* this last week is a fact, a group that is *also* seeing it
 * next Tuesday is a clash you can still go and fix.
 */
data class RepeatWarning(
    val groupName: String,
    val day: Int,
    val inPast: Boolean,
)

/**
 * Builds the repeat warning for assigning an outfit to [targetDay] in front of [chosenGroupIds].
 *
 * This is D13, and it is the app's whole purpose brought forward to the moment it is useful. The
 * requirements only ask for a *history* the user can go and look up; but "never repeat an outfit
 * in front of them" is the stated goal, and the app already knows the answer at the instant the
 * assignment is made. Leaving the user to remember to check turns a question the app can answer
 * into one they have to think of.
 *
 * Rules, each of which exists because the alternative reads wrong:
 *
 * - **One line per group, not per sighting.** A group that has seen an outfit four times produces
 *   one warning, not four; a wall of lines is noise, and the most relevant occasion is the point.
 * - **The nearest occasion wins**, measured in days from [targetDay]. The most recent time they
 *   saw it is what decides whether repeating feels stale, and a clash a week either side matters
 *   more than one six months ago.
 * - **[targetDay] itself is never a warning.** The entry being edited sits on that day, and an
 *   outfit cannot clash with itself. Callers exclude the entry by id as well; this is the
 *   belt-and-braces half.
 * - Groups with no history produce nothing, so an outfit that is new to everyone is silent rather
 *   than reassuring — there is nothing to say.
 *
 * Ordered nearest-first so the most pointed warning is the one that survives if the UI shows only
 * a couple.
 */
fun repeatWarnings(
    sightings: List<Sighting>,
    chosenGroupIds: Set<Long>,
    targetDay: Int,
    today: Int,
): List<RepeatWarning> =
    sightings
        .asSequence()
        .filter { it.groupId in chosenGroupIds }
        .filter { it.day != targetDay }
        .groupBy { it.groupId }
        .mapNotNull { (_, forGroup) ->
            val nearest = forGroup.minByOrNull { sighting ->
                // Distance first, then prefer the past: if a group saw it exactly as many days
                // before as after, "they saw this" is the more useful thing to say.
                kotlin.math.abs(sighting.day - targetDay) * 2 + if (sighting.day < today) 0 else 1
            } ?: return@mapNotNull null

            RepeatWarning(
                groupName = nearest.groupName,
                day = nearest.day,
                inPast = nearest.day < today,
            )
        }
        .sortedWith(compareBy({ kotlin.math.abs(it.day - targetDay) }, { it.groupName.lowercase() }))
