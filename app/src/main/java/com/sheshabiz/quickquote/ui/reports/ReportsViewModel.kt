package com.sheshabiz.quickquote.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sheshabiz.quickquote.data.db.entity.InvoiceStatus
import com.sheshabiz.quickquote.data.db.entity.QuoteStatus
import com.sheshabiz.quickquote.data.repository.BusinessRepository
import com.sheshabiz.quickquote.data.repository.InvoiceRepository
import com.sheshabiz.quickquote.data.repository.QuoteRepository
import com.sheshabiz.quickquote.data.repository.SaleRepository
import com.sheshabiz.quickquote.domain.PdfGenerator
import com.sheshabiz.quickquote.domain.model.ReportPdfData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import java.io.File
import java.util.Calendar

enum class ReportPeriod(val label: String) {
    TODAY("Today"),
    WEEK("This week"),
    MONTH("This month"),
    ALL("All time")
}

data class ReportsUiState(
    val period: ReportPeriod = ReportPeriod.MONTH,
    val salesTotal: Double = 0.0,
    val salesCount: Int = 0,
    val quotesCreated: Int = 0,
    val quotesAccepted: Int = 0,
    val quotesTotal: Double = 0.0,
    val invoicesPaidTotal: Double = 0.0,
    val invoicesPaidCount: Int = 0,
    val invoicesUnpaidTotal: Double = 0.0,
    val invoicesUnpaidCount: Int = 0,
    val invoicesOverdueCount: Int = 0,
    val isLoading: Boolean = true
)

class ReportsViewModel(
    private val saleRepository: SaleRepository,
    private val quoteRepository: QuoteRepository,
    private val invoiceRepository: InvoiceRepository,
    private val businessRepository: BusinessRepository,
    private val pdfGenerator: PdfGenerator
) : ViewModel() {

    private val period = MutableStateFlow(ReportPeriod.MONTH)

    val uiState: StateFlow<ReportsUiState> = combine(
        saleRepository.observeAll(),
        quoteRepository.observeAll(),
        invoiceRepository.observeAll(),
        period
    ) { sales, quotes, invoices, selectedPeriod ->
        val now = System.currentTimeMillis()
        val cutoff = cutoffFor(selectedPeriod, now)

        val salesInRange = sales.filter { it.createdAt >= cutoff }
        val quotesInRange = quotes.filter { it.createdAt >= cutoff }
        val invoicesInRange = invoices.filter { it.createdAt >= cutoff }
        val paidInvoices = invoicesInRange.filter { it.status == InvoiceStatus.PAID }
        val unpaidInvoices = invoicesInRange.filter { it.status == InvoiceStatus.UNPAID }

        ReportsUiState(
            period = selectedPeriod,
            salesTotal = salesInRange.sumOf { it.total },
            salesCount = salesInRange.size,
            quotesCreated = quotesInRange.size,
            quotesAccepted = quotesInRange.count { it.status == QuoteStatus.ACCEPTED },
            quotesTotal = quotesInRange.sumOf { it.total },
            invoicesPaidTotal = paidInvoices.sumOf { it.total },
            invoicesPaidCount = paidInvoices.size,
            invoicesUnpaidTotal = unpaidInvoices.sumOf { it.total },
            invoicesUnpaidCount = unpaidInvoices.size,
            invoicesOverdueCount = unpaidInvoices.count { it.dueDate < now },
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportsUiState())

    fun onPeriodChange(newPeriod: ReportPeriod) {
        period.value = newPeriod
    }

    /** Generates the printable PDF for whatever is currently on screen — null if the
     * business profile isn't set up yet, since the PDF header needs it. */
    suspend fun generatePdfFile(): File? {
        val profile = businessRepository.observeProfile().first() ?: return null
        val s = uiState.value
        val data = ReportPdfData(
            periodLabel = s.period.label,
            generatedAt = System.currentTimeMillis(),
            salesTotal = s.salesTotal,
            salesCount = s.salesCount,
            quotesCreated = s.quotesCreated,
            quotesAccepted = s.quotesAccepted,
            quotesTotal = s.quotesTotal,
            invoicesPaidTotal = s.invoicesPaidTotal,
            invoicesPaidCount = s.invoicesPaidCount,
            invoicesUnpaidTotal = s.invoicesUnpaidTotal,
            invoicesUnpaidCount = s.invoicesUnpaidCount,
            invoicesOverdueCount = s.invoicesOverdueCount
        )
        return pdfGenerator.generateReport(profile, data)
    }

    private fun cutoffFor(selectedPeriod: ReportPeriod, now: Long): Long = when (selectedPeriod) {
        ReportPeriod.TODAY -> startOfToday(now)
        ReportPeriod.WEEK -> now - 7L * 24 * 60 * 60 * 1000
        ReportPeriod.MONTH -> now - 30L * 24 * 60 * 60 * 1000
        ReportPeriod.ALL -> 0L
    }

    private fun startOfToday(now: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = now
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
