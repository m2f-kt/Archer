package com.m2f.archer.crud.raise

import arrow.core.left
import arrow.core.right
import com.m2f.archer.crud.Ice
import com.m2f.archer.datasource.DataSource
import com.m2f.archer.failure.DataEmpty
import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.repository.toRepository
import com.m2f.archer.utils.runArcherTest
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ArcherRaiseTest {

    @Test
    fun `null binding`() = runArcherTest {
        val a: Int? = null
        val b = 10

        nil { a.bind() }.shouldBeNull()
        nil { a.bind() } shouldBe nullable { a.bind() }
        nil { b.bind() }.shouldBe(10)
        nil { b.bind() } shouldBe nullable { b.bind() }
    }

    @Test
    fun `result dsl`() = runArcherTest {
        val b = 10

        result { b } shouldBe either { b }
    }

    @Test
    fun `bool dsl`() = runArcherTest {
        val a: Int? = null
        val resultSuccess = 10.right()
        val resultFailure = DataNotFound.left()

        bool { a.bind() } shouldBe false
        bool { a } shouldBe true
        bool { resultSuccess.bind() } shouldBe true
        bool { resultFailure.bind() } shouldBe false
        bool { raise(DataEmpty) } shouldBe false
    }

    @Test
    fun `unit dsl`() = runArcherTest {
        val a: Int? = null
        val b = 10

        unit { a.bind() } shouldBe Unit
        unit { b.bind() } shouldBe Unit
    }

    @Test
    fun `DataSource execution`() = runArcherTest {
        val datasource = DataSource<Int, String> { it.toString() }

        ice { datasource.execute(0) } shouldBe Ice.Content("0")
    }

    @Test
    fun `Repository execution`() = runArcherTest {
        val repo = DataSource<Int, String> { it.toString() }.toRepository()

        ice { repo.execute(0) } shouldBe Ice.Content("0")
    }

    @Test
    fun `Repository Unit execution`() = runArcherTest {
        val repo = DataSource<Unit, String> { it.toString() }.toRepository()

        ice { repo.execute() } shouldBe Ice.Content("kotlin.Unit")
    }

    @Test
    fun `DataSource Unit execution`() = runArcherTest {
        val ds = DataSource<Unit, String> { it.toString() }

        ice { ds.execute() } shouldBe Ice.Content("kotlin.Unit")
    }
}
