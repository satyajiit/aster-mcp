package com.aster.service.execution

import kotlinx.coroutines.CompletableDeferred
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/** Child closure survives cancellation of the handler's suspended continuation. */
internal class ExecutionChildren : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<ExecutionChildren>
    private val monitor = Any()
    private val drained = CompletableDeferred<Unit>()
    private var count = 0
    private var handlerFinished = false

    fun begin(): Child = synchronized(monitor) {
        check(!handlerFinished)
        count++
        Child()
    }

    inner class Child internal constructor() {
        private var closed = false
        fun complete() = synchronized(monitor) {
            if (!closed) {
                closed = true
                count--
                if (handlerFinished && count == 0) drained.complete(Unit)
            }
        }
    }

    suspend fun awaitClosed() {
        synchronized(monitor) {
            handlerFinished = true
            if (count == 0) drained.complete(Unit)
        }
        drained.await()
    }
}
