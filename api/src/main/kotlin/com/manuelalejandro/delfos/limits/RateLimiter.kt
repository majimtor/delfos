package com.manuelalejandro.delfos.limits

import java.time.Clock
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Fixed one-minute windows per key, kept in memory only. Keys are client addresses and are never
 * written anywhere else; the map forgets a key as soon as its window has passed.
 */
class RateLimiter(private val clock: Clock, private val window: Duration = Duration.ofMinutes(1)) {

    class Decision(val allowed: Boolean, val retryAfterSeconds: Long)

    private class Window(val startMillis: Long, val count: AtomicInteger = AtomicInteger())

    private val windows = ConcurrentHashMap<String, Window>()
    private val checks = AtomicInteger()

    fun check(key: String, limit: Int): Decision {
        val now = clock.millis()
        val windowStart = now - now % window.toMillis()
        val current = windows.compute(key) { _, existing -> if (existing != null && existing.startMillis == windowStart) existing else Window(windowStart) }!!
        val count = current.count.incrementAndGet()
        if (checks.incrementAndGet() % PRUNE_EVERY == 0) prune(windowStart)
        val retryAfter = (windowStart + window.toMillis() - now + 999) / 1000
        return Decision(count <= limit, retryAfter)
    }

    private fun prune(windowStart: Long) {
        windows.entries.removeIf { it.value.startMillis != windowStart }
    }

    private companion object {
        const val PRUNE_EVERY = 1000
    }
}
