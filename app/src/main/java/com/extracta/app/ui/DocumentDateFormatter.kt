package com.extracta.app.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val documentDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

fun formatDocumentDate(instant: Instant): String =
    documentDateFormatter.format(instant.atZone(ZoneId.systemDefault()))
