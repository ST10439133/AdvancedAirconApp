package com.insy7315.advancedairconapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.insy7315.advancedairconapp.BuildConfig
import com.insy7315.advancedairconapp.data.converters.DateConverter
import com.insy7315.advancedairconapp.data.converters.PartItemListConverter
import com.insy7315.advancedairconapp.data.converters.StringListConverter
import com.insy7315.advancedairconapp.data.dao.BrochureDao
import com.insy7315.advancedairconapp.data.dao.BuildingDao
import com.insy7315.advancedairconapp.data.dao.JobCardDao
import com.insy7315.advancedairconapp.data.dao.JobDao
import com.insy7315.advancedairconapp.data.dao.NotificationDao
import com.insy7315.advancedairconapp.data.dao.ProductDao
import com.insy7315.advancedairconapp.data.dao.QuoteDao
import com.insy7315.advancedairconapp.data.dao.ServiceRequestDao
import com.insy7315.advancedairconapp.data.dao.SyncQueueDao
import com.insy7315.advancedairconapp.data.dao.UserDao
import com.insy7315.advancedairconapp.data.entities.Brochure
import com.insy7315.advancedairconapp.data.entities.BuildingEntity
import com.insy7315.advancedairconapp.data.entities.Job
import com.insy7315.advancedairconapp.data.entities.JobCard
import com.insy7315.advancedairconapp.data.entities.Notification
import com.insy7315.advancedairconapp.data.entities.Product
import com.insy7315.advancedairconapp.data.entities.Quote
import com.insy7315.advancedairconapp.data.entities.ServiceRequest
import com.insy7315.advancedairconapp.data.entities.SyncQueueEntity
import com.insy7315.advancedairconapp.data.entities.User

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
    version = 15,
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

        /**
         * 14 → 15: adds the three decline columns to service_requests.
         */
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE service_requests ADD COLUMN declineReason TEXT")
                db.execSQL("ALTER TABLE service_requests ADD COLUMN declinedBy TEXT")
                db.execSQL("ALTER TABLE service_requests ADD COLUMN declinedAt INTEGER")
            }
        }

        fun getDatabase(context: Context): ArcticFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    ArcticFlowDatabase::class.java,
                    "arcticflow_database"
                ).addMigrations(MIGRATION_14_15)

                // Dev convenience only — keep destructive fallback in debug
                // builds so you can iterate on schema without writing a
                // migration every time.
                if (BuildConfig.DEBUG) {
                    builder.fallbackToDestructiveMigration()
                }

                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }
    }
}