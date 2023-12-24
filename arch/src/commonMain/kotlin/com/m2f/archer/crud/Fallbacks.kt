package com.m2f.archer.crud

import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.failure.Failure
import com.m2f.archer.failure.Invalid
import com.m2f.archer.failure.NoConnection
import com.m2f.archer.failure.ServerFailure

/**
 * Failures that will be used for network calls to fallback into storage
 */
val mainAiraloFallbacks: List<Failure> = listOf(
    NoConnection,
    ServerFailure,
    DataNotFound,
    Invalid
)

/**
 * Failures that will be used for storage calls to fallback into network
 */
val storageAiraloFallbacks: List<Failure> = listOf(
    DataNotFound,
    Invalid
)
