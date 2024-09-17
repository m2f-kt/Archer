package com.m2f.archer.datasource

import com.m2f.archer.crud.Ice
import com.m2f.archer.mapper.andThen
import com.m2f.archer.repository.toRepository
import com.m2f.archer.utils.archerTest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class KleisliArrowTest : FunSpec({

    archerTest("Datasource thunk kleisli arrow") {
        var effect = 0
        val a = DataSource<Int, String> { effect.toString().also { effect++ } }
        val b = DataSource<Unit, Int> { 0 }
        val c = a.andThen { string -> b.execute() }

        ice { c.execute(0) } shouldBe Ice.Content(0)
        ice { a.execute(0) } shouldBe Ice.Content("1")
        ice { b.execute() } shouldBe Ice.Content(0)
        effect shouldBe 2
    }

    archerTest("DataSource kleisli arrow") {
        val a = DataSource<Int, String> { 3.toString() }
        val b = DataSource<String, Int> { it.toInt() }
        val c = a.andThen(b)

        ice { c.execute(0) } shouldBe Ice.Content(3)
        ice { a.execute(0) } shouldBe Ice.Content("3")
        ice { b.execute("0") } shouldBe Ice.Content(0)
    }

    archerTest("Repository thunk kleisli arrow") {
        var effect = 0
        val a = DataSource<Int, String> { effect.toString().also { effect++ } }.toRepository()
        val b = DataSource<Unit, Int> { 0 }.toRepository()
        val c = a.andThen { string -> b.execute() }

        ice { c.execute(0) } shouldBe Ice.Content(0)
        ice { a.execute(0) } shouldBe Ice.Content("1")
        ice { b.execute() } shouldBe Ice.Content(0)
        effect shouldBe 2
    }

    archerTest("Repository kleisli arrow") {
        val a = DataSource<Int, String> { 3.toString() }.toRepository()
        val b = DataSource<String, Int> { it.toInt() }.toRepository()
        val c = a.andThen(b)

        ice { c.execute(0) } shouldBe Ice.Content(3)
        ice { a.execute(0) } shouldBe Ice.Content("3")
        ice { b.execute("0") } shouldBe Ice.Content(0)
    }
})
