package com.m2f.archer.crud.cache

import com.m2f.archer.configuration.Configuration
import com.m2f.archer.configuration.DefaultConfiguration
import com.m2f.archer.crud.ArcherRaise
import com.m2f.archer.crud.cache.memcache.CacheMetaInformation
import kotlinx.datetime.Instant

suspend inline fun <reified A> ArcherRaise.invalidateCache(
    configuration: Configuration = DefaultConfiguration,
    key: Any,
) {
    @Suppress("NullableToStringCall")
    val info = CacheMetaInformation(
        key = key.toString(),
        classIdentifier = A::class.simpleName.toString(),
    )

    configuration.cache.put(info, Instant.DISTANT_PAST)
}
