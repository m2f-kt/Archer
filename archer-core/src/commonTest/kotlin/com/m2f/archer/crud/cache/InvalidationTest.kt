package com.m2f.archer.crud.cache

import com.m2f.archer.crud.Ice
import com.m2f.archer.crud.StoreDataSource
import com.m2f.archer.crud.getDataSource
import com.m2f.archer.crud.operation.MainSync
import com.m2f.archer.crud.operation.Store
import com.m2f.archer.query.Get
import com.m2f.archer.query.Put
import com.m2f.archer.utils.archerTest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.minutes

class InvalidationTest : FunSpec({

    archerTest("Invalidating key enforces a stored data to return it's content no matter what") {

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
            invalidateCache<Int>(1)
            strategy.get(Store, 1)
        } shouldBe Ice.Content(2)
    }

    archerTest("Invalidate a strategy directly") {
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
            strategy.invalidate(1)
        } shouldBe Ice.Content(1)
    }
})
