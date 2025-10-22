---
sidebar_position: 4
---

# Configuration

Archer provides a flexible configuration system through `Configuration` and `Settings` that controls caching behavior, error fallback strategies, and DSL execution context.

## Overview

The configuration system consists of two main components:

- **`Settings`** - Interface defining behavior settings (fallback strategies, cache management)
- **`Configuration`** - Extends Settings and provides DSL functions and repository strategies

## Default Configuration

The simplest way to use Archer is with the default configuration:

```kotlin
import com.m2f.archer.configuration.Configuration

// Default configuration with standard settings
val config = Configuration.Default
```

### Default Settings

The default configuration includes:

```kotlin
// Failures that trigger fallback from main to store
mainFallbacks = { failure ->
    failure is DataNotFound ||
    failure is Invalid ||
    failure is NotModified ||
    failure is NoConnection ||
    failure is ServerFailure ||
    failure is Redirect ||
    failure is UnhandledNetworkFailure
}

// Failures that trigger fallback from store to main
storageFallbacks = { failure ->
    failure is DataNotFound ||
    failure is Invalid
}

// Cache is enabled by default
ignoreCache = false
```

## Cache Implementation and Testing

### Understanding the Default Cache

The default configuration relies on `MemoizedExpirationCache`, which is a **platform-sensitive cache implementation** that uses databases under the hood to keep records of expiration times and store metadata. This cache implementation:

- Uses SQLDelight to persist cache expiration metadata across application sessions
- Requires database drivers that are platform-specific (Android, iOS, JVM, etc.)
- Stores cache metadata including keys, expiration times, and creation timestamps
- Provides thread-safe access through mutex locks

```kotlin
// Default cache implementation (from Settings.Default)
override val cache: CacheDataSource<CacheMetaInformation, Instant> by lazy {
    MemoizedExpirationCache()
}
```

The `MemoizedExpirationCache` implementation can be found in `archer-core/src/commonMain/kotlin/com/m2f/archer/crud/cache/memcache/MemoizedExpirationCache.kt:22` and requires a database connection to function properly.

### Testing Considerations

When writing tests, the default `MemoizedExpirationCache` can create several challenges:

1. **Database Setup Complexity** - Requires setting up database drivers and schemas for testing
2. **Platform Dependencies** - May not work consistently across all test environments
3. **Test Isolation** - Shared database state between tests can cause failures
4. **Performance** - Database operations can slow down unit tests

**For testing, it is strongly recommended to create a custom testing configuration** that uses an in-memory cache instead of the database-backed implementation.

### Testing Configuration Examples

Archer provides testing configurations that demonstrate how to create your own custom configurations for tests. These examples are located in `archer-core/src/commonTest/kotlin/com/m2f/archer/crud/cache/configuration/Configuration.kt:15`.

#### Simple In-Memory Testing Configuration

The simplest approach is to replace the cache with an `InMemoryDataSource`:

```kotlin
import com.m2f.archer.configuration.Settings
import com.m2f.archer.datasource.InMemoryDataSource
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlin.time.Instant

// Simple in-memory cache configuration for testing
val inMemoryCacheConfiguration: (scheduler: TestCoroutineScheduler) -> Settings = { scheduler ->
    object : Settings by Settings.Default {
        // Replace database cache with in-memory implementation
        override val cache = InMemoryDataSource()

        // Use test scheduler for time control
        override fun getCurrentTime(): Instant =
            Instant.fromEpochMilliseconds(scheduler.currentTime)
    }
}

// Usage in tests - use scoping functions
@Test
fun testUserRepository() = runTest {
    val testSettings = inMemoryCacheConfiguration(testScheduler)

    // Use settings.configuration extension property and with() for scoping
    with(testSettings.configuration) {
        ice {
            // Your test code here
            userRepository.get(MainSync, userId)
        }
    }
}
```

This configuration:
- Uses `InMemoryDataSource` which stores data in memory without any database
- Delegates all other settings to `Settings.Default`
- Provides time control through the test scheduler for testing cache expiration

#### Testing Configuration with Fake Database

If you need to test database-specific cache behavior, you can use a fake database. This is useful when testing the actual `MemoizedExpirationCache` implementation with database persistence in an isolated test environment.

First, create a fake database repository for your tests:

