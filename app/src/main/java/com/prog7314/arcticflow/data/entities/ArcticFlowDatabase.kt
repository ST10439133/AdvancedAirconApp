package com.prog7314.arcticflow.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.prog7314.arcticflow.data.converters.DateConverter
import com.prog7314.arcticflow.data.dao.BrochureDao
import com.prog7314.arcticflow.data.dao.ProductDao
import com.prog7314.arcticflow.data.dao.UserDao
import com.prog7314.arcticflow.data.entities.Brochure
import com.prog7314.arcticflow.data.entities.Product
import com.prog7314.arcticflow.data.entities.User

@Database(
    entities = [Product::class, Brochure::class, User::class],
    version = 2,  // Increment version
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class ArcticFlowDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun brochureDao(): BrochureDao
    abstract fun userDao(): UserDao  // Add this

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
                    .fallbackToDestructiveMigration()  // Will recreate DB with new schema
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}