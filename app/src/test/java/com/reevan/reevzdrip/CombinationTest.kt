package com.reevan.reevzdrip

import com.reevan.reevzdrip.data.Garment
import com.reevan.reevzdrip.data.GarmentType
import com.reevan.reevzdrip.data.combinationOf
import com.reevan.reevzdrip.data.orderedForCollage
import com.reevan.reevzdrip.ui.common.MAX_COLLAGE_TILES
import com.reevan.reevzdrip.ui.combinations.pickableExtras
import com.reevan.reevzdrip.ui.common.collagePlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinationTest {

    private var nextId = 1L
    private fun garment(name: String, type: GarmentType) =
        Garment(id = nextId++, name = name, type = type, addedOn = 0)

    // --- combinationOf -------------------------------------------------------------------

    @Test
    fun `a blank name is stored as null so unnamed has one representation`() {
        assertNull(combinationOf(name = "", createdOn = 0).name)
        assertNull(combinationOf(name = "   ", createdOn = 0).name)
        assertNull(combinationOf(name = null, createdOn = 0).name)
    }

    @Test
    fun `a name is trimmed and capitalised like a garment name`() {
        assertEquals("Office Friday", combinationOf(name = "  office friday ", createdOn = 0).name)
    }

    // --- orderedForCollage ---------------------------------------------------------------

    @Test
    fun `garments are ordered top, pant, shoes, accessory`() {
        val shuffled = listOf(
            garment("Watch", GarmentType.ACCESSORY),
            garment("Sneakers", GarmentType.SHOES),
            garment("Shirt", GarmentType.TOP),
            garment("Jeans", GarmentType.PANT),
        )
        assertEquals(
            listOf("Shirt", "Jeans", "Sneakers", "Watch"),
            orderedForCollage(shuffled).map { it.name },
        )
    }

    @Test
    fun `within a type the order is alphabetical and case-insensitive`() {
        val accessories = listOf(
            garment("watch", GarmentType.ACCESSORY),
            garment("Belt", GarmentType.ACCESSORY),
            garment("chain", GarmentType.ACCESSORY),
        )
        assertEquals(
            listOf("Belt", "chain", "watch"),
            orderedForCollage(accessories).map { it.name },
        )
    }

    @Test
    fun `the order is total, so a collage cannot shuffle between reads`() {
        // Same name and type: without the id tiebreaker these two could swap places, which shows
        // up as a collage that rearranges itself for no reason.
        val a = garment("Black Tee", GarmentType.TOP)
        val b = garment("Black Tee", GarmentType.TOP)
        assertEquals(
            orderedForCollage(listOf(a, b)).map { it.id },
            orderedForCollage(listOf(b, a)).map { it.id },
        )
    }

    @Test
    fun `ordering keeps every garment and adds none`() {
        val all = GarmentType.entries.map { garment("X", it) }
        assertEquals(all.size, orderedForCollage(all).size)
        assertEquals(all.map { it.id }.toSet(), orderedForCollage(all).map { it.id }.toSet())
    }

    @Test
    fun `ordering an empty outfit is empty, not an error`() {
        assertTrue(orderedForCollage(emptyList()).isEmpty())
    }

    // --- collagePlan ---------------------------------------------------------------------

    @Test
    fun `up to four garments each get their own tile`() {
        (0..MAX_COLLAGE_TILES).forEach { count ->
            val plan = collagePlan(count)
            assertEquals("count $count", count, plan.visible)
            assertEquals("count $count", 0, plan.overflow)
        }
    }

    @Test
    fun `beyond four, the last tile becomes a counter`() {
        // Five garments show three photos and "+2" — not four photos and a silently dropped one.
        assertEquals(3, collagePlan(5).visible)
        assertEquals(2, collagePlan(5).overflow)

        assertEquals(3, collagePlan(12).visible)
        assertEquals(9, collagePlan(12).overflow)
    }

    @Test
    fun `the overflow count always accounts for every garment`() {
        // The invariant that stops a card lying about how many pieces an outfit has.
        (0..30).forEach { count ->
            val plan = collagePlan(count)
            assertEquals(
                "count $count leaked a garment",
                count,
                plan.visible + plan.overflow,
            )
        }
    }

    @Test
    fun `a plan never asks for more tiles than the grid has`() {
        (0..30).forEach { count ->
            assertTrue(
                "count $count wanted ${collagePlan(count).visible} tiles",
                collagePlan(count).visible <= MAX_COLLAGE_TILES,
            )
        }
    }

    @Test
    fun `a negative count is clamped rather than producing a negative layout`() {
        assertEquals(0, collagePlan(-3).visible)
        assertEquals(0, collagePlan(-3).overflow)
    }

    // --- pickableExtras ------------------------------------------------------------------

    @Test
    fun `the builder offers nothing extra when every garment is still in the wardrobe`() {
        val shirt = garment("Shirt", GarmentType.TOP)
        val jeans = garment("Jeans", GarmentType.PANT)
        assertTrue(pickableExtras(listOf(shirt, jeans), listOf(shirt)).isEmpty())
    }

    @Test
    fun `an archived garment in the outfit is offered so the count matches the tiles`() {
        // The bug this exists to stop: the builder said "3 selected" over two ticked tiles,
        // because the archived garment was selected but not on screen — and so could not be
        // deselected either.
        val shirt = garment("Shirt", GarmentType.TOP)
        val jeans = garment("Jeans", GarmentType.PANT)
        val archived = garment("Old Belt", GarmentType.ACCESSORY).copy(archived = true)

        val extras = pickableExtras(
            wardrobe = listOf(shirt, jeans),
            inOutfit = listOf(shirt, jeans, archived),
        )
        assertEquals(listOf(archived.id), extras.map { it.id })
    }

    @Test
    fun `building a brand new outfit never surfaces an archived garment`() {
        val shirt = garment("Shirt", GarmentType.TOP)
        assertTrue(pickableExtras(listOf(shirt), inOutfit = emptyList()).isEmpty())
    }
}
