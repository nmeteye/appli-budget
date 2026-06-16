package com.nicolas.familybudget

import com.nicolas.familybudget.domain.invest.CompoundInterestSimulator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompoundInterestSimulatorTest {

    private val sim = CompoundInterestSimulator()

    @Test
    fun zeroRate_isPlainSum() {
        // 0 % : capital final = capital initial + versements.
        val r = sim.project(initialCents = 100_000, monthlyContributionCents = 10_000, annualRate = 0.0, months = 12)
        assertEquals(100_000 + 10_000 * 12, r.futureValueCents)
        assertEquals(0L, r.gainCents)
    }

    @Test
    fun positiveRate_generatesGain() {
        val r = sim.project(initialCents = 100_000, monthlyContributionCents = 10_000, annualRate = 0.05, months = 120)
        assertTrue("le capital final doit depasser les versements", r.futureValueCents > r.totalContributedCents)
        assertTrue(r.gainCents > 0)
        assertEquals(121, r.monthlyBalances.size) // 0..120 inclus
    }

    @Test
    fun requiredContribution_reachesTarget() {
        val target = 2_000_000L // 20 000 €
        val months = 60
        val monthly = sim.requiredMonthlyContribution(target, initialCents = 0, annualRate = 0.03, months = months)
        val projected = sim.project(0, monthly, 0.03, months)
        // Tolerance d'arrondi : ~1 % de l'objectif.
        val diff = kotlin.math.abs(projected.futureValueCents - target)
        assertTrue("ecart trop grand: $diff", diff < target / 100)
    }
}
