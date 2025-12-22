package com.example.server

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RequestLimitsCidrTest {

    @Test
    fun `cidr allowlist matches ip`() {
        val blocks = parseCidrList("10.0.0.0/8, 192.168.1.0/24")
        assertTrue(isAllowedByCidr("10.10.0.5", blocks))
        assertTrue(isAllowedByCidr("192.168.1.99", blocks))
        assertFalse(isAllowedByCidr("192.168.2.1", blocks))
    }
}

