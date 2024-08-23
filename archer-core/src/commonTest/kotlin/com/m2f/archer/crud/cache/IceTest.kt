package com.m2f.archer.crud.cache

import com.m2f.archer.crud.Ice
import com.m2f.archer.crud.fold
import com.m2f.archer.crud.getDataSource
import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.failure.Idle
import com.m2f.archer.utils.archerTest
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeNone
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.arrow.core.shouldBeSome
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Exhaustive
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.of

class IceTest : FunSpec({

    archerTest("regular call wraps into Ice.Content") {
        val getDataSource = getDataSource<Int, String> { "Success" }
        ice { getDataSource.get(0) } shouldBe Ice.Content("Success")
    }

    archerTest("regular failures wraps into Ice.Error") {
        val getDataSource = getDataSource<Int, String> { raise(DataNotFound) }
        ice { getDataSource.get(0) } shouldBe Ice.Error(DataNotFound)
    }

    archerTest("idle failures wraps into Ice.Idle") {
        val getDataSource = getDataSource<Int, String> { raise(Idle) }
        ice { getDataSource.get(0) } shouldBe Ice.Idle
    }

    archerTest("Ice catamomrphism") {
        val succes = getDataSource<Int, String> { "Success" }
        val idle = getDataSource<Int, String> { raise(Idle) }
        val failure = getDataSource<Int, String> { raise(DataNotFound) }

        val iceSucces = ice { succes.get(0) }
        val iceIdle = ice { idle.get(0) }
        val iceError = ice { failure.get(0) }

        val fold: (Ice<String>) -> String = { ice ->
            ice.fold(
                ifIdle = { "Idle" },
                ifContent = { "Success" },
                ifError = { "Failure" }
            )
        }

        Exhaustive.of(iceSucces, iceIdle, iceError).checkAll {
            fold(it) shouldBe when (it) {
                is Ice.Content -> "Success"
                is Ice.Idle -> "Idle"
                is Ice.Error -> "Failure"
            }
        }
    }

    archerTest("Ice binding in ice DSL") {
        val one = Ice.Content(1)
        ice { one.bind() + one.bind() } shouldBe ice { 2 }
    }

    archerTest("Ice binding failure in ice DSL") {
        val fail = Ice.Error(DataNotFound)
        val idle = Ice.Idle
        ice { fail.bind() } shouldBe Ice.Error(DataNotFound)
        ice { idle.bind() } shouldBe Ice.Idle
    }

    archerTest("Ice binding in either DSL") {
        val one = Ice.Content(1)
        either { one.bind() + one.bind() } shouldBeRight 2
    }

    archerTest("Ice binding failure in either DSL") {
        val fail = Ice.Error(DataNotFound)
        val idle = Ice.Idle
        either<String> { fail.bind() } shouldBeLeft DataNotFound
        either<String> { idle.bind() } shouldBeLeft Idle
    }

    archerTest("Ice binding in option DSL") {
        val one = Ice.Content(1)
        option { one.bind() + one.bind() } shouldBeSome 2
    }

    archerTest("Ice binding failure in option DSL") {
        val fail = Ice.Error(DataNotFound)
        val idle = Ice.Idle
        option<String> { fail.bind() }.shouldBeNone()
        option<String> { idle.bind() }.shouldBeNone()
    }

    archerTest("Ice binding in nullable DSL") {
        val one = Ice.Content(1)
        nullable { one.bind() + one.bind() } shouldBe 2
    }

    archerTest("Ice binding failure in nullable DSL") {
        val fail = Ice.Error(DataNotFound)
        val idle = Ice.Idle
        nullable<String> { fail.bind() }.shouldBeNull()
        nullable<String> { idle.bind() }.shouldBeNull()
    }
})
