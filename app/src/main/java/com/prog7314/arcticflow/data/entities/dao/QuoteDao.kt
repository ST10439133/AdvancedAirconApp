// app/src/main/java/com/prog7314/arcticflow/data/dao/QuoteDao.kt
package com.prog7314.arcticflow.data.dao

import androidx.room.*
import com.prog7314.arcticflow.data.entities.Quote
import com.prog7314.arcticflow.data.entities.QuoteStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface QuoteDao {
    @Insert
    suspend fun insertQuote(quote: Quote): Long

    @Update
    suspend fun updateQuote(quote: Quote)

    @Delete
    suspend fun deleteQuote(quote: Quote)

    @Query("SELECT * FROM quotes WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun getQuotesByCustomer(customerId: String): Flow<List<Quote>>

    @Query("SELECT * FROM quotes WHERE technicianId = :technicianId ORDER BY createdAt DESC")
    fun getQuotesByTechnician(technicianId: String): Flow<List<Quote>>

    @Query("SELECT * FROM quotes WHERE requestId = :requestId")
    suspend fun getQuotesByRequest(requestId: Int): Quote?

    @Query("SELECT * FROM quotes WHERE id = :quoteId")
    suspend fun getQuoteById(quoteId: Int): Quote?

    @Query("SELECT * FROM quotes WHERE status = :status ORDER BY createdAt DESC")
    fun getQuotesByStatus(status: QuoteStatus): Flow<List<Quote>>

    @Query("UPDATE quotes SET status = :status WHERE id = :quoteId")
    suspend fun updateQuoteStatus(quoteId: Int, status: QuoteStatus)

    // ===== NEW: needed by Manager DashboardViewModel =====
    @Query("SELECT * FROM quotes ORDER BY createdAt DESC")
    suspend fun getAllQuotesOnce(): List<Quote>
}