```kotlin
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.m2f.archer.ExpirationRegistryQueries
import com.m2f.archer.crud.GetRepository
import com.m2f.archer.crud.cache.CacheExpiration.Never
import com.m2f.archer.crud.cache.cache
import com.m2f.archer.crud.getDataSource
import com.m2f.archer.crud.operation.StoreSync
import com.m2f.archer.sqldelight.CacheExpirationDatabase

// Platform-specific fake database driver factory
// (JVM example shown - use appropriate driver for your platform)
object FakeDatabaseDriverFactory {
    suspend fun createDriver(schema: SqlSchema<*>): SqlDriver =
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
            schema.create(it)
        }
}

// Create a fake queries repository for testing
val fakeQueriesRepo: GetRepository<Unit, ExpirationRegistryQueries>
    get() = getDataSource<Unit, ExpirationRegistryQueries> {
        CacheExpirationDatabase(
            FakeDatabaseDriverFactory.createDriver(CacheExpirationDatabase.Schema)
        ).expirationRegistryQueries
    }.cache(expiration = Never).create(StoreSync)
```

Then use it in your test configuration:

```kotlin
import com.m2f.archer.crud.cache.memcache.MemoizedExpirationCache

// Configuration with fake database for testing MemoizedExpirationCache
val testConfiguration: (scheduler: TestCoroutineScheduler) -> Settings = { scheduler ->
    object : Settings by Settings.Default {
        // Use MemoizedExpirationCache with a fake database
        override val cache = MemoizedExpirationCache(
            repo = fakeQueriesRepo  // Fake in-memory database repository
        )

        override fun getCurrentTime(): Instant =
            Instant.fromEpochMilliseconds(scheduler.currentTime)
    }
}

// Usage in tests
@Test
fun testCacheWithDatabase() = runTest {
    val testSettings = testConfiguration(testScheduler)

    with(testSettings.configuration) {
        ice {
            // This uses MemoizedExpirationCache backed by in-memory SQLite
            userRepository.get(MainSync, userId)
        }
    }
}
```

**Note:** For most tests, the simpler `InMemoryDataSource` approach is preferred. Only use a fake database when you specifically need to test database-backed cache behavior.

### Creating Your Own Testing Configuration

To create your own testing configuration:

1. **Choose your cache implementation:**
   - `InMemoryDataSource()` - Simple, fast, no persistence (recommended for most tests)
   - `MemoizedExpirationCache(repo = fakeQueriesRepo)` - Database-backed for integration tests

2. **Control time for expiration testing:**
   ```kotlin
   override fun getCurrentTime(): Instant =
       Instant.fromEpochMilliseconds(testScheduler.currentTime)
   ```

3. **Customize fallback behavior if needed:**
   ```kotlin
   override val mainFallbacks = { _: Failure -> false }  // No fallbacks in tests
   override val storageFallbacks = { _: Failure -> false }
   ```

### Example: Complete Test Configuration

Here's a complete example of a custom test configuration:

```kotlin
import com.m2f.archer.configuration.Settings
import com.m2f.archer.configuration.configuration
import com.m2f.archer.datasource.InMemoryDataSource
import com.m2f.archer.failure.Failure
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlin.time.Instant

object TestConfiguration {
    fun create(scheduler: TestCoroutineScheduler): Settings =
        object : Settings {
            // Use in-memory cache for fast, isolated tests
            override val cache = InMemoryDataSource()

            // Keep default fallbacks - they're usually correct
            // Only override if you need specific test behavior
            override val mainFallbacks = Settings.Default.mainFallbacks
            override val storageFallbacks = Settings.Default.storageFallbacks

            // Cache enabled for expiration testing
            override val ignoreCache = false

            // Controlled time for deterministic cache expiration
            override fun getCurrentTime(): Instant =
                Instant.fromEpochMilliseconds(scheduler.currentTime)
        }
}

// Usage in tests - use settings.configuration and scoping
@Test
fun testCacheExpiration() = runTest {
    val testSettings = TestConfiguration.create(testScheduler)

    // Use with() for scoping - configuration created at the top of the chain
    with(testSettings.configuration) {
        ice {
            // Test cache behavior with controlled time
            val result = repository.get(MainSync, userId)

            // Advance time to test expiration
            testScheduler.advanceTimeBy(6.minutes)

            // Cache should be expired now
            val refreshed = repository.get(MainSync, userId)
        }
    }
}
```

### Best Practices for Testing

