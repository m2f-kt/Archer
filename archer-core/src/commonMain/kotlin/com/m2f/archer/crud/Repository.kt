package com.m2f.archer.crud

import com.m2f.archer.failure.Failure
import com.m2f.archer.query.Delete
import com.m2f.archer.query.Get
import com.m2f.archer.query.Put
import com.m2f.archer.repository.Repository

typealias CRUDRepository<Q, A> = Repository<Failure, Q, A & Any>
typealias GetRepository<K, A> = CRUDRepository<Get<K>, A & Any>
typealias DeleteRepository<K> = CRUDRepository<Delete<K>, Unit>
typealias PutRepository<K, A> = CRUDRepository<Put<K, A & Any>, A & Any>
