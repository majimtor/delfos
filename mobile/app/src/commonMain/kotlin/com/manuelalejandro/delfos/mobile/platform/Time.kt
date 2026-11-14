package com.manuelalejandro.delfos.mobile.platform

/** An instant of the contract (ISO 8601, UTC) as a short local time, or the text itself when it does not parse. */
internal expect fun formatTime(isoInstant: String): String
