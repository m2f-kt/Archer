package com.m2f.archer.crud.cache

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.m2f.archer.crud.StoreDataSource
import com.m2f.archer.crud.cache.CacheExpiration.After
import com.m2f.archer.crud.cache.CacheExpiration.Always
import com.m2f.archer.crud.cache.CacheExpiration.Never
import com.m2f.archer.crud.cache.memcache.CacheMetaInformation
import com.m2f.archer.crud.cache.memcache.MemoizedExpirationCache
import com.m2f.archer.crud.get
import com.m2f.archer.crud.getDataSource
import com.m2f.archer.crud.operation.MainSyncOperation
import com.m2f.archer.crud.operation.StoreOperation
import com.m2f.archer.crud.operation.StoreSyncOperation
import com.m2f.archer.crud.put
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
            val expiresAfter10Millis = store.expires(After(time), InMemoryDataSource())

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

        val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test")).map { "$it from Store" }

        test("create a never expiring strategy") {
            val cacheStrategyNever = main cacheWith store expires Never

            //The cache never expires, so it should always return the value that we set in the default constructor
            cacheStrategyNever.get(StoreSyncOperation, 0) shouldBeRight "Test from Store"
        }

        test("creating an always expiring strategy") {
            val cacheStrategyAlways = main cacheWith store expires Always

            //The cache always expires, so it should always return the value after storing it.
            cacheStrategyAlways.get(StoreSyncOperation, 0) shouldBeRight "main from Store"
        }

        test("test expiration with a time expiring strategy") {
            val cacheStrategyAfter = main cacheWith store expiresIn 50.milliseconds

            //Fetch the value from the main source and store it afterward
            cacheStrategyAfter.get(MainSyncOperation, 0) shouldBeRight "main from Store"

            //Wait few milliseconds to let the cache expire
            delay(100L)

            //The cache is expired so if we enforce data from store should be invalid
            cacheStrategyAfter.get(StoreOperation, 0) shouldBeLeft Invalid

        }

        test("test no-expiration with a time expiring strategy") {
            val cacheStrategyAfter = main cacheWith store expiresIn 50.milliseconds

            //Fetch the value from the main source and store it afterward
            cacheStrategyAfter.get(MainSyncOperation, 0) shouldBeRight "main from Store"

            //there's no delay so the cache did not expire
            cacheStrategyAfter.get(StoreOperation, 0) shouldBeRight "main from Store"

        }
    }

    context("expiration rules") {
        val main = getDataSource<Int, String> { "main" }

        val fakeCacheInstant = object : CacheDataSource<CacheMetaInformation, Instant> {
            override suspend fun delete(q: Delete<CacheMetaInformation>): Either<Failure, Unit> = Unit.right()

            override suspend fun invoke(q: KeyQuery<CacheMetaInformation, out Instant>): Either<Failure, Instant> = when (q) {
                is Put -> q.value?.right() ?: DataEmpty.left()
                is Get -> DataNotFound.left()
            }

        }

        val store: StoreDataSource<Int, String> = InMemoryDataSource<Int, String>().map { "$it from Store" }

        test("with a time expiration if there is no stored expiration date, the data then is expired") {

            val cacheRegistry = MemoizedExpirationCache()
            val cacheStrategyAfter = (main cacheWith store.expires(
                After(24.hours),
                cacheRegistry
            )).build()

            //get from main and store it
            cacheStrategyAfter.get(MainSyncOperation, 0) shouldBeRight "main from Store"

            cacheRegistry.delete(Delete(CacheMetaInformation(
                key = 0.toString(),
                classIdentifier = String::class.simpleName.toString(),
                classFullIdentifier = String::class.qualifiedName.toString()
            )))

            //as we don't store the expirations the data should be expired
            cacheStrategyAfter.get(StoreOperation, 0) shouldBeLeft Invalid
        }

        test("If the data is empty should remove the expiration date") {

            val info = CacheMetaInformation(
                key = "0",
                classIdentifier = String::class.simpleName.toString(),
                classFullIdentifier = String::class.qualifiedName.toString(),
            )

            val expiration = mapOf(info to Clock.System.now() + 24.hours)
            val expirationCache = InMemoryDataSource(expiration)

            val store: StoreDataSource<Int, String> = InMemoryDataSource()

            //the data does not exist
            store.expires(After(24.hours), expirationCache)
                .get(0) shouldBeLeft DataNotFound

            //the stored expiration should be removed
            expirationCache.get(info) shouldBeLeft DataNotFound
        }
    }
})