package com.reevan.reevzdrip

import com.reevan.reevzdrip.data.GarmentType
import com.reevan.reevzdrip.data.garmentOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers [garmentOf], which is the single place the storage rules for a garment live. Every entry
 * point that creates one goes through it, so these are the rules for the whole app.
 */
class GarmentTest {

    @Test
    fun `name is trimmed and capitalised`() {
        val garment = garmentOf(name = "  blue oxford shirt ", type = GarmentType.TOP, addedOn = 0)
        assertEquals("Blue Oxford Shirt", garment.name)
    }

    @Test
    fun `existing capitalisation inside a word is preserved`() {
        // Title-casing properly would wreck these; only the first letter is ever touched.
        assertEquals("UNIQLO Tee", garmentOf(name = "UNIQLO tee", type = GarmentType.TOP, addedOn = 0).name)
        assertEquals("H&M Jeans", garmentOf(name = "H&M jeans", type = GarmentType.PANT, addedOn = 0).name)
        assertEquals("Levi's 501", garmentOf(name = "levi's 501", type = GarmentType.PANT, addedOn = 0).name)
    }

    @Test
    fun `blank photo name is stored as null so no photo has one representation`() {
        assertNull(garmentOf(name = "Tee", type = GarmentType.TOP, photoName = "", addedOn = 0).photoName)
        assertNull(garmentOf(name = "Tee", type = GarmentType.TOP, photoName = "   ", addedOn = 0).photoName)
        assertNull(garmentOf(name = "Tee", type = GarmentType.TOP, photoName = null, addedOn = 0).photoName)
    }

    @Test
    fun `a real photo name survives, trimmed`() {
        val garment = garmentOf(
            name = "Tee",
            type = GarmentType.TOP,
            photoName = " a1b2c3.jpg ",
            addedOn = 0,
        )
        assertEquals("a1b2c3.jpg", garment.photoName)
    }

    @Test
    fun `type and addedOn are carried through untouched`() {
        val garment = garmentOf(name = "Watch", type = GarmentType.ACCESSORY, addedOn = 20_355)
        assertEquals(GarmentType.ACCESSORY, garment.type)
        assertEquals(20_355, garment.addedOn)
    }

    @Test
    fun `every type has a display label`() {
        // The editor renders one chip per entry; a blank label would be an invisible chip.
        GarmentType.entries.forEach { type ->
            assert(type.label.isNotBlank()) { "$type has no label" }
        }
    }
}
