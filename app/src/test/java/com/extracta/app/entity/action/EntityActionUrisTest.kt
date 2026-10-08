package com.extracta.app.entity.action

import org.junit.Assert.assertEquals
import org.junit.Test

class EntityActionUrisTest {
    @Test
    fun valuesAreEncodedForTheirUriComponents() {
        assertEquals("tel:%2B91%2098%2076", EntityActionUris.dial("+91 98 76"))
        assertEquals("mailto:person%2Bscan%40example.test", EntityActionUris.email("person+scan@example.test"))
        assertEquals("geo:0,0?q=12%20Main%20St.%2C%20Pune", EntityActionUris.map("12 Main St., Pune"))
    }
}
