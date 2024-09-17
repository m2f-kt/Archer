package com.m2f.archer.repository

import com.m2f.archer.crud.ArcherRaise

sealed interface Repository<in Q, out A> {
    suspend operator fun ArcherRaise.invoke(q: Q): A
}
