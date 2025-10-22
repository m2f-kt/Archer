---
sidebar_position: 2
---

# Concurrency in Archer

Understanding how Archer handles concurrent operations safely and efficiently across multiple platforms.

## Overview

Archer is built with concurrency as a first-class feature, leveraging **Kotlin coroutines** and **Arrow's Software Transactional Memory (STM)** to provide safe, efficient concurrent operations across all supported platforms (JVM, Android, JavaScript, Native).

### Core Concurrency Principles

1. **Suspension-based API** - All I/O operations are suspend functions
2. **Platform-aware** - Concurrency features adapt to platform capabilities
3. **STM-backed** - Critical data structures use transactional memory
4. **Composable** - Concurrency features can be added via extensions
5. **Safe by default** - Built-in protections against race conditions

## Concurrency Features

### 1. Mutex Extension

The `mutex()` extension provides mutual exclusion for DataSource operations, ensuring thread-safe access.

```kotlin
val threadSafeDataSource = myDataSource.mutex()

// Multiple concurrent calls are serialized
coroutineScope {
    launch { threadSafeDataSource.get(key1) }
    launch { threadSafeDataSource.get(key2) }
    // Only one operation executes at a time
}
```

**When to use:**
- Protecting non-thread-safe DataSources
- Ensuring atomic read-modify-write operations
- Preventing race conditions in shared state

**Implementation:**
```kotlin
fun <Q, A> DataSource<Q, A>.mutex(): DataSource<Q, A> =
    object : DataSource<Q, A> {
        val mutex by lazy { Mutex() }
        override suspend fun ArcherRaise.invoke(q: Q): A =
            mutex.withLock { this@mutex.run { invoke(q) } }
    }
```

**Location:** `archer-core/src/commonMain/kotlin/com/m2f/archer/datasource/concurrency/Mutex.kt`

### 2. Parallelism Control

The `parallelism()` extension limits the number of concurrent operations that can execute simultaneously.

```kotlin
// Limit to 3 concurrent operations
val limitedDataSource = myDataSource.parallelism(3)

coroutineScope {
    // Even with 10 launches, only 3 execute in parallel
    repeat(10) {
        launch { limitedDataSource.get(it) }
    }
}
```

**When to use:**
- Rate limiting API calls
- Controlling resource consumption
- Preventing server overload
- Managing database connection pools

**Platform Support:**

| Platform | Implementation |
|----------|----------------|
| JVM | `Dispatchers.IO.limitedParallelism(parallelism)` |
| Android | `Dispatchers.IO.limitedParallelism(parallelism)` |
| Native | `Dispatchers.IO.limitedParallelism(parallelism)` |
| JavaScript | No-op (single-threaded) |

**Implementation (JVM/Android/Native):**
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
actual fun <Q, A> DataSource<Q, A>.parallelism(parallelism: Int): DataSource<Q, A> =
    object : DataSource<Q, A> {
        val dispatcher = Dispatchers.IO.limitedParallelism(parallelism)
        override suspend fun ArcherRaise.invoke(q: Q): A =
            withContext(dispatcher) { this@parallelism.run { invoke(q) } }
    }
```

**Location:**
- Common: `archer-core/src/commonMain/kotlin/com/m2f/archer/datasource/concurrency/Mutex.kt`
- JVM: `archer-core/src/jvmMain/kotlin/com/m2f/archer/datasource/concurrency/Mutex.jvm.kt`
- Android: `archer-core/src/androidMain/kotlin/com/m2f/archer/datasource/concurrency/Mutex.android.kt`
- Native: `archer-core/src/nativeMain/kotlin/com/m2f/archer/datasource/concurrency/Mutex.native.kt`
- JS: `archer-core/src/jsMain/kotlin/com/m2f/archer/datasource/concurrency/Mutex.js.kt`

### 3. Software Transactional Memory (STM)

Archer's `InMemoryDataSource` uses Arrow's STM for lock-free, atomic operations.

```kotlin
val inMemoryCache = InMemoryDataSource<String, User>(
    initialValues = mapOf("user1" to User(...))
)

// All operations are atomic
atomically {
    inMemoryCache.put("user2", User(...))
    inMemoryCache.get("user1")
}
```

**Key Features:**
- **Atomic transactions** - All-or-nothing semantics
- **Automatic conflict detection** - Retries on conflicts
- **Composable** - Transactions can be nested
- **Lock-free** - Better performance under contention

**Implementation:**
```kotlin
actual class InMemoryDataSource<K, A> actual constructor(
    initialValues: Map<K, A>
) : CacheDataSource<K, A> {
    private val values: TMap<K, A> = runBlocking {
        TMap.new<K, A>().apply {
            atomically {
                initialValues.forEach { (key, value) ->
                    insert(key, value)
                }
            }
        }
    }

    actual override suspend fun ArcherRaise.invoke(
        q: KeyQuery<K, out A>
    ): A = atomically {
        when (q) {
            is Put -> {
                q.value?.also { values[q.key] = it }
                    ?: raise(DataNotFound)
            }
            is Get -> {
                values[q.key] ?: raise(DataNotFound)
            }
        }
    }
}
```

**Location:**
- JVM: `archer-core/src/jvmMain/kotlin/com/m2f/archer/datasource/InMemoryDataSource.jvm.kt`
- Android: `archer-core/src/androidMain/kotlin/com/m2f/archer/datasource/InMemoryDataSource.android.kt`
- Native: `archer-core/src/nativeMain/kotlin/com/m2f/archer/datasource/InMemoryDataSource.native.kt`

### 4. Protected Cache Operations

Some cache implementations (like `MemoizedExpirationCache`) use mutex protection for database operations.

```kotlin
class MemoizedExpirationCache : CacheDataSource<CacheMetaInformation, Instant> {
    private val mutex: Mutex = Mutex()

