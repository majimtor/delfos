package com.manuelalejandro.delfos.mobile.platform

import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSISO8601DateFormatOptions
import platform.Foundation.NSISO8601DateFormatWithFractionalSeconds
import platform.Foundation.NSISO8601DateFormatWithInternetDateTime
import platform.Foundation.NSISO8601DateFormatter

internal actual fun formatTime(isoInstant: String): String {
    val withFraction = NSISO8601DateFormatter().apply {
        formatOptions = NSISO8601DateFormatWithInternetDateTime or NSISO8601DateFormatWithFractionalSeconds
    }
    val date = withFraction.dateFromString(isoInstant) ?: NSISO8601DateFormatter().dateFromString(isoInstant) ?: return isoInstant
    return NSDateFormatter().apply {
        timeStyle = NSDateFormatterShortStyle
        dateStyle = NSDateFormatterNoStyle
    }.stringFromDate(date)
}
