package com.nicolas.budgetcouple.data

import kotlinx.serialization.Serializable

enum class TxType { INCOME, EXPENSE }

@Serializable
data class Account(
    val id: String,
    val name: String,
    val owner: String,
    val colorArgb: Long,
    val balance: Double
)

@Serializable
data class Tx(
    val id: String,
    val accountId: String,
    val type: TxType,
    val category: String,
    val label: String,
    val amount: Double
)

@Serializable
data class AppState(
    val accounts: List<Account> = defaultAccounts(),
    val transactions: List<Tx> = defaultTransactions(),
    val savingsGoal: Double = 0.20
)

object Categories {
    val income = listOf("Salaire", "Prime", "Autre revenu")
    val expense = listOf(
        "Logement", "Courses", "Transport", "Loisirs",
        "Santé", "Abonnements", "Restaurant", "Divers"
    )
    val needs = setOf("Logement", "Courses", "Transport", "Santé")
}

fun defaultAccounts() = listOf(
    Account("a1", "Compte commun", "Couple", 0xFF5C7A6BL, 2400.0),
    Account("a2", "Compte d'Alex", "Alex", 0xFFC08A3EL, 1850.0),
    Account("a3", "Compte de Sam", "Sam", 0xFFA8443AL, 1320.0),
)

fun defaultTransactions() = listOf(
    Tx("t1", "a1", TxType.INCOME, "Salaire", "Salaire Alex", 2600.0),
    Tx("t2", "a1", TxType.INCOME, "Salaire", "Salaire Sam", 2200.0),
    Tx("t3", "a1", TxType.EXPENSE, "Logement", "Loyer", 1250.0),
    Tx("t4", "a1", TxType.EXPENSE, "Courses", "Supermarché", 480.0),
    Tx("t5", "a1", TxType.EXPENSE, "Abonnements", "Streaming + box", 65.0),
    Tx("t6", "a2", TxType.EXPENSE, "Restaurant", "Resto / midi", 220.0),
    Tx("t7", "a3", TxType.EXPENSE, "Loisirs", "Sport + sorties", 140.0),
    Tx("t8", "a2", TxType.EXPENSE, "Transport", "Essence", 130.0),
)

/** Agrégats calculés à la volée. */
data class Totals(
    val income: Double,
    val expense: Double,
    val net: Double,
    val balance: Double,
    val rate: Double
)

fun AppState.totals(): Totals {
    val income = transactions.filter { it.type == TxType.INCOME }.sumOf { it.amount }
    val expense = transactions.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
    val balance = accounts.sumOf { it.balance }
    return Totals(
        income = income,
        expense = expense,
        net = income - expense,
        balance = balance,
        rate = if (income > 0) (income - expense) / income else 0.0
    )
}

fun AppState.expensesByCategory(): List<Pair<String, Double>> =
    transactions.filter { it.type == TxType.EXPENSE }
        .groupBy { it.category }
        .map { (c, list) -> c to list.sumOf { it.amount } }
        .sortedByDescending { it.second }

/** Capital futur d'un versement mensuel constant (intérêts composés). */
fun futureValue(monthly: Double, years: Int, annualRate: Double): Double {
    val r = annualRate / 12.0
    val n = years * 12
    return if (r == 0.0) monthly * n
    else monthly * ((Math.pow(1 + r, n.toDouble()) - 1) / r)
}