    override suspend fun ArcherRaise.invoke(
        q: KeyQuery<CacheMetaInformation, out Instant>
    ): Instant = mutex.withLock {
        // Database operations protected by mutex
        val queries = repo.get(Unit)
        queries.transactionWithResult {
            // ... atomic database transaction
        }
    }
}
```

**Location:** `archer-core/src/commonMain/kotlin/com/m2f/archer/crud/cache/memcache/MemoizedExpirationCache.kt`

## Concurrency Patterns

### Request Deduplication

Prevent duplicate concurrent requests for the same resource:

```kotlin
class RequestDeduplicator<K, V> {
    private val ongoing = ConcurrentHashMap<K, Deferred<V>>()

    suspend fun get(key: K, fetch: suspend () -> V): V {
        val deferred = ongoing.getOrPut(key) {
            coroutineScope {
                async {
                    try {
                        fetch()
                    } finally {
                        ongoing.remove(key)
                    }
                }
            }
        }
        return deferred.await()
    }
}

// Usage
val deduplicator = RequestDeduplicator<UserId, User>()
val user = deduplicator.get(userId) {
    userDataSource.get(userId)
}
```

### Parallel Composite Loading

Load multiple resources concurrently:

```kotlin
suspend fun loadUserProfile(userId: UserId): UserProfile = coroutineScope {
    val user = async { userRepo.get(userId) }
    val posts = async { postsRepo.get(userId) }
    val friends = async { friendsRepo.get(userId) }

    UserProfile(
        user = user.await(),
        posts = posts.await(),
        friends = friends.await()
    )
}
```

### Request Batching

Batch multiple requests into a single operation:

```kotlin
class RequestBatcher<K, V>(
    private val batchSize: Int = 10,
    private val batchDelay: Duration = 100.milliseconds
) {
    private val channel = Channel<Pair<K, CompletableDeferred<V>>>()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val batch = mutableListOf<Pair<K, CompletableDeferred<V>>>()

            while (true) {
                select {
                    channel.onReceive { item ->
                        batch.add(item)
                        if (batch.size >= batchSize) {
                            processBatch(batch)
                        }
                    }
                    onTimeout(batchDelay.inWholeMilliseconds) {
                        if (batch.isNotEmpty()) {
                            processBatch(batch)
                        }
                    }
                }
            }
        }
    }

    suspend fun get(key: K): V {
        val deferred = CompletableDeferred<V>()
        channel.send(key to deferred)
        return deferred.await()
    }
}
```

### Polling with Flow

Periodic data updates using Kotlin Flow:

```kotlin
fun pollForUpdates(interval: Duration): Flow<Data> = flow {
    while (true) {
        val data = dataSource.get()
        emit(data)
        delay(interval)
    }
}

// Usage with StateFlow
class ViewModel {
    private val _state = MutableStateFlow<Data?>(null)
    val state: StateFlow<Data?> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            pollForUpdates(5.seconds).collect { data ->
                _state.value = data
            }
        }
    }
}
```

### Background Prefetching

Prefetch data before it's needed:

```kotlin
class PrefetchingRepository<K, V>(
    private val dataSource: DataSource<K, V>
) {
    private val prefetchScope = CoroutineScope(Dispatchers.IO)

    fun prefetch(key: K) {
        prefetchScope.launch {
            try {
                dataSource.get(key)
            } catch (e: Exception) {
                // Handle prefetch failures silently
            }
        }
    }

    suspend fun get(key: K): V = dataSource.get(key)
}

// Usage
val repo = PrefetchingRepository(userDataSource)
repo.prefetch(nextUserId) // Non-blocking
val currentUser = repo.get(currentUserId) // Might hit cache
```

### Search with Debouncing

Debounce rapid user input:

```kotlin
class SearchViewModel : ViewModel() {
    private val searchQuery = MutableStateFlow("")

