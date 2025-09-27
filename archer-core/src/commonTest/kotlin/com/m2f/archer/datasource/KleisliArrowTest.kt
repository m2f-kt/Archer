package com.m2f.archer.datasource

import com.m2f.archer.crud.Ice
import com.m2f.archer.mapper.andThen
import com.m2f.archer.repository.toRepository
import com.m2f.archer.utils.runArcherTest
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class KleisliArrowTest {

    @Test
    fun `Datasource thunk kleisli arrow`() = runTest {
        runArcherTest {
            var effect = 0
            val a = DataSource<Int, String> { effect.toString().also { effect++ } }
            val b = DataSource<Unit, Int> { 0 }
            val c = a.andThen { string -> b.execute() }

            val result1 = ice { c.execute(0) }
            result1 shouldBe Ice.Content(0)

            val result2 = ice { a.execute(0) }
            result2 shouldBe Ice.Content("1")

            val result3 = ice { b.execute() }
            result3 shouldBe Ice.Content(0)

            effect shouldBe 2
        }
    }

    @Test
    fun `DataSource kleisli arrow`() = runTest {
        runArcherTest {
            val a = DataSource<Int, String> { 3.toString() }
            val b = DataSource<String, Int> { it.toInt() }
            val c = a.andThen(b)

            val result1 = ice { c.execute(0) }
            result1 shouldBe Ice.Content(3)

            val result2 = ice { a.execute(0) }
            result2 shouldBe Ice.Content("3")

            val result3 = ice { b.execute("0") }
            result3 shouldBe Ice.Content(0)
        }
    }

    @Test
    fun `Repository thunk kleisli arrow`() = runTest {
        runArcherTest {
            var effect = 0
            val a = DataSource<Int, String> { effect.toString().also { effect++ } }.toRepository()
            val b = DataSource<Unit, Int> { 0 }.toRepository()
            val c = a.andThen { string -> b.execute() }

            val result1 = ice { c.execute(0) }
            result1 shouldBe Ice.Content(0)

            val result2 = ice { a.execute(0) }
            result2 shouldBe Ice.Content("1")

            val result3 = ice { b.execute() }
            result3 shouldBe Ice.Content(0)

            effect shouldBe 2
        }
    }

    @Test
    fun `Repository kleisli arrow`() = runTest {
        runArcherTest {
            val a = DataSource<Int, String> { 3.toString() }.toRepository()
            val b = DataSource<String, Int> { it.toInt() }.toRepository()
            val c = a.andThen(b)

            val result1 = ice { c.execute(0) }
            result1 shouldBe Ice.Content(3)

            val result2 = ice { a.execute(0) }
            result2 shouldBe Ice.Content("3")

            val result3 = ice { b.execute("0") }
            result3 shouldBe Ice.Content(0)
        }
    }
}