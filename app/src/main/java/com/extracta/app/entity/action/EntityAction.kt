package com.extracta.app.entity.action

sealed interface EntityAction {
    val label: String

    data class AddCalendarEvent(
        val title: String,
        val startMillis: Long,
        val endMillis: Long,
        val allDay: Boolean,
        override val label: String = "Add to calendar",
    ) : EntityAction

    data class OpenDialer(
        val phoneNumber: String,
        override val label: String = "Open dialer",
    ) : EntityAction

    data class ComposeEmail(
        val address: String,
        override val label: String = "Compose email",
    ) : EntityAction

    data class OpenUrl(
        val url: String,
        override val label: String = "Open in browser",
    ) : EntityAction

    data class OpenMap(
        val address: String,
        override val label: String = "Open in maps",
    ) : EntityAction

    data class Copy(
        val value: String,
        override val label: String = "Copy",
    ) : EntityAction
}
