package com.m2f.archer.crud.cache

/**
 * A cache that supports clearing all entries at once.
 *
 * Implementations should clear all stored expiration metadata,
 * causing subsequent reads to treat all cached data as expired
 * and fall back to the main (network) data source.
 */
interface ClearableCache {
    suspend fun clearAll()
}
