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

Within any DSL block, you can use `archerRecover` to handle failures explicitly:

```kotlin
ice {
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

Default implementation is `MemoizedExpirationCache`.

### getCurrentTime

Function providing the current time for cache expiration checks:

```kotlin
fun getCurrentTime(): Instant
```

Default uses `Clock.System.now()`.

## Best Practices

1. **Use Default Configuration** - Start with `Configuration.Default` for most cases
2. **DSL Functions** - Choose the right DSL based on your return type needs:
   - UI states → `ice`
   - Error handling → `either`
   - Optional values → `nullable`
   - Boolean checks → `bool`
3. **Custom Settings** - Only create custom settings when you need specific fallback behavior
4. **Cache Expiration** - Always set expiration times for cached data
5. **archerRecover** - Use for explicit failure handling within DSL blocks

## See Also

- [Basic Usage](/docs/examples/basic-usage) - Examples using configuration
- [Result Types](/docs/usage/result-types) - Understanding Ice, Either, and other result types
- [Repositories](/docs/usage/repositories) - Repository strategies and operations
