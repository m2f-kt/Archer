package com.m2f.archer.crud.operation

sealed interface Operation

data object MainOperation : Operation
data object StoreOperation : Operation
data object MainSyncOperation : Operation
data object StoreSyncOperation : Operation
