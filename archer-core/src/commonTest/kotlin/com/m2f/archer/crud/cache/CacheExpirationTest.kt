package com.m2f.archer.crud.cache

import com.m2f.archer.crud.StoreDataSource
import com.m2f.archer.crud.cache.CacheExpiration.After
import com.m2f.archer.crud.cache.CacheExpiration.Always
import com.m2f.archer.crud.cache.CacheExpiration.Never
import com.m2f.archer.crud.cache.memcache.CacheMetaInformation
import com.m2f.archer.crud.cache.memcache.MemoizedExpirationCache
import com.m2f.archer.crud.either
import com.m2f.archer.crud.getDataSource
import com.m2f.archer.crud.operation.MainSync
import com.m2f.archer.crud.operation.Store
import com.m2f.archer.crud.operation.StoreSync
import com.m2f.archer.datasource.InMemoryDataSource
import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.failure.Invalid
import com.m2f.archer.mapper.map
import com.m2f.archer.query.Delete
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class CacheExpirationTest : FunSpec({

    // "passing expiration"

    test("never expires") {
        val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test"))
        val neverExpires = store.expires(Never)
        either { neverExpires.get(0) } shouldBeRight "Test"
    }

    test("always expires") {
        val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test"))
        val alwaysExpires = store.expires(Always)
        either { alwaysExpires.get(0) } shouldBeLeft Invalid
    }

    // "expires with time"
    test("fetching after time passed") {
        val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test"))
        val time = 10.milliseconds
        val expiresAfter10Millis = store.expires(After(time), InMemoryDataSource())
        either { expiresAfter10Millis.put(0, "test10") }
        delay(15L)
        either { expiresAfter10Millis.get(0) } shouldBeLeft Invalid
    }

    test("fetching before time passes") {
        val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test"))
        val time = 50.milliseconds
        val expiresAfter50Millis = store.expires(After(time), InMemoryDataSource())
        either { expiresAfter50Millis.put(0, "test10_2") }
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

        val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test")).map { "$it from Store" }
        val cacheStrategyAfter = main cacheWith store expiresIn 50.milliseconds

        // Fetch the value from the main source and store it afterward
        either { cacheStrategyAfter.get(MainSync, 0) } shouldBeRight "main from Store"

        // Wait few milliseconds to let the cache expire
        delay(100L)

        // The cache is expired so if we enforce data from store should be invalid
        either { cacheStrategyAfter.get(Store, 0) } shouldBeLeft Invalid
    }

    test("test no-expiration with a time expiring strategy") {
        val main = getDataSource<Int, String> { "main" }

        val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test")).map { "$it from Store" }
        val cacheStrategyAfter = main cacheWith store expiresIn 50.milliseconds

        // Fetch the value from the main source and store it afterward
        either { cacheStrategyAfter.get(MainSync, 0) } shouldBeRight "main from Store"

        // there's no delay so the cache did not expire
        either { cacheStrategyAfter.get(Store, 0) } shouldBeRight "main from Store"
    }

    // "expiration rules"

    test("with a time expiration if there is no stored expiration date, the data then is expired") {
        val main = getDataSource<Int, String> { "main" }

        val store: StoreDataSource<Int, String> = InMemoryDataSource<Int, String>().map { "$it from Store" }

        val cacheRegistry = MemoizedExpirationCache()
        val cacheStrategyAfter = (
            main cacheWith store.expires(
                After(24.hours),
                cacheRegistry
            )
            ).build()

        // get from main and store it
        either { cacheStrategyAfter.get(MainSync, 0) } shouldBeRight "main from Store"

        either {
            cacheRegistry.run {
                delete(
                    Delete(
                        CacheMetaInformation(
                            key = 0.toString(),
                            classIdentifier = String::class.simpleName.toString()
                        )
                    )
                )
            }
        }
        // as we don't store the expirations the data should be expired
        either { cacheStrategyAfter.get(Store, 0) } shouldBeLeft Invalid
    }

    test("If the data is empty should remove the expiration date") {

        val info = CacheMetaInformation(
            key = "0",
            classIdentifier = String::class.simpleName.toString()
        )

        val expiration = mapOf(info to Clock.System.now() + 1.minutes)
        val expirationCache = InMemoryDataSource(expiration)

        val store: StoreDataSource<Int, String> = InMemoryDataSource()

        // the data does not exist
        either {
            store.expires(After(24.hours), expirationCache)
                .get(0)
        } shouldBeLeft DataNotFound

        // the stored expiration should be removed
        either { expirationCache.get(info) } shouldBeLeft DataNotFound
    }
})
