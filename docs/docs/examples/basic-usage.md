---
sidebar_position: 1
---

# Basic Usage

Complete examples showing how to use Archer in real-world scenarios.

## Key Concepts

### Error Handling

Archer uses a typed error system based on `Failure` types. Instead of try/catch blocks, use:

- **`archerRecover`** - Handle raised failures with a recover block
- **`catch`** - Handle both failures and exceptions

```kotlin
// Basic recovery from failures
with(Configuration.Default) {
    archerRecover(
        block = { /* code to execute */ },
        recover = { failure -> /* called if we raise in the block */ }
    )
}

// Handle both failures and exceptions
with(Configuration.Default) {
    archerRecover(
        block = { /* code to execute */ },
        recover = { failure -> /* called if we raise a Failure */ },
        catch = { exception -> /* called if there is an exception thrown */ }
    )
}
```

**Important**: All failures must be of type `Failure` (defined in `archer-core`). The architecture is based on raising typed `Failure` instances.

### Operations

Repositories support four operation strategies (from `Operation.kt`):

- **`Main`** - Fetch from main data source only
- **`Store`** - Fetch from store/cache only
- **`MainSync`** - Fetch from main and sync to store
- **`StoreSync`** - Fetch from store, fallback to main if needed

### Ice States

The `Ice` type represents three possible states and can be handled in multiple ways:

```kotlin
// Method 1: when expression
when (ice) {
    is Ice.Idle -> // Loading state
    is Ice.Content -> // Success with value
    is Ice.Error -> // Failure with error
}

// Method 2: fold function
ice.fold(
    ifIdle = { /* handle loading */ },
    ifContent = { value -> /* handle success */ },
    ifError = { failure -> /* handle error */ }
)
```

## Simple API Integration

### Scenario
Fetch user data from a REST API with in-memory caching.

```kotlin
import com.m2f.archer.configuration.Configuration
import com.m2f.archer.crud.*
import com.m2f.archer.crud.operation.*
import com.m2f.archer.datasource.InMemoryDataSource
import com.m2f.archer.failure.Failure
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlin.time.Duration.Companion.minutes

// Domain model
data class User(
    val id: Int,
    val name: String,
    val email: String
)

// API model
data class UserDto(
    val id: Int,
    val name: String,
    val email: String
)

// Extension to convert API model to domain
fun UserDto.toDomain() = User(
    id = id,
    name = name,
    email = email
)

// Create the data source
val httpClient = HttpClient()

val userApiDataSource = getDataSource<Int, User> { userId ->
    val response = httpClient.get("https://api.example.com/users/$userId")
    if (response.status.value == 404) {
        raise(Failure.DataNotFound)
    }
    response.body<UserDto>().toDomain()
}

// Add caching
val cache: StoreDataSource<Int, User> = InMemoryDataSource()

val userRepository = userApiDataSource
    .cacheWith(cache)
    .expiresIn(5.minutes)

// Use it with archerRecover
suspend fun getUser(userId: Int): User = with(Configuration.Default) {
    archerRecover(
        block = {
            userRepository.get(Main, userId)
        },
        recover = { failure: Failure ->
            // Handle the failure, maybe return a default value or rethrow
            raise(failure)
        }
    )
}

// Or use it with catch to handle exceptions too
suspend fun getUserSafe(userId: Int): User = with(Configuration.Default) {
    archerRecover(
        block = {
            userRepository.get(Main, userId)
        },
        recover = { failure: Failure ->
            // Called if we raise a Failure
            raise(failure)
        },
        catch = { exception: Throwable ->
            // Called if there is an exception thrown
            raise(Failure.Unhandled(exception))
        }
    )
}
```

## Database Integration

### Scenario
Store and retrieve users from a local database with Room.

