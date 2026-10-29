package com.iptvtv.data

import com.iptvtv.data.local.CategoryEntity
import com.iptvtv.data.local.ChannelEntity
import com.iptvtv.data.local.CountryEntity
import com.iptvtv.data.local.StreamEntity
import com.iptvtv.model.Category
import com.iptvtv.model.Channel
import com.iptvtv.model.Country
import com.iptvtv.model.Stream

fun ChannelEntity.toModel() = Channel(
    id = id,
    name = name,
    countryCode = country,
    categories = categories.split(',').filter { it.isNotEmpty() },
    isNsfw = isNsfw,
    logoUrl = logoUrl,
    network = network,
    website = website,
    streamCount = streamCount,
)

fun StreamEntity.toModel() = Stream(
    channelId = channelId,
    url = url,
    quality = quality,
    userAgent = userAgent,
    referrer = referrer,
    labels = labels.split('|').filter { it.isNotEmpty() },
)

fun CountryEntity.toModel() = Country(code, name, flag, channelCount)

fun CategoryEntity.toModel() = Category(id, name, channelCount)
