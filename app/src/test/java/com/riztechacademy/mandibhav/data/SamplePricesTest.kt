package com.riztechacademy.mandibhav.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SamplePricesTest {

    @Test
    fun everySamplePriceHasAUniqueId() {
        // Ids are list keys, and from the navigation lesson on, navigation
        // arguments too: a duplicate crashes the list and opens the wrong price.
        val ids = SamplePrices.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }
}
