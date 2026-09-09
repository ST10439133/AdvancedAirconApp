// app/src/main/java/com/prog7314/arcticflow/data/ArcticFlowDatabase.kt
package com.prog7314.arcticflow.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.prog7314.arcticflow.data.converters.DateConverter
import com.prog7314.arcticflow.data.converters.PartItemListConverter
import com.prog7314.arcticflow.data.converters.StringListConverter
import com.prog7314.arcticflow.data.dao.*
import com.prog7314.arcticflow.data.entities.*

@Database(
    entities = [
        Product::class,
        Brochure::class,
        User::class,
        BuildingEntity::class,
        ServiceRequest::class,
        Quote::class,
        Job::class,
        Notification::class,
        JobCard::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(
    DateConverter::class,
    PartItemListConverter::class,
    StringListConverter::class
)
abstract class ArcticFlowDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun brochureDao(): BrochureDao
    abstract fun userDao(): UserDao
    abstract fun buildingDao(): BuildingDao
    abstract fun serviceRequestDao(): ServiceRequestDao
    abstract fun quoteDao(): QuoteDao
    abstract fun jobDao(): JobDao
    abstract fun notificationDao(): NotificationDao
    abstract fun jobCardDao(): JobCardDao

    companion object {
        @Volatile
        private var INSTANCE: ArcticFlowDatabase? = null

        fun getDatabase(context: Context): ArcticFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ArcticFlowDatabase::class.java,
                    "arcticflow_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}