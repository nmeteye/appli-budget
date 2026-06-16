package com.nicolas.familybudget.domain

import com.nicolas.familybudget.core.TimeRange
import com.nicolas.familybudget.data.local.BudgetBucket
import com.nicolas.familybudget.data.repository.AccountRepository
import com.nicolas.familybudget.data.repository.CategoryRepository
import com.nicolas.familybudget.data.repository.TransactionRepository
import com.nicolas.familybudget.domain.model.BudgetSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import kotlin.math.absoluteValue

class BudgetSummaryUseCase @Inject constructor(
    private val transactions: TransactionRepository,
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
) {
    fun observe(): Flow<BudgetSummary> {
        val (from, to) = TimeRange.currentMonth()
        return combine(
            transactions.observeIncomeBetween(from, to),
            transactions.observeExpenseByCategory(from, to),
            categories.observeAll(),
            accounts.observeActive(),
        ) { income, expenseByCat, cats, accs ->
            val bucketOf = cats.associate { it.id to it.bucket }

            var needs = 0L
            var wants = 0L
            var savings = 0L
            expenseByCat.forEach { spend ->
                val amount = spend.totalCents.absoluteValue
                when (bucketOf[spend.categoryId]) {
                    BudgetBucket.NEEDS -> needs += amount
                    BudgetBucket.SAVINGS -> savings += amount
                    else -> wants += amount // WANTS, INCOME mal classe, ou non categorise
                }
            }

            val liquid = accs.filter { it.type.isLiquidSavings }.sumOf { it.balanceCents }
            val netWorth = accs.sumOf { it.balanceCents }

            BudgetSummary(
                incomeCents = income,
                needsCents = needs,
                wantsCents = wants,
                savingsCents = savings,
                essentialMonthlyCents = needs,
                liquidSavingsCents = liquid,
                netWorthCents = netWorth,
            )
        }
    }
}
