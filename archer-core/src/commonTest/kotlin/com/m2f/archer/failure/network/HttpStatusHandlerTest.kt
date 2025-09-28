package com.m2f.archer.failure.network

import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.failure.Invalid
import com.m2f.archer.failure.NetworkFailure
import com.m2f.archer.failure.NotModified
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class HttpStatusHandlerTest {

    @Test
    fun `304 should return NotModified`() {
        NotModified shouldBe httpCodeToNetworkFailure(304)
    }

    @Test
    fun `404 should return NotFound`() {
        DataNotFound shouldBe httpCodeToNetworkFailure(404)
    }

    @Test
    fun `422 should return Invalid`() {
        Invalid shouldBe httpCodeToNetworkFailure(422)
    }

    @Test
    fun `from 300 to 399 should return redirect failure`() {
        val testCodes = listOf(300, 301, 302, 350, 399)
        for (code in testCodes) {
            NetworkFailure.Redirect shouldBe httpCodeToNetworkFailure(code)
        }
    }

    @Test
    fun `from 400 to 499 should return Unauthorised failure`() {
        val testCodes = listOf(400, 401, 403, 450, 499)
        for (code in testCodes) {
            NetworkFailure.Unauthorised shouldBe httpCodeToNetworkFailure(code)
        }
    }

    @Test
    fun `from 500 to 599 should return Server failure`() {
        val testCodes = listOf(500, 501, 502, 503, 550, 599)
        for (code in testCodes) {
            NetworkFailure.ServerFailure shouldBe httpCodeToNetworkFailure(code)
        }
    }

    @Test
    fun `from weird code should return Unhandled failure`() {
        val testCodes = listOf(601, 700, 800, 999, 1000)
        for (code in testCodes) {
            NetworkFailure.UnhandledNetworkFailure shouldBe httpCodeToNetworkFailure(code)
        }
    }
}
