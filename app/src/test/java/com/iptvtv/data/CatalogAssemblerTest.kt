package com.iptvtv.data

import com.iptvtv.data.remote.CategoryDto
import com.iptvtv.data.remote.ChannelDto
import com.iptvtv.data.remote.CountryDto
import com.iptvtv.data.remote.LogoDto
import com.iptvtv.data.remote.StreamDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogAssemblerTest {

    private fun assemble(
        blocked: List<String> = emptyList(),
        streams: List<StreamDto>,
        logos: List<LogoDto> = emptyList(),
        channels: List<ChannelDto>,
    ): CatalogAssembler.Catalog {
        val a = CatalogAssembler()
        blocked.forEach(a::addBlocked)
        streams.forEach(a::addStream)
        logos.forEach(a::addLogo)
        channels.forEach(a::addChannel)
        a.addCountry(CountryDto("ID", "Indonesia", "🇮🇩"))
        a.addCountry(CountryDto("US", "United States", "🇺🇸"))
        a.addCategory(CategoryDto("news", "News"))
        a.addCategory(CategoryDto("xxx", "XXX"))
        return a.build()
    }

    @Test
    fun `keeps only channels that have streams and are not blocked or closed`() {
        val catalog = assemble(
            blocked = listOf("Blocked.us"),
            streams = listOf(
                StreamDto(channel = "TVRI.id", url = "https://a/tvri.m3u8"),
                StreamDto(channel = "Blocked.us", url = "https://a/b.m3u8"),
                StreamDto(channel = "Closed.us", url = "https://a/c.m3u8"),
                StreamDto(channel = null, url = "https://a/orphan.m3u8"),
            ),
            channels = listOf(
                ChannelDto(id = "TVRI.id", name = "TVRI", country = "ID", categories = listOf("news")),
                ChannelDto(id = "Blocked.us", name = "Blocked", country = "US"),
                ChannelDto(id = "Closed.us", name = "Closed", country = "US", closed = "2020-01-01"),
                ChannelDto(id = "NoStream.us", name = "No stream", country = "US"),
            ),
        )
        assertEquals(listOf("TVRI.id"), catalog.channels.map { it.id })
        assertEquals(1, catalog.streams.size)
        assertEquals(",news,", catalog.channels.single().categories)
    }

    @Test
    fun `counts channels per country and category`() {
        val catalog = assemble(
            streams = listOf(
                StreamDto(channel = "A.id", url = "u1"),
                StreamDto(channel = "B.id", url = "u2"),
            ),
            channels = listOf(
                ChannelDto(id = "A.id", name = "A", country = "ID", categories = listOf("news")),
                ChannelDto(id = "B.id", name = "B", country = "ID", categories = listOf("news", "xxx")),
            ),
        )
        assertEquals(2, catalog.countries.first { it.code == "ID" }.channelCount)
        assertEquals(0, catalog.countries.first { it.code == "US" }.channelCount)
        assertEquals(2, catalog.categories.first { it.id == "news" }.channelCount)
        assertEquals(1, catalog.categories.first { it.id == "xxx" }.channelCount)
    }

    @Test
    fun `marks xxx category as nsfw even when flag is false`() {
        val catalog = assemble(
            streams = listOf(StreamDto(channel = "X.us", url = "u")),
            channels = listOf(ChannelDto(id = "X.us", name = "X", country = "US", categories = listOf("xxx"))),
        )
        assertTrue(catalog.channels.single().isNsfw)
    }

    @Test
    fun `picks main feed bitmap logo over feed or svg logos`() {
        val catalog = assemble(
            streams = listOf(StreamDto(channel = "A.id", url = "u")),
            logos = listOf(
                LogoDto(channel = "A.id", feed = "HD", width = 800, format = "PNG", url = "feed.png"),
                LogoDto(channel = "A.id", width = 800, format = "SVG", url = "main.svg"),
                LogoDto(channel = "A.id", width = 300, format = "PNG", url = "main.png"),
                LogoDto(channel = "Other.id", width = 300, format = "PNG", url = "other.png"),
            ),
            channels = listOf(ChannelDto(id = "A.id", name = "A", country = "ID")),
        )
        assertEquals("main.png", catalog.channels.single().logoUrl)
    }

    @Test
    fun `orders streams main feed first, then without warnings, then by quality`() {
        val catalog = assemble(
            streams = listOf(
                StreamDto(channel = "A.id", url = "geo", quality = "1080p", labels = listOf("Geo-blocked")),
                StreamDto(channel = "A.id", url = "sd", quality = "480p"),
                StreamDto(channel = "A.id", url = "feed", quality = "1080p", feed = "West"),
                StreamDto(channel = "A.id", url = "hd", quality = "720p"),
            ),
            channels = listOf(ChannelDto(id = "A.id", name = "A", country = "ID")),
        )
        val order = catalog.streams.sortedBy { it.sortOrder }.map { it.url }
        assertEquals(listOf("hd", "sd", "geo", "feed"), order)
    }

    @Test
    fun `quality value parses resolution`() {
        assertEquals(1080, CatalogAssembler.qualityValue("1080p"))
        assertEquals(0, CatalogAssembler.qualityValue(null))
        assertFalse(CatalogAssembler.qualityValue("abc") > 0)
    }
}
