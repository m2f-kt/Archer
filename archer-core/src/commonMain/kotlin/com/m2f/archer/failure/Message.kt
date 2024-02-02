package com.m2f.archer.failure

sealed class Message(
    open val message: String
) {

    data class Simple(
        override val message: String
    ) : Message(message)

    data class NetworkCodeMessage(
        val code: String,
        override val message: String
    ) : Message(message)

    data class NetworkCodeAndTitleMessage(
        val code: String,
        val title: String,
        override val message: String
    ) : Message(message)
}
