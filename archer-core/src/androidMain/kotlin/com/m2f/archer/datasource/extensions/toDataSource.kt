package com.m2f.archer.datasource.extensions

import android.content.SharedPreferences
import com.m2f.archer.crud.GetDataSource
import com.m2f.archer.crud.GetRepository
import com.m2f.archer.crud.StrategyBuilder
import com.m2f.archer.crud.cacheStrategy
import com.m2f.archer.crud.operation.MainSync
import com.m2f.archer.crud.operation.Operation
import com.m2f.archer.datasource.SharedPreferencesDataSource
import com.m2f.archer.datasource.stringToSerializableBijection
import com.m2f.archer.mapper.Bijection

inline fun <K, reified A> SharedPreferences.toDataSource(
    prefix: String = "",
    bijection: Bijection<String, A & Any> = stringToSerializableBijection(),
): SharedPreferencesDataSource<K, A & Any> = SharedPreferencesDataSource(this, prefix, bijection)

inline fun <reified K, reified A> GetDataSource<K, A & Any>.cache(
    preferences: SharedPreferences,
    operation: Operation = MainSync,
): GetRepository<K, A> =
    cacheStrategy(this, preferences.toDataSource()).create(operation)

inline infix fun <K, reified A> GetDataSource<K, A & Any>.cacheWith(storage: SharedPreferences): StrategyBuilder<K, A> =
    StrategyBuilder(this, storage.toDataSource())