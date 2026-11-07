package io.github.diiafyra.motoman.core.domain.fuel

/**
 * Reads the litres typed in the form as millilitres, exactly. Accepts a dot or a comma as the decimal
 * separator, since Vietnamese keyboards and receipts use the comma, and up to three decimals (one
 * millilitre). Returns null for anything else, including thousands separators, which no fill-up needs.
 */
public fun parseVolumeMl(text: String): Long? {
    val trimmed = text.trim()
    val match = VOLUME_PATTERN.matchEntire(trimmed) ?: return null
    val whole = match.groupValues[1]
    val fraction = match.groupValues[2].padEnd(3, '0')
    if (whole.length > MAX_WHOLE_DIGITS) return null
    return whole.toLong() * 1_000 + fraction.toLong()
}

private const val MAX_WHOLE_DIGITS = 6

/** Digits, then optionally a separator and one to three digits. "4." and ".5" are rejected. */
private val VOLUME_PATTERN = Regex("""(\d+)(?:[.,](\d{1,3}))?""")
