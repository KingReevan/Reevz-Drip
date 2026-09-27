package com.reevan.reevzdrip.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * A deliberately quiet, near-neutral palette.
 *
 * Every screen in this app is wall-to-wall photographs of clothes. A saturated palette would
 * compete with that content — the garment is the thing you are trying to recognise, not the
 * chrome around it. So the scheme is greys with a single dark-slate accent carrying the
 * interactive roles, and colour is spent only where it means something (errors).
 *
 * Sibling app Reevz Mealz went the other way, with a loud fixed arcade palette, because a list of
 * meal names has no imagery of its own to protect.
 */

// Light — warm-neutral greys on an off-white ground.
val SlateInk = Color(0xFF2E2E31)
val SlateInkDim = Color(0xFF5A5A5E)
val StoneLine = Color(0xFFC9C9CE)
val StoneLineSoft = Color(0xFFE5E5E9)
val StoneFill = Color(0xFFF1F1F4)
val StoneCard = Color(0xFFFFFFFF)
val StoneGround = Color(0xFFFAFAFB)
val ErrorLight = Color(0xFFB3261E)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFF9DEDC)
val OnErrorContainerLight = Color(0xFF410E0B)

// Dark — true-ish blacks so photographs sit on a neutral ground, not on navy.
val PaperInk = Color(0xFFF1F1F4)
val PaperInkDim = Color(0xFFADADB2)
val CharcoalLine = Color(0xFF48484C)
val CharcoalLineSoft = Color(0xFF2B2B2E)
val CharcoalFill = Color(0xFF232326)
val CharcoalCard = Color(0xFF18181B)
val CharcoalGround = Color(0xFF0F0F11)
val ErrorDark = Color(0xFFF2B8B5)
val OnErrorDark = Color(0xFF601410)
val ErrorContainerDark = Color(0xFF8C1D18)
val OnErrorContainerDark = Color(0xFFF9DEDC)
