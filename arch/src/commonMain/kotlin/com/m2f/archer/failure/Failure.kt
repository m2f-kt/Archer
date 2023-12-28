package com.m2f.archer.failure

sealed interface Failure

/**Data is no longer valid*/
data object Invalid : Failure

/** Data can't be found */
data object DataNotFound : Failure

/** Data is empty */
data object DataEmpty : Failure

data object Unhandled : Failure
