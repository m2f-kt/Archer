@file:OptIn(ExperimentalTime::class)

package com.m2f.archer.crud.cache

import com.m2f.archer.crud.ArcherRaise
import com.m2f.archer.crud.cache.memcache.CacheMetaInformation
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

suspend inline fun <reified A> ArcherRaise.invalidateCache(
    key: Any,
) {
    @Suppress("NullableToStringCall")
    val info = CacheMetaInformation(
        key = key.toString(),
        classIdentifier = A::class.simpleName.toString(),
    )

    cache.put(info, Instant.DISTANT_PAST)
}

/**
 * Invalidates all cached expiration entries at once.
 *
 * This clears every entry in the expiration registry, causing all
 * cached repositories to treat their data as expired on the next read
 * and fall back to the main (network) data source.
 *
 * Only works when the underlying [cache] implements [ClearableCache]
 * (which [com.m2f.archer.crud.cache.memcache.MemoizedExpirationCache] does by default).
 * If the cache does not implement [ClearableCache], this is a no-op.
 */
suspend fun ArcherRaise.invalidateAllCaches() {
    (cache as? ClearableCache)?.clearAll()
}
