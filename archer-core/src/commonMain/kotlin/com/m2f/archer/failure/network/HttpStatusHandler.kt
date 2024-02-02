package com.m2f.archer.failure.network

import com.m2f.archer.failure.NetworkFailure

@Suppress("MagicNumber")
fun httpCodeToNetworkFailure(httpCode: Int): NetworkFailure {
    return when (httpCode) {
        in 300..399 -> NetworkFailure.Redirect
        in 400..499 -> NetworkFailure.Unauthorised
        in 500..599 -> NetworkFailure.ServerFailure
        else -> NetworkFailure.UnhandledNetworkFailure
    }
}
