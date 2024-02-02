package com.m2f.archer.crud.cache

import com.m2f.archer.crud.get
import com.m2f.archer.crud.getDataSource
import com.m2f.archer.failure.Message
import com.m2f.archer.failure.NetworkFailure
import com.m2f.archer.failure.NetworkFailure.NoConnection
import com.m2f.archer.failure.Unhandled
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.core.spec.style.FunSpec

class HandleExceptionsTest : FunSpec({

    test("datasource should eturn Unhandled if the is an exception") {

        val runtimeException = RuntimeException()
        fun crash(): Nothing {
            throw runtimeException
        }

        val ds = getDataSource<Int, Int> { crash() }

        ds.get(0) shouldBeLeft Unhandled(runtimeException)
    }

    test("Network error with 3 kind of messages") {

        // Simple message
        val dataSourceSimpleMessage = getDataSource<Int, String> {
            raise(NetworkFailure.NetworkError(message = Message.Simple("Network Failure")))
        }

        dataSourceSimpleMessage.get(0) shouldBeLeft NetworkFailure.NetworkError(
            message = Message.Simple("Network Failure")
        )

        val dataSourceCodeMessage = getDataSource<Int, String> {
            raise(
                NetworkFailure.NetworkError(
                    message = Message.NetworkCodeMessage(
                        code = "123",
                        message = "Error due to code 123"
                    )
                )
            )
        }

        dataSourceCodeMessage.get(0) shouldBeLeft NetworkFailure.NetworkError(
            message = Message.NetworkCodeMessage(
                code = "123", message = "Error due to code 123"
            )
        )

        val dataSourceCodeTitleMessage = getDataSource<Int, String> {
            raise(
                NetworkFailure.NetworkError(
                    message = Message.NetworkCodeAndTitleMessage(
                        code = "123",
                        title = "Error 123",
                        message = "Error due to code 123"
                    )
                )
            )
        }

        dataSourceCodeTitleMessage.get(0) shouldBeLeft NetworkFailure.NetworkError(
            message = Message.NetworkCodeAndTitleMessage(
                code = "123",
                title = "Error 123",
                message = "Error due to code 123"
            )
        )
    }

    test("no connection") {

        val dataSource = getDataSource<Int, String> { raise(NoConnection) }

        dataSource.get(0) shouldBeLeft NoConnection
    }
})
