package com.m2f.archer.crud

import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.failure.Failure
import com.m2f.archer.failure.Invalid

/**
 * Failures that will be used for network calls to fallback into storage
 */
val mainFallbacks: List<Failure> = listOf(
    DataNotFound,
    Invalid
)

/**
 * Failures that will be used for storage calls to fallback into network
 */
val storageFallbacks: List<Failure> = listOf(
    DataNotFound,
    Invalid
)
