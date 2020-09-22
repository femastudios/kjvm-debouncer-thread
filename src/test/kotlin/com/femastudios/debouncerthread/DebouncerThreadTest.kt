package com.femastudios.debouncerthread

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep
import kotlin.concurrent.thread

internal class DebouncerThreadTest {

    private fun setupParametrizedTest(
        waitTime: Long,
        maxWaitTime: Long? = null
    ): Pair<MutableList<List<Int>>, DebouncerThread<Int>> {
        val list = mutableListOf<List<Int>>()
        val pdb = DebouncerThread<Int>(waitTime, maxWaitTime) {
            list.add(it)
        }
        return list to pdb
    }

    @Test
    internal fun simpleTest() {
        var canRun = false
        val db = DebouncerThread(100) {
            assertTrue(canRun)
            canRun = false
        }
        // TEST 1
        repeat(10) {
            db.debounce()
            sleep(50)
        }
        canRun = true
        sleep(150) // Wait for debounce
        assertFalse(canRun)

        // TEST 2
        canRun = true
        db.debounceNow()
        sleep(10) // Wait for immediate debounce
        assertFalse(canRun)
    }

    @Test
    internal fun simpleParametrizedTest() {
        val (list, pdb) = setupParametrizedTest(100, 200)

        // TEST 1
        pdb.debounce()
        sleep(50)
        pdb.debounce(1)
        sleep(50)
        pdb.debounce(2)

        sleep(150) // Wait for debounce
        assertEquals(listOf(listOf(1, 2)), list)
        list.clear()

        // TEST 2
        repeat(6) {
            pdb.debounce(it)
            sleep(50)
        }

        sleep(150) // Wait for debounce
        assertEquals(listOf(listOf(0, 1, 2, 3), listOf(4, 5)), list)
        list.clear()

        // TEST 3
        pdb.debounceNow(11)
        sleep(10) // Wait for immediate debounce
        assertEquals(listOf(listOf(11)), list)
    }

    @Test
    internal fun multiThreadedTest() {
        val (list, pdb) = setupParametrizedTest(100)

        (1..50).map {
            thread {
                pdb.debounce(it)
            }
        }.forEach { it.join() }

        sleep(200) // Wait for debounce
        assertEquals(1, list.size)
        assertEquals((1..50).toSet(), list.single().toSet())
    }

    @Test
    internal fun noMaxTimeTest() {
        val (list, pdb) = setupParametrizedTest(50)

        // TEST 1
        repeat(50) {
            sleep(10)
            pdb.debounce(it + 1)
        }

        sleep(100) // Wait for debounce
        assertEquals(listOf((1..50).toList()), list)
        list.clear()

        // TEST 2
        repeat(5) {
            sleep(100)
            pdb.debounce(it + 1)
        }
        sleep(100) // Wait for debounce
        assertEquals((1..5).toList().map { listOf(it) }, list)
    }
}