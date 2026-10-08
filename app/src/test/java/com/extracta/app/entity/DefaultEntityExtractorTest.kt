package com.extracta.app.entity

import com.extracta.app.domain.model.EntityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultEntityExtractorTest {
    private val extractor = DefaultEntityExtractor()

    @Test
    fun anonymizedFixtureExtractsSupportedTypesWithExactOffsets() {
        val text = """
            Contact: help@example.test.
            Portal: https://example.test/case/42.
            Call +91 98765 43210 on 2026-11-05 at 09:30.
            Balance: ₹1,250.50
            Visit 42 Example Road
        """.trimIndent()

        val entities = extractor.extract(text)

        assertEquals(
            entities.toString(),
            listOf(
                EntityType.EMAIL,
                EntityType.URL,
                EntityType.PHONE,
                EntityType.DATE_TIME,
                EntityType.DATE_TIME,
                EntityType.MONEY,
                EntityType.ADDRESS,
            ),
            entities.map { it.type },
        )
        entities.forEach { entity ->
            assertEquals(entity.originalText, text.substring(entity.start, entity.end))
        }
        assertEquals("help@example.test", entities[0].normalizedValue)
        assertEquals("https://example.test/case/42", entities[1].normalizedValue)
        assertEquals("+919876543210", entities[2].normalizedValue)
        assertEquals("2026-11-05", entities[3].normalizedValue)
        assertEquals("09:30", entities[4].normalizedValue)
        assertEquals("INR 1250.5", entities[5].normalizedValue)
        assertEquals("42 Example Road", entities[6].normalizedValue)
    }

    @Test
    fun punctuationIsExcludedAndRepeatedValuesKeepDistinctOffsets() {
        val text = "(www.example.test), then www.example.test!"
        val entities = extractor.extract(text)

        assertEquals(2, entities.size)
        assertEquals(listOf("www.example.test", "www.example.test"), entities.map { it.originalText })
        assertEquals(1, entities[0].start)
        assertTrue(entities[1].start > entities[0].end)
    }

    @Test
    fun invalidAndAmbiguousCandidatesAreRejected() {
        val text = "bad@@example, 2026-02-30, 25:90, 12/11/2026, 12345"

        val entities = extractor.extract(text)
        assertTrue(entities.toString(), entities.isEmpty())
    }

    @Test
    fun higherPriorityEntityWinsAnOverlap() {
        val text = "https://example.test/order/12345678"

        val entities = extractor.extract(text)

        assertEquals(1, entities.size)
        assertEquals(EntityType.URL, entities.single().type)
        assertEquals(text, entities.single().originalText)
    }

    @Test
    fun blankTextProducesNoEntitiesAndRunsDeterministically() {
        assertTrue(extractor.extract("  \n").isEmpty())
        val fixture = "Email team@example.test on 5 October 2026 at 4:15 PM"
        assertEquals(extractor.extract(fixture), extractor.extract(fixture))
    }

    @Test
    fun normalizationPreservesCaseSensitiveEmailAndUrlComponents() {
        val text = "User.Name@EXAMPLE.TEST https://EXAMPLE.TEST/Case/AbC on 5 Oct 2026"
        val entities = extractor.extract(text)

        assertEquals("User.Name@example.test", entities[0].normalizedValue)
        assertEquals("https://example.test/Case/AbC", entities[1].normalizedValue)
        assertEquals("2026-10-05", entities[2].normalizedValue)
    }
}
