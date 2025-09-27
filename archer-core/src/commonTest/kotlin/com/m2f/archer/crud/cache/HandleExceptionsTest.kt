package com.m2f.archer.crud.cache

import com.m2f.archer.crud.getDataSource
import com.m2f.archer.failure.Message
import com.m2f.archer.failure.NetworkFailure
import com.m2f.archer.failure.NetworkFailure.NoConnection
import com.m2f.archer.failure.Unhandled
import com.m2f.archer.utils.runArcherTest
import io.kotest.assertions.arrow.core.shouldBeLeft
import kotlin.test.Test

class HandleExceptionsTest {

    @Test
    fun `datasource should return Unhandled if the is an exception`() = runArcherTest {
        val runtimeException = RuntimeException()
        fun crash(): Nothing {
            throw runtimeException
        }

        val ds = getDataSource<Int, Int> { crash() }

        val result = either { ds.get(0) }
        result shouldBeLeft Unhandled(runtimeException)
    }

    @Test
    fun `Network error with 3 kind of messages`() = runArcherTest {
        // Simple message
        val dataSourceSimpleMessage = getDataSource<Int, String> {
            raise(NetworkFailure.NetworkError(message = Message.Simple("Network Failure")))
        }

        val result1 = either { dataSourceSimpleMessage.get(0) }
        result1 shouldBeLeft NetworkFailure.NetworkError(message = Message.Simple("Network Failure"))

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

        val result2 = either { dataSourceCodeMessage.get(0) }
        result2 shouldBeLeft NetworkFailure.NetworkError(
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

        val result3 = either { dataSourceCodeTitleMessage.get(0) }
        result3 shouldBeLeft NetworkFailure.NetworkError(
            message = Message.NetworkCodeAndTitleMessage(
                code = "123",
                title = "Error 123",
                message = "Error due to code 123"
            )
        )
    }

    @Test
    fun `no connection`() = runArcherTest {
        val dataSource = getDataSource<Int, String> { raise(NoConnection) }

        val result = either { dataSource.get(0) }
        result shouldBeLeft NoConnection
    }
}