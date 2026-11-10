package com.manuelalejandro.delfos.evaluation

import kotlin.math.floor

internal fun Double.isWholeNumber(): Boolean = isFinite() && this == floor(this)

/** Length in Unicode code points, as the contract counts text; surrogate pairs count once. */
internal fun String.codePointLength(): Int = count { !it.isLowSurrogate() }
