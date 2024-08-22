package com.m2f.archer.datasource

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable

class StringToSerializableBijectionKtTest : FunSpec({

    @Serializable
    data class Serial(val value: String)

    test("bijection should work") {
        val bijection = stringToSerializableBijection<Serial>()
        val serial = Serial("test")
        val string = bijection.to(serial)
        val deserialized = bijection.from(string)
        deserialized shouldBe serial
    }

    test("bijection should work with unknown keys") {
        val bijection = stringToSerializableBijection<Serial>()
        val serial = Serial("test")
        val string = """{"value":"test","unknown":"key"}"""
        val deserialized = bijection.from(string)
        deserialized shouldBe serial
    }
})
