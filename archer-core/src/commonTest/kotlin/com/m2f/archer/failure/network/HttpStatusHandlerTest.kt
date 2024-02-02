package com.m2f.archer.failure.network

import com.m2f.archer.failure.NetworkFailure
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

class HttpStatusHandlerTest : FunSpec({

    test("from 300 to 399 should return redirect failure") {
        Arb.int(300..399).checkAll {
            httpCodeToNetworkFailure(it) shouldBe NetworkFailure.Redirect
        }
    }

    test("from 400 to 499 should return Unauthorised failure") {
        Arb.int(400..499).checkAll {
            httpCodeToNetworkFailure(it) shouldBe NetworkFailure.Unauthorised
        }
    }

    test("from 500 to 599 should return Server failure") {
        Arb.int(500..599).checkAll {
            httpCodeToNetworkFailure(it) shouldBe NetworkFailure.ServerFailure
        }
    }

    test("from weird code should return Unhandled failure") {
        Arb.int(601..1000).checkAll {
            httpCodeToNetworkFailure(it) shouldBe NetworkFailure.UnhandledNetworkFailure
        }
    }
})