1. **Always use in-memory cache for unit tests** - Faster and more reliable than database-backed cache
2. **Use test scheduler** - Enables controlled time advancement for cache expiration testing
3. **Keep default fallbacks** - They handle common error scenarios correctly. Only override for specific test needs
4. **Create reusable test configurations** - Define once, use across all tests
5. **Use `Configuration.ignoreCache()`** - When you want to bypass cache entirely in specific tests
6. **Use scoping functions** - Always use `with(settings.configuration) { ice { ... } }` pattern instead of creating Configuration instances everywhere

## Operations and Fallback Strategies

Understanding operations and how fallbacks work with them is crucial to using Archer effectively. This section explains the relationship between operations, repository strategies, and fallback configurations.

### The Four Operations

Archer provides four operation types that control how data flows between your main data source (typically a remote API) and your store (typically local cache/database):

```kotlin
import com.m2f.archer.crud.operation.Main
import com.m2f.archer.crud.operation.Store
import com.m2f.archer.crud.operation.MainSync
import com.m2f.archer.crud.operation.StoreSync
```

#### Main

Fetches data **only from the main data source** (usually remote API). No caching, no fallbacks.

```kotlin
with(Configuration.Default) {
    ice {
        // Only calls the API, doesn't touch the cache
        userRepository.get(Main, userId)
    }
}
```

**Use when:**
- You need guaranteed fresh data
- You're posting/updating data to a server
- Cache should be bypassed completely

**Behavior:**
- Calls main data source
- Returns result or raises failure
- No fallback to store
- Does not update cache

#### Store

Fetches data **only from the store** (local cache/database). No network calls, no fallbacks.

```kotlin
with(Configuration.Default) {
    ice {
        // Only reads from cache, never calls the API
        userRepository.get(Store, userId)
    }
}
```

**Use when:**
- You want offline-first behavior
- You know data is already cached
- You want to display cached data while loading fresh data separately

**Behavior:**
- Calls store data source
- Returns cached result or raises failure (e.g., DataNotFound)
- No fallback to main
- No network calls

#### MainSync

The **most commonly used operation**. Tries main first, then falls back to store if configured fallback conditions are met, and **syncs successful main responses to the store**.

```kotlin
with(Configuration.Default) {
    ice {
        // Tries API first, falls back to cache on network errors
        userRepository.get(MainSync, userId)
    }
}
```

**Use when:**
- You want fresh data with offline fallback
- This is the default for most use cases
- You want automatic cache updates on successful fetches

**Behavior:**
1. Calls main data source
2. On success: Writes response to store, returns data
3. On failure: Checks `mainFallbacks` function
   - If `mainFallbacks(failure)` returns `true`: Falls back to store
   - If `mainFallbacks(failure)` returns `false`: Raises the failure

**Implementation** (from `archer-core/src/commonMain/kotlin/com/m2f/archer/repository/MainSyncRepository.kt:18`):

```kotlin
// Simplified version showing the logic
override suspend fun ArcherRaise.invoke(q: Get<K>): A =
    archerRecover(
        block = {
            // Try to get from main and store it
            storeDataSource.put(q.key, mainDataSource.get(q.key))
        },
        recover = { failure ->
            if (fallbackChecks(failure)) {
                // Fallback to store if configured
                storeDataSource.get(q.key)
            } else {
                raise(failure)
            }
        }
    )
```

#### StoreSync

Tries store first, then falls back to main if configured fallback conditions are met. When falling back to main, it **automatically syncs the data back to store** (by calling MainSync internally).

```kotlin
with(Configuration.Default) {
    ice {
        // Tries cache first, falls back to API if not found
        userRepository.get(StoreSync, userId)
    }
}
```

**Use when:**
- You want offline-first with automatic refresh
- You want to minimize network calls
- Cache-first is acceptable for your use case

**Behavior:**
1. Calls store data source
2. On success: Returns cached data
3. On failure: Checks `storageFallbacks` function
   - If `storageFallbacks(failure)` returns `true`: Falls back to MainSync (which fetches from main and updates store)
   - If `storageFallbacks(failure)` returns `false`: Raises the failure

**Implementation** (from `archer-core/src/commonMain/kotlin/com/m2f/archer/repository/StoreSyncRepository.kt:19`):

```kotlin
// Simplified version showing the logic
override suspend fun ArcherRaise.invoke(q: Get<K>): A =
    archerRecover(
        block = {
            storeDataSource.get(q.key)
        },
        recover = { failure ->
            if (fallbackChecks(failure)) {
                // Fallback to MainSync, which updates the store
                MainSyncRepository(mainDataSource, storeDataSource, mainFallbackChecks).get(q.key)
            } else {
                raise(failure)
            }
        }
    )
```

