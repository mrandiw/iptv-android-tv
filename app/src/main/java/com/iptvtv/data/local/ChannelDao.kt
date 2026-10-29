package com.iptvtv.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface ChannelDao {

    @Query("SELECT COUNT(*) FROM channels")
    suspend fun channelCount(): Int

    @Query("SELECT * FROM channels WHERE id = :id")
    suspend fun channel(id: String): ChannelEntity?

    @Query(
        """
        SELECT * FROM channels
        WHERE country = :country AND (:includeNsfw OR isNsfw = 0)
        ORDER BY (logoUrl IS NULL), name COLLATE NOCASE
        LIMIT :limit
        """
    )
    suspend fun byCountry(country: String, includeNsfw: Boolean, limit: Int): List<ChannelEntity>

    @Query(
        """
        SELECT * FROM channels
        WHERE categories LIKE '%,' || :category || ',%' AND (:includeNsfw OR isNsfw = 0)
        ORDER BY (logoUrl IS NULL), streamCount DESC, name COLLATE NOCASE
        LIMIT :limit
        """
    )
    suspend fun byCategory(category: String, includeNsfw: Boolean, limit: Int): List<ChannelEntity>

    @Query(
        """
        SELECT * FROM channels
        WHERE (name LIKE '%' || :query || '%' OR network LIKE '%' || :query || '%')
          AND (:includeNsfw OR isNsfw = 0)
        ORDER BY (name LIKE :query || '%') DESC, name COLLATE NOCASE
        LIMIT :limit
        """
    )
    suspend fun search(query: String, includeNsfw: Boolean, limit: Int): List<ChannelEntity>

    @Query("SELECT * FROM streams WHERE channelId = :channelId ORDER BY sortOrder")
    suspend fun streams(channelId: String): List<StreamEntity>

    @Query("SELECT * FROM countries WHERE channelCount > 0 ORDER BY name COLLATE NOCASE")
    suspend fun countries(): List<CountryEntity>

    @Query("SELECT * FROM countries WHERE code = :code")
    suspend fun country(code: String): CountryEntity?

    @Query("SELECT * FROM categories WHERE channelCount > 0 ORDER BY channelCount DESC")
    suspend fun categories(): List<CategoryEntity>

    @Insert suspend fun insertChannels(items: List<ChannelEntity>)
    @Insert suspend fun insertStreams(items: List<StreamEntity>)
    @Insert suspend fun insertCountries(items: List<CountryEntity>)
    @Insert suspend fun insertCategories(items: List<CategoryEntity>)

    @Query("DELETE FROM channels") suspend fun clearChannels()
    @Query("DELETE FROM streams") suspend fun clearStreams()
    @Query("DELETE FROM countries") suspend fun clearCountries()
    @Query("DELETE FROM categories") suspend fun clearCategories()

    /** Replaces the whole catalog atomically, so readers never see a half-written state. */
    @Transaction
    suspend fun replaceAll(
        channels: List<ChannelEntity>,
        streams: List<StreamEntity>,
        countries: List<CountryEntity>,
        categories: List<CategoryEntity>,
    ) {
        clearStreams()
        clearChannels()
        clearCountries()
        clearCategories()
        insertChannels(channels)
        insertStreams(streams)
        insertCountries(countries)
        insertCategories(categories)
    }
}
