package org.pmiops.workbench.cohortbuilder.util

import java.text.MessageFormat
import java.util.function.Predicate
import org.pmiops.workbench.exceptions.BadRequestException

class Validation<K> private constructor(private val predicate: Predicate<K>) {
    private var throwException: Boolean? = null

    fun test(param: K): Validation<*> {
        return if (predicate.test(param)) throwException() else ok()
    }

    fun throwException(): Validation<*> {
        this.throwException = true
        return this
    }

    fun ok(): Validation<*> {
        this.throwException = false
        return this
    }

    fun throwException(message: String) {
        if (throwException!!) {
            throw BadRequestException(message)
        }
    }

    fun throwException(message: String, vararg args: Any) {
        if (throwException!!) {
            throw BadRequestException(MessageFormat(message).format(args))
        }
    }

    companion object {

        fun <K> from(predicate: Predicate<K>): Validation<K> {
            return Validation(predicate)
        }
    }
}
