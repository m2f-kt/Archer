package com.m2f.archer.crud.cache

import com.m2f.archer.crud.either
import com.m2f.archer.crud.getDataSource
import com.m2f.archer.crud.plus
import com.m2f.archer.crud.putDataSource
import com.m2f.archer.mapper.Bijection
import com.m2f.archer.mapper.map
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MapperTest : FunSpec({

    test("identity") {
        val identity: (String) -> String = { it }

        val get = getDataSource<Int, String> { "main" }

        val getMapped = get.map { identity(it) }

        either { get.get(0) shouldBe getMapped.get(0) }
    }

    test("bijection identity") {

        val identity = object : Bijection<String, String> {
            override fun from(s: String): String = s

            override fun to(t: String): String = t
        }

        val get = getDataSource<Unit, String> { "main" }
        val put = putDataSource<Unit, String> { _, value -> value }
        val store = get + put
        val storeMaped = store.map(identity)

        either { store.get(Unit) } shouldBe either { storeMaped.get(Unit) }
        either { store.put(Unit, "Hello") } shouldBe either { storeMaped.put(Unit, "Hello") }
    }
})
