package com.m2f.archer.crud.cache

import com.m2f.archer.crud.GetDataSource
import com.m2f.archer.crud.cache.CacheExpiration.Never
import com.m2f.archer.crud.either
import com.m2f.archer.crud.fallbackWith
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
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec

class RepositoryStrategyTest : FunSpec({

    test("Create a fallback strategy with infix function") {

        val mainGet = getDataSource<Int, String> { key -> if (key == 0) "main get" else raise(DataNotFound) }
        val storeGet = getDataSource<Int, String> { "store get" }
        val storePut = putDataSource<Int, String> { _, value -> "store put: $value" }

        /*FallbackWith creates a Repository using MainSync
         * this strategy always tries to get data form the main Data source and save the result
         * in the store. If the main one fails then it just tries to return the last saved data.*/

        val repository = mainGet fallbackWith storeGet + storePut

        either { repository.get(0) } shouldBeRight "store put: main get"
        either { repository.get(1) } shouldBeRight "store get"
    }

    test("Uncurry a strategy") {
        /*A strategy is an objects that returns a Repository given an operation
         * but we also provide a get function that calls the get function of the
         * Repository without the need to get the instance of the Repository*/

        val mainGet = getDataSource<Int, String> { key -> if (key == 0) "main get" else raise(DataNotFound) }
        val storeGet = getDataSource<Int, String> { "store get" }
        val storePut = putDataSource<Int, String> { _, value -> "store put: $value" }

        val strategy = mainGet.cacheWith(storeGet + storePut) expires Never

        either { strategy.get(Main, 0) } shouldBeRight "main get"
        either { strategy.get(Store, 0) } shouldBeRight "store get"
        either { strategy.get(MainSync, 0) } shouldBeRight "store put: main get"
        either { strategy.get(StoreSync, 0) } shouldBeRight "store get"

        val expiredStore = (storeGet.validate { false } + storePut)
        val expiredStrategy = mainGet.cacheWith(expiredStore) expires Never

        either { expiredStrategy.get(StoreSync, 0) } shouldBeRight "store put: main get"
    }

    test("StoreSync will return by default the stored data") {
        val mainGet = getDataSource<Int, String> { key -> if (key == 0) "main get" else raise(DataNotFound) }
        val storeGet = getDataSource<Int, String> { "store get" }
        val storePut = putDataSource<Int, String> { _, value -> "store put: $value" }

        val strategy = mainGet.cacheWith(storeGet + storePut) expires Never
        either { strategy.get(StoreSync, 0) } shouldBeRight "store get"
    }

    test("StoreSync will fail if there is an unrecoverable failure") {
        val mainGet = getDataSource<Int, String> { key -> if (key == 0) "main get" else raise(DataNotFound) }
        val storeGetFailUnrecoverable = getDataSource<Int, String> { raise(Unknown) }
        val storePut = putDataSource<Int, String> { _, value -> "store put: $value" }

        val strategy = mainGet.cacheWith(storeGetFailUnrecoverable + storePut) expires Never
        either { strategy.get(StoreSync, 0) } shouldBeLeft Unknown
    }

    test("StoreSync will fallback to the main data source and save result if there is an recoverable failure") {
        val mainGet = getDataSource<Int, String> { key -> if (key == 0) "main get" else raise(DataNotFound) }
        val storeGetFailRecoverable = getDataSource<Int, String> { raise(Invalid) }
        val storePut = putDataSource<Int, String> { _, value -> "store put: $value" }

        val strategy = mainGet.cacheWith(storeGetFailRecoverable + storePut) expires Never
        either { strategy.get(StoreSync, 0) } shouldBeRight "store put: main get"
    }

    test("MainSync will fail if there is an unrecoverable failure") {
        val mainGetFailUnrecoverable = GetDataSource<Int, String> { raise(Unknown) }
        val store = InMemoryDataSource<Int, String>()

        val strategy = mainGetFailUnrecoverable cacheWith store expires Never
        either { strategy.get(MainSync, 0) } shouldBeLeft Unknown
    }
})
