---
sidebar_position: 1
---

# DataSources

DataSources are the foundation of Archer. They represent a source of data with a simple contract.

## Types of DataSources

### GetDataSource

A `GetDataSource` is the simplest type - it retrieves data based on a parameter.

```kotlin
interface GetDataSource<in K, out V> {
    suspend fun get(key: K): V
}
```

#### Creating a GetDataSource

Use the DSL to create a GetDataSource:

```kotlin
val remoteDataSource = getDataSource<Int, String> { param: Int ->
    // Your implementation
    api.fetchData(param)
}
```

**Example: API Data Source**

```kotlin
data class User(val id: Int, val name: String)

val userApiDataSource = getDataSource<Int, User> { userId ->
    // Call your API
    val response = apiClient.getUser(userId)
    // Map to domain model
    response.toDomain()
}
```

### StoreDataSource

A `StoreDataSource` extends GetDataSource with the ability to store data.

```kotlin
interface StoreDataSource<K, V> : GetDataSource<K, V> {
    suspend fun get(key: K): V
    suspend fun put(key: K, value: V)
}
```

#### Built-in Store DataSources

**InMemoryDataSource**

Simple in-memory cache:

```kotlin
val cache: StoreDataSource<String, User> = InMemoryDataSource()
```

**Custom Store DataSource**

Implement your own storage:

```kotlin
class DatabaseDataSource : StoreDataSource<UserId, User> {
    override suspend fun get(key: UserId): User {
        return database.userDao().getUser(key.value)
            ?: raise(UserNotFound(key))
    }

    override suspend fun put(key: UserId, value: User) {
        database.userDao().insertUser(value.toEntity())
    }
}
```

### DeleteDataSource

For data sources that support deletion:

```kotlin
interface DeleteDataSource<in K> {
    suspend fun delete(key: K)
}
```

**Example:**

```kotlin
class UserStorageDataSource :
    StoreDataSource<UserId, User>,
    DeleteDataSource<UserId> {

    override suspend fun get(key: UserId): User {
        // Fetch from storage
    }

    override suspend fun put(key: UserId, value: User) {
        // Store in storage
    }

    override suspend fun delete(key: UserId) {
        database.userDao().deleteUser(key.value)
    }
}
```

## Data Source Composition

DataSources can be composed to create more complex behaviors.

### Mapping

Transform data from one type to another:

```kotlin
// API returns UserDto, but we want User domain model
val apiDataSource = getDataSource<Int, UserDto> { id ->
    api.getUser(id)
}

val domainDataSource = apiDataSource.map { dto ->
    dto.toDomain()
}
```

### Validation

Add validation to your data sources:

```kotlin
val validatedDataSource = dataSource.validate { user ->
    if (user.name.isBlank()) {
        raise(InvalidUserData("Name cannot be blank"))
    }
}
```

## Error Handling

DataSources use Arrow's `raise` mechanism for error handling:

```kotlin
val dataSource = getDataSource<UserId, User> { userId ->
    val response = api.getUser(userId.value)

    when {
        response.isSuccessful -> response.body()!!.toDomain()
        response.code() == 404 -> raise(UserNotFound(userId))
        else -> raise(NetworkError(response.message()))
    }
}
```

## Best Practices

### 1. Keep DataSources Simple

DataSources should focus on data retrieval/storage, not business logic:

```kotlin
// Good: Simple data fetching
val userDataSource = getDataSource<UserId, User> { userId ->
    api.getUser(userId.value).toDomain()
}

// Bad: Business logic in DataSource
val userDataSource = getDataSource<UserId, User> { userId ->
    val user = api.getUser(userId.value).toDomain()
    if (user.isPremium) {
        // Don't put business logic here!
        loadPremiumFeatures()
    }
    user
}
```

### 2. Handle Mapping at the DataSource Level

```kotlin
// Map API models to domain models in the DataSource
val dataSource = getDataSource<Int, User> { id ->
    apiClient.getUser(id).toDomain()  // Mapping here
}
```

### 3. Use Typed Errors

```kotlin
sealed interface UserError : DomainError {
    data class NotFound(val userId: UserId) : UserError
    data class InvalidData(val reason: String) : UserError
    data class NetworkError(val cause: Throwable) : UserError
}

val dataSource = getDataSource<UserId, User> { userId ->
    try {
        api.getUser(userId.value).toDomain()
    } catch (e: Exception) {
        raise(UserError.NetworkError(e))
    }
}
```

## Next Steps

- [Repositories](/docs/usage/repositories) - Combine DataSources into repositories
- [Result Types](/docs/usage/result-types) - Handle results with Ice, Either, or Nullable
- [Examples](/docs/examples/basic-usage) - See practical examples
