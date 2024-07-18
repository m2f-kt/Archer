package com.m2f.archer.crud.raise

import arrow.core.left
import arrow.core.right
import com.m2f.archer.crud.bool
import com.m2f.archer.crud.either
import com.m2f.archer.crud.nil
import com.m2f.archer.crud.nullable
import com.m2f.archer.crud.result
import com.m2f.archer.crud.unit
import com.m2f.archer.failure.DataEmpty
import com.m2f.archer.failure.DataNotFound
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class ArcherRaiseTest : FunSpec({

    test("null binding") {

        val a: Int? = null
        val b = 10

        nil { a.bind() }.shouldBeNull()
        nil { a.bind() } shouldBe nullable { a.bind() }
        nil { b.bind() }.shouldBe(10)
        nil { b.bind() } shouldBe nullable { b.bind() }
    }

    test("result dsl") {
        val b = 10

        result { b } shouldBe either { b }
    }

    test("bool dsl") {

        val a: Int? = null
        val resultSuccess = 10.right()
        val resultFailure = DataNotFound.left()

        bool { a.bind() } shouldBe false
        bool { a } shouldBe true
        bool { resultSuccess.bind() } shouldBe true
        bool { resultFailure.bind() } shouldBe false
        bool { raise(DataEmpty) } shouldBe false
    }

    test("unit dsl") {
        val a: Int? = null
        val b = 10

        unit { a.bind() } shouldBe Unit
        unit { b.bind() } shouldBe Unit
    }
})