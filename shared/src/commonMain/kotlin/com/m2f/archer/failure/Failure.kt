package com.m2f.archer.failure

sealed interface Failure

/**Data is no longer valid*/
data object Invalid : Failure

/** Data can't be found */
data object DataNotFound : Failure

/** Data is empty */
data object DataEmpty : Failure

sealed interface NetworkFailure : Failure

/**No connection*/
data object NoConnection : NetworkFailure

/**Server Error*/
data object ServerFailure : NetworkFailure

data object Redirect : NetworkFailure

data object Unauthorised : NetworkFailure

/**Network Error with an optional message*/
data class NetworkError(val message: Message? = null) : NetworkFailure

data object UnhandledNetworkFailure : NetworkFailure

data class Unknown(val exception: Throwable) : Failure
