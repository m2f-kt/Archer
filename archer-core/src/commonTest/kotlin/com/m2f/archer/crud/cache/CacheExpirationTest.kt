package com.m2f.archer.crud.cache

import arrow.core.raise.ensureNotNull
import com.m2f.archer.configuration.Configuration
import com.m2f.archer.configuration.DefaultConfiguration
import com.m2f.archer.configuration.DefaultConfiguration.expires
import com.m2f.archer.crud.ArcherRaise
import com.m2f.archer.crud.StoreDataSource
import com.m2f.archer.crud.cache.CacheExpiration.After
import com.m2f.archer.crud.cache.CacheExpiration.Always
import com.m2f.archer.crud.cache.CacheExpiration.Never
import com.m2f.archer.crud.cache.configuration.testConfiguration
import com.m2f.archer.crud.cache.memcache.CacheMetaInformation
import com.m2f.archer.crud.either
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
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class CacheExpirationTest : FunSpec({


        test("never expires") {
            val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test"))
            val neverExpires = store.expires(testConfiguration, Never)
            either { neverExpires.get(0) } shouldBeRight "Test"
        }

        test("always expires get") {
            val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test"))
            val alwaysExpires = store.expires(testConfiguration, Always)
            either { alwaysExpires.get(0) } shouldBeLeft Invalid
        }

        test("always expires put") {
            val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test"))
            val alwaysExpires = store.expires(testConfiguration, Always)
            either { alwaysExpires.put(0, "hello") } shouldBeRight "hello"
        }

        // "expires with time"
        test("fetching after time passed") {
            val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test"))
            val time = 10.milliseconds
            val expiresAfter10Millis = store.expires(testConfiguration, After(time))
            either { expiresAfter10Millis.put(0, "test10") }
            delay(15L)
            either { expiresAfter10Millis.get(0) } shouldBeLeft Invalid
        }

        test("fetching before time passes") {
            val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test"))
            val time = 1.minutes
            val expiresAfter50Millis = store.expires(testConfiguration, After(time))
            either { expiresAfter50Millis.put(0, "test10_2") }
            delay(1000L)
            either { expiresAfter50Millis.get(0) } shouldBeRight "test10_2"
        }

        //    "create an a caching strategy with expiration"

        test("create a never expiring strategy") {
            val main = getDataSource<Int, String> { "main" }

            val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test")).map { "$it from Store" }
            val cacheStrategyNever = main cacheWith store expires Never

            // The cache never expires, so it should always return the value that we set in the default constructor
            either { cacheStrategyNever.get(StoreSync, 0) } shouldBeRight "Test from Store"
        }

        test("creating an always expiring strategy") {
            val main = getDataSource<Int, String> { "main" }

            val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test")).map { "$it from Store" }
            val cacheStrategyAlways = main cacheWith store expires Always

            // The cache always expires, so it should always return the value after storing it.
            either { cacheStrategyAlways.get(StoreSync, 0) } shouldBeRight "main from Store"
        }

        test("test expiration with a time expiring strategy") {
            val main = getDataSource<Int, String> { "main" }

            with(testConfiguration) {
                val store: StoreDataSource<Int, String> =
                    InMemoryDataSource(mapOf(0 to "Test")).map { "$it from Store" }
                val cacheStrategyAfter = main cacheWith store expiresIn 50.milliseconds

                // Fetch the value from the main source and store it afterward
                either { cacheStrategyAfter.get(MainSync, 0) } shouldBeRight "main from Store"

                // Wait few milliseconds to let the cache expire
                delay(100L)

                // The cache is expired so if we enforce data from store should be invalid
                either { cacheStrategyAfter.get(Store, 0) } shouldBeLeft Invalid
            }
        }

        test("test no-expiration with a time expiring strategy") {
            val main = getDataSource<Int, String> { "main" }

            with(testConfiguration) {
                val store: StoreDataSource<Int, String> =
                    InMemoryDataSource(mapOf(0 to "Test")).map { "$it from Store" }
                val cacheStrategyAfter = main cacheWith store expiresIn 50.milliseconds

                // Fetch the value from the main source and store it afterward
                either { cacheStrategyAfter.get(MainSync, 0) } shouldBeRight "main from Store"

                // there's no delay so the cache did not expire
                either { cacheStrategyAfter.get(Store, 0) } shouldBeRight "main from Store"
            }
        }

        // "expiration rules"

        test("with a time expiration if there is no stored expiration date, the data then is expired") {
            val main = getDataSource<Int, String> { "main" }

            val store: StoreDataSource<Int, String> = InMemoryDataSource<Int, String>().map { "$it from Store" }

            val emptyCacheConfiguration = object : Configuration() {
                override val mainFallbacks: (Failure) -> Boolean = DefaultConfiguration.mainFallbacks
                override val storageFallbacks: (Failure) -> Boolean = DefaultConfiguration.storageFallbacks
                override val cache: CacheDataSource<CacheMetaInformation, Instant> =
                    object : CacheDataSource<CacheMetaInformation, Instant> {
                        override suspend fun ArcherRaise.delete(q: Delete<CacheMetaInformation>) {
                        }

                        override suspend fun ArcherRaise.invoke(q: KeyQuery<CacheMetaInformation, out Instant>): Instant =
                            when (q) {
                                is Get -> raise(DataNotFound)
                                is Put -> ensureNotNull(q.value) { raise(DataEmpty) }
                            }
                    }
            }

            emptyCacheConfiguration.run {
                val cacheStrategyAfter = main cacheWith store expiresIn 24.hours
                // get from main and store it
                either { cacheStrategyAfter.get(MainSync, 0) } shouldBeRight "main from Store"

                // as we don't store the expirations the data should be expired
                either { cacheStrategyAfter.get(Store, 0) } shouldBeLeft Invalid
            }
        }

        test("If the data is empty should remove the expiration date") {

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
                override val cache: CacheDataSource<CacheMetaInformation, Instant> = expirationCache
            }

                // the data does not exist
                either {
                    store.expires(customConfig, After(24.hours))
                        .get(0)
                } shouldBeLeft DataNotFound

                // the stored expiration should be removed
                either { expirationCache.get(info) } shouldBeLeft DataNotFound
        }
})
