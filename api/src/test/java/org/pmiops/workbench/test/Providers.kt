package org.pmiops.workbench.test

import javax.inject.Provider

object Providers {

    fun <T> of(t: T): Provider<T> {
        return Provider { t }
    }
}
