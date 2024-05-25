package com.m2f.archer.datasource

import android.content.SharedPreferences
import arrow.core.raise.catch
import arrow.core.raise.ensureNotNull
import com.m2f.archer.crud.ArcherRaise
import com.m2f.archer.crud.cache.CacheDataSource
import com.m2f.archer.failure.DataEmpty
import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.failure.Invalid
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
) : CacheDataSource<K, A> {

    override suspend fun ArcherRaise.invoke(q: KeyQuery<K, out A>): A =
        when (q) {
            is Get -> {
                val json = sharedPreferences.getString(q.key.toString().prependIndent(prefix), "")
                ensureNotNull(json) { raise(DataNotFound) }
                catch({ bijection.from(json) }) {
                    raise(Invalid)
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

    override suspend fun ArcherRaise.delete(q: Delete<K>) =
        sharedPreferences.edit().remove(q.key.toString().prependIndent(prefix)).apply()
}
