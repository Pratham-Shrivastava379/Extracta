package com.extracta.app.entity

import com.extracta.app.domain.model.DetectedEntity
import com.extracta.app.domain.model.EntityType
import java.math.BigDecimal
import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle
import java.net.URI
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultEntityExtractor @Inject constructor() : EntityExtractor {
    override fun extract(text: String): List<DetectedEntity> {
        if (text.isBlank()) return emptyList()

        val candidates = buildList {
            addMatches(text, EMAIL, EntityType.EMAIL, ::normalizeEmail)
            addMatches(text, URL, EntityType.URL, ::normalizeUrl)
            addDateCandidates(text)
            addTimeCandidates(text)
            addMoneyCandidates(text)
            addPhoneCandidates(text)
            addAddressCandidates(text)
        }

        val accepted = mutableListOf<DetectedEntity>()
        candidates
            .sortedWith(
                compareBy<Candidate> { TYPE_PRIORITY.getValue(it.entity.type) }
                    .thenByDescending { it.entity.originalText.length }
                    .thenBy { it.entity.start },
            )
            .forEach { candidate ->
                if (accepted.none { it.overlaps(candidate.entity) }) accepted += candidate.entity
            }
        return accepted.sortedWith(compareBy<DetectedEntity> { it.start }.thenBy { it.end })
    }

    private fun MutableList<Candidate>.addMatches(
        text: String,
        regex: Regex,
        type: EntityType,
        normalize: (String) -> String,
    ) {
        regex.findAll(text).forEach { match ->
            val source = match.value
            add(Candidate(entity(type, source, normalize(source), match.range.first)))
        }
    }

    private fun MutableList<Candidate>.addDateCandidates(text: String) {
        ISO_DATE.findAll(text).forEach { match ->
            val normalized = parseDate(match.value, ISO_FORMAT) ?: return@forEach
            add(Candidate(entity(EntityType.DATE_TIME, match.value, normalized.toString(), match.range.first)))
        }
        NAMED_DATE.findAll(text).forEach { match ->
            val normalized = NAMED_FORMATS.firstNotNullOfOrNull { parseDate(match.value, it) }
                ?: return@forEach
            add(Candidate(entity(EntityType.DATE_TIME, match.value, normalized.toString(), match.range.first)))
        }
    }

    private fun MutableList<Candidate>.addTimeCandidates(text: String) {
        TIME.findAll(text).forEach { match ->
            val source = match.value
            val formatter = if (source.contains(Regex("(?i)[ap]m"))) TWELVE_HOUR_FORMAT else TIME_FORMAT
            val normalized = try {
                LocalTime.parse(source.uppercase(Locale.ROOT).replace(" ", ""), formatter).toString()
            } catch (_: DateTimeException) {
                return@forEach
            }
            add(Candidate(entity(EntityType.DATE_TIME, source, normalized, match.range.first)))
        }
    }

    private fun MutableList<Candidate>.addMoneyCandidates(text: String) {
        MONEY.findAll(text).forEach { match ->
            val source = match.value
            val currency = when {
                source.contains('$') || source.contains(Regex("(?i)\\bUSD\\b")) -> "USD"
                source.contains('€') || source.contains(Regex("(?i)\\bEUR\\b")) -> "EUR"
                source.contains('£') || source.contains(Regex("(?i)\\bGBP\\b")) -> "GBP"
                source.contains('₹') || source.contains(Regex("(?i)\\bINR\\b")) -> "INR"
                else -> return@forEach
            }
            val amountText = NUMBER.find(source)?.value?.replace(",", "") ?: return@forEach
            val amount = runCatching { BigDecimal(amountText).stripTrailingZeros().toPlainString() }
                .getOrNull() ?: return@forEach
            add(Candidate(entity(EntityType.MONEY, source, "$currency $amount", match.range.first)))
        }
    }

    private fun MutableList<Candidate>.addPhoneCandidates(text: String) {
        PHONE.findAll(text).forEach { match ->
            val source = match.value.trim()
            val leadingTrim = match.value.indexOf(source)
            val digits = source.filter(Char::isDigit)
            if (
                digits.length !in 7..15 ||
                source.count { it == '+' } > 1 ||
                source.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))
            ) return@forEach
            val normalized = if (source.startsWith('+')) "+$digits" else digits
            add(Candidate(entity(EntityType.PHONE, source, normalized, match.range.first + leadingTrim)))
        }
    }

    private fun MutableList<Candidate>.addAddressCandidates(text: String) {
        ADDRESS.findAll(text).forEach { match ->
            val source = match.value.trim()
            val leadingTrim = match.value.indexOf(source)
            val normalized = source.replace(Regex("\\s+"), " ")
            add(Candidate(entity(EntityType.ADDRESS, source, normalized, match.range.first + leadingTrim)))
        }
    }

    private fun parseDate(value: String, formatter: DateTimeFormatter): LocalDate? =
        try {
            LocalDate.parse(value.replace(Regex("(?<=\\d)(st|nd|rd|th)"), ""), formatter)
        } catch (_: DateTimeException) {
            null
        }

    private fun normalizeEmail(value: String): String {
        val separator = value.lastIndexOf('@')
        return value.substring(0, separator) + "@" +
            value.substring(separator + 1).lowercase(Locale.ROOT)
    }

    private fun normalizeUrl(value: String): String {
        val withScheme = if (value.startsWith("www.", ignoreCase = true)) {
            "https://$value"
        } else {
            value
        }
        return runCatching {
            val uri = URI(withScheme)
            URI(
                uri.scheme.lowercase(Locale.ROOT),
                uri.userInfo,
                uri.host.lowercase(Locale.ROOT),
                uri.port,
                uri.path,
                uri.query,
                uri.fragment,
            ).toASCIIString()
        }.getOrDefault(withScheme)
    }

    private fun entity(
        type: EntityType,
        original: String,
        normalized: String,
        start: Int,
    ) = DetectedEntity(type, original, normalized, start, start + original.length)

    private fun DetectedEntity.overlaps(other: DetectedEntity): Boolean =
        start < other.end && other.start < end

    private data class Candidate(val entity: DetectedEntity)

    private companion object {
        // Earlier types win overlaps. Longer candidates win within a type.
        val TYPE_PRIORITY = mapOf(
            EntityType.EMAIL to 0,
            EntityType.URL to 1,
            EntityType.DATE_TIME to 2,
            EntityType.MONEY to 3,
            EntityType.PHONE to 4,
            EntityType.ADDRESS to 5,
        )
        val EMAIL = Regex("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b")
        val URL = Regex("(?i)\\b(?:https?://|www\\.)[A-Z0-9](?:[A-Z0-9._~:/?#\\[\\]@!$&'()*+,;=%-]*[A-Z0-9/_~#=%-])?")
        val ISO_DATE = Regex("(?<!\\d)\\d{4}-\\d{2}-\\d{2}(?!\\d)")
        val NAMED_DATE = Regex("(?i)\\b(?:0?[1-9]|[12]\\d|3[01])(?:st|nd|rd|th)?\\s+(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\\s+\\d{4}\\b")
        val TIME = Regex("(?i)(?<![\\d:])(?:[01]?\\d|2[0-3]):[0-5]\\d(?:\\s?[AP]M)?(?![\\d:])")
        val MONEY = Regex("(?i)(?<![A-Z0-9])(?:[$€£₹]\\s?\\d{1,3}(?:,\\d{3})*(?:\\.\\d{1,2})?|(?:USD|EUR|GBP|INR)\\s+\\d{1,3}(?:,\\d{3})*(?:\\.\\d{1,2})?)(?![A-Z0-9])")
        val NUMBER = Regex("\\d{1,3}(?:,\\d{3})*(?:\\.\\d{1,2})?")
        val PHONE = Regex("(?<![\\w$])\\+?(?:\\d[ ()-]?){6,14}\\d(?![\\w])")
        val ADDRESS = Regex("(?im)(?<!\\w)\\d{1,5}\\s+[A-Z][A-Z0-9.'-]*(?:\\s+[A-Z][A-Z0-9.'-]*){0,4}\\s+(?:Street|St|Road|Rd|Avenue|Ave|Boulevard|Blvd|Lane|Ln|Drive|Dr|Court|Ct)\\.?\\b")
        val ISO_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE.withResolverStyle(ResolverStyle.STRICT)
        val NAMED_FORMATS = listOf("d MMMM uuuu", "d MMM uuuu").map { pattern ->
            DateTimeFormatterBuilder().parseCaseInsensitive()
                .appendPattern(pattern)
                .toFormatter(Locale.ENGLISH)
                .withResolverStyle(ResolverStyle.STRICT)
        }
        val TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm").withResolverStyle(ResolverStyle.STRICT)
        val TWELVE_HOUR_FORMAT = DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH)
            .withResolverStyle(ResolverStyle.STRICT)
    }
}
