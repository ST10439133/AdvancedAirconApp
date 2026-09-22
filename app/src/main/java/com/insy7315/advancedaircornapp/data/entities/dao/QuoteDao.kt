package com.insy7315.advancedaircornapp.data.dao

import androidx.room.*
import com.insy7315.advancedaircornapp.data.entities.Quote
import com.insy7315.advancedaircornapp.data.entities.QuoteStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface QuoteDao {

    @Insert
    suspend fun insertQuote(quote: Quote): Long

    @Update
    suspend fun updateQuote(quote: Quote)

    @Delete
    suspend fun deleteQuote(quote: Quote)

    // Reactive queries

    @Query("SELECT * FROM quotes WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun getQuotesByCustomer(customerId: String): Flow<List<Quote>>

    @Query("SELECT * FROM quotes WHERE technicianId = :technicianId ORDER BY createdAt DESC")
    fun getQuotesByTechnician(technicianId: String): Flow<List<Quote>>

    @Query("SELECT * FROM quotes WHERE status = :status ORDER BY createdAt DESC")
    fun getQuotesByStatus(status: QuoteStatus): Flow<List<Quote>>

    @Query("SELECT * FROM quotes ORDER BY createdAt DESC")
    fun getAllQuotesFlow(): Flow<List<Quote>>

    @Query("SELECT * FROM quotes WHERE requestId IN (:requestIds) ORDER BY createdAt DESC")
    fun getQuotesByRequestIds(requestIds: List<Int>): Flow<List<Quote>>

    @Query("SELECT * FROM quotes WHERE requestId = :requestId")
    fun observeQuotesByRequest(requestId: Int): Flow<List<Quote>>

    // One-shot queries

    @Query("SELECT * FROM quotes WHERE id = :quoteId")
    suspend fun getQuoteById(quoteId: Int): Quote?

    @Query("SELECT * FROM quotes WHERE requestId = :requestId LIMIT 1")
    suspend fun getQuotesByRequest(requestId: Int): Quote?

    @Query("UPDATE quotes SET status = :status WHERE id = :quoteId")
    suspend fun updateQuoteStatus(quoteId: Int, status: QuoteStatus)

    @Query("SELECT * FROM quotes ORDER BY createdAt DESC")
    suspend fun getAllQuotesOnce(): List<Quote>
}