### Understanding Fallbacks

Fallbacks control **when to switch from one data source to another** when an operation fails. Archer provides two fallback functions in `Settings`:

#### mainFallbacks

Controls when **MainSync** falls back from main to store.

```kotlin
// Default configuration (from Settings.Default)
override val mainFallbacks = { failure: Failure ->
    failure is DataNotFound ||
    failure is Invalid ||
    failure is NotModified ||
    failure is NoConnection ||
    failure is ServerFailure ||
    failure is Redirect ||
    failure is UnhandledNetworkFailure
}
```

**This means:** When using `MainSync`, if the API call fails with any network-related error, Archer will automatically try to return cached data instead.

**Example:**
```kotlin
with(Configuration.Default) {
    ice {
        // If API fails with NoConnection, automatically returns cached data
        userRepository.get(MainSync, userId)
    }
}
```

#### storageFallbacks

Controls when **StoreSync** falls back from store to main.

```kotlin
// Default configuration (from Settings.Default)
override val storageFallbacks = { failure: Failure ->
    failure is DataNotFound ||
    failure is Invalid
}
```

**This means:** When using `StoreSync`, if the cache is empty (DataNotFound) or expired (Invalid), Archer will automatically fetch from the API and update the cache.

**Example:**
```kotlin
with(Configuration.Default) {
    ice {
        // If cache is empty, automatically fetches from API and caches it
        userRepository.get(StoreSync, userId)
    }
}
```

### When Operations Use Fallbacks

This table shows which operations use which fallback functions:

| Operation | Uses mainFallbacks | Uses storageFallbacks | Syncs to Store |
|-----------|-------------------|----------------------|----------------|
| `Main` | ❌ No | ❌ No | ❌ No |
| `Store` | ❌ No | ❌ No | ❌ No |
| `MainSync` | ✅ Yes | ❌ No | ✅ Yes (on success) |
| `StoreSync` | ✅ Yes (via MainSync) | ✅ Yes | ✅ Yes (when falling back) |

### How Repository Strategies Work with Operations

When you create a repository using `cacheStrategy`, Archer creates different repository implementations based on the operation:

```kotlin
// From Configuration.kt:28
fun <K, A> cacheStrategy(
    mainDataSource: GetDataSource<K, A>,
    storeDataSource: StoreDataSource<K, A>,
): GetRepositoryStrategy<K, A> = GetRepositoryStrategy { operation ->
    when (operation) {
        is Main -> mainDataSource.toRepository()
        is Store -> storeDataSource.toRepository()
        is MainSync -> MainSyncRepository(mainDataSource, storeDataSource, mainFallbacks)
        is StoreSync -> StoreSyncRepository(storeDataSource, mainDataSource, storageFallbacks, mainFallbacks)
    }
}
```

**This means:** The same repository can behave differently based on which operation you pass:

```kotlin
val userRepository = with(Configuration.Default) {
    apiDataSource.cacheWith(dbDataSource).expiresIn(5.minutes)
}

with(Configuration.Default) {
    ice {
        // Four different behaviors with the same repository
        val fresh = userRepository.get(Main, userId)        // API only
        val cached = userRepository.get(Store, userId)      // Cache only
        val freshWithFallback = userRepository.get(MainSync, userId)  // API → Cache fallback
        val cacheFirst = userRepository.get(StoreSync, userId)        // Cache → API fallback
    }
}
```

### Why You Should Rarely Override Fallbacks

The default fallback configurations are designed to handle the most common scenarios correctly:

**✅ Default `mainFallbacks` handles:**
- Network errors (no connection, server failures)
- Empty responses (data not found)
- Invalid/expired cache markers
- HTTP redirects

**✅ Default `storageFallbacks` handles:**
- Cache miss (data not found)
- Expired cache (invalid)

**These defaults work correctly for ~95% of use cases.** Only override fallbacks when you have specific requirements:

```kotlin
// ❌ Usually NOT needed - defaults are fine
object StrictSettings : Settings by Settings.Default {
    override val mainFallbacks = { _: Failure -> false }  // Never fall back
    override val storageFallbacks = { _: Failure -> false }
}

// ✅ Rare valid case - custom error handling
object CustomSettings : Settings by Settings.Default {
    override val mainFallbacks = { failure: Failure ->
        // Only fall back on network errors, not on 404s
        failure is NoConnection || failure is ServerFailure
    }
}
```

