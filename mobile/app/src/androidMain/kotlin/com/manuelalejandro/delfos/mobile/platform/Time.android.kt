package com.manuelalejandro.delfos.mobile.platform

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle

internal actual fun formatTime(isoInstant: String): String = try {
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(Instant.parse(isoInstant).atZone(ZoneId.systemDefault()))
} catch (e: DateTimeParseException) {
    isoInstant
}
