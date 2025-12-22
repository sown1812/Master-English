package com.example.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ForwardedHeaderParseTest {

    @Test
    fun `parses forwarded header for IP`() {
        assertEquals("203.0.113.4", parseForwardedFor("for=203.0.113.4;proto=https"))
        assertEquals("203.0.113.4", parseForwardedFor("for=\"203.0.113.4\";proto=https"))
        assertEquals("2001:db8::1", parseForwardedFor("for=\"[2001:db8::1]\";proto=https"))
        assertEquals("203.0.113.4", parseForwardedFor("for=203.0.113.4, for=198.51.100.3"))
    }

    @Test
    fun `returns null for missing for`() {
        assertNull(parseForwardedFor(null))
        assertNull(parseForwardedFor(""))
        assertNull(parseForwardedFor("proto=https"))
    }
}

