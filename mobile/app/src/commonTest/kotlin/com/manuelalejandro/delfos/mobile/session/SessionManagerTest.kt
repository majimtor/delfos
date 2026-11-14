package com.manuelalejandro.delfos.mobile.session

import com.manuelalejandro.delfos.mobile.manager.FakeTokenStore
import com.manuelalejandro.delfos.mobile.manager.SESSION
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionManagerTest {
    @Test
    fun restoresTheStoredSessionWithoutARoundTrip() {
        val store = FakeTokenStore("""{"token":"token-123","username":"demo-manager","isAdmin":false}""")
        val manager = SessionManager(store)
        assertEquals(SessionState.SignedIn("demo-manager", false, "token-123"), manager.state.value)
        assertEquals("token-123", manager.token)
    }

    @Test
    fun signInWritesTheStoreAndSignOutClearsIt() {
        val store = FakeTokenStore()
        val manager = SessionManager(store)
        assertEquals(SessionState.SignedOut, manager.state.value)
        manager.signIn(SESSION)
        assertTrue(manager.signedIn)
        assertNotNull(store.value)
        assertTrue("token-123" in store.value!!)
        manager.signOut()
        assertNull(store.value)
        assertNull(manager.token)
    }

    @Test
    fun aStoredValueThatDoesNotParseIsDropped() {
        val store = FakeTokenStore("not json")
        val manager = SessionManager(store)
        assertEquals(SessionState.SignedOut, manager.state.value)
        assertEquals(1, store.cleared)
    }
}
