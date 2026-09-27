package com.reevan.reevzdrip.ui.plan

/**
 * Whether a day's plan can still be changed.
 *
 * **This deliberately differs from Reevz Mealz.** Mealz locks a day the moment it *begins*,
 * because from then on its Today screen owns what actually happened, and letting the plan be
 * rewritten afterwards would make the plan-versus-actual comparison meaningless.
 *
 * Drip has no plan-versus-actual split (D4), and the requirement is explicit: *"I will only be
 * able to plan for days that have not arrived yet (future days, not past). I can also plan for the
 * current day."* So today is **open** here, where in Mealz it is closed. Porting Mealz's rule
 * unchanged would have quietly broken the one day the user most needs to edit — the morning you
 * realise the plan is wrong.
 *
 * Past days are locked because a past entry *is* the record of what you wore (D7). Editing one
 * would not be correcting a plan, it would be rewriting history — and the whole point of the
 * history is to be trusted when it says a group has already seen something.
 */
enum class PlanLock {
    /** Today or later. The plan can be added to, changed and removed. */
    OPEN,

    /** The day is over. What was planned is now what was worn, and it stays as it was. */
    PASSED,
}

/**
 * [day] and [today] are epoch days, so this compares calendar dates rather than instants — which
 * is what makes the cutoff exactly midnight rather than twenty-four hours from now.
 */
fun planLock(day: Int, today: Int): PlanLock =
    if (day >= today) PlanLock.OPEN else PlanLock.PASSED

/** True for today and every day after it — the days the plan may be written to. */
fun isPlannable(day: Int, today: Int): Boolean = planLock(day, today) == PlanLock.OPEN

/** True only for [PlanLock.OPEN]. */
val PlanLock.isOpen: Boolean
    get() = this == PlanLock.OPEN
