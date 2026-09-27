package com.reevan.reevzdrip.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Today's epoch day, re-emitted the moment the date changes.
 *
 * Used by every screen whose meaning depends on where "now" falls — Home ("today's outfits") and
 * both history views (`day < today`). **Not** used by Plan, which reads the clock once on purpose
 * so its lock cannot flicker across midnight mid-edit (D24 vs D25). The two behaviours are
 * genuinely different requirements, so pick deliberately rather than by habit.
 *
 * Two mechanisms cover the two ways midnight passes, and both are needed:
 *
 * - **Screen left open overnight** — the flow sleeps exactly until the next local midnight and
 *   then emits again. Waiting for the instant costs one wakeup; polling every minute would cost
 *   1,440 to catch the same moment.
 * - **Backgrounded and reopened the next day** — collectors use `SharingStarted.WhileSubscribed`,
 *   which stops the flow shortly after the UI stops collecting and restarts it on return, re-reading
 *   the clock. That is also the belt-and-braces half: if Doze defers the sleep while the screen is
 *   off, the restart on resume corrects it before anything is shown.
 */
fun todayFlow(): Flow<Int> = flow {
    while (true) {
        emit(todayEpochDay())
        delay(millisUntilNextMidnight())
    }
}
