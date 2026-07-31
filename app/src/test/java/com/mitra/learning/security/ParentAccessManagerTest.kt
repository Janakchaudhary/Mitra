package com.mitra.learning.security

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ParentAccessManagerTest {
    @Test
    fun `parent access can be locked immediately`() = runTest {
        val manager = ParentAccessManager(this)
        manager.unlock(5)
        assertTrue(manager.unlocked.value)
        manager.lock()
        assertFalse(manager.unlocked.value)
    }

    @Test
    fun `parent access expires`() = runTest {
        val manager = ParentAccessManager(this)
        manager.unlock(1)
        assertTrue(manager.unlocked.value)
        advanceTimeBy(60_001)
        runCurrent()
        assertFalse(manager.unlocked.value)
    }
    @Test
    fun `brief background does not immediately relock parent area`() = runTest {
        val manager = ParentAccessManager(this)
        manager.unlock(5)
        manager.onAppBackgrounded(graceMillis = 30_000)
        advanceTimeBy(10_000)
        manager.onAppForegrounded()
        runCurrent()
        assertTrue(manager.unlocked.value)
    }

}
