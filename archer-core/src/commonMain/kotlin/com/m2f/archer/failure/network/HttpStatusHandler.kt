package com.m2f.archer.failure.network

import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.failure.Failure
import com.m2f.archer.failure.Invalid
import com.m2f.archer.failure.NetworkFailure
import com.m2f.archer.failure.NotModified

@Suppress("MagicNumber")
fun httpCodeToNetworkFailure(httpCode: Int): Failure {
    return when (httpCode) {
        304 -> NotModified
        404 -> DataNotFound
        422 -> Invalid
        in 300..399 -> NetworkFailure.Redirect
        in 400..499 -> NetworkFailure.Unauthorised
        in 500..599 -> NetworkFailure.ServerFailure
        else -> NetworkFailure.UnhandledNetworkFailure
    }
}
