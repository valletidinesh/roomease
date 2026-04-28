package com.roomease.app

import com.roomease.app.data.model.GroupRotationState
import com.roomease.app.domain.RotationEngine
import org.junit.Assert.*
import org.junit.Test

class RotationEngineTest {

    @Test
    fun `test basic rotation advancement`() {
        val sequence = listOf("userA", "userB", "userC")
        val state = GroupRotationState(
            sequence = sequence,
            currentCycleOrder = sequence,
            cycleIndex = 0
        )

        // User A cooks
        val stateAfterA = RotationEngine.markDone(state, "userA")
        assertEquals("userB", RotationEngine.getAssigned(stateAfterA))
        assertEquals(1, stateAfterA.cycleIndex)

        // User B cooks
        val stateAfterB = RotationEngine.markDone(stateAfterA, "userB")
        assertEquals("userC", RotationEngine.getAssigned(stateAfterB))
        assertEquals(2, stateAfterB.cycleIndex)

        // User C cooks - Cycle resets
        val stateAfterC = RotationEngine.markDone(stateAfterB, "userC")
        assertEquals("userA", RotationEngine.getAssigned(stateAfterC))
        assertEquals(0, stateAfterC.cycleIndex)
        assertEquals(2, stateAfterC.currentCycleNum)
    }

    @Test
    fun `test override swap logic`() {
        val sequence = listOf("userA", "userB", "userC")
        val state = GroupRotationState(
            sequence = sequence,
            currentCycleOrder = sequence,
            cycleIndex = 0
        )

        // Assigned: A, but C cooks instead (Override)
        val stateAfterC = RotationEngine.markDone(state, "userC")
        
        // After C overrides A:
        // New order should be [C, A, B] because C took A's spot, and A moved to C's spot
        // Index advances to 1, which points to A.
        assertEquals("userA", RotationEngine.getAssigned(stateAfterC))
        assertEquals(1, stateAfterC.cycleIndex)
        assertEquals("userC", stateAfterC.lastActualUserId)
    }

    @Test
    fun `test safety on empty list`() {
        val state = GroupRotationState(currentCycleOrder = emptyList())
        assertNull(RotationEngine.getAssigned(state))
    }
}
