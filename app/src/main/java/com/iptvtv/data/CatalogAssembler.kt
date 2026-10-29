package com.iptvtv.data

import com.iptvtv.data.local.CategoryEntity
import com.iptvtv.data.local.ChannelEntity
import com.iptvtv.data.local.CountryEntity
import com.iptvtv.data.local.StreamEntity
import com.iptvtv.data.remote.CategoryDto
import com.iptvtv.data.remote.ChannelDto
import com.iptvtv.data.remote.CountryDto
import com.iptvtv.data.remote.LogoDto
import com.iptvtv.data.remote.StreamDto
import com.iptvtv.model.NSFW_CATEGORY

/**
 * Joins the separate iptv-org API files into a playable catalog.
 *
 * Feed it in this order: blocklist, streams, logos, channels, then countries/categories.
 * Streams come first so we only keep logos and channels that can actually be played,
 * which keeps memory low (most channels in channels.json have no stream).
 *
 * Pure Kotlin with no Android dependencies, so it is unit tested directly.
 */
class CatalogAssembler {
    private val blocked = HashSet<String>()
    private val streamsByChannel = HashMap<String, MutableList<StreamDto>>()
    private val bestLogo = HashMap<String, LogoDto>()
    private val channels = ArrayList<ChannelEntity>()
    private val countries = ArrayList<CountryDto>()
    private val categories = ArrayList<CategoryDto>()

    fun addBlocked(channelId: String) {
        blocked += channelId
    }

    fun addStream(stream: StreamDto) {
        val channelId = stream.channel ?: return // Streams without a channel have no metadata; skip.
        if (channelId in blocked || stream.url.isBlank()) return
        streamsByChannel.getOrPut(channelId) { ArrayList(2) } += stream
    }

    fun addLogo(logo: LogoDto) {
        if (logo.channel !in streamsByChannel) return
        val current = bestLogo[logo.channel]
        if (current == null || logoScore(logo) > logoScore(current)) bestLogo[logo.channel] = logo
    }

    fun addChannel(dto: ChannelDto) {
        val streams = streamsByChannel[dto.id] ?: return
        if (dto.closed != null || dto.id in blocked) return
        channels += ChannelEntity(
            id = dto.id,
            name = dto.name,
            country = dto.country,
            categories = dto.categories.joinToString(",", prefix = ",", postfix = ","),
            isNsfw = dto.isNsfw || NSFW_CATEGORY in dto.categories,
            logoUrl = bestLogo[dto.id]?.url,
            network = dto.network,
            website = dto.website,
            streamCount = streams.size,
        )
    }

    fun addCountry(dto: CountryDto) {
        countries += dto
    }

    fun addCategory(dto: CategoryDto) {
        categories += dto
    }

    fun build(): Catalog {
        val keptIds = channels.mapTo(HashSet()) { it.id }
        val streamEntities = streamsByChannel
            .filterKeys { it in keptIds }
            .flatMap { (channelId, list) ->
                list.sortedWith(streamOrder).mapIndexed { index, s ->
                    StreamEntity(
                        channelId = channelId,
                        sortOrder = index,
                        url = s.url,
                        quality = s.quality,
                        userAgent = s.userAgent,
                        referrer = s.referrer,
                        labels = s.labels.joinToString("|"),
                    )
                }
            }

        val perCountry = channels.groupingBy { it.country }.eachCount()
        val perCategory = HashMap<String, Int>()
        channels.forEach { ch ->
            ch.categories.split(',').filter { it.isNotEmpty() }.forEach { perCategory.merge(it, 1, Int::plus) }
        }

        return Catalog(
            channels = channels.toList(),
            streams = streamEntities,
            countries = countries.map { CountryEntity(it.code, it.name, it.flag, perCountry[it.code] ?: 0) },
            categories = categories.map { CategoryEntity(it.id, it.name, perCategory[it.id] ?: 0) },
        )
    }

    data class Catalog(
        val channels: List<ChannelEntity>,
        val streams: List<StreamEntity>,
        val countries: List<CountryEntity>,
        val categories: List<CategoryEntity>,
    )

    companion object {
        /** Higher is better: main feed, in use, a bitmap format, then a reasonable size. */
        internal fun logoScore(logo: LogoDto): Int {
            var score = 0
            if (logo.feed == null) score += 4_000
            if (logo.inUse) score += 2_000
            if (!logo.format.equals("SVG", ignoreCase = true)) score += 1_000
            score += minOf(logo.width, 800)
            return score
        }

        internal fun qualityValue(quality: String?): Int =
            quality?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 0

        /** Main feed first, then streams without warnings, then higher resolution. */
        internal val streamOrder: Comparator<StreamDto> = compareBy<StreamDto> { it.feed != null }
            .thenBy { "Geo-blocked" in it.labels }
            .thenBy { "Not 24/7" in it.labels }
            .thenByDescending { qualityValue(it.quality) }
    }
}
