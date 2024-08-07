package com.m2f.archer.crud.cache

import com.m2f.archer.crud.Ice
import com.m2f.archer.crud.StoreDataSource
import com.m2f.archer.crud.cache.configuration.testConfiguration
import com.m2f.archer.crud.getDataSource
import com.m2f.archer.crud.ice
import com.m2f.archer.crud.operation.MainSync
import com.m2f.archer.crud.operation.Store
import com.m2f.archer.failure.Invalid
import com.m2f.archer.query.Get
import com.m2f.archer.query.Put
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.minutes

class InvalidationTest : FunSpec({

    with(testConfiguration) {

        test("Invalidating key enforces a stored data to return invalid") {

            val main = getDataSource<Int, _> { 1 }
            val store = StoreDataSource<Int, Int> { query ->
                when (query) {
                    is Get -> query.key * 2
                    is Put -> query.key
                }
            }

            val strategy = with(testConfiguration) {
                main cacheWith store expiresIn 5.minutes
            }

            ice {
                strategy.get(MainSync, 1)
                invalidateCache<Int>(testConfiguration, 1)
                strategy.get(Store, 1)
            } shouldBe Ice.Error(Invalid)
        }

        test("Invalidate a strategy directly") {
            val main = getDataSource<Int, _> { 1 }
            val store = StoreDataSource<Int, Int> { query ->
                when (query) {
                    is Get -> query.key * 2
                    is Put -> query.key
                }
            }

            val strategy = main cacheWith store expiresIn 5.minutes

            ice {
                strategy.get(MainSync, 1)
                strategy.invalidate(testConfiguration, 1)
                strategy.get(Store, 1)
            } shouldBe Ice.Error(Invalid)
        }
    }

})