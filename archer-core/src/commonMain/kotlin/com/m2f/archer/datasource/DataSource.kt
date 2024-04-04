package com.m2f.archer.datasource

import com.m2f.archer.crud.ArcherRaise

/**
 * A data source is a component that provides data to the application.
 * It can be a remote API, a local database, or a cache.
 *
 * @param F The type of the Failure that the data source can return.
 * @param Q The type of the query that the data source needs to run.
 * @param A The type of the result that the data source returns.
 */
fun interface DataSource<out F, in Q, out A> {
    /**
     * Runs the data source with the given query.
     *
     * @param q The query that the data source needs to run.
     * @return The result of the data source.
     *
     * Either is a branched type that can be Left<F> or Right<A>.
     * Normally, the Left side is used for errors and the Right side is used for success.
     */
    suspend operator fun ArcherRaise.invoke(q: Q): A & Any
}
