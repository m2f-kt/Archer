package com.m2f.archer.crud.cache

import com.m2f.archer.crud.post
import com.m2f.archer.crud.postDataSource
import com.m2f.archer.crud.put
import com.m2f.archer.crud.putDataSource
import com.m2f.archer.failure.DataEmpty
import com.m2f.archer.failure.Invalid
import com.m2f.archer.mapper.Bijection
import com.m2f.archer.mapper.map
import com.m2f.archer.query.Put
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PutDataSourceTest : FunSpec({

    test("creating a putDataSource with the DSL") {
        val putDataSource = putDataSource<Int, String> { key, value -> "put $value with key $key" }

        putDataSource.put(1, "test") shouldBeRight "put test with key 1"
        putDataSource.invoke(Put(1, null)) shouldBeLeft DataEmpty
    }

    test("Creating a post data source with DSL") {
        val postDataSource = postDataSource<Int, String> { _ -> "success" }
        postDataSource.post(0) shouldBeRight "success"
        postDataSource.invoke(Put(0, "success")) shouldBeLeft Invalid
    }

    test("identity rule") {

        val identity = object : Bijection<String, String> {
            override fun from(s: String): String = s

            override fun to(t: String): String = t
        }
        val putDataSource = putDataSource<Int, String> { key, value -> "put $value with key $key" }

        val mappedPutDataSource = putDataSource.map(identity)

        putDataSource.put(0, "hello") shouldBe mappedPutDataSource.put(0, "hello")
    }

    test("map does not add any side effect on null values") {
        val identity = object : Bijection<String, String> {
            override fun from(s: String): String = s

            override fun to(t: String): String = t
        }
        val putDataSource = putDataSource<Int, String> { key, value -> "put $value with key $key" }

        val mappedPutDataSource = putDataSource.map(identity)

        putDataSource.post(0) shouldBe mappedPutDataSource.post(0)
    }
})
