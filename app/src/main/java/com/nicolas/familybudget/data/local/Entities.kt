package com.nicolas.familybudget.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** Type de compte, avec les produits d'epargne francais courants. */
enum class AccountType(val label: String, val isLiquidSavings: Boolean = false) {
    CURRENT("Compte courant"),
    CASH("Especes"),
    LIVRET_A("Livret A", isLiquidSavings = true),
    LDDS("LDDS", isLiquidSavings = true),
    LEP("LEP", isLiquidSavings = true),
    LIVRET_JEUNE("Livret Jeune", isLiquidSavings = true),
    SAVINGS("Autre livret", isLiquidSavings = true),
    PEA("PEA"),
    ASSURANCE_VIE("Assurance-vie"),
    PER("PER (retraite)"),
    SECURITIES("Compte-titres"),
    CREDIT_CARD("Carte / credit"),
}

enum class TransactionType { INCOME, EXPENSE, TRANSFER }

enum class CategoryKind { INCOME, EXPENSE }

/**
 * Besoins / Envies / Epargne : base de la regle 50/30/20 et du budget par enveloppes
 * (comptabilite mentale). Une categorie de depense est rangee dans l'un des trois seaux.
 */
enum class BudgetBucket(val label: String) {
    NEEDS("Besoins essentiels"),
    WANTS("Envies / confort"),
    SAVINGS("Epargne / dette"),
    INCOME("Revenu"),
}

enum class TransactionSource { MANUAL, IMPORT, SYNC }

/** Famille des objectifs : sert a deriver l'horizon de placement. */
enum class GoalType(val label: String) {
    EMERGENCY_FUND("Epargne de precaution"),
    REAL_ESTATE("Projet immobilier"),
    EDUCATION("Etudes des enfants"),
    RETIREMENT("Retraite"),
    CUSTOM("Objectif personnalise"),
}

enum class MemberRole { ADULT, CHILD }

@Entity(tableName = "accounts", indices = [Index(value = ["syncId"], unique = true)])
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    /** Solde courant en centimes. DERIVE : recalcule a partir des transactions,
     *  jamais synchronise (sinon divergence irreversible entre appareils). */
    val balanceCents: Long = 0,
    /** A qui appartient le compte : "Commun", prenom d'un membre, etc. */
    val ownerLabel: String = "Commun",
    val colorArgb: Int? = null,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    // --- Metadonnees de synchronisation multi-appareils ---
    /** Identite stable inter-appareils (le `id` Long reste local). */
    val syncId: String = UUID.randomUUID().toString(),
    /** Budget partage auquel ce compte appartient (null = pas encore synchronise). */
    val budgetId: String? = null,
    /** Horodatage de la derniere modification, pose par le client (LWW). */
    val updatedAt: Long = System.currentTimeMillis(),
    /** true = reste a pousser au serveur. */
    val isDirty: Boolean = true,
    /** Tombstone : supprime logiquement (jamais de DELETE physique pour propager). */
    val isDeleted: Boolean = false,
)

@Entity(
    tableName = "transactions",
    indices = [Index("accountId"), Index("categoryId"), Index("dateEpochMillis"), Index(value = ["syncId"], unique = true)]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    /** Montant signe en centimes : negatif = depense, positif = entree. */
    val amountCents: Long,
    val dateEpochMillis: Long,
    val categoryId: Long? = null,
    val label: String,
    val note: String? = null,
    val type: TransactionType,
    /** Pour relier les 2 lignes d'un virement entre comptes. */
    val transferGroupId: String? = null,
    val source: TransactionSource = TransactionSource.MANUAL,
    val createdAt: Long = System.currentTimeMillis(),
    // --- Metadonnees de synchronisation multi-appareils ---
    val syncId: String = UUID.randomUUID().toString(),
    val budgetId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = true,
    val isDeleted: Boolean = false,
)

@Entity(tableName = "categories", indices = [Index(value = ["syncId"], unique = true)])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: CategoryKind,
    val bucket: BudgetBucket,
    val emoji: String = "",
    /** Budget mensuel d'enveloppe en centimes (0 = pas de plafond). */
    val monthlyBudgetCents: Long = 0,
    // --- Metadonnees de synchronisation multi-appareils ---
    val syncId: String = UUID.randomUUID().toString(),
    val budgetId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = true,
    val isDeleted: Boolean = false,
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: GoalType,
    val targetAmountCents: Long,
    val currentAmountCents: Long = 0,
    /** Date cible (epoch millis) : sert a calculer l'horizon. Null = sans echeance. */
    val targetDateEpochMillis: Long? = null,
    val monthlyContributionCents: Long = 0,
    /** 1 = priorite maximale. */
    val priority: Int = 3,
    val linkedAccountId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val role: MemberRole,
    val birthYear: Int? = null,
    val notes: String? = null,
)