### Complete Example: Operations in Practice

Here's a complete example showing how to use operations effectively:

```kotlin
import com.m2f.archer.configuration.Configuration
import com.m2f.archer.crud.operation.*
import kotlin.time.Duration.Companion.minutes

// Setup: Create repository at the top level
val userRepository = with(Configuration.Default) {
    apiDataSource.cacheWith(dbDataSource).expiresIn(5.minutes)
}

// Use different operations for different scenarios
class UserViewModel {
    // Load user with offline support
    suspend fun loadUser(userId: Int): Ice<User> = with(Configuration.Default) {
        ice {
            // MainSync: Fresh data with cache fallback
            userRepository.get(MainSync, userId)
        }
    }

    // Force refresh (pull-to-refresh)
    suspend fun refreshUser(userId: Int): Ice<User> = with(Configuration.Default) {
        ice {
            // Main: Always fetch fresh, update cache on success via repository
            userRepository.get(Main, userId)
        }
    }

    // Show cached data while loading
    suspend fun getCachedUser(userId: Int): User? = with(Configuration.Default) {
        nullable {
            // Store: Return cached data immediately, or null
            userRepository.get(Store, userId)
        }
    }

    // Offline-first approach
    suspend fun getUser(userId: Int): Ice<User> = with(Configuration.Default) {
        ice {
            // StoreSync: Try cache first, fetch from API if missing
            userRepository.get(StoreSync, userId)
        }
    }
}
```

### Flow: MainSync in Detail

Let's trace what happens with a `MainSync` operation:

```kotlin
with(Configuration.Default) {
    ice {
        userRepository.get(MainSync, userId = 123)
    }
}
```

**Scenario 1: API succeeds**
1. Call `apiDataSource.get(123)` → Returns `User("Alice")`
2. Call `dbDataSource.put(123, User("Alice"))` → Caches the user
3. Return `User("Alice")`

**Scenario 2: API fails with network error, cache has data**
1. Call `apiDataSource.get(123)` → Raises `NoConnection`
2. Check `mainFallbacks(NoConnection)` → Returns `true`
3. Call `dbDataSource.get(123)` → Returns cached `User("Alice")`
4. Return cached `User("Alice")`

**Scenario 3: API fails with network error, no cache**
1. Call `apiDataSource.get(123)` → Raises `NoConnection`
2. Check `mainFallbacks(NoConnection)` → Returns `true`
3. Call `dbDataSource.get(123)` → Raises `DataNotFound`
4. Return original error `NoConnection` (not `DataNotFound`)

### Flow: StoreSync in Detail

```kotlin
with(Configuration.Default) {
    ice {
        userRepository.get(StoreSync, userId = 123)
    }
}
```

**Scenario 1: Cache has valid data**
1. Call `dbDataSource.get(123)` → Returns cached `User("Alice")`
2. Return `User("Alice")` (no API call!)

**Scenario 2: Cache is empty**
1. Call `dbDataSource.get(123)` → Raises `DataNotFound`
2. Check `storageFallbacks(DataNotFound)` → Returns `true`
3. Fall back to `MainSync`:
   - Call `apiDataSource.get(123)` → Returns `User("Alice")`
   - Call `dbDataSource.put(123, User("Alice"))` → Caches it
4. Return `User("Alice")`

**Scenario 3: Cache is expired (Invalid)**
1. Call `dbDataSource.get(123)` → Raises `Invalid` (cache expired)
2. Check `storageFallbacks(Invalid)` → Returns `true`
3. Fall back to `MainSync`:
   - Call `apiDataSource.get(123)` → Returns `User("Alice")`
   - Call `dbDataSource.put(123, User("Alice"))` → Updates cache
4. Return `User("Alice")`

### Visual Summary

```
┌─────────────────────────────────────────────────────────────┐
│                        Configuration                        │
│  - mainFallbacks: (Failure) -> Boolean                      │
│  - storageFallbacks: (Failure) -> Boolean                   │
│  - cache: CacheDataSource                                   │
└─────────────────────────────────────────────────────────────┘
                               │
                               │ creates
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                   Repository Strategy                       │
│  Based on Operation, returns appropriate repository         │
└─────────────────────────────────────────────────────────────┘
                               │
          ┌────────────────────┼────────────────────┐
          │                    │                    │
          ▼                    ▼                    ▼
     ┌────────┐          ┌──────────┐        ┌──────────┐
     │  Main  │          │MainSync  │        │StoreSync │
     │  Store │          │          │        │          │
     └────────┘          └──────────┘        └──────────┘
          │                    │                    │
      No fallback       Uses mainFallbacks   Uses storageFallbacks
      Single source                          then mainFallbacks
```