```kotlin
import androidx.room.*

// Room Entity
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val email: String
)

// DAO
@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUser(userId: Int): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: Int)
}

// Extension functions
fun UserEntity.toDomain() = User(
    id = id,
    name = name,
    email = email
)

fun User.toEntity() = UserEntity(
    id = id,
    name = name,
    email = email
)

// Create the DataSource
class UserDatabaseDataSource(
    private val userDao: UserDao
) : StoreDataSource<Int, User>, DeleteDataSource<Int> {

    override suspend fun get(key: Int): User {
        return userDao.getUser(key)?.toDomain()
            ?: raise(Failure.DataNotFound)
    }

    override suspend fun put(key: Int, value: User) {
        userDao.insertUser(value.toEntity())
    }

    override suspend fun delete(key: Int) {
        userDao.deleteUser(key)
    }
}

// Combine API and Database
val userRepository = userApiDataSource
    .cacheWith(UserDatabaseDataSource(database.userDao()))
    .expiresIn(5.minutes)
```

## Android ViewModel Integration

### Scenario
Load and display user data in an Android app with proper state management.

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m2f.archer.crud.GetRepositoryStrategy
import com.m2f.archer.crud.Ice
import com.m2f.archer.crud.ice
import com.m2f.archer.crud.operation.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class UserViewModel(
    private val userRepository: GetRepositoryStrategy<Int, User>
) : ViewModel() {

    private val _userState = MutableStateFlow<Ice<User>>(Ice.Idle)
    val userState: StateFlow<Ice<User>> = _userState.asStateFlow()

    fun loadUser(userId: Int, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _userState.value = Ice.Idle

            val operation = if (forceRefresh) {
                Main
            } else {
                StoreSync
            }

            _userState.value = ice {
                userRepository.get(operation, userId)
            }
        }
    }

    fun refreshUser(userId: Int) {
        loadUser(userId, forceRefresh = true)
    }
}

// In your Compose UI - Three ways to handle Ice states

// Method 1: Using when expression
@Composable
fun UserScreen(
    userId: Int,
    viewModel: UserViewModel = viewModel()
) {
    val userState by viewModel.userState.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadUser(userId)
    }

    when (val state = userState) {
        is Ice.Idle -> LoadingIndicator()

        is Ice.Content -> UserContent(
            user = state.value,
            onRefresh = { viewModel.refreshUser(userId) }
        )

        is Ice.Error -> ErrorView(
            error = state.error,
            onRetry = { viewModel.loadUser(userId) }
        )
    }
}

// Method 2: Using fold function
@Composable
fun UserScreenWithFold(
    userId: Int,
    viewModel: UserViewModel = viewModel()
) {
    val userState by viewModel.userState.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadUser(userId)
    }

    userState.fold(
        ifIdle = { LoadingIndicator() },
        ifContent = { user ->
            UserContent(
                user = user,
                onRefresh = { viewModel.refreshUser(userId) }
            )
        },
        ifError = { error ->
            ErrorView(
                error = error,
                onRetry = { viewModel.loadUser(userId) }
            )
        }
    )
}
```

## Validation

### Scenario
Validate data returned from a data source.

Archer provides a `validate` extension function that works with `DataSource`. When validation fails, it raises `Failure.Invalid`.

```kotlin
import com.m2f.archer.crud.validate.validate
import com.m2f.archer.failure.Invalid

// Create a data source with validation
val validatedUserDataSource = getDataSource<Int, User> { userId ->
    httpClient.get("https://api.example.com/users/$userId")
        .body<UserDto>()
        .toDomain()
}.validate { user ->
    // Return true if valid, false if invalid
    user.name.isNotBlank() && user.email.contains("@")
}

// When validation fails, Invalid failure is raised
suspend fun getUserWithValidation(userId: Int) = with(Configuration.Default) {
    archerRecover(
        block = {
            validatedUserDataSource.get(userId)
        },
        recover = { failure ->
            when (failure) {
                Invalid -> {
                    // Handle validation failure
                    println("User data is invalid")
                    raise(failure)
                }
                else -> raise(failure)
            }
        }
    )
}

// Example: Validate email format
val emailDataSource = getDataSource<String, String> { email -> email }
    .validate { it.contains("@") && it.contains(".") }

// Example: Validate number range
val ageDataSource = getDataSource<Int, Int> { age -> age }
    .validate { it in 0..150 }
```

## Next Steps

- [Recipes](/docs/examples/recipes) - Common patterns and solutions
- [DataSources](/docs/usage/datasources) - Learn more about data sources
- [Repositories](/docs/usage/repositories) - Deep dive into repositories
