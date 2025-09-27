@file:OptIn(ExperimentalTime::class)

package com.m2f.archer.crud.cache

import arrow.core.raise.ensureNotNull
import com.m2f.archer.configuration.Configuration
import com.m2f.archer.configuration.DefaultConfiguration
import com.m2f.archer.crud.ArcherRaise
import com.m2f.archer.crud.StoreDataSource
import com.m2f.archer.crud.cache.CacheExpiration.After
import com.m2f.archer.crud.cache.CacheExpiration.Always
import com.m2f.archer.crud.cache.CacheExpiration.Never
import com.m2f.archer.crud.cache.configuration.inMemoryCacheConfiguration
import com.m2f.archer.crud.cache.memcache.CacheMetaInformation
import com.m2f.archer.crud.getDataSource
import com.m2f.archer.crud.operation.MainSync
import com.m2f.archer.crud.operation.Store
import com.m2f.archer.crud.operation.StoreSync
import com.m2f.archer.datasource.InMemoryDataSource
import com.m2f.archer.failure.DataEmpty
import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.failure.Failure
import com.m2f.archer.failure.Invalid
import com.m2f.archer.mapper.map
import com.m2f.archer.query.Delete
import com.m2f.archer.query.Get
import com.m2f.archer.query.KeyQuery
import com.m2f.archer.query.Put
import com.m2f.archer.utils.runArcherTest
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class CacheExpirationTest {

    @OptIn(ExperimentalTime::class)
    val emptyCacheConfiguration = object : Configuration() {
        override val mainFallbacks: (Failure) -> Boolean = DefaultConfiguration.mainFallbacks
        override val storageFallbacks: (Failure) -> Boolean = DefaultConfiguration.storageFallbacks
        override val ignoreCache: Boolean = DefaultConfiguration.ignoreCache
        override fun getCurrentTime(): Instant = DefaultConfiguration.getCurrentTime()

        override val cache: CacheDataSource<CacheMetaInformation, Instant> =
            object : CacheDataSource<CacheMetaInformation, Instant> {
                override suspend fun ArcherRaise.delete(q: Delete<CacheMetaInformation>) {
                    /* no-op */
                }

                override suspend fun ArcherRaise.invoke(q: KeyQuery<CacheMetaInformation, out Instant>): Instant =
                    when (q) {
                        is Get -> raise(DataNotFound)
                        is Put -> ensureNotNull(q.value) { raise(DataEmpty) }
                    }
            }
    }

    @Test
    fun `never expires`() = runArcherTest {
        val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test"))
        val neverExpires = store.expires(Never)
        val result = either { neverExpires.get(0) }
        result shouldBeRight "Test"
    }

    @Test
    fun `always expires get`() = runArcherTest {
        val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test"))
        val alwaysExpires = store.expires(Always)
        val result = either { alwaysExpires.get(0) }
        result shouldBeLeft Invalid
    }

    @Test
    fun `always expires put`() = runArcherTest {
        val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test"))
        val alwaysExpires = store.expires(Always)
        val result1 = either { alwaysExpires.put(0, "hello") }
        result1 shouldBeRight "hello"
        val result2 = either { alwaysExpires.get(0) }
        result2 shouldBeLeft Invalid
    }

    @OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
    @Test
    fun `fetching after time passed`() = runArcherTest(configuration = inMemoryCacheConfiguration) {
        val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test"))
        val time = 50.milliseconds
        val expiresAfter50Millis = store.expires(After(time))

        // Put a value
        println("configuration in test: $this")
        println("current time: ${getCurrentTime()}")
        unit { expiresAfter50Millis.put(0, "test10") }
        advanceTimeBy(1.minutes)

        val result = either { expiresAfter50Millis.get(0) }
        println("after time: ${getCurrentTime()}")
        result shouldBeLeft Invalid
    }

    @Test
    fun `fetching before time passes`() = runArcherTest {
        val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test"))
        val time = 1000.milliseconds // Use long expiration time
        val expiresAfter1000Millis = store.expires(After(time))

        // Put a value
        either { expiresAfter1000Millis.put(0, "test10_2") }

        // Don't sleep - immediately check that cache is still valid
        val result = either { expiresAfter1000Millis.get(0) }
        result shouldBeRight "test10_2"
    }

    //    "create an a caching strategy with expiration"

    @Test
    fun `create a never expiring strategy`() = runArcherTest {
        val main = getDataSource<Int, String> { "main" }

        val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test")).map { "$it from Store" }
        val cacheStrategyNever = main cacheWith store expires Never

        // The cache never expires, so it should always return the value that we set in the default constructor
        val result = either { cacheStrategyNever.get(StoreSync, 0) }
        result shouldBeRight "Test from Store"
    }

    @Test
    fun `creating an always expiring strategy`() = runArcherTest {
        val main = getDataSource<Int, String> { "main" }

        val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test")).map { "$it from Store" }
        val cacheStrategyAlways = main cacheWith store expires Always

        // The cache always expires, so it should always return the value after storing it.
        val result = either { cacheStrategyAlways.get(StoreSync, 0) }
        result shouldBeRight "main from Store"
    }

    @Test
    fun `test expiration with a time expiring strategy`() = runArcherTest {
        val main = getDataSource<Int, String> { "main" }

        val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test")).map { "$it from Store" }
        val cacheStrategyAfter = main cacheWith store expiresIn 50.milliseconds

        // Fetch the value from the main source and store it afterward
        val result1 = either { cacheStrategyAfter.get(MainSync, 0) }
        result1 shouldBeRight "main from Store"

        // Wait few milliseconds to let the cache expire
        delay(100L)

        // The cache is expired so if we enforce data from store should be valid
        val result2 = either { cacheStrategyAfter.get(Store, 0) }
        result2 shouldBeRight "main from Store"
    }

    @Test
    fun `test no-expiration with a time expiring strategy`() = runArcherTest {
        val main = getDataSource<Int, String> { "main" }

        val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test")).map { "$it from Store" }
        val cacheStrategyAfter = main cacheWith store expiresIn 50.milliseconds

        // Fetch the value from the main source and store it afterward
        val result1 = either { cacheStrategyAfter.get(MainSync, 0) }
        result1 shouldBeRight "main from Store"

        // there's no delay so the cache did not expire
        val result2 = either { cacheStrategyAfter.get(Store, 0) }
        result2 shouldBeRight "main from Store"
    }

    @Test
    fun `with a time expiration if there is no stored expiration date the data take from Store is shown`() =
        runArcherTest(configuration = { emptyCacheConfiguration }) {
            val main = getDataSource<Int, String> { "main" }

            val store: StoreDataSource<Int, String> = InMemoryDataSource<Int, String>().map { "$it from Store" }

            val cacheStrategyAfter = main cacheWith store expiresIn 24.hours
            // get from main and store it
            val result1 = either { cacheStrategyAfter.get(MainSync, 0) }
            result1 shouldBeRight "main from Store"

            // as we don't store the expirations the data should be expired
            val result2 = either { cacheStrategyAfter.get(Store, 0) }
            result2 shouldBeRight "main from Store"
        }

    @Test
    fun `If the data is empty should remove the expiration date`() = runArcherTest {
        val info = CacheMetaInformation(
            key = "0",
            classIdentifier = String::class.simpleName.toString()
        )

        val expiration = mapOf(info to Clock.System.now() + 1.minutes)
        val expirationCache = InMemoryDataSource(expiration)

        val store: StoreDataSource<Int, String> = InMemoryDataSource()

        val customConfig = object : Configuration() {
            override val mainFallbacks = DefaultConfiguration.mainFallbacks
            override val storageFallbacks = DefaultConfiguration.storageFallbacks
            override val ignoreCache: Boolean = DefaultConfiguration.ignoreCache
            override fun getCurrentTime(): Instant = DefaultConfiguration.getCurrentTime()

            override val cache: CacheDataSource<CacheMetaInformation, Instant> = expirationCache
        }

        // the data does not exist
        val result1 = with(customConfig) {
            either {
                store.expires(After(24.hours)).get(0)
            }
        }
        result1 shouldBeLeft DataNotFound

        // the stored expiration should be removed
        val result2 = either { expirationCache.get(info) }
        result2 shouldBeLeft DataNotFound
    }
}