### Key Takeaways

1. **Use `MainSync` for most cases** - Fresh data with offline fallback is usually what you want
2. **Use `Main` for critical fresh data** - When you must have up-to-date information
3. **Use `Store` for offline mode** - When you know data is cached or want to fail fast
4. **Use `StoreSync` for offline-first** - When minimizing network calls is important
5. **Keep default fallbacks** - They handle common error scenarios correctly
6. **Operations are passed at call-time** - Same repository, different behavior based on operation
7. **Fallbacks only apply to sync operations** - `Main` and `Store` never use fallbacks

## How DSL Builders Work with Configuration

All DSL builders in Archer (`ice`, `either`, `nullable`, `bool`, `unit`, etc.) can work with configurations in multiple ways. Understanding this is key to using Archer effectively.

### Three Ways to Call DSL Builders

Every DSL builder can be called in three different ways:

```kotlin
// 1. Standalone with default configuration
ice {
    userRepository.get(Main, userId)
}

// 2. Standalone with explicit configuration
ice(Configuration.Default) {
    userRepository.get(Main, userId)
}

// 3. Inside a Configuration scope
with(Configuration.Default) {
    ice {
        userRepository.get(Main, userId)
    }
}
```

**Important differences:**

- **Method 1 & 2** call the top-level DSL function from `ArcherRaise.kt`
- **Method 3** calls the DSL function as a member of `Configuration`, implicitly using the scoped configuration
- All three produce the same result when using `Configuration.Default`, but allow different configuration strategies

### ArcherRaise Context and Configuration Preservation

Every DSL builder provides an `ArcherRaise` context that preserves the parent configuration. The `ArcherRaise` class extends `Configuration`, giving you access to all configuration members within the DSL block.

```kotlin
// The outer ice uses Configuration.Default
ice(Configuration.Default) {
    // Inside here, 'this' is ArcherRaise which extends Configuration
    // You have access to all Configuration members

    // You can call repository methods
    userRepository.get(Main, userId)

    // You can also create nested DSL blocks
    val anotherResult = ice {
        // This inherits the parent configuration
        anotherRepository.get(Main, anotherId)
    }
}
```

### Nested Configuration Scoping

You can override the configuration for specific operations by scoping to a different configuration:

```kotlin
// Outer block uses default configuration
ice(Configuration.Default) {
    val user = userRepository.get(Main, userId)

    // Override configuration for a specific operation
    with(Configuration.ignoreCache()) {
        archerRecover(
            block = {
                // This operation bypasses cache
                freshDataRepository.get(Main, dataId)
            },
            recover = { failure ->
                raise(failure)
            }
        )
    }

    // Back to default configuration
    user
}
```

### Custom Configuration Example

```kotlin
// Define a custom configuration
object StrictConfiguration : Settings {
    override val mainFallbacks = { _: Failure -> false }
    override val storageFallbacks = { _: Failure -> false }
    override val ignoreCache = false
    override val cache = MemoizedExpirationCache()
    override fun getCurrentTime() = Clock.System.now()
}

val strictConfig = Configuration(StrictConfiguration)

// Use it in different ways
suspend fun getUser(id: Int): Ice<User> {
    // Method 1: Pass configuration explicitly
    return ice(strictConfig) {
        userRepository.get(Main, id)
    }
}

suspend fun getUser2(id: Int): Ice<User> = with(strictConfig) {
    // Method 2: Use configuration scope
    ice {
        userRepository.get(Main, id)
    }
}

suspend fun getUser3(id: Int): Ice<User> = ice {
    // Method 3: Default config, but override for specific operation
    with(strictConfig) {
        archerRecover(
            block = { userRepository.get(Main, id) },
            recover = { failure -> raise(failure) }
        )
    }
}
```

## DSL Functions

Configuration provides multiple DSL functions for error handling with different return types:

### ice

Returns `Ice<A>` representing three states: Idle, Content, or Error.

```kotlin
suspend fun getUser(id: Int): Ice<User> = ice {
    userRepository.get(Main, id)
}

// Handling the result
when (val result = getUser(1)) {
    is Ice.Idle -> println("Loading...")
    is Ice.Content -> println("User: ${result.value}")
    is Ice.Error -> println("Error: ${result.error}")
}
```

