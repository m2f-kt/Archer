package com.m2f.archer.mapper

/**
 * A bijection represents a two way mapping from S to T and from T to S
 */
interface Bijection<S, T> {

    fun from(s: S): T & Any
    fun to(t: T): S & Any
}
