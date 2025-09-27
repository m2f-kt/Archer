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
