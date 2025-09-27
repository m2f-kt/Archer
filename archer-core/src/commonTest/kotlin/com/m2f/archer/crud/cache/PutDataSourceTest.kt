package com.m2f.archer.crud.cache

import com.m2f.archer.crud.postDataSource
import com.m2f.archer.crud.putDataSource
import com.m2f.archer.failure.DataEmpty
import com.m2f.archer.failure.Invalid
import com.m2f.archer.mapper.Bijection
import com.m2f.archer.mapper.map
import com.m2f.archer.query.Put
import com.m2f.archer.utils.runArcherTest
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class PutDataSourceTest {

    @Test
    fun `creating a putDataSource with the DSL`() = runArcherTest {
        val putDataSource = putDataSource<Int, String> { key, value -> "put $value with key $key" }

        val result1 = either { putDataSource.put(1, "test") }
        result1 shouldBeRight "put test with key 1"

        val result2 = either { putDataSource.run { invoke(Put(1, null)) } }
        result2 shouldBeLeft DataEmpty
    }

    @Test
    fun `Creating a post data source with DSL`() = runArcherTest {
        val postDataSource = postDataSource<Int, String> { _ -> "success" }

        val result1 = either { postDataSource.post(0) }
        result1 shouldBeRight "success"

        val result2 = either { postDataSource.run { invoke(Put(0, "success")) } }
        result2 shouldBeLeft Invalid
    }

    @Test
    fun `identity rule`() = runArcherTest {
        val identity = object : Bijection<String, String> {
            override fun from(s: String): String = s

            override fun to(t: String): String = t
        }
        val putDataSource = putDataSource<Int, String> { key, value -> "put $value with key $key" }

        val mappedPutDataSource = putDataSource.map(identity)

        val result1 = either { putDataSource.put(0, "hello") }
        val result2 = either { mappedPutDataSource.put(0, "hello") }
        result2 shouldBe result1
    }

    @Test
    fun `map does not add any side effect on null values`() = runArcherTest {
        val identity = object : Bijection<String, String> {
            override fun from(s: String): String = s

            override fun to(t: String): String = t
        }
        val putDataSource = putDataSource<Int, String> { key, value -> "put $value with key $key" }

        val mappedPutDataSource = putDataSource.map(identity)

        val result1 = either { putDataSource.post(0) }
        val result2 = either { mappedPutDataSource.post(0) }
        result2 shouldBe result1
    }
}