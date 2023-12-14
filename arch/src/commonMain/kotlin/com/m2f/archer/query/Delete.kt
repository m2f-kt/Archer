package com.m2f.archer.query

data class Delete<K>(val key: K): Query<K>
