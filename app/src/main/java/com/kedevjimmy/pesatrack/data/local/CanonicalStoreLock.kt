package com.kedevjimmy.pesatrack.data.local

/**
 * One reentrant monitor for canonical stores and message ingestion.
 *
 * Acquire this before any store operation or SQLite transaction. Separate store
 * monitors invert the order when ingestion owns SQLite and calls a store while
 * Home owns that store and waits for SQLite. Nested store calls use this same
 * monitor; SQLite transactions still provide rollback/commit atomicity.
 *
 * Do not hold this across network requests or coroutine suspension.
 */
internal object CanonicalStoreLock {
    val monitor = Any()
}
