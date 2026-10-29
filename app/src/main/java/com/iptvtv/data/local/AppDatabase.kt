package com.iptvtv.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ChannelEntity::class, StreamEntity::class, CountryEntity::class, CategoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "iptv.db")
                // The DB is only a cache of remote data; it is safe to rebuild it on schema change.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
