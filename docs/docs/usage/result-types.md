---
sidebar_position: 3
---

# Result Types

Archer supports multiple result types to handle success and failure scenarios. All result types use Arrow's typed errors foundation.

## Ice (Idle | Content | Error)

`Ice` is a three-state result type unique to Archer, perfect for UI states.

```kotlin
sealed class Ice<out E, out A> {
    data object Idle : Ice<Nothing, Nothing>
    data class Content<out A>(val value: A) : Ice<Nothing, A>
    data class Error<out E>(val error: E) : Ice<E, Nothing>
}
```

### Usage

```kotlin
val result: Ice<DomainError, User> = ice {
    repository.get(StoreSync.StoreFirst, userId)
}

when (result) {
    is Ice.Idle -> showLoadingState()
    is Ice.Content -> showUser(result.value)
    is Ice.Error -> showError(result.error)
}
```

### Why Ice?

Ice is particularly useful for UI state management:

```kotlin
class UserViewModel {
    private val _userState = MutableStateFlow<Ice<UserError, User>>(Ice.Idle)
    val userState: StateFlow<Ice<UserError, User>> = _userState

    fun loadUser(userId: Int) {
        viewModelScope.launch {
            _userState.value = Ice.Idle  // Show loading

            _userState.value = ice {
                repository.get(StoreSync.StoreFirst, userId)
            }
        }
    }
}
```

### Ice Operations

**fold**

Handle all three states:

```kotlin
result.fold(
    ifIdle = { showLoading() },
    ifContent = { user -> showUser(user) },
    ifError = { error -> showError(error) }
)
```

**getOrNull**

Extract value or return null:

```kotlin
val user: User? = result.getOrNull()
```

**getOrElse**

Provide a default value:

```kotlin
val user: User = result.getOrElse { User.DEFAULT }
```

**map**

Transform the content:

```kotlin
val userName: Ice<UserError, String> = userIce.map { it.name }
```

## Either (Left | Right)

`Either` is Arrow's classic functional error handling type.

```kotlin
sealed class Either<out A, out B> {
    data class Left<out A>(val value: A) : Either<A, Nothing>()
    data class Right<out B>(val value: B) : Either<Nothing, B>()
}
```

### Usage

```kotlin
val result: Either<DomainError, User> = either {
    repository.get(StoreSync.StoreFirst, userId)
}

when (result) {
    is Either.Left -> handleError(result.value)
    is Either.Right -> handleSuccess(result.value)
}
```

### Either Operations

**fold**

```kotlin
val message = result.fold(
    ifLeft = { error -> "Error: ${error.message}" },
    ifRight = { user -> "Hello, ${user.name}" }
)
```

**getOrNull**

```kotlin
val user: User? = result.getOrNull()
```

**getOrElse**

```kotlin
val user: User = result.getOrElse { User.ANONYMOUS }
```

**map**

```kotlin
val userName: Either<UserError, String> = userEither.map { it.name }
```

**mapLeft**

Transform the error:

```kotlin
val result: Either<String, User> = userEither.mapLeft { error ->
    "Failed to load user: ${error.message}"
}
```

## Nullable

The simplest result type - just a nullable value.

### Usage

```kotlin
val user: User? = nullable {
    repository.get(StoreSync.StoreFirst, userId)
}

if (user != null) {
    showUser(user)
} else {
    showError()
}
```

### When to Use Nullable

- Simple operations where you don't need error details
- Prototyping or quick scripts
- When integrating with nullable-based APIs

## Choosing a Result Type

### Use Ice when:
- Building UI with loading/content/error states
- You need to represent "not yet loaded" separately from errors
- Working with state management (MVI, MVVM)

### Use Either when:
- You need detailed error information
- Writing functional code with Arrow
- Composing multiple fallible operations
- You want compile-time guarantees for error handling

### Use Nullable when:
- Errors don't need specific handling
- Integrating with Java/nullable-based code
- Quick scripts or prototypes
- Simple existence checks

## Raising Errors

All result types use Arrow's `raise` mechanism:

