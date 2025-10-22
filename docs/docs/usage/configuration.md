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

// Usage in tests
@Test
fun testUserRepository() = runTest {
    val testSettings = inMemoryCacheConfiguration(testScheduler)
    val config = Configuration(testSettings)

    config.ice {
        // Your test code here
        userRepository.get(MainSync, userId)
    }
}
```

This configuration:
- Uses `InMemoryDataSource` which stores data in memory without any database
- Delegates all other settings to `Settings.Default`
- Provides time control through the test scheduler for testing cache expiration

#### Testing Configuration with Fake Database

If you need to test database-specific cache behavior, you can use a fake database:

```kotlin
import com.m2f.archer.crud.cache.memcache.MemoizedExpirationCache

// Configuration with fake database for testing MemoizedExpirationCache
val testConfiguration: (scheduler: TestCoroutineScheduler) -> Settings = { scheduler ->
    object : Settings by Settings.Default {
        // Use MemoizedExpirationCache with a fake database
        override val cache = MemoizedExpirationCache(
            repo = fakeQueriesRepo  // Fake database repository
        )

        override fun getCurrentTime(): Instant =
            Instant.fromEpochMilliseconds(scheduler.currentTime)
    }
}
```

This approach is useful when you want to test the actual cache behavior with database persistence, but in an isolated test environment.

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
import com.m2f.archer.configuration.Configuration
import com.m2f.archer.configuration.Settings
import com.m2f.archer.datasource.InMemoryDataSource
import com.m2f.archer.failure.Failure
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlin.time.Instant

object TestConfiguration {
    fun create(scheduler: TestCoroutineScheduler): Configuration {
        val testSettings = object : Settings {
            // Use in-memory cache for fast, isolated tests
            override val cache = InMemoryDataSource()

            // No automatic fallbacks in tests for predictable behavior
            override val mainFallbacks = { _: Failure -> false }
            override val storageFallbacks = { _: Failure -> false }

            // Cache enabled for expiration testing
            override val ignoreCache = false

            // Controlled time for deterministic cache expiration
            override fun getCurrentTime(): Instant =
                Instant.fromEpochMilliseconds(scheduler.currentTime)
        }

        return Configuration(testSettings)
    }
}

// Usage in tests
@Test
fun testCacheExpiration() = runTest {
    val config = TestConfiguration.create(testScheduler)

    config.ice {
        // Test cache behavior with controlled time
        val result = repository.get(MainSync, userId)

        // Advance time to test expiration
        testScheduler.advanceTimeBy(6.minutes)

        // Cache should be expired now
        val refreshed = repository.get(MainSync, userId)
    }
}
```

### Best Practices for Testing

1. **Always use in-memory cache for unit tests** - Faster and more reliable than database-backed cache
2. **Use test scheduler** - Enables controlled time advancement for cache expiration testing
3. **Disable automatic fallbacks in tests** - Makes test behavior more predictable
4. **Create reusable test configurations** - Define once, use across all tests
5. **Use `Configuration.ignoreCache()`** - When you want to bypass cache entirely in specific tests

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

Implement custom `Settings` for advanced control:

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

val customConfig = Configuration(CustomSettings)

// Use custom configuration
customConfig.ice {
    userRepository.get(MainSync, userId)
}
```

## Repository Configuration

Configuration provides helper functions for creating repository strategies:

### cacheStrategy

Create a strategy with custom main and store data sources:

```kotlin
val strategy = Configuration.Default.cacheStrategy(
    mainDataSource = apiDataSource,
    storeDataSource = dbDataSource
)

// Use with different operations
ice {
    strategy.get(Main, userId)      // Main only
    strategy.get(Store, userId)     // Store only
    strategy.get(MainSync, userId)  // Main → Store
    strategy.get(StoreSync, userId) // Store → Main
}
```

### fallbackWith

Create a MainSync repository (main with fallback to store):

```kotlin
val repository = with(Configuration.Default) {
    apiDataSource fallbackWith dbDataSource
}

ice {
    repository.get(userId)
}
```

### expiresIn / expires

Add cache expiration to a repository strategy:

```kotlin
import kotlin.time.Duration.Companion.minutes
import com.m2f.archer.crud.cache.CacheExpiration

// Expire after duration
val strategy = apiDataSource
    .cacheWith(dbDataSource)
    .expiresIn(5.minutes)

// Custom expiration
val strategy2 = apiDataSource
    .cacheWith(dbDataSource)
    .expires(CacheExpiration.After(10.minutes))

// Never expire
val strategy3 = apiDataSource
    .cacheWith(dbDataSource)
    .expires(CacheExpiration.Never)
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

## See Also

- [Basic Usage](/docs/examples/basic-usage) - Examples using configuration
- [Result Types](/docs/usage/result-types) - Understanding Ice, Either, and other result types
- [Repositories](/docs/usage/repositories) - Repository strategies and operations