### either / result

Returns `Either<Failure, A>` (Result is an alias for Either).

```kotlin
suspend fun getUser(id: Int): Either<Failure, User> = either {
    userRepository.get(Main, id)
}

// Handling the result
getUser(1).fold(
    ifLeft = { failure -> println("Failed: $failure") },
    ifRight = { user -> println("Success: $user") }
)
```

### nullable / nil

Returns `A?` - the value or null on failure.

```kotlin
suspend fun getUser(id: Int): User? = nullable {
    userRepository.get(Main, id)
}

val user = getUser(1)
if (user != null) {
    println("Got user: $user")
}
```

### bool

Returns `Boolean` - true on success, false on failure.

```kotlin
suspend fun hasUser(id: Int): Boolean = bool {
    userRepository.get(Main, id)
    // If successful, returns true
}

if (hasUser(1)) {
    println("User exists")
}
```

### unit

Returns `Unit` - executes the block and discards the result.

```kotlin
suspend fun deleteUser(id: Int): Unit = unit {
    userRepository.delete(id)
    // Failures are swallowed, always returns Unit
}
```

## Using archerRecover

`archerRecover` is a special function that requires a `Settings` context to work. Since `ArcherRaise` (the context inside DSL blocks) extends `Configuration`, and `Configuration` implements `Settings`, `archerRecover` is available within any DSL block.

### Basic Usage

Within any DSL block, you can use `archerRecover` to handle failures explicitly:

```kotlin
ice {
    // Inside ice block, 'this' is ArcherRaise which is a Settings
    archerRecover(
        block = {
            // Try to get user from repository
            userRepository.get(Main, userId)
        },
        recover = { failure ->
            // Handle specific failures
            when (failure) {
                is DataNotFound -> getDefaultUser()
                is NetworkFailure -> getCachedUser()
                else -> raise(failure)
            }
        }
    )
}

// With exception handling
ice {
    archerRecover(
        block = {
            userRepository.get(Main, userId)
        },
        recover = { failure ->
            // Handle raised failures
            raise(failure)
        },
        catch = { exception ->
            // Handle thrown exceptions
            raise(Failure.Unhandled(exception))
        }
    )
}
```

### Configuration Scoping with archerRecover

You can change the configuration for a specific `archerRecover` call by scoping to a different `Settings`:

```kotlin
ice(Configuration.Default) {
    val user = userRepository.get(Main, userId)

    // Use a different configuration for this specific operation
    with(Configuration.ignoreCache()) {
        archerRecover(
            block = {
                // This bypasses cache due to the ignoreCache configuration
                freshRepository.get(Main, dataId)
            },
            recover = { failure ->
                raise(failure)
            }
        )
    }

    user
}
```

**Important:** `archerRecover` preserves the configuration from its `Settings` context, which means fallback strategies and cache behavior are determined by the active configuration when `archerRecover` is called.

## Custom Configuration

### Ignoring Cache

Create a configuration that bypasses the cache:

```kotlin
val noCacheConfig = Configuration.ignoreCache()

// Use in a specific operation
noCacheConfig.ice {
    userRepository.get(Main, userId)
}
```

### Custom Settings

Implement custom `Settings` for advanced control (though this is rarely needed):

```kotlin
object CustomSettings : Settings {
    // Only retry on network failures
    override val mainFallbacks = { failure: Failure ->
        failure is NetworkFailure
    }

    // Never fallback from storage
    override val storageFallbacks = { _: Failure -> false }

    override val ignoreCache = false

    override val cache = MemoizedExpirationCache()

    override fun getCurrentTime() = Clock.System.now()
}

// Use custom configuration with scoping
with(CustomSettings.configuration) {
    ice {
        userRepository.get(MainSync, userId)
    }
}
```

## Repository Configuration

Configuration provides helper functions for creating repository strategies:

### cacheStrategy

Create a strategy with custom main and store data sources:

```kotlin
// Create strategy once at the top level
val strategy = with(Configuration.Default) {
    cacheStrategy(
        mainDataSource = apiDataSource,
        storeDataSource = dbDataSource
    )
}

// Use with different operations
with(Configuration.Default) {
    ice {
        strategy.get(Main, userId)      // Main only
        strategy.get(Store, userId)     // Store only
        strategy.get(MainSync, userId)  // Main → Store
        strategy.get(StoreSync, userId) // Store → Main
    }
}
```

