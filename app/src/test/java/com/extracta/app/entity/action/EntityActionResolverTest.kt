package com.extracta.app.entity.action

import com.extracta.app.domain.model.DetectedEntity
import com.extracta.app.domain.model.EntityType
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EntityActionResolverTest {
    private val clock = Clock.fixed(Instant.parse("2026-10-07T04:00:00Z"), ZoneId.of("Asia/Kolkata"))
    private val resolver = EntityActionResolver(clock)

    @Test
    fun eachEntityGetsOnlySuitableActionsAndCopy() {
        val expected = mapOf(
            EntityType.PHONE to EntityAction.OpenDialer::class,
            EntityType.EMAIL to EntityAction.ComposeEmail::class,
            EntityType.URL to EntityAction.OpenUrl::class,
            EntityType.ADDRESS to EntityAction.OpenMap::class,
        )

        expected.forEach { (type, actionClass) ->
            val actions = resolver.resolve(entity(type, "source", "value"))
            assertEquals(actionClass, actions.first()::class)
            assertTrue(actions.last() is EntityAction.Copy)
        }
        assertEquals(listOf(EntityAction.Copy("$12")), resolver.resolve(entity(EntityType.MONEY, "$12", "USD 12")))
    }

    @Test
    fun dateCreatesAllDayEventAndTimeCreatesOneHourEvent() {
        val date = resolver.resolve(entity(EntityType.DATE_TIME, "8 Oct 2026", "2026-10-08")).first()
            as EntityAction.AddCalendarEvent
        assertTrue(date.allDay)
        assertEquals(86_400_000L, date.endMillis - date.startMillis)

        val time = resolver.resolve(entity(EntityType.DATE_TIME, "14:30", "14:30")).first()
            as EntityAction.AddCalendarEvent
        assertEquals(false, time.allDay)
        assertEquals(3_600_000L, time.endMillis - time.startMillis)
    }

    @Test
    fun malformedDateStillOffersCopy() {
        assertEquals(
            listOf(EntityAction.Copy("tomorrowish")),
            resolver.resolve(entity(EntityType.DATE_TIME, "tomorrowish", "invalid")),
        )
    }

    private fun entity(type: EntityType, source: String, normalized: String) =
        DetectedEntity(type, source, normalized, 0, source.length)
}
