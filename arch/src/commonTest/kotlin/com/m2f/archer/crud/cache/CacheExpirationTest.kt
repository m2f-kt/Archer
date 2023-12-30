package com.m2f.archer.crud.cache

import com.m2f.archer.crud.StoreDataSource
import com.m2f.archer.crud.cache.CacheExpiration.After
import com.m2f.archer.crud.cache.CacheExpiration.Always
import com.m2f.archer.crud.cache.CacheExpiration.Never
import com.m2f.archer.crud.get
import com.m2f.archer.crud.getDataSource
import com.m2f.archer.crud.operation.MainSyncOperation
import com.m2f.archer.crud.operation.StoreOperation
import com.m2f.archer.crud.operation.StoreSyncOperation
import com.m2f.archer.crud.put
import com.m2f.archer.datasource.InMemoryDataSource
import com.m2f.archer.failure.Invalid
import com.m2f.archer.mapper.map
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class CacheExpirationTest : FunSpec({

    context("passing expiration") {
        val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test"))

        test("never expires") {
            val neverExpires = store.expires(Never)
            neverExpires.get(0) shouldBeRight "Test"
        }

        test("always expires") {
            val alwaysExpires = store.expires(Always)
            alwaysExpires.get(0) shouldBeLeft Invalid
        }

        context("expires with time") {
            val time = 50.milliseconds
            val expiresAfter10Millis = store.expires(After(time))

            test("fetching after time passed") {
                expiresAfter10Millis.put(0, "test10")
                delay(100L)
                expiresAfter10Millis.get(0) shouldBeLeft Invalid
            }

            test("fetching before time passes") {
                expiresAfter10Millis.put(0, "test10_2")
                expiresAfter10Millis.get(0) shouldBeRight "test10_2"
            }
        }
    }

    context("create an a caching strategy with expiration") {
        val main = getDataSource<Int, String> { "main" }

        val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test"))
            .map { "$it from Store" }

        test("create a never expiring strategy") {
            val cacheStrategyNever = main.cache(store, Never)

            //The cache never expires, so it should always return the value that we set in the default constructor
            cacheStrategyNever.get(StoreSyncOperation, 0) shouldBeRight "Test from Store"
        }

        test("creating an always expiring strategy") {
            val cacheStrategyAlways = main.cache(store, Always)

            //The cache always expires, so it should always return the value after storing it.
            cacheStrategyAlways.get(StoreSyncOperation, 0) shouldBeRight "main from Store"
        }

        test("test expiration with a time expiring strategy") {
            val cacheStrategyAfter = main.cache(store, After(50.milliseconds))

            //Fetch the value from the main source and store it afterward
            cacheStrategyAfter.get(MainSyncOperation, 0) shouldBeRight "main from Store"

            //Wait few milliseconds to let the cache expire
            delay(100L)

            //The cache is expired so if we enforce data from store should be invalid
            cacheStrategyAfter.get(StoreOperation, 0) shouldBeLeft Invalid

        }

        test("test no-expiration with a time expiring strategy") {
            val cacheStrategyAfter = main.cache(store, After(50.milliseconds))

            //Fetch the value from the main source and store it afterward
            cacheStrategyAfter.get(MainSyncOperation, 0) shouldBeRight "main from Store"

            //there's no delay so the cache did not expire
            //The cache is expired so if we enforce data from store should be invalid
            cacheStrategyAfter.get(StoreOperation, 0) shouldBeRight "main from Store"

        }

    }
})