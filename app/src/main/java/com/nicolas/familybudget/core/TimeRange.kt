package com.nicolas.familybudget.core

import java.time.LocalDate
import java.time.ZoneId

object TimeRange {
    private fun startOfDayMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun currentMonth(): Pair<Long, Long> {
        val now = LocalDate.now()
        val first = now.withDayOfMonth(1)
        val firstNext = first.plusMonths(1)
        return startOfDayMillis(first) to (startOfDayMillis(firstNext) - 1)
    }
}