```kotlin
val result = ice {
    val user = repository.get(StoreSync.StoreFirst, userId)

    if (!user.isActive) {
        raise(UserNotActive(userId))
    }

    user
}
```

### Custom Error Types

Define your domain errors:

```kotlin
sealed interface UserError : DomainError {
    data class NotFound(val userId: Int) : UserError
    data class NotActive(val userId: Int) : UserError
    data class InvalidData(val reason: String) : UserError
}

sealed interface NetworkError : DomainError {
    data class Timeout(val url: String) : NetworkError
    data class Unauthorized(val endpoint: String) : NetworkError
    data class ServerError(val code: Int, val message: String) : NetworkError
}
```

Use them in your DataSources:

```kotlin
val userDataSource = getDataSource<Int, User> { userId ->
    try {
        val response = api.getUser(userId)

        when {
            response.code == 404 -> raise(UserError.NotFound(userId))
            response.code == 401 -> raise(NetworkError.Unauthorized("/users/$userId"))
            !response.isSuccessful -> raise(
                NetworkError.ServerError(response.code, response.message)
            )
            else -> response.body.toDomain()
        }
    } catch (e: TimeoutException) {
        raise(NetworkError.Timeout("/users/$userId"))
    }
}
```

## Error Recovery

### With Either

```kotlin
val user: Either<UserError, User> = either {
    repository.get(StoreSync.StoreFirst, userId)
}.recover { error ->
    when (error) {
        is UserError.NotFound -> User.ANONYMOUS
        is UserError.NotActive -> User.GUEST
        else -> raise(error)
    }
}
```

### With Ice

```kotlin
val user: Ice<UserError, User> = ice {
    repository.get(StoreSync.StoreFirst, userId)
}.recover { error ->
    if (error is UserError.NotFound) {
        User.ANONYMOUS
    } else {
        raise(error)
    }
}
```

## Combining Results

### Sequential Operations

```kotlin
val result = either {
    val user = userRepository.get(StoreSync.StoreFirst, userId).bind()
    val profile = profileRepository.get(StoreSync.StoreFirst, user.profileId).bind()
    val preferences = prefsRepository.get(StoreSync.StoreFirst, user.id).bind()

    UserData(user, profile, preferences)
}
```

### Parallel Operations

```kotlin
suspend fun loadUserData(userId: Int): Either<AppError, UserDashboard> = either {
    coroutineScope {
        val userDeferred = async { userRepository.get(StoreSync.StoreFirst, userId) }
        val postsDeferred = async { postsRepository.get(StoreSync.StoreFirst, userId) }
        val followersDeferred = async { followersRepository.get(StoreSync.StoreFirst, userId) }

        UserDashboard(
            user = userDeferred.await().bind(),
            posts = postsDeferred.await().bind(),
            followers = followersDeferred.await().bind()
        )
    }
}
```

## Best Practices

### 1. Use Typed Errors

```kotlin
// Good: Specific error types
sealed interface UserError : DomainError {
    data class NotFound(val id: Int) : UserError
    data class Unauthorized(val id: Int) : UserError
}

// Avoid: Generic exceptions
throw Exception("User not found")
```

### 2. Handle Errors at the Right Level

```kotlin
// Good: Handle at UI layer
viewModelScope.launch {
    userRepository.get(userId).fold(
        ifLeft = { error -> showError(error) },
        ifRight = { user -> showUser(user) }
    )
}

// Avoid: Catching too early
val user = try {
    repository.get(userId)
} catch (e: Exception) {
    null  // Lost error information
}
```

### 3. Provide Meaningful Error Messages

```kotlin
sealed interface UserError : DomainError {
    val message: String

    data class NotFound(val userId: Int) : UserError {
        override val message = "User with ID $userId was not found"
    }

    data class InvalidEmail(val email: String) : UserError {
        override val message = "Email '$email' is not valid"
    }
}
```

## Next Steps

- [Examples](/docs/examples/basic-usage) - See practical examples
- [Recipes](/docs/examples/recipes) - Common patterns and solutions
