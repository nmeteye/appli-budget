package com.nicolas.familybudget.data.sync.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/* ============================================================
 *  DTO miroir des tables Postgres (snake_case via @SerialName).
 *  Convention : updated_at est un epoch millis (bigint cote SQL),
 *  ce qui simplifie le last-write-wins (pas de parsing de date).
 *  Les references inter-tables se font par sync_id, JAMAIS par l'id
 *  Long local (qui differe d'un appareil a l'autre).
 *  Le solde des comptes n'est PAS transmis : il est derive localement.
 * ============================================================ */

@Serializable
data class RemoteBudget(
    val id: String,
    val name: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("updated_at") val updatedAt: Long = 0,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
)

@Serializable
data class RemoteAccount(
    @SerialName("sync_id") val syncId: String,
    @SerialName("budget_id") val budgetId: String,
    val name: String,
    val type: String,
    @SerialName("owner_label") val ownerLabel: String,
    @SerialName("color_argb") val colorArgb: Int? = null,
    val archived: Boolean,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("is_deleted") val isDeleted: Boolean,
)

@Serializable
data class RemoteCategory(
    @SerialName("sync_id") val syncId: String,
    @SerialName("budget_id") val budgetId: String,
    val name: String,
    val kind: String,
    val bucket: String,
    val emoji: String,
    @SerialName("monthly_budget_cents") val monthlyBudgetCents: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("is_deleted") val isDeleted: Boolean,
)

@Serializable
data class RemoteTransaction(
    @SerialName("sync_id") val syncId: String,
    @SerialName("budget_id") val budgetId: String,
    @SerialName("account_sync_id") val accountSyncId: String,
    @SerialName("category_sync_id") val categorySyncId: String? = null,
    @SerialName("amount_cents") val amountCents: Long,
    @SerialName("date_epoch_millis") val dateEpochMillis: Long,
    val label: String,
    val note: String? = null,
    val type: String,
    @SerialName("transfer_group_id") val transferGroupId: String? = null,
    val source: String,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("is_deleted") val isDeleted: Boolean,
)
