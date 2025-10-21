---
sidebar_position: 2
---

# Repositories

Repositories in Archer combine multiple DataSources to implement data access patterns like caching, validation, and synchronization.

## Creating Repositories

### Basic Repository

The simplest repository wraps a single DataSource:

```kotlin
val remoteDataSource = getDataSource<UserId, User> { userId ->
    api.getUser(userId.value).toDomain()
}

val repository = remoteDataSource.toRepository()
```

### Repository with Caching

Use the `cacheWith` operator to add caching:

```kotlin
val remoteDataSource = getDataSource<UserId, User> { userId ->
    api.getUser(userId.value).toDomain()
}

val cache: StoreDataSource<UserId, User> = InMemoryDataSource()

val repository = remoteDataSource cacheWith cache
```

### Repository with Cache Expiration

Add time-based expiration to your cache:

```kotlin
import kotlin.time.Duration.Companion.minutes

val repository = remoteDataSource cacheWith cache expiresIn 5.minutes
```

## Repository Strategies

### StoreSync Strategies

Control how data is synchronized between remote and local sources:

#### StoreSync.StoreFirst

Always try the local cache first:

```kotlin
val user = repository.get(StoreSync.StoreFirst, userId)
// 1. Check cache
// 2. If not found or expired, fetch from remote
// 3. Update cache
// 4. Return data
```

#### StoreSync.NetworkFirst

Always fetch from network first:

```kotlin
val user = repository.get(StoreSync.NetworkFirst, userId)
// 1. Fetch from remote
// 2. Update cache
// 3. Return data
```

#### StoreSync.NetworkOnly

Skip cache entirely:

```kotlin
val user = repository.get(StoreSync.NetworkOnly, userId)
// 1. Fetch from remote
// 2. Return data (cache not updated)
```

#### StoreSync.StoreOnly

Use only the cache:

```kotlin
val user = repository.get(StoreSync.StoreOnly, userId)
// 1. Fetch from cache
// 2. Return data (or raise error if not found)
```

### Complete Example

```kotlin
data class User(val id: Int, val name: String, val email: String)

// Remote data source
val apiDataSource = getDataSource<Int, User> { userId ->
    val response = httpClient.get("https://api.example.com/users/$userId")
    response.toDomain()
}

// Local storage
class UserDatabaseDataSource(
    private val database: Database
) : StoreDataSource<Int, User> {

    override suspend fun get(key: Int): User {
        return database.userDao().getUser(key)?.toDomain()
            ?: raise(UserNotFound(key))
    }

    override suspend fun put(key: Int, value: User) {
        database.userDao().insertUser(value.toEntity())
    }
}

// Create repository with 5-minute cache
val userRepository = apiDataSource
    .cacheWith(UserDatabaseDataSource(database))
    .expiresIn(5.minutes)

// Use the repository
suspend fun getUser(userId: Int): User = ice {
    userRepository.get(StoreSync.StoreFirst, userId)
}.getOrNull() ?: throw UserNotFoundException()
```

## Advanced Patterns

### Validation

Add validation to your repository:

```kotlin
val validatedRepository = repository.validate { user ->
    when {
        user.name.isBlank() -> raise(InvalidUserError("Name is required"))
        user.email.isBlank() -> raise(InvalidUserError("Email is required"))
        !user.email.contains("@") -> raise(InvalidUserError("Invalid email"))
    }
}
```

### Mapping

Transform repository data:

```kotlin
// Repository returns User, but we need UserProfile
val profileRepository = userRepository.map { user ->
    UserProfile(
        displayName = user.name,
        avatarUrl = user.avatarUrl,
        memberSince = user.createdAt
    )
}
```

### Multiple DataSources

Combine multiple remote sources:

```kotlin
val primaryApi = getDataSource<Int, User> { id ->
    primaryClient.getUser(id).toDomain()
}

val fallbackApi = getDataSource<Int, User> { id ->
    fallbackClient.getUser(id).toDomain()
}

// Try primary, fall back to secondary
val repository = primaryApi
    .recover { error ->
        if (error is NetworkError) {
            fallbackApi.get(error.userId)
        } else {
            raise(error)
        }
    }
    .cacheWith(cache)
    .expiresIn(10.minutes)
```

## Delete Operations

If your StoreDataSource implements `DeleteDataSource`, you can delete entries:

```kotlin
class UserRepository(
    private val store: UserDatabaseDataSource
) : DeleteDataSource<UserId> by store {

    suspend fun removeUser(userId: UserId) {
        delete(userId)
    }
}
```

## Testing Repositories

Repositories are easy to test:

```kotlin
class FakeUserDataSource : StoreDataSource<Int, User> {
    private val users = mutableMapOf<Int, User>()

    override suspend fun get(key: Int): User {
        return users[key] ?: raise(UserNotFound(key))
    }

    override suspend fun put(key: Int, value: User) {
        users[key] = value
    }
}

@Test
fun `repository caches data correctly`() = runTest {
    val fake = FakeUserDataSource()
    val repository = remoteDataSource cacheWith fake

    // First call hits remote
    val user1 = repository.get(StoreSync.StoreFirst, 1)

    // Second call hits cache
    val user2 = repository.get(StoreSync.StoreOnly, 1)

    assertEquals(user1, user2)
}
```

## Best Practices

### 1. Choose the Right Strategy

```kotlin
// User profile: Cache-first (rarely changes)
val profileRepo = api cacheWith cache expiresIn 1.hours
val profile = profileRepo.get(StoreSync.StoreFirst, userId)

// Real-time data: Network-first (always fresh)
val liveDataRepo = api cacheWith cache expiresIn 30.seconds
val liveData = liveDataRepo.get(StoreSync.NetworkFirst, dataId)

// Configuration: Store-only (loaded once)
val configRepo = api cacheWith cache expiresIn 24.hours
val config = configRepo.get(StoreSync.StoreOnly, "config")
```

### 2. Handle Errors Appropriately

```kotlin
val user = ice {
    repository.get(StoreSync.StoreFirst, userId)
}.fold(
    ifIdle = { /* Handle idle state */ },
    ifContent = { user -> /* Use user */ },
    ifError = { error ->
        when (error) {
            is NetworkError -> /* Show offline message */
            is UserNotFound -> /* Show not found message */
            else -> /* Show generic error */
        }
    }
)
```

### 3. Configure Cache Expiration Based on Data Type

```kotlin
// Frequently changing data
val newsRepo = api cacheWith cache expiresIn 5.minutes

// Stable reference data
val countriesRepo = api cacheWith cache expiresIn 7.days

// User-specific data
val userPrefsRepo = api cacheWith cache expiresIn 1.hours
```

## Next Steps

- [Result Types](/docs/usage/result-types) - Learn about Ice, Either, and Nullable
- [Examples](/docs/examples/basic-usage) - See complete examples
- [Recipes](/docs/examples/recipes) - Common patterns and solutions
