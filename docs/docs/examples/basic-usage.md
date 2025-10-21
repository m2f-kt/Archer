---
sidebar_position: 1
---

# Basic Usage

Complete examples showing how to use Archer in real-world scenarios.

## Simple API Integration

### Scenario
Fetch user data from a REST API with in-memory caching.

```kotlin
import arrow.core.Either
import arrow.core.raise.either
import com.m2f.archer.core.*
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

// Errors
sealed interface UserError : DomainError {
    data class NotFound(val userId: Int) : UserError
    data class NetworkError(val message: String) : UserError
}

// Create the data source
val httpClient = HttpClient()

val userApiDataSource = getDataSource<Int, User> { userId ->
    try {
        val response = httpClient.get("https://api.example.com/users/$userId")
        if (response.status.value == 404) {
            raise(UserError.NotFound(userId))
        }
        response.body<UserDto>().toDomain()
    } catch (e: Exception) {
        raise(UserError.NetworkError(e.message ?: "Unknown error"))
    }
}

// Add caching
val cache: StoreDataSource<Int, User> = InMemoryDataSource()

val userRepository = userApiDataSource
    .cacheWith(cache)
    .expiresIn(5.minutes)

// Use it
suspend fun getUser(userId: Int): Either<UserError, User> = either {
    userRepository.get(StoreSync.StoreFirst, userId)
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
            ?: raise(UserError.NotFound(key))
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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class UserViewModel(
    private val userRepository: Repository<Int, User>
) : ViewModel() {

    private val _userState = MutableStateFlow<Ice<UserError, User>>(Ice.Idle)
    val userState: StateFlow<Ice<UserError, User>> = _userState.asStateFlow()

    fun loadUser(userId: Int, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _userState.value = Ice.Idle

            val strategy = if (forceRefresh) {
                StoreSync.NetworkFirst
            } else {
                StoreSync.StoreFirst
            }

            _userState.value = ice {
                userRepository.get(strategy, userId)
            }
        }
    }

    fun refreshUser(userId: Int) {
        loadUser(userId, forceRefresh = true)
    }
}

// In your Compose UI
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
```

## Multi-Source Repository

### Scenario
Try primary API, fallback to secondary, with local caching.

```kotlin
val primaryApi = getDataSource<Int, User> { userId ->
    try {
        httpClient.get("https://primary-api.example.com/users/$userId")
            .body<UserDto>()
            .toDomain()
    } catch (e: Exception) {
        raise(UserError.NetworkError("Primary API failed: ${e.message}"))
    }
}

val secondaryApi = getDataSource<Int, User> { userId ->
    try {
        httpClient.get("https://secondary-api.example.com/users/$userId")
            .body<UserDto>()
            .toDomain()
    } catch (e: Exception) {
        raise(UserError.NetworkError("Secondary API failed: ${e.message}"))
    }
}

// Combine with fallback logic
val resilientDataSource = getDataSource<Int, User> { userId ->
    ice {
        primaryApi.get(userId)
    }.recover { primaryError ->
        ice {
            secondaryApi.get(userId)
        }.getOrElse { secondaryError ->
            // Both failed, raise the primary error
            raise(primaryError)
        }
    }.bind()
}

// Add caching
val userRepository = resilientDataSource
    .cacheWith(databaseDataSource)
    .expiresIn(10.minutes)
```

## Pagination

### Scenario
Load paginated data from an API.

```kotlin
data class Page<T>(
    val items: List<T>,
    val page: Int,
    val totalPages: Int
)

data class PaginationParams(
    val page: Int,
    val pageSize: Int
)

val paginatedUsersDataSource = getDataSource<PaginationParams, Page<User>> { params ->
    val response = httpClient.get("https://api.example.com/users") {
        parameter("page", params.page)
        parameter("size", params.pageSize)
    }

    response.body<PageDto<UserDto>>().let { pageDto ->
        Page(
            items = pageDto.items.map { it.toDomain() },
            page = pageDto.page,
            totalPages = pageDto.totalPages
        )
    }
}

// Usage
suspend fun loadUsers(page: Int): Either<UserError, Page<User>> = either {
    paginatedUsersDataSource.get(PaginationParams(page, 20))
}
```

## Validation

### Scenario
Validate user data before saving.

```kotlin
sealed interface ValidationError : DomainError {
    data class InvalidEmail(val email: String) : ValidationError
    data class InvalidName(val name: String) : ValidationError
    data class InvalidAge(val age: Int) : ValidationError
}

fun validateUser(user: User) {
    if (user.name.isBlank()) {
        raise(ValidationError.InvalidName(user.name))
    }

    if (!user.email.contains("@")) {
        raise(ValidationError.InvalidEmail(user.email))
    }

    if (user.age < 0 || user.age > 150) {
        raise(ValidationError.InvalidAge(user.age))
    }
}

// Add validation to repository
class ValidatedUserRepository(
    private val repository: Repository<Int, User>
) {
    suspend fun saveUser(user: User): Either<DomainError, User> = either {
        validateUser(user)
        repository.put(user.id, user)
        user
    }
}
```

## Next Steps

- [Recipes](/docs/examples/recipes) - Common patterns and solutions
- [DataSources](/docs/usage/datasources) - Learn more about data sources
- [Repositories](/docs/usage/repositories) - Deep dive into repositories
