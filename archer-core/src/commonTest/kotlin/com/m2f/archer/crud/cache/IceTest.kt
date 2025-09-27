package com.m2f.archer.crud.cache

import arrow.core.Some
import com.m2f.archer.crud.Ice
import com.m2f.archer.crud.fold
import com.m2f.archer.crud.getDataSource
import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.failure.Idle
import com.m2f.archer.utils.runArcherTest
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeNone
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class IceTest {

    @Test
    fun `regular call wraps into Ice Content`() = runArcherTest {
        val getDataSource = getDataSource<Int, String> { "Success" }
        val result = ice { getDataSource.get(0) }
        result shouldBe Ice.Content("Success")
    }

    @Test
    fun `regular failures wraps into Ice Error`() = runArcherTest {
        val getDataSource = getDataSource<Int, String> { raise(DataNotFound) }
        val result = ice { getDataSource.get(0) }
        result shouldBe Ice.Error(DataNotFound)
    }

    @Test
    fun `idle failures wraps into Ice Idle`() = runArcherTest {
        val getDataSource = getDataSource<Int, String> { raise(Idle) }
        val result = ice { getDataSource.get(0) }
        result shouldBe Ice.Idle
    }

    @Test
    fun `Ice catamomrphism`() = runArcherTest {
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

        // Test each ice type individually
        val testCases = listOf(iceSucces, iceIdle, iceError)
        for (testCase in testCases) {
            val result = fold(testCase)
            val expected = when (testCase) {
                is Ice.Content -> "Success"
                is Ice.Idle -> "Idle"
                is Ice.Error -> "Failure"
            }
            result shouldBe expected
        }
    }

    @Test
    fun `Ice binding in ice DSL`() = runArcherTest {
        val one = Ice.Content(1)
        val result = ice { one.bind() + one.bind() }
        val expected = ice { 2 }
        result shouldBe expected
    }

    @Test
    fun `Ice binding failure in ice DSL`() = runArcherTest {
        val fail = Ice.Error(DataNotFound)
        val idle = Ice.Idle
        val result1 = ice { fail.bind() }
        result1 shouldBe Ice.Error(DataNotFound)

        val result2 = ice { idle.bind() }
        result2 shouldBe Ice.Idle
    }

    @Test
    fun `Ice binding in either DSL`() = runArcherTest {
        val one = Ice.Content(1)
        val result = either { one.bind() + one.bind() }
        result shouldBeRight 2
    }

    @Test
    fun `Ice binding failure in either DSL`() = runArcherTest {
        val fail = Ice.Error(DataNotFound)
        val idle = Ice.Idle

        val result1 = either<String> { fail.bind() }
        result1 shouldBeLeft DataNotFound

        val result2 = either<String> { idle.bind() }
        result2 shouldBeLeft Idle
    }

    @Test
    fun `Ice binding in option DSL`() = runArcherTest {
        val one = Ice.Content(1)
        val result = option { one.bind() + one.bind() }
        result shouldBe Some(2)
    }

    @Test
    fun `Ice binding failure in option DSL`() = runArcherTest {
        val fail = Ice.Error(DataNotFound)
        val idle = Ice.Idle

        val result1 = option<String> { fail.bind() }
        result1.shouldBeNone()

        val result2 = option<String> { idle.bind() }
        result2.shouldBeNone()
    }

    @Test
    fun `Ice binding in nullable DSL`() = runArcherTest {
        val one = Ice.Content(1)
        val result = nullable<Int> { one.bind() + one.bind() }
        result shouldBe 2
    }

    @Test
    fun `Ice binding failure in nullable DSL`() = runArcherTest {
        val fail = Ice.Error(DataNotFound)
        val idle = Ice.Idle

        val result1 = nullable<String> { fail.bind() }
        result1.shouldBeNull()

        val result2 = nullable<String> { idle.bind() }
        result2.shouldBeNull()
    }
}
