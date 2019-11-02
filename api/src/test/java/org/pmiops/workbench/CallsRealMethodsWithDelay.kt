package org.pmiops.workbench

import org.mockito.internal.stubbing.answers.CallsRealMethods
import org.mockito.invocation.InvocationOnMock

class CallsRealMethodsWithDelay(private val delay: Long) : CallsRealMethods() {

    @Throws(Throwable::class)
    override fun answer(invocation: InvocationOnMock): Any {
        Thread.sleep(delay)
        return super.answer(invocation)
    }
}
