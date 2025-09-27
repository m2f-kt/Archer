package com.m2f.archer.datasource

import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlin.test.Test

class StringToSerializableBijectionKtTest {

    @Serializable
    data class Serial(val value: String)

    @Test
    fun `bijection should work`() {
        val bijection = stringToSerializableBijection<Serial>()
        val serial = Serial("test")
        val string = bijection.to(serial)
        val deserialized = bijection.from(string)
        deserialized shouldBe serial
    }

    @Test
    fun `bijection should work with unknown keys`() {
        val bijection = stringToSerializableBijection<Serial>()
        val serial = Serial("test")
        val string = """{"value":"test","unknown":"key"}"""
        val deserialized = bijection.from(string)
        deserialized shouldBe serial
    }
}
