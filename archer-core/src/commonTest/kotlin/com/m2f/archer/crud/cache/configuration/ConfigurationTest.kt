package com.m2f.archer.crud.cache.configuration

import com.m2f.archer.configuration.Configuration
import com.m2f.archer.configuration.DefaultConfiguration
import com.m2f.archer.crud.ArcherRaise
import com.m2f.archer.crud.Ice
import com.m2f.archer.crud.StoreDataSource
import com.m2f.archer.crud.cache.CacheDataSource
import com.m2f.archer.crud.cache.cacheWith
import com.m2f.archer.crud.cache.memcache.CacheMetaInformation
import com.m2f.archer.crud.getDataSource
import com.m2f.archer.crud.operation.MainSync
import com.m2f.archer.crud.operation.Store
import com.m2f.archer.failure.Failure
import com.m2f.archer.query.Delete
import com.m2f.archer.query.KeyQuery
import com.m2f.archer.utils.archerTest
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Clock.System
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class ConfigurationTest : FunSpec({

    val neverExpiringCacheConfiguration = object : Configuration() {
        override val mainFallbacks: (Failure) -> Boolean = DefaultConfiguration.mainFallbacks
        override val storageFallbacks: (Failure) -> Boolean = DefaultConfiguration.storageFallbacks
        override val ignoreCache: Boolean = DefaultConfiguration.ignoreCache
        override val cache: CacheDataSource<CacheMetaInformation, Instant> =
            object : CacheDataSource<CacheMetaInformation, Instant> {
                override suspend fun ArcherRaise.delete(q: Delete<CacheMetaInformation>) {
                    /*no-op*/
                }

                override suspend fun ArcherRaise.invoke(q: KeyQuery<CacheMetaInformation, out Instant>): Instant {
                    return System.now() + 1000.days
                }
            }
    }

    val alwaysExpiringCacheConfiguration = object : Configuration() {
        override val mainFallbacks: (Failure) -> Boolean = DefaultConfiguration.mainFallbacks
        override val storageFallbacks: (Failure) -> Boolean = DefaultConfiguration.storageFallbacks
        override val ignoreCache: Boolean = DefaultConfiguration.ignoreCache
        override val cache: CacheDataSource<CacheMetaInformation, Instant> =
            object : CacheDataSource<CacheMetaInformation, Instant> {
                override suspend fun ArcherRaise.delete(q: Delete<CacheMetaInformation>) {
                    /*no-op*/
                }

                override suspend fun ArcherRaise.invoke(q: KeyQuery<CacheMetaInformation, out Instant>): Instant {
                    return System.now() - 1000.days
                }
            }
    }
    archerTest("ice dsl preserves the configuration") {
        with(inMemoryCacheConfiguration()) {
            val mainDataSource = getDataSource<Int, String> { "main" }
            val storeDataSource = StoreDataSource<Int, String> { "store" }

            val a = mainDataSource cacheWith storeDataSource expiresIn 1000.minutes
            val b = mainDataSource cacheWith storeDataSource expiresIn 10.minutes
            val c = mainDataSource cacheWith storeDataSource expiresIn 1.milliseconds

            // As the strategy was created under testConfiguration scope it does preserve it
            ice { a.get(Store, 1) } shouldBe Ice.Content("store")
            ice { a.get(MainSync, 1) }
            ice { a.get(Store, 1) } shouldBe Ice.Content("store")

            // As b was created in the alwaysExpiringCacheConfiguration store will always be invalid
            with(alwaysExpiringCacheConfiguration) {
                ice { b.get(Store, 1) } shouldBe Ice.Content("store")
                ice { b.get(MainSync, 1) }
                ice { b.get(Store, 1) } shouldBe Ice.Content("store")
            }

            // As c was created in the neverExpiringCacheConfiguration store will always be valid
            with(neverExpiringCacheConfiguration) {
                ice { c.get(Store, 1) } shouldBe Ice.Content("store")
                ice { c.get(MainSync, 1) } shouldBe Ice.Content("store")
                ice { c.get(Store, 1) } shouldBe Ice.Content("store")
            }
        }
    }

    archerTest("either dsl preserves the configuration") {
        with(inMemoryCacheConfiguration()) {
            val mainDataSource = getDataSource<Int, String> { "main" }
            val storeDataSource = StoreDataSource<Int, String> { "store" }

            val a = mainDataSource cacheWith storeDataSource expiresIn 1000.minutes
            val b =
                with(alwaysExpiringCacheConfiguration) { mainDataSource cacheWith storeDataSource expiresIn 10.minutes }
            val c =
                with(
                    neverExpiringCacheConfiguration
                ) { mainDataSource cacheWith storeDataSource expiresIn 1.milliseconds }

            // As the strategy was created under testConfiguration scope it does preserve it
            either { a.get(Store, 1) } shouldBeRight "store"
            either { a.get(MainSync, 1) }
            either { a.get(Store, 1) } shouldBeRight "store"

            // As b was created in the alwaysExpiringCacheConfiguration store will always be invalid
            either { b.get(Store, 1) } shouldBeRight "store"
            either { b.get(MainSync, 1) }
            either { b.get(Store, 1) } shouldBeRight "store"

            // As c was created in the neverExpiringCacheConfiguration store will always be valid
            either { c.get(Store, 1) } shouldBeRight "store"
            either { c.get(MainSync, 1) } shouldBeRight "store"
            either { c.get(Store, 1) } shouldBeRight "store"
        }
    }

    archerTest("result dsl preserves the configuration") {
        with(inMemoryCacheConfiguration()) {
            val mainDataSource = getDataSource<Int, String> { "main" }
            val storeDataSource = StoreDataSource<Int, String> { "store" }

            val a = mainDataSource cacheWith storeDataSource expiresIn 1000.minutes
            val b =
                with(alwaysExpiringCacheConfiguration) { mainDataSource cacheWith storeDataSource expiresIn 10.minutes }
            val c =
                with(
                    neverExpiringCacheConfiguration
                ) { mainDataSource cacheWith storeDataSource expiresIn 1.milliseconds }

            // As the strategy was created under testConfiguration scope it does preserve it
            result { a.get(Store, 1) } shouldBeRight "store"
            result { a.get(MainSync, 1) }
            result { a.get(Store, 1) } shouldBeRight "store"

            // As b was created in the alwaysExpiringCacheConfiguration store will always be invalid
            result { b.get(Store, 1) } shouldBeRight "store"
            result { b.get(MainSync, 1) }
            result { b.get(Store, 1) } shouldBeRight "store"

            // As c was created in the neverExpiringCacheConfiguration store will always be valid
            result { c.get(Store, 1) } shouldBeRight "store"
            result { c.get(MainSync, 1) } shouldBeRight "store"
            result { c.get(Store, 1) } shouldBeRight "store"
        }
    }

    archerTest("bool dsl preserves the configuration") {
        with(inMemoryCacheConfiguration()) {
            val mainDataSource = getDataSource<Int, String> { "main" }
            val storeDataSource = StoreDataSource<Int, String> { "store" }

            val a = mainDataSource cacheWith storeDataSource expiresIn 1000.minutes
            val b =
                with(alwaysExpiringCacheConfiguration) { mainDataSource cacheWith storeDataSource expiresIn 10.minutes }
            val c =
                with(
                    neverExpiringCacheConfiguration
                ) { mainDataSource cacheWith storeDataSource expiresIn 1.milliseconds }

            // As the strategy was created under testConfiguration scope it does preserve it
            bool { a.get(Store, 1) }.shouldBeTrue()
            bool { a.get(MainSync, 1) }
            bool { a.get(Store, 1) }.shouldBeTrue()

            // As b was created in the alwaysExpiringCacheConfiguration store will always be invalid
            bool { b.get(Store, 1) }.shouldBeTrue()
            bool { b.get(MainSync, 1) }
            bool { b.get(Store, 1) }.shouldBeTrue()

            // As c was created in the neverExpiringCacheConfiguration store will always be valid
            bool { c.get(Store, 1) }.shouldBeTrue()
            bool { c.get(MainSync, 1) }.shouldBeTrue()
            bool { c.get(Store, 1) }.shouldBeTrue()
        }
    }
})
