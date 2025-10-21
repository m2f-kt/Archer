---
sidebar_position: 2
---

# Recipes

Common patterns and solutions for everyday problems.

## Offline-First Architecture

### Problem
You want your app to work offline and sync when online.

### Solution

```kotlin
class OfflineFirstRepository<K, V>(
    private val remoteDataSource: GetDataSource<K, V>,
    private val localDataSource: StoreDataSource<K, V>
) {
    suspend fun get(key: K): Either<DomainError, V> = either {
        // Try local first
        ice {
            localDataSource.get(key)
        }.recover { localError ->
            // Local failed, try remote
            ice {
                val value = remoteDataSource.get(key)
                // Save to local for next time
                localDataSource.put(key, value)
                value
            }.getOrElse { remoteError ->
                // Both failed
                raise(remoteError)
            }
        }.bind()
    }

    suspend fun refresh(key: K): Either<DomainError, V> = either {
        val value = remoteDataSource.get(key)
        localDataSource.put(key, value)
        value
    }
}
```

## Retry Logic

### Problem
Network requests fail temporarily and should be retried.

### Solution

```kotlin
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

suspend fun <T> retryWithBackoff(
    maxAttempts: Int = 3,
    initialDelay: Long = 1000,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(maxAttempts - 1) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong()
        }
    }
    return block() // Last attempt
}

val resilientDataSource = getDataSource<Int, User> { userId ->
    retryWithBackoff(maxAttempts = 3) {
        ice {
            apiClient.getUser(userId).toDomain()
        }.bind()
    }
}
```

## Request Deduplication

### Problem
Multiple concurrent requests for the same data should be deduplicated.

### Solution

```kotlin
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DeduplicatingDataSource<K, V>(
    private val delegate: GetDataSource<K, V>
) : GetDataSource<K, V> {
    private val mutexes = mutableMapOf<K, Mutex>()
    private val cache = mutableMapOf<K, V>()

    override suspend fun get(key: K): V {
        val mutex = synchronized(mutexes) {
            mutexes.getOrPut(key) { Mutex() }
        }

        return mutex.withLock {
            cache[key] ?: delegate.get(key).also { cache[key] = it }
        }
    }
}
```

## Authentication Token Management

### Problem
API requests need authentication tokens that expire and need refresh.

### Solution

```kotlin
sealed interface AuthError : DomainError {
    data object TokenExpired : AuthError
    data object Unauthorized : AuthError
}

class AuthenticatedDataSource<K, V>(
    private val delegate: GetDataSource<K, V>,
    private val tokenProvider: suspend () -> String,
    private val refreshToken: suspend () -> String
) : GetDataSource<K, V> {

    private var currentToken: String? = null

    override suspend fun get(key: K): V {
        val token = currentToken ?: tokenProvider().also { currentToken = it }

        return ice {
            makeRequest(key, token)
        }.recover { error ->
            when (error) {
                is AuthError.TokenExpired -> {
                    // Refresh token and retry
                    val newToken = refreshToken()
                    currentToken = newToken
                    makeRequest(key, newToken)
                }
                else -> raise(error)
            }
        }.bind()
    }

    private suspend fun makeRequest(key: K, token: String): V {
        // Add token to request and delegate
        return delegate.get(key)
    }
}
```

## Cache Invalidation

### Problem
Cache needs to be invalidated when data changes.

### Solution

```kotlin
class InvalidatableCache<K, V> : StoreDataSource<K, V> {
    private val cache = mutableMapOf<K, V>()
    private val timestamps = mutableMapOf<K, Long>()

    override suspend fun get(key: K): V {
        return cache[key] ?: raise(DataNotFound)
    }

    override suspend fun put(key: K, value: V) {
        cache[key] = value
        timestamps[key] = System.currentTimeMillis()
    }

    fun invalidate(key: K) {
        cache.remove(key)
        timestamps.remove(key)
    }

    fun invalidateAll() {
        cache.clear()
        timestamps.clear()
    }

    fun invalidateOlderThan(duration: kotlin.time.Duration) {
        val cutoff = System.currentTimeMillis() - duration.inWholeMilliseconds
        timestamps.filterValues { it < cutoff }
            .keys
            .forEach { key ->
                cache.remove(key)
                timestamps.remove(key)
            }
    }
}

// Usage
val cache = InvalidatableCache<Int, User>()

val repository = remoteDataSource
    .cacheWith(cache)
    .expiresIn(5.minutes)

// Invalidate when user updates
suspend fun updateUser(user: User) {
    api.updateUser(user)
    cache.invalidate(user.id)
}
```

## Search with Debouncing

### Problem
Search queries should be debounced to avoid excessive API calls.

### Solution

```kotlin
import kotlinx.coroutines.flow.*

class SearchRepository(
    private val searchDataSource: GetDataSource<String, List<User>>
) {
    fun search(queries: Flow<String>): Flow<Ice<DomainError, List<User>>> =
        queries
            .debounce(300) // Wait 300ms after typing stops
            .distinctUntilChanged()
            .filter { it.length >= 3 } // Minimum 3 characters
            .map { query ->
                ice {
                    searchDataSource.get(query)
                }
            }
}

// Usage in ViewModel
class SearchViewModel(
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<Ice<DomainError, List<User>>> =
        searchRepository.search(_searchQuery)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                Ice.Idle
            )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }
}
```

## Composite Data Loading

### Problem
Load related data from multiple sources and combine them.

### Solution

