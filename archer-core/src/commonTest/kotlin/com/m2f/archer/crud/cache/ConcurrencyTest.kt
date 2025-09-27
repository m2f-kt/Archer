package com.m2f.archer.crud.cache

import arrow.fx.stm.TVar
import arrow.fx.stm.atomically
import com.m2f.archer.crud.getDataSource
import com.m2f.archer.datasource.InMemoryDataSource
import com.m2f.archer.datasource.concurrency.mutex
import com.m2f.archer.datasource.concurrency.parallelism
import com.m2f.archer.utils.runArcherTest
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.test.Test

class ConcurrencyTest {

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

    @Test
    fun mutex() = runArcherTest {
        var count1 = 0
        val get = getDataSource<Unit, Int> { count1++ }
            .mutex()

        either {
            massiveRun {
                get.get(Unit)
            }

            val result = get.get(Unit)
            result shouldBe i * j
        }
    }

    @Test
    fun `limitParallelism`() = runArcherTest {
        var count1 = 0
        val get = getDataSource<Unit, Int> { count1++ }
            .parallelism(1)

        either {
            massiveRun {
                get.get(Unit)
            }

            val result = get.get(Unit)
            result shouldBe i * j
        }
    }

    @Test
    fun `InMemoryDataSource is thread safe`() = runArcherTest {
        val dataSource = InMemoryDataSource<Int, Int>()

        val count = TVar.new(0)

        withContext(Dispatchers.Default) {
            either {
                massiveRun {
                    val new = atomically {
                        count.read()
                            .also { count.write(it + 1) }
                    }
                    dataSource.put(new, new)
                }
            }
        }

        val result = List(i * j) { it }.mapIndexed { _, item -> either { dataSource.get(item) } }
            .filter { it.isRight() }

        result.size shouldBe i * j
    }
}
