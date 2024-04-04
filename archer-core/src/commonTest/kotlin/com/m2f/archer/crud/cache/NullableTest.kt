package com.m2f.archer.crud.cache

import com.m2f.archer.crud.getDataSource
import com.m2f.archer.crud.nullable
import com.m2f.archer.failure.DataNotFound
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class NullableTest : FunSpec({

    test("regular call wraps into non null content") {
        val getDataSource = getDataSource<Int, String> { "Success" }
        nullable { getDataSource.get(0) } shouldBe "Success"
    }

    test("failure call wraps into null content") {
        val getDataSource = getDataSource<Int, String> { raise(DataNotFound) }
        nullable { getDataSource.get(0) }.shouldBeNull()
    }
})