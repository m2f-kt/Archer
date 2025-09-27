package com.m2f.archer.crud.cache

import com.m2f.archer.crud.Ice
import com.m2f.archer.crud.StoreDataSource
import com.m2f.archer.crud.getDataSource
import com.m2f.archer.crud.operation.MainSync
import com.m2f.archer.crud.operation.Store
import com.m2f.archer.query.Get
import com.m2f.archer.query.Put
import com.m2f.archer.utils.runArcherTest
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes

class InvalidationTest {

    @Test
    fun `Invalidating key enforces a stored data to return it's content no matter what`() = runTest {
        runArcherTest {
            val main = getDataSource<Int, _> { 1 }
            val store = StoreDataSource<Int, Int> { query ->
                when (query) {
                    is Get -> query.key * 2
                    is Put -> query.key
                }
            }

            val strategy = main cacheWith store expiresIn 5.minutes

            val result = ice {
                strategy.get(MainSync, 1)
                invalidateCache<Int>(1)
                strategy.get(Store, 1)
            }
            result shouldBe Ice.Content(2)
        }
    }

    @Test
    fun `Invalidate a strategy directly`() = runTest {
        runArcherTest {
            val main = getDataSource<Int, _> { 1 }
            val store = StoreDataSource<Int, Int> { query ->
                when (query) {
                    is Get -> query.key * 2
                    is Put -> query.key
                }
            }

            val strategy = main cacheWith store expiresIn 5.minutes

            val result = ice {
                strategy.get(MainSync, 1)
                strategy.invalidate(1)
            }
            result shouldBe Ice.Content(1)
        }
    }
}