### fallbackWith

Create a MainSync repository (main with fallback to store):

```kotlin
// Create repository once at the top level
val repository = with(Configuration.Default) {
    apiDataSource fallbackWith dbDataSource
}

// Use in your application code
with(Configuration.Default) {
    ice {
        repository.get(userId)
    }
}
```

### expiresIn / expires

Add cache expiration to a repository strategy:

```kotlin
import kotlin.time.Duration.Companion.minutes
import com.m2f.archer.crud.cache.CacheExpiration

// Create strategies with expiration at the top level
val strategy = with(Configuration.Default) {
    // Expire after duration
    apiDataSource
        .cacheWith(dbDataSource)
        .expiresIn(5.minutes)
}

val strategy2 = with(Configuration.Default) {
    // Custom expiration
    apiDataSource
        .cacheWith(dbDataSource)
        .expires(CacheExpiration.After(10.minutes))
}

val strategy3 = with(Configuration.Default) {
    // Never expire
    apiDataSource
        .cacheWith(dbDataSource)
        .expires(CacheExpiration.Never)
}
```

## Settings Properties

### mainFallbacks

Function determining when to fallback from main data source to store:

```kotlin
val mainFallbacks: (Failure) -> Boolean
```

Returns `true` if the failure should trigger a fallback to the store.

### storageFallbacks

Function determining when to fallback from store to main data source:

```kotlin
val storageFallbacks: (Failure) -> Boolean
```

Returns `true` if the failure should trigger a fallback to main.

### ignoreCache

Boolean flag to bypass cache reads/writes:

```kotlin
val ignoreCache: Boolean
```

When `true`, all operations skip the cache.

### cache

The cache implementation for storing expiration metadata:

```kotlin
val cache: CacheDataSource<CacheMetaInformation, Instant>
```

Default implementation is `MemoizedExpirationCache`, which is platform-sensitive and uses databases to persist cache metadata. See [Cache Implementation and Testing](#cache-implementation-and-testing) for details on the default cache and how to configure alternative caches for testing.

### getCurrentTime

Function providing the current time for cache expiration checks:

```kotlin
fun getCurrentTime(): Instant
```

Default uses `Clock.System.now()`.

## Best Practices

1. **Use Default Configuration** - Start with `Configuration.Default` for most cases
   ```kotlin
   // Simplest form - uses default configuration
   ice { userRepository.get(Main, userId) }
   ```

2. **DSL Functions** - Choose the right DSL based on your return type needs:
   - UI states → `ice`
   - Error handling → `either`
   - Optional values → `nullable`
   - Boolean checks → `bool`

3. **Configuration Scoping** - Understand the three ways to call DSL builders:
   ```kotlin
   // Method 1: Standalone with default (most common)
   ice { /* ... */ }

   // Method 2: Explicit configuration
   ice(customConfig) { /* ... */ }

   // Method 3: Configuration scope
   with(customConfig) { ice { /* ... */ } }
   ```

4. **ArcherRaise Context** - Remember that DSL blocks provide an `ArcherRaise` context that preserves configuration:
   ```kotlin
   ice(Configuration.Default) {
       // 'this' is ArcherRaise, which preserves the configuration
       val result = ice { /* inherits parent config */ }
   }
   ```

5. **Nested Configuration Override** - Use `with()` to override configuration for specific operations:
   ```kotlin
   ice {
       // Default configuration
       with(Configuration.ignoreCache()) {
           // Override just for this operation
           archerRecover(...)
       }
   }
   ```

6. **Custom Settings** - Only create custom settings when you need specific fallback behavior

7. **Cache Expiration** - Always set expiration times for cached data

8. **archerRecover** - Use for explicit failure handling within DSL blocks, and remember it inherits the active `Settings` context

9. **Testing Configurations** - Always use custom testing configurations with `InMemoryDataSource` instead of the default `MemoizedExpirationCache` to avoid database dependencies and improve test performance. See [Cache Implementation and Testing](#cache-implementation-and-testing) for examples

10. **Create Configuration at the Top** - Use `settings.configuration` to create Configuration from Settings, and create it once at the top of your call chain rather than creating Configuration instances throughout your code

## See Also

- [Basic Usage](/docs/examples/basic-usage) - Examples using configuration
- [Result Types](/docs/usage/result-types) - Understanding Ice, Either, and other result types
- [Repositories](/docs/usage/repositories) - Repository strategies and operations
