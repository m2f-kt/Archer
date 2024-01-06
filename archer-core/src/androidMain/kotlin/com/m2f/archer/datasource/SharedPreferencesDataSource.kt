package com.m2f.archer.datasource

import android.content.SharedPreferences
import arrow.core.Either
import arrow.core.raise.either
import com.m2f.archer.crud.DeleteDataSource
import com.m2f.archer.crud.StoreDataSource
import com.m2f.archer.failure.DataEmpty
import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.failure.Failure
import com.m2f.archer.mapper.Bijection
import com.m2f.archer.query.Delete
import com.m2f.archer.query.Get
import com.m2f.archer.query.KeyQuery
import com.m2f.archer.query.Put

/**
 * A data source that uses shared preferences to store data
 * @constructor sharedPreferences The shared preferences used to store the data
 * @constructor prefix A prefix used to modify the key used to reference the data stored
 * @constructor serializer A serializer used to convert the data to a string and vice versa
 *
 * Iso is a data type that allows us to convert from one type to another. In this case we are using
 * it to convert from a String to a data type A
 */
class SharedPreferencesDataSource<K, A>(
    private val sharedPreferences: SharedPreferences,
    private val prefix: String = "",
    private val bijection: Bijection<String, A>
) : StoreDataSource<K, A>, DeleteDataSource<K> {

    override suspend fun invoke(q: KeyQuery<K, out A>): Either<Failure, A> = either {
        when (q) {
            is Get -> {
                val json = sharedPreferences.getString(q.key.toString().prependIndent(prefix), "")
                if (json.isNullOrBlank()) {
                    raise(DataNotFound)
                } else {
                    bijection.from(json)
                }
            }

            is Put -> {
                q.value
                    ?.also {
                        sharedPreferences.edit()
                            .putString(q.key.toString().prependIndent(prefix), bijection.to(it))
                            .apply()
                    } ?: raise(DataEmpty)
            }
        }
    }

    override suspend fun delete(q: Delete<K>): Either<Failure, Unit> = either {
        sharedPreferences.edit().remove(q.key.toString().prependIndent(prefix)).apply()
    }
}
