package com.iptvtv.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChannelSourceTest {
    @Test
    fun `encode and decode round trip`() {
        listOf(
            ChannelSource.ByCountry("ID"),
            ChannelSource.ByCategory("news"),
            ChannelSource.BySearch("bbc: world"),
        ).forEach { assertEquals(it, ChannelSource.decode(it.encode())) }
    }

    @Test
    fun `decode rejects unknown values`() {
        assertNull(ChannelSource.decode(null))
        assertNull(ChannelSource.decode(""))
        assertNull(ChannelSource.decode("planet:mars"))
        assertNull(ChannelSource.decode("nocolon"))
    }
}
