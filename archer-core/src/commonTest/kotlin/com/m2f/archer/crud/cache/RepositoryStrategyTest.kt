package com.m2f.archer.crud.cache

import com.m2f.archer.crud.GetDataSource
import com.m2f.archer.crud.cache.CacheExpiration.Never
import com.m2f.archer.crud.getDataSource
import com.m2f.archer.crud.operation.Main
import com.m2f.archer.crud.operation.MainSync
import com.m2f.archer.crud.operation.Store
import com.m2f.archer.crud.operation.StoreSync
import com.m2f.archer.crud.plus
import com.m2f.archer.crud.putDataSource
import com.m2f.archer.crud.validate.validate
import com.m2f.archer.datasource.InMemoryDataSource
import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.failure.Invalid
import com.m2f.archer.failure.Unknown
import com.m2f.archer.utils.runArcherTest
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import kotlin.test.Test

class RepositoryStrategyTest {

    @Test
    fun `Create a fallback strategy with infix function`() = runArcherTest {
        val mainGet = getDataSource<Int, String> { key -> if (key == 0) "main get" else raise(DataNotFound) }
        val storeGet = getDataSource<Int, String> { "store get" }
        val storePut = putDataSource<Int, String> { _, value -> "store put: $value" }

        /*FallbackWith creates a Repository using MainSync
         * this strategy always tries to get data form the main Data source and save the result
         * in the store. If the main one fails then it just tries to return the last saved data.*/

        val repository = mainGet fallbackWith storeGet + storePut

        val result1 = either { repository.get(0) }
        result1 shouldBeRight "store put: main get"

        val result2 = either { repository.get(1) }
        result2 shouldBeRight "store get"
    }

    @Test
    fun `Uncurry a strategy`() = runArcherTest {
        /*A strategy is an objects that returns a Repository given an operation
         * but we also provide a get function that calls the get function of the
         * Repository without the need to get the instance of the Repository*/

        val mainGet = getDataSource<Int, String> { key -> if (key == 0) "main get" else raise(DataNotFound) }
        val storeGet = getDataSource<Int, String> { "store get" }
        val storePut = putDataSource<Int, String> { _, value -> "store put: $value" }

        val strategy = mainGet.cacheWith(storeGet + storePut) expires Never

        val result1 = either { strategy.get(Main, 0) }
        result1 shouldBeRight "main get"

        val result2 = either { strategy.get(Store, 0) }
        result2 shouldBeRight "store get"

        val result3 = either { strategy.get(MainSync, 0) }
        result3 shouldBeRight "store put: main get"

        val result4 = either { strategy.get(StoreSync, 0) }
        result4 shouldBeRight "store get"

        val expiredStore = (storeGet.validate { false } + storePut)
        val expiredStrategy = mainGet.cacheWith(expiredStore) expires Never

        val result5 = either { expiredStrategy.get(StoreSync, 0) }
        result5 shouldBeRight "store put: main get"
    }

    @Test
    fun `StoreSync will return by default the stored data`() = runArcherTest {
        val mainGet = getDataSource<Int, String> { key -> if (key == 0) "main get" else raise(DataNotFound) }
        val storeGet = getDataSource<Int, String> { "store get" }
        val storePut = putDataSource<Int, String> { _, value -> "store put: $value" }

        val strategy = mainGet.cacheWith(storeGet + storePut) expires Never
        val result = either { strategy.get(StoreSync, 0) }
        result shouldBeRight "store get"
    }

    @Test
    fun `StoreSync will fail if there is an unrecoverable failure`() = runArcherTest {
        val mainGet = getDataSource<Int, String> { key -> if (key == 0) "main get" else raise(DataNotFound) }
        val storeGetFailUnrecoverable = getDataSource<Int, String> { raise(Unknown) }
        val storePut = putDataSource<Int, String> { _, value -> "store put: $value" }

        val strategy = mainGet.cacheWith(storeGetFailUnrecoverable + storePut) expires Never
        val result = either { strategy.get(StoreSync, 0) }
        result shouldBeLeft Unknown
    }

    @Test
    fun `StoreSync will fallback to the main data source and save result if there is an recoverable failure`() = runArcherTest {
        val mainGet = getDataSource<Int, String> { key -> if (key == 0) "main get" else raise(DataNotFound) }
        val storeGetFailRecoverable = getDataSource<Int, String> { raise(Invalid) }
        val storePut = putDataSource<Int, String> { _, value -> "store put: $value" }

        val strategy = mainGet.cacheWith(storeGetFailRecoverable + storePut) expires Never
        val result = either { strategy.get(StoreSync, 0) }
        result shouldBeRight "store put: main get"
    }

    @Test
    fun `MainSync will fail if there is an unrecoverable failure`() = runArcherTest {
        val mainGetFailUnrecoverable = GetDataSource<Int, String> { raise(Unknown) }
        val store = InMemoryDataSource<Int, String>()

        val strategy = mainGetFailUnrecoverable cacheWith store expires Never
        val result = either { strategy.get(MainSync, 0) }
        result shouldBeLeft Unknown
    }
}
