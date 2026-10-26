package com.insy7315.advancedairconapp.data.dao

import androidx.room.*
import com.insy7315.advancedairconapp.data.entities.Quote
import com.insy7315.advancedairconapp.data.entities.QuoteStatus
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

    /**
     * Returns every quote the customer should see.
     *
     * A quote belongs to the customer if EITHER:
     *   - the quote's customerId matches, OR
     *   - the quote is attached to a request this customer owns.
     */
    @Query("""
        SELECT q.* FROM quotes q
        LEFT JOIN service_requests r ON r.id = q.requestId
        WHERE q.customerId = :customerId
           OR r.userId = :customerId
        ORDER BY q.createdAt DESC
    """)
    fun getQuotesVisibleToCustomer(customerId: String): Flow<List<Quote>>

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

    @Query("SELECT * FROM quotes WHERE id = :quoteId")
    suspend fun getQuoteById(quoteId: Int): Quote?

    @Query("SELECT * FROM quotes WHERE serverId = :serverId LIMIT 1")
    suspend fun getQuoteByServerId(serverId: Int): Quote?

    @Query("SELECT * FROM quotes WHERE requestId = :requestId LIMIT 1")
    suspend fun getQuotesByRequest(requestId: Int): Quote?

    @Query("UPDATE quotes SET status = :status, updatedAt = :updatedAt WHERE id = :quoteId")
    suspend fun updateQuoteStatus(
        quoteId: Int,
        status: QuoteStatus,
        updatedAt: Long = System.currentTimeMillis()
    )

    // callers that specifically mean "this is the payment finalizer"
    @Query("UPDATE quotes SET status = :status, updatedAt = :updatedAt WHERE id = :quoteId")
    suspend fun updateQuoteStatusAndTotal(
        quoteId: Int,
        status: QuoteStatus,
        updatedAt: Long = System.currentTimeMillis()
    )

    // update pricing (discounts / adjustments)
    @Query("""
        UPDATE quotes 
        SET grandTotal = :grandTotal, 
            discountType = :discountType,
            discountValue = :discountValue,
            discountAmount = :discountAmount,
            finalTotal = :finalTotal,
            notes = :notes,
            updatedAt = :updatedAt
        WHERE id = :quoteId
    """)
    suspend fun updateQuotePricing(
        quoteId: Int,
        grandTotal: Double,
        discountType: String,
        discountValue: Double,
        discountAmount: Double,
        finalTotal: Double,
        notes: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("SELECT * FROM quotes ORDER BY createdAt DESC")
    suspend fun getAllQuotesOnce(): List<Quote>

    @Query("UPDATE quotes SET serverId = :serverId WHERE id = :localId")
    suspend fun setServerId(localId: Int, serverId: Int)


    // PAYMENT LIFECYCLE


    // Record a successful payment against a quote and flip to PAID.
    @Query("""
        UPDATE quotes
        SET status = 'PAID',
            paymentReference = :reference,
            paidAt = :paidAt,
            updatedAt = :paidAt
        WHERE id = :quoteId
    """)
    suspend fun markPaid(
        quoteId: Int,
        reference: String,
        paidAt: Long = System.currentTimeMillis()
    )

    // Flip a quote to AWAITING_PAYMENT after the job card is submitted.
    @Query("UPDATE quotes SET status = 'AWAITING_PAYMENT', updatedAt = :updatedAt WHERE id = :quoteId")
    suspend fun markAwaitingPayment(
        quoteId: Int,
        updatedAt: Long = System.currentTimeMillis()
    )
}