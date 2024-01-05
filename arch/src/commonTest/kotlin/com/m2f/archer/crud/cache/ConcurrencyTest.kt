package com.m2f.archer.crud.cache

import arrow.fx.stm.TVar
import arrow.fx.stm.atomically
import com.m2f.archer.crud.cache.memcache.CacheMetaInformation
import com.m2f.archer.crud.cache.memcache.MemoizedExpirationCache
import com.m2f.archer.crud.get
import com.m2f.archer.crud.getDataSource
import com.m2f.archer.crud.put
import com.m2f.archer.datasource.InMemoryDataSource
import com.m2f.archer.datasource.concurrency.mutex
import com.m2f.archer.datasource.concurrency.parallelism
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class ConcurrencyTest : FunSpec({

    val i = 100
    val j = 1000

    suspend fun massiveRun(n: Int = i, k: Int = j, action: suspend () -> Unit) {
        coroutineScope { // scope for coroutines
            repeat(n) {
                launch {
                    repeat(k) { action() }
                }
            }
        }
        println("Completed ${n * k} actions")
    }

    test("mutex") {
        var count1 = 0
        val get = getDataSource<Unit, Int> { count1++ }
            .mutex()

        massiveRun {
            get.get(Unit)
        }

        get.get(Unit) shouldBeRight i * j
    }

    test("limitParallelism") {
        var count1 = 0
        val get = getDataSource<Unit, Int> { count1++ }
            .parallelism(1)

        massiveRun {
            get.get(Unit)
        }

        get.get(Unit) shouldBeRight i * j
    }

    test("InMemoryDataSource is thread safe") {

        val dataSource = InMemoryDataSource<Int, Int>()

        val count = TVar.new(0)

        withContext(Dispatchers.Default) {
            massiveRun {
                val new = atomically {
                    count.read()
                        .also { count.write(it + 1) }
                }
                dataSource.put(new, new)
            }
        }

        val result = List(i * j) { it }
            .mapIndexed { index, item -> dataSource.get(item) }
            .filter { it.isRight() }

        result.size shouldBe i * j
    }

    test("MemoizedExpirationCache is thread safe") {
        val dataSource = MemoizedExpirationCache()
        val countInt = TVar.new(0)
        val count = CacheMetaInformation("0", "String", "String")

        withContext(Dispatchers.Default) {
            massiveRun {
                val new = atomically {
                    countInt.read()
                        .also { countInt.write(it + 1) }
                }
                dataSource.put(count.copy(key = new.toString()), Clock.System.now())
            }
        }
    }
})
