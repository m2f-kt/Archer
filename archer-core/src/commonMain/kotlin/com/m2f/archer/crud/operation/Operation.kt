package com.m2f.archer.crud.operation

sealed interface Operation

data object Main : Operation
data object Store : Operation
data object MainSync : Operation
data object StoreSync : Operation