```kotlin
data class UserProfile(
    val user: User,
    val posts: List<Post>,
    val followers: List<User>
)

suspend fun loadUserProfile(userId: Int): Either<DomainError, UserProfile> = either {
    coroutineScope {
        // Load in parallel
        val userDeferred = async {
            ice { userRepository.get(StoreSync.StoreFirst, userId) }
        }

        val postsDeferred = async {
            ice { postsRepository.get(StoreSync.StoreFirst, userId) }
        }

        val followersDeferred = async {
            ice { followersRepository.get(StoreSync.StoreFirst, userId) }
        }

        // Combine results
        UserProfile(
            user = userDeferred.await().bind(),
            posts = postsDeferred.await().bind(),
            followers = followersDeferred.await().bind()
        )
    }
}
```

## Optimistic Updates

### Problem
Update UI immediately while syncing in background.

### Solution

```kotlin
class OptimisticUpdateRepository<K, V>(
    private val remoteDataSource: GetDataSource<K, V>,
    private val localDataSource: StoreDataSource<K, V>
) {
    suspend fun update(key: K, update: (V) -> V): Either<DomainError, V> = either {
        // Get current value
        val current = localDataSource.get(key)

        // Apply update locally (optimistic)
        val updated = update(current)
        localDataSource.put(key, updated)

        // Try to sync with remote
        ice {
            remoteDataSource.put(key, updated)
        }.recover { error ->
            // Rollback on failure
            localDataSource.put(key, current)
            raise(error)
        }.bind()

        updated
    }
}

// Usage
suspend fun toggleUserFollow(userId: Int): Either<DomainError, User> =
    optimisticRepo.update(userId) { user ->
        user.copy(isFollowing = !user.isFollowing)
    }
```

## Conditional Caching

### Problem
Cache some items but not others based on criteria.

### Solution

```kotlin
class ConditionalCache<K, V>(
    private val delegate: StoreDataSource<K, V>,
    private val shouldCache: (K, V) -> Boolean
) : StoreDataSource<K, V> {

    override suspend fun get(key: K): V {
        return delegate.get(key)
    }

    override suspend fun put(key: K, value: V) {
        if (shouldCache(key, value)) {
            delegate.put(key, value)
        }
    }
}

// Usage: Don't cache error states or temporary data
val cache = ConditionalCache(
    delegate = InMemoryDataSource(),
    shouldCache = { key, user ->
        user.isActive && !user.isTemporary
    }
)
```

## Request Batching

### Problem
Batch multiple requests into a single API call.

### Solution

```kotlin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay

class BatchingDataSource<K, V>(
    private val batchFetcher: suspend (Set<K>) -> Map<K, V>,
    private val batchWindow: Long = 50 // ms
) : GetDataSource<K, V> {

    private val pending = Channel<Pair<K, CompletableDeferred<V>>>(Channel.UNLIMITED)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            processBatches()
        }
    }

    override suspend fun get(key: K): V {
        val deferred = CompletableDeferred<V>()
        pending.send(key to deferred)
        return deferred.await()
    }

    private suspend fun processBatches() {
        while (true) {
            val batch = mutableMapOf<K, CompletableDeferred<V>>()

            // Collect requests for batch window
            val first = pending.receive()
            batch[first.first] = first.second

            delay(batchWindow)

            // Drain any additional requests
            while (!pending.isEmpty) {
                val (key, deferred) = pending.receive()
                batch[key] = deferred
            }

            // Execute batch
            val results = batchFetcher(batch.keys)

            // Complete all requests
            batch.forEach { (key, deferred) ->
                results[key]?.let { deferred.complete(it) }
                    ?: deferred.completeExceptionally(Exception("Key not found: $key"))
            }
        }
    }
}
```

## Polling for Updates

### Problem
Periodically check for updates from the server.

### Solution

```kotlin
fun <T> pollingFlow(
    interval: Duration,
    fetcher: suspend () -> T
): Flow<T> = flow {
    while (true) {
        emit(fetcher())
        delay(interval)
    }
}

// Usage
class NotificationsViewModel(
    private val notificationsRepo: Repository<Unit, List<Notification>>
) : ViewModel() {

    val notifications: StateFlow<Ice<DomainError, List<Notification>>> =
        pollingFlow(interval = 30.seconds) {
            ice {
                notificationsRepo.get(StoreSync.NetworkFirst, Unit)
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            Ice.Idle
        )
}
```

## Prefetching

### Problem
Preload data that will likely be needed soon.

### Solution

```kotlin
class PrefetchingRepository<K, V>(
    private val repository: Repository<K, V>,
    private val prefetchStrategy: (K) -> List<K>
) {
    private val prefetchScope = CoroutineScope(Dispatchers.IO)

    suspend fun get(key: K): V = ice {
        // Get the requested item
        val result = repository.get(StoreSync.StoreFirst, key)

        // Prefetch related items in background
        prefetchScope.launch {
            prefetchStrategy(key).forEach { prefetchKey ->
                ice {
                    repository.get(StoreSync.StoreFirst, prefetchKey)
                }
            }
        }

        result
    }.bind()
}

// Usage: Prefetch next items in a list
val prefetchingRepo = PrefetchingRepository(
    repository = userRepository,
    prefetchStrategy = { currentId ->
        // Prefetch next 5 users
        (currentId + 1..currentId + 5).toList()
    }
)
```

## Next Steps

- [DataSources](/docs/usage/datasources) - Learn more about data sources
- [Repositories](/docs/usage/repositories) - Deep dive into repositories
- [Result Types](/docs/usage/result-types) - Master error handling
