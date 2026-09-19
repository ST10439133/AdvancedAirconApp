package com.prog7314.arcticflow.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.prog7314.arcticflow.data.converters.DateConverter
import com.prog7314.arcticflow.data.converters.PartItemListConverter
import com.prog7314.arcticflow.data.converters.StringListConverter
import com.prog7314.arcticflow.data.dao.BrochureDao
import com.prog7314.arcticflow.data.dao.BuildingDao
import com.prog7314.arcticflow.data.dao.JobCardDao
import com.prog7314.arcticflow.data.dao.JobDao
import com.prog7314.arcticflow.data.dao.NotificationDao
import com.prog7314.arcticflow.data.dao.ProductDao
import com.prog7314.arcticflow.data.dao.QuoteDao
import com.prog7314.arcticflow.data.dao.ServiceRequestDao
import com.prog7314.arcticflow.data.dao.SyncQueueDao
import com.prog7314.arcticflow.data.dao.UserDao
import com.prog7314.arcticflow.data.entities.Brochure
import com.prog7314.arcticflow.data.entities.BuildingEntity
import com.prog7314.arcticflow.data.entities.Job
import com.prog7314.arcticflow.data.entities.JobCard
import com.prog7314.arcticflow.data.entities.Notification
import com.prog7314.arcticflow.data.entities.Product
import com.prog7314.arcticflow.data.entities.Quote
import com.prog7314.arcticflow.data.entities.ServiceRequest
import com.prog7314.arcticflow.data.entities.SyncQueueEntity
import com.prog7314.arcticflow.data.entities.User

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
        JobCard::class,
        SyncQueueEntity::class
    ],
    version = 9,
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
    abstract fun syncQueueDao(): SyncQueueDao

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