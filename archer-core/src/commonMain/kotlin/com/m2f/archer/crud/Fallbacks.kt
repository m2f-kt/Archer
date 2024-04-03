package com.m2f.archer.crud

import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.failure.Failure
import com.m2f.archer.failure.Invalid
import com.m2f.archer.failure.NetworkFailure.NoConnection
import com.m2f.archer.failure.NetworkFailure.Redirect
import com.m2f.archer.failure.NetworkFailure.ServerFailure
import com.m2f.archer.failure.NetworkFailure.UnhandledNetworkFailure

/**
 * Failures that will be used for network calls to fallback into storage
 */
val mainFallbacks: List<Failure> = listOf(
    DataNotFound,
    Invalid,
    NoConnection,
    ServerFailure,
    Redirect,
    UnhandledNetworkFailure
)

/**
 * Failures that will be used for storage calls to fallback into network
 */
val storageFallbacks: List<Failure> = listOf(
    DataNotFound,
    Invalid
)
