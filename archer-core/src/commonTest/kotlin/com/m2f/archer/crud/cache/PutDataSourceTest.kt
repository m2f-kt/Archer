package com.m2f.archer.crud.cache

import com.m2f.archer.crud.postDataSource
import com.m2f.archer.crud.putDataSource
import com.m2f.archer.failure.DataEmpty
import com.m2f.archer.failure.Invalid
import com.m2f.archer.mapper.Bijection
import com.m2f.archer.mapper.map
import com.m2f.archer.query.Put
import com.m2f.archer.utils.archerTest
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PutDataSourceTest : FunSpec({

    archerTest("creating a putDataSource with the DSL") {
        val putDataSource = putDataSource<Int, String> { key, value -> "put $value with key $key" }

        either { putDataSource.put(1, "test") } shouldBeRight "put test with key 1"
        either { putDataSource.run { invoke(Put(1, null)) } } shouldBeLeft DataEmpty
    }

    archerTest("Creating a post data source with DSL") {
        val postDataSource = postDataSource<Int, String> { _ -> "success" }
        either { postDataSource.post(0) } shouldBeRight "success"
        either { postDataSource.run { invoke(Put(0, "success")) } }.shouldBeLeft(Invalid)
    }

    archerTest("identity rule") {

        val identity = object : Bijection<String, String> {
            override fun from(s: String): String = s

            override fun to(t: String): String = t
        }
        val putDataSource = putDataSource<Int, String> { key, value -> "put $value with key $key" }

        val mappedPutDataSource = putDataSource.map(identity)

        either { putDataSource.put(0, "hello") } shouldBe either {
            mappedPutDataSource.put(
                0,
                "hello"
            )
        }
    }

    archerTest("map does not add any side effect on null values") {
        val identity = object : Bijection<String, String> {
            override fun from(s: String): String = s

            override fun to(t: String): String = t
        }
        val putDataSource = putDataSource<Int, String> { key, value -> "put $value with key $key" }

        val mappedPutDataSource = putDataSource.map(identity)

        either { putDataSource.post(0) } shouldBe either { mappedPutDataSource.post(0) }
    }
})
