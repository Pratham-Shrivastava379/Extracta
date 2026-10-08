package com.extracta.app.entity.action

import com.extracta.app.domain.model.DetectedEntity
import com.extracta.app.domain.model.EntityType
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

class EntityActionResolver @Inject constructor(
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    fun resolve(entity: DetectedEntity): List<EntityAction> = buildList {
        when (entity.type) {
            EntityType.DATE_TIME -> calendarAction(entity)?.let(::add)
            EntityType.PHONE -> add(EntityAction.OpenDialer(entity.normalizedValue))
            EntityType.EMAIL -> add(EntityAction.ComposeEmail(entity.normalizedValue))
            EntityType.URL -> add(EntityAction.OpenUrl(entity.normalizedValue))
            EntityType.ADDRESS -> add(EntityAction.OpenMap(entity.normalizedValue))
            EntityType.MONEY -> Unit
        }
        add(EntityAction.Copy(entity.originalText))
    }

    private fun calendarAction(entity: DetectedEntity): EntityAction.AddCalendarEvent? {
        val zone = clock.zone
        val date = runCatching { LocalDate.parse(entity.normalizedValue) }.getOrNull()
        if (date != null) {
            val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            return EntityAction.AddCalendarEvent(entity.originalText, start, end, allDay = true)
        }

        val time = runCatching { LocalTime.parse(entity.normalizedValue) }.getOrNull() ?: return null
        val start = LocalDate.now(clock).atTime(time).atZone(zone).toInstant().toEpochMilli()
        return EntityAction.AddCalendarEvent(
            title = entity.originalText,
            startMillis = start,
            endMillis = start + ONE_HOUR_MILLIS,
            allDay = false,
        )
    }

    private companion object {
        const val ONE_HOUR_MILLIS = 60L * 60L * 1_000L
    }
}