    val searchResults: StateFlow<List<Result>> = searchQuery
        .debounce(300.milliseconds)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                flow {
                    val results = searchRepository.search(query)
                    emit(results)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }
}
```

## Platform Differences

### JVM/Android/Native

Full concurrency support with:
- Thread-safe `Mutex` implementation
- `Dispatchers.IO.limitedParallelism()` for parallelism control
- Arrow STM for `InMemoryDataSource`
- Multi-threaded coroutine execution

### JavaScript

Limited concurrency due to single-threaded nature:
- `mutex()` - Implemented but effectively no-op
- `parallelism()` - No-op (always single-threaded)
- `InMemoryDataSource` - Uses simple `MutableMap` (no STM needed)
- Coroutines still work for async operations (via event loop)

## Testing Concurrency

Archer includes comprehensive concurrency tests:

```kotlin
@Test
fun testMutexConcurrency() = runArcherTest {
    var counter = 0
    val dataSource = getDataSource<Unit, Unit> {
        val current = counter
        delay(1.milliseconds) // Simulate race condition
        counter = current + 1
    }.mutex()

    // 100 coroutines × 1000 operations = 100,000 operations
    massiveRun(n = 100, k = 1000) {
        dataSource.get(Unit)
    }

    assertEquals(100_000, counter) // All increments succeeded
}
```

**Test Location:** `archer-core/src/commonTest/kotlin/com/m2f/archer/crud/cache/ConcurrencyTest.kt`

## Dependencies

Archer's concurrency features are built on:

```kotlin
// gradle/libs.versions.toml
kotlinx-coroutines = "1.10.2"
arrow = "2.1.2"

// Libraries
kotlinx-coroutines-core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2"
fx-stm = "io.arrow-kt:arrow-fx-stm:2.1.2"
arrow-fx = "io.arrow-kt:arrow-fx-coroutines:2.1.2"
```

## Best Practices

### 1. Choose the Right Tool

- **Mutex** - For protecting non-thread-safe operations
- **Parallelism** - For rate limiting and resource management
- **STM** - For complex atomic operations on shared state
- **Channels** - For producer-consumer patterns
- **Flow** - For reactive streams and UI state

### 2. Avoid Blocking

```kotlin
// ❌ Bad - blocks thread
runBlocking {
    dataSource.get(key)
}

// ✅ Good - suspends coroutine
suspend fun fetchData(key: Key) {
    dataSource.get(key)
}
```

### 3. Use Structured Concurrency

```kotlin
// ✅ All children complete or cancelled together
suspend fun loadData() = coroutineScope {
    val user = async { userRepo.get(userId) }
    val posts = async { postsRepo.get(userId) }

    UserData(user.await(), posts.await())
}
```

### 4. Handle Cancellation

```kotlin
suspend fun longRunningOperation() = coroutineScope {
    try {
        // Work that might be cancelled
        val data = dataSource.get(key)
        process(data)
    } catch (e: CancellationException) {
        // Clean up resources
        cleanup()
        throw e // Re-throw to propagate cancellation
    }
}
```

### 5. Lazy Initialization

```kotlin
// ✅ Create mutex only when needed
class DataSourceWrapper<Q, A>(private val ds: DataSource<Q, A>) {
    private val mutex by lazy { Mutex() }

    suspend fun getSafe(q: Q): A = mutex.withLock {
        ds.get(q)
    }
}
```

## Performance Considerations

### STM vs Mutex

**STM (InMemoryDataSource):**
- ✅ Lock-free, better under high contention
- ✅ Composable transactions
- ❌ Higher overhead for simple operations
- ❌ Not available on JavaScript

**Mutex:**
- ✅ Lower overhead for simple operations
- ✅ Works on all platforms
- ❌ Can cause thread blocking
- ❌ Not composable

### Parallelism Tuning

```kotlin
// Too low - underutilizes resources
dataSource.parallelism(1)

// Too high - may overwhelm resources
dataSource.parallelism(1000)

// Good - matches resource capacity
dataSource.parallelism(10) // e.g., database connection pool size
```

## Architecture Summary

```
┌─────────────────────────────────────────────────────┐
│              Suspend Functions                      │
│         (All DataSource/Repository ops)             │
└─────────────────────────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
    ┌────▼────┐    ┌────▼────┐    ┌────▼────┐
    │  Mutex  │    │   STM   │    │Parallel │
    │Extension│    │(TMap/   │    │  ism    │
    │         │    │ TVar)   │    │Extension│
    └─────────┘    └─────────┘    └─────────┘
         │               │               │
         └───────────────┼───────────────┘
                         │
              ┌──────────▼──────────┐
              │  Kotlin Coroutines  │
              │  Dispatchers.IO     │
              └─────────────────────┘
```

## Next Steps

- [DataSources](/docs/usage/datasources) - Learn about DataSource types
- [Repositories](/docs/usage/repositories) - Understand repository strategies
- [Examples: Recipes](/docs/examples/recipes) - More concurrency patterns
- [Result Types](/docs/usage/result-types) - Error handling in async code

## References

- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Arrow FX STM](https://arrow-kt.io/docs/fx/stm/)
- [Structured Concurrency](https://kotlinlang.org/docs/coroutines-basics.html#structured-concurrency)
