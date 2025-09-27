package com.m2f.archer.crud.cache

import com.m2f.archer.crud.getDataSource
import com.m2f.archer.crud.plus
import com.m2f.archer.crud.putDataSource
import com.m2f.archer.mapper.Bijection
import com.m2f.archer.mapper.map
import com.m2f.archer.utils.runArcherTest
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class MapperTest {

    @Test
    fun `identity`() = runArcherTest {
        val identity: (String) -> String = { it }

        val get = getDataSource<Int, String> { "main" }

        val getMapped = get.map { identity(it) }

        either {
            val getResult = get.get(0)
            val getMappedResult = getMapped.get(0)
            getMappedResult shouldBe getResult
        }
    }

    @Test
    fun `bijection identity`() = runArcherTest {
        val identity = object : Bijection<String, String> {
            override fun from(s: String): String = s

            override fun to(t: String): String = t
        }

        val get = getDataSource<Unit, String> { "main" }
        val put = putDataSource<Unit, String> { _, value -> value }
        val store = get + put
        val storeMaped = store.map(identity)

        val result1 = either { store.get(Unit) }
        val result2 = either { storeMaped.get(Unit) }
        result2 shouldBe result1

        val result3 = either { store.put(Unit, "Hello") }
        val result4 = either { storeMaped.put(Unit, "Hello") }
        result4 shouldBe result3
    }
}