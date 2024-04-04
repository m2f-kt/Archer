package com.m2f.archer.startup

import android.content.Context
import androidx.startup.Initializer

internal lateinit var applicationContext: Context
    private set

object ArcherContext

class ArcherInitializer : Initializer<ArcherContext> {
    override fun create(context: Context): ArcherContext {
        applicationContext = context.applicationContext
        return ArcherContext
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}
