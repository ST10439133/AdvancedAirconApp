package com.insy7315.advancedaircornapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.insy7315.advancedaircornapp.data.converters.DateConverter
import com.insy7315.advancedaircornapp.data.converters.PartItemListConverter
import com.insy7315.advancedaircornapp.data.converters.StringListConverter
import com.insy7315.advancedaircornapp.data.dao.BrochureDao
import com.insy7315.advancedaircornapp.data.dao.BuildingDao
import com.insy7315.advancedaircornapp.data.dao.JobCardDao
import com.insy7315.advancedaircornapp.data.dao.JobDao
import com.insy7315.advancedaircornapp.data.dao.NotificationDao
import com.insy7315.advancedaircornapp.data.dao.ProductDao
import com.insy7315.advancedaircornapp.data.dao.QuoteDao
import com.insy7315.advancedaircornapp.data.dao.ServiceRequestDao
import com.insy7315.advancedaircornapp.data.dao.SyncQueueDao
import com.insy7315.advancedaircornapp.data.dao.UserDao
import com.insy7315.advancedaircornapp.data.entities.Brochure
import com.insy7315.advancedaircornapp.data.entities.BuildingEntity
import com.insy7315.advancedaircornapp.data.entities.Job
import com.insy7315.advancedaircornapp.data.entities.JobCard
import com.insy7315.advancedaircornapp.data.entities.Notification
import com.insy7315.advancedaircornapp.data.entities.Product
import com.insy7315.advancedaircornapp.data.entities.Quote
import com.insy7315.advancedaircornapp.data.entities.ServiceRequest
import com.insy7315.advancedaircornapp.data.entities.SyncQueueEntity
import com.insy7315.advancedaircornapp.data.entities.User

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