package com.nicolas.familybudget.data.sync

import com.nicolas.familybudget.data.local.AccountType
import com.nicolas.familybudget.data.local.TransactionType

/** Compte distant renvoye par un agregateur. */
data class RemoteAccount(
    val externalId: String,
    val name: String,
    val type: AccountType,
    val balanceCents: Long,
)

data class RemoteTransaction(
    val externalId: String,
    val accountExternalId: String,
    val amountCents: Long,
    val dateEpochMillis: Long,
    val label: String,
    val type: TransactionType,
)

/**
 * Abstraction de synchro bancaire (DSP2 / Open Banking).
 *
 * On NE peut PAS embarquer de credentials d'agregateur dans une app open source :
 * l'enregistrement DSP2 (agent / TPP) et les cles client sont propres a ton compte.
 * Branche ici ton agregateur (Powens/Budget Insight, Bridge, Tink, GoCardless...)
 * en implementant cette interface, puis fournis ton implementation dans le module Hilt.
 */
interface BankSyncProvider {
    val displayName: String
    suspend fun fetchAccounts(): List<RemoteAccount>
    suspend fun fetchTransactions(sinceEpochMillis: Long): List<RemoteTransaction>
}

/** Provider de demonstration : donnees fictives, aucun reseau. Defaut hors-ligne. */
class MockBankSyncProvider : BankSyncProvider {
    override val displayName: String = "Demo (hors-ligne)"

    override suspend fun fetchAccounts(): List<RemoteAccount> = listOf(
        RemoteAccount("demo-courant", "Compte courant (demo)", AccountType.CURRENT, 152_340),
        RemoteAccount("demo-livreta", "Livret A (demo)", AccountType.LIVRET_A, 540_000),
    )

    override suspend fun fetchTransactions(sinceEpochMillis: Long): List<RemoteTransaction> {
        val now = System.currentTimeMillis()
        return listOf(
            RemoteTransaction("d1", "demo-courant", -4_590, now, "Supermarche", TransactionType.EXPENSE),
            RemoteTransaction("d2", "demo-courant", -1_290, now, "Abonnement", TransactionType.EXPENSE),
            RemoteTransaction("d3", "demo-courant", 245_000, now, "Salaire", TransactionType.INCOME),
        )
    }
}

/**
 * Squelette d'integration d'un agregateur reel (ex. Powens).
 * Renseigne baseUrl + token (obtenus via ton flux OAuth / webview de connexion),
 * puis implemente les appels HTTP (Retrofit/Ktor) dans fetchAccounts/fetchTransactions.
 *
 * Etapes typiques :
 *  1. Cote serveur (le tien) : enregistrement TPP + obtention client_id/secret.
 *  2. App : ouverture de la webview de connexion bancaire de l'agregateur.
 *  3. App : recuperation d'un access_token utilisateur.
 *  4. Appels REST : GET /users/me/accounts, GET /users/me/transactions.
 */
class PowensBankSyncProvider(
    private val baseUrl: String,
    private val accessTokenProvider: suspend () -> String,
) : BankSyncProvider {
    override val displayName: String = "Powens (DSP2)"

    override suspend fun fetchAccounts(): List<RemoteAccount> {
        TODO("Implementer l'appel REST GET $baseUrl/users/me/accounts avec le bearer token")
    }

    override suspend fun fetchTransactions(sinceEpochMillis: Long): List<RemoteTransaction> {
        TODO("Implementer GET $baseUrl/users/me/transactions?min_date=...")
    }
}
