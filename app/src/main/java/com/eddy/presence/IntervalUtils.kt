package com.eddy.presence

// Negative values = raw seconds (e.g. -10 = 10s, test only).
// Positive values = minutes.
internal fun intervalToMs(intervalMinutes: Int): Long =
    if (intervalMinutes < 0) (-intervalMinutes) * 1_000L else intervalMinutes * 60_000L

internal fun intervalLabel(intervalMinutes: Int): String =
    if (intervalMinutes < 0) "${-intervalMinutes}s" else "${intervalMinutes}m"
