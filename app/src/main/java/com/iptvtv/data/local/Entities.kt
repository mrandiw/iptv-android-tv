package com.iptvtv.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "channels",
    indices = [Index("country"), Index("name")],
)
data class ChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val country: String,
    /** Category ids wrapped in commas, e.g. ",news,sports,", so a LIKE '%,news,%' match is exact. */
    val categories: String,
    val isNsfw: Boolean,
    val logoUrl: String?,
    val network: String?,
    val website: String?,
    val streamCount: Int,
)

@Entity(
    tableName = "streams",
    indices = [Index("channelId")],
)
data class StreamEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val channelId: String,
    /** Lower plays first. */
    val sortOrder: Int,
    val url: String,
    val quality: String?,
    val userAgent: String?,
    val referrer: String?,
    /** Labels joined with "|", e.g. "Geo-blocked|Not 24/7". */
    val labels: String,
)

@Entity(tableName = "countries")
data class CountryEntity(
    @PrimaryKey val code: String,
    val name: String,
    val flag: String,
    val channelCount: Int,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val channelCount: Int,
)
