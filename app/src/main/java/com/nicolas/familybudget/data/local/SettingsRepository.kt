package com.nicolas.familybudget.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "household_profile")

/** Appetit pour le risque : pondere l'allocation long terme du moteur de placement. */
enum class RiskAppetite(val label: String, val equityShareLongTerm: Double) {
    PRUDENT("Prudent", 0.30),
    EQUILIBRE("Equilibre", 0.55),
    DYNAMIQUE("Dynamique", 0.80),
}

/**
 * Profil du foyer saisi par l'utilisateur. Les depenses reelles sont, elles,
 * derivees des operations ; ici on stocke surtout les hypotheses declaratives.
 */
data class HouseholdProfile(
    val monthlyNetIncomeCents: Long = 0,
    val monthlyFixedChargesCents: Long = 0,
    val emergencyFundMonths: Int = 4,
    val riskAppetite: RiskAppetite = RiskAppetite.PRUDENT,
    val lepEligible: Boolean = false,
    val hasHighInterestDebt: Boolean = false,
    val onboardingDone: Boolean = false,
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val income = longPreferencesKey("monthly_net_income")
        val charges = longPreferencesKey("monthly_fixed_charges")
        val efMonths = intPreferencesKey("emergency_fund_months")
        val risk = stringPreferencesKey("risk_appetite")
        val lep = booleanPreferencesKey("lep_eligible")
        val debt = booleanPreferencesKey("high_interest_debt")
        val onboarding = booleanPreferencesKey("onboarding_done")
    }

    val profile: Flow<HouseholdProfile> = context.dataStore.data.map { p ->
        HouseholdProfile(
            monthlyNetIncomeCents = p[Keys.income] ?: 0,
            monthlyFixedChargesCents = p[Keys.charges] ?: 0,
            emergencyFundMonths = p[Keys.efMonths] ?: 4,
            riskAppetite = (p[Keys.risk]?.let { runCatching { RiskAppetite.valueOf(it) }.getOrNull() })
                ?: RiskAppetite.PRUDENT,
            lepEligible = p[Keys.lep] ?: false,
            hasHighInterestDebt = p[Keys.debt] ?: false,
            onboardingDone = p[Keys.onboarding] ?: false,
        )
    }

    suspend fun update(transform: (HouseholdProfile) -> HouseholdProfile) {
        context.dataStore.edit { p ->
            val current = HouseholdProfile(
                monthlyNetIncomeCents = p[Keys.income] ?: 0,
                monthlyFixedChargesCents = p[Keys.charges] ?: 0,
                emergencyFundMonths = p[Keys.efMonths] ?: 4,
                riskAppetite = (p[Keys.risk]?.let { runCatching { RiskAppetite.valueOf(it) }.getOrNull() })
                    ?: RiskAppetite.PRUDENT,
                lepEligible = p[Keys.lep] ?: false,
                hasHighInterestDebt = p[Keys.debt] ?: false,
                onboardingDone = p[Keys.onboarding] ?: false,
            )
            val next = transform(current)
            p[Keys.income] = next.monthlyNetIncomeCents
            p[Keys.charges] = next.monthlyFixedChargesCents
            p[Keys.efMonths] = next.emergencyFundMonths
            p[Keys.risk] = next.riskAppetite.name
            p[Keys.lep] = next.lepEligible
            p[Keys.debt] = next.hasHighInterestDebt
            p[Keys.onboarding] = next.onboardingDone
        }
    }
}
