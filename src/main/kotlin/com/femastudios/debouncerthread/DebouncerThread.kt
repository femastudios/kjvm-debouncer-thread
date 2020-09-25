package com.femastudios.debouncerthread

import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


/**
 * Thread that "debounces" an operation.
 *
 * Every time the [debounce] method is called, the thread waits for [waitTime] milliseconds before calling
 * [operation]. If another [debounce] is called in the mean time, the timer is reset and another full [waitTime]
 * milliseconds will be waited. All parameters of type [T] passed to the [debounce] method are kept in a [List] that is
 * passed to [operation].
 *
 * When [maxWaitTime] is specified, it is guaranteed that no more than [maxWaitTime] milliseconds will pass between each
 * [operation] call. Not specifying this value means that a continuous stream of close [debounce] calls (less
 * than [waitTime] between each call) will result in never calling [operation] and a forever growing list of
 * parameters.
 *
 * @param waitTime the time (in ms) that should pass between a [debounce] and the execution of the [operation]
 * @param maxWaitTime the maximum amount of time (in ms) that should pass between any [debounce] call and the execution
 * of the [operation]
 * @param operation the operation to execute on [debounce]
 */
class DebouncerThread<T>(
    val waitTime: Long,
    val maxWaitTime: Long? = null,
    name: String = nextName(),
    start: Boolean = true,
    val operation: (List<T>) -> Unit,
) : Thread(name) {

    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    private var notified = false
    private val params = mutableListOf<T>()
    private var firstRequestTime: Long? = null
    private var actionsScheduled = false
    private var executeImmediately = false

    init {
        require(waitTime > 0) { "waitTime must be a positive value" }
        if (maxWaitTime != null) require(maxWaitTime >= waitTime) { "maxWaitTime must be greater than waitTime" }
        if (start) {
            start()
        }
    }

    /**
     * Requests the execution of the operation adding the given [param]
     */
    fun debounce(param: T) {
        debounce(param, false)
    }

    /**
     * Requests the execution of the operation adding no param
     */
    fun debounce() {
        debounce(false)
    }

    /**
     * Requests the immediate execution of the operation adding the given [param]
     * This call bypasses [waitTime] and [maxWaitTime] completely.
     */
    fun debounceNow(param: T) {
        debounce(param, true)
    }

    /**
     * Requests the immediate execution of the operation adding no param
     * This call bypasses [waitTime] and [maxWaitTime] completely.
     */
    fun debounceNow() {
        debounce(true)
    }

    private fun debounce(debounceNow: Boolean) {
        lock.withLock {
            if (firstRequestTime == null) {
                firstRequestTime = System.currentTimeMillis()
            }
            actionsScheduled = true
            notified = true
            if (debounceNow) {
                executeImmediately = true
            }
            condition.signal()
        }
    }

    private fun debounce(param: T, debounceNow: Boolean) {
        lock.withLock {
            params.add(param)
            debounce(debounceNow)
        }
    }

    override fun run() {
        while (!interrupted()) {
            var paramsCopy: ArrayList<T>?
            lock.withLock {
                if (!actionsScheduled) {
                    try {
                        condition.await()
                    } catch (e: InterruptedException) {
                        return
                    }
                }
                while (!interrupted() && notified && !executeImmediately) {
                    notified = false
                    val frt = firstRequestTime
                    val realWait = if (maxWaitTime == null || frt == null) {
                        waitTime
                    } else {
                        waitTime.coerceAtMost(frt + maxWaitTime - System.currentTimeMillis())
                    }
                    if (realWait > 0) {
                        try {
                            condition.await(realWait, TimeUnit.MILLISECONDS)
                        } catch (e: InterruptedException) {
                            return
                        }
                    }
                }
                executeImmediately = false
                if (actionsScheduled) {
                    actionsScheduled = false
                    paramsCopy = ArrayList<T>(params)
                    params.clear()
                    firstRequestTime = null
                } else {
                    paramsCopy = null
                }
            }
            paramsCopy?.let {
                operation(it)
            }
        }
    }

    companion object {
        private val COUNTER = AtomicInteger(0)
        internal fun nextName(): String {
            return "DebouncerThread-" + COUNTER.incrementAndGet()
        }
    }
}

/**
 * Creates a new [DebouncerThread] with no parameters.
 */
fun DebouncerThread(
    waitTime: Long,
    maxWaitTime: Long? = null,
    name: String = DebouncerThread.nextName(),
    start: Boolean = true,
    executeOperation: () -> Unit
): DebouncerThread<Nothing> {
    return DebouncerThread<Nothing>(waitTime, maxWaitTime, name, start) { executeOperation() }
}
