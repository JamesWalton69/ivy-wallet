package com.ivy.legacy.data.model

import androidx.compose.runtime.Immutable
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.legacy.IvyWalletCtx
import com.ivy.legacy.utils.atEndOfDay
import com.ivy.legacy.utils.dateNowLocal
import com.ivy.legacy.utils.dateNowUTC
import com.ivy.legacy.utils.endOfMonth
import com.ivy.legacy.utils.formatLocal
import com.ivy.legacy.utils.startOfMonth
import com.ivy.legacy.utils.withDayOfMonthSafe
import com.ivy.ui.time.TimeFormatter
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

private const val MonthNameAbbreviationLength = 3

enum class PeriodType {
    DAY,
    WEEK,
    MONTH,
    CUSTOM
}

@Suppress("DataClassFunctions")
@Immutable
data class TimePeriod(
    val month: Month? = null,
    val year: Int? = null,
    val fromToRange: FromToTimeRange? = null,
    val lastNRange: LastNTimeRange? = null,
    val day: LocalDate? = null,
    val weekStart: LocalDate? = null,
) {
    val periodType: PeriodType
        get() = when {
            day != null -> PeriodType.DAY
            weekStart != null -> PeriodType.WEEK
            month != null -> PeriodType.MONTH
            else -> PeriodType.CUSTOM
        }

    companion object {
        fun today(timeProvider: TimeProvider? = null): TimePeriod {
            val date = timeProvider?.localNow()?.toLocalDate() ?: dateNowLocal()
            return TimePeriod(day = date)
        }

        fun currentWeek(timeProvider: TimeProvider? = null): TimePeriod {
            val date = timeProvider?.localNow()?.toLocalDate() ?: dateNowLocal()
            val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            return TimePeriod(weekStart = monday)
        }

        /**
         * Examples:
         * 1. startDateOfMonth = 1, today = Nov. 10
         * return Nov. 1 - Nov. 30
         *
         * 2. startDateOfMonth = 10, today = Nov. 9
         * return Oct. 10 - Nov. 9
         *
         * 3. startDateOfMonth = 10, today = Nov. 10
         * return Nov. 10 - Dec. 9
         */
        fun currentMonth(startDayOfMonth: Int): TimePeriod {
            val dateNowUTC = dateNowUTC()
            val dayToday = dateNowUTC.dayOfMonth

            // Examples month = Nov. startDate = 7; Period = from Nov (7) till Dec (6)
            // => new period starts if today => startDayOfMonth
            val newPeriodStarted = dayToday >= startDayOfMonth

            val periodDate = if (newPeriodStarted) {
                // new monthly period has already started then observe it => current month
                dateNowUTC
            } else {
                // new monthly period hasn't yet started then observe the ongoing one => previous month
                dateNowUTC.minusMonths(1)
            }

            return TimePeriod(
                month = Month.fromMonthValue(
                    periodDate.monthValue
                ),
                year = periodDate.year
            )
        }
    }

    fun isValid(): Boolean =
        day != null || weekStart != null || month != null || fromToRange != null || lastNRange != null

    fun toRange(
        startDateOfMonth: Int,
        timeConverter: TimeConverter,
        timeProvider: TimeProvider,
    ): FromToTimeRange = with(timeConverter) {
        when {
            day != null -> {
                val from = day.atStartOfDay().toUTC()
                val to = day.atTime(LocalTime.MAX).toUTC()
                FromToTimeRange(
                    from = from,
                    to = to
                )
            }

            weekStart != null -> {
                val from = weekStart.atStartOfDay().toUTC()
                val weekEnd = weekStart.plusDays(6)
                val to = weekEnd.atTime(LocalTime.MAX).toUTC()
                FromToTimeRange(
                    from = from,
                    to = to
                )
            }

            month != null -> {
                val date = if (year != null) month.toDate().withYear(year) else month.toDate()
                val (from, to) = if (startDateOfMonth != 1) {
                    customStartDayOfMonthPeriodRange(
                        date = date,
                        startDateOfMonth = startDateOfMonth,
                        timeConverter = timeConverter,
                    )
                } else {
                    Pair(startOfMonth(date, timeConverter), endOfMonth(date, timeConverter))
                }

                FromToTimeRange(
                    from = from,
                    to = to
                )
            }

            fromToRange != null -> {
                fromToRange
            }

            lastNRange != null -> {
                FromToTimeRange(
                    from = lastNRange.fromDate(timeProvider),
                    to = timeProvider.utcNow()
                )
            }

            else -> {
                val date = dateNowUTC()
                FromToTimeRange(
                    from = startOfMonth(date, timeConverter),
                    to = endOfMonth(date, timeConverter)
                )
            }
        }
    }

    private fun customStartDayOfMonthPeriodRange(
        date: LocalDate,
        startDateOfMonth: Int,
        timeConverter: TimeConverter,
    ): Pair<Instant, Instant> = with(timeConverter) {
        val from = date
            .withDayOfMonthSafe(startDateOfMonth)
            .atStartOfDay()
            .toUTC()

        val to = date
            // startDayOfMonth != 1 just shift N day the month forward so to should +1 month
            .plusMonths(1)
            .withDayOfMonthSafe(startDateOfMonth)
            // e.g. Correct: 14.10-13.11 (Incorrect: 14.10-14.11)
            .minusDays(1)
            .atEndOfDay()
            .toUTC()

        from to to
    }

    fun toDisplayShort(
        startDateOfMonth: Int,
        timeConverter: TimeConverter,
        timeProvider: TimeProvider,
        timeFormatter: TimeFormatter,
    ): String = with(timeFormatter) {
        when {
            day != null -> {
                val today = timeProvider.localNow().toLocalDate()
                when (day) {
                    today -> "Today"
                    today.minusDays(1) -> "Yesterday"
                    today.plusDays(1) -> "Tomorrow"
                    else -> {
                        val pattern = if (today.year == day.year) "MMM dd" else "MMM dd, yyyy"
                        day.formatLocal(pattern)
                    }
                }
            }

            weekStart != null -> {
                val today = timeProvider.localNow().toLocalDate()
                val currentMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                if (weekStart == currentMonday) {
                    "This Week"
                } else if (weekStart == currentMonday.minusWeeks(1)) {
                    "Last Week"
                } else {
                    val weekEnd = weekStart.plusDays(6)
                    "${weekStart.formatLocal("MMM d")} - ${weekEnd.formatLocal("MMM d")}"
                }
            }

            month != null -> {
                if (startDateOfMonth == 1) {
                    displayMonthStartingOn1st(month = month, timeProvider)
                } else {
                    val range = toRange(
                        startDateOfMonth = startDateOfMonth,
                        timeConverter = timeConverter,
                        timeProvider = timeProvider,
                    )
                    val style = TimeFormatter.Style.DateOnly(includeWeekDay = false)
                    "${range.from?.formatLocal(style)} - ${range.to?.formatLocal(style)}"
                }
            }

            fromToRange != null -> {
                fromToRange.toDisplay(timeFormatter)
            }

            lastNRange != null -> {
                "Last ${lastNRange.forDisplay()}"
            }

            else -> "Custom"
        }
    }

    fun toDisplayLong(
        startDateOfMonth: Int,
        timeProvider: TimeProvider,
        timeConverter: TimeConverter,
        timeFormatter: TimeFormatter
    ): String {
        return when {
            day != null -> {
                val today = timeProvider.localNow().toLocalDate()
                when (day) {
                    today -> "Today"
                    today.minusDays(1) -> "Yesterday"
                    today.plusDays(1) -> "Tomorrow"
                    else -> {
                        val pattern = if (today.year == day.year) "MMM dd" else "MMM dd, yyyy"
                        day.formatLocal(pattern)
                    }
                }
            }

            weekStart != null -> {
                val today = timeProvider.localNow().toLocalDate()
                val currentMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                if (weekStart == currentMonday) {
                    "This Week"
                } else {
                    val weekEnd = weekStart.plusDays(6)
                    "${weekStart.formatLocal("MMM d")} - ${weekEnd.formatLocal("MMM d")}"
                }
            }

            month != null -> {
                if (startDateOfMonth == 1) {
                    displayMonthStartingOn1st(month = month, timeProvider)
                } else {
                    toRange(
                        startDateOfMonth = startDateOfMonth,
                        timeConverter = timeConverter,
                        timeProvider = timeProvider
                    ).toDisplay(timeFormatter)
                }
            }

            fromToRange != null -> {
                fromToRange.toDisplay(timeFormatter)
            }

            lastNRange != null -> {
                "the last ${lastNRange.forDisplay()}"
            }

            else -> {
                toRange(
                    startDateOfMonth = startDateOfMonth,
                    timeConverter = timeConverter,
                    timeProvider = timeProvider
                ).toDisplay(timeFormatter)
            }
        }
    }

    fun next(
        ivyContext: IvyWalletCtx,
        startDayOfMonth: Int,
    ): TimePeriod {
        return when {
            day != null -> {
                val nextDay = day.plusDays(1)
                val newPeriod = TimePeriod(day = nextDay)
                ivyContext.updateSelectedPeriodInMemory(newPeriod)
                newPeriod
            }
            weekStart != null -> {
                val nextWeek = weekStart.plusWeeks(1)
                val newPeriod = TimePeriod(weekStart = nextWeek)
                ivyContext.updateSelectedPeriodInMemory(newPeriod)
                newPeriod
            }
            month != null -> {
                val year = year ?: dateNowUTC().year
                month.incrementMonthPeriod(ivyContext, 1L, year = year)
            }
            else -> this
        }
    }

    fun previous(
        ivyContext: IvyWalletCtx,
        startDayOfMonth: Int,
    ): TimePeriod {
        return when {
            day != null -> {
                val prevDay = day.minusDays(1)
                val newPeriod = TimePeriod(day = prevDay)
                ivyContext.updateSelectedPeriodInMemory(newPeriod)
                newPeriod
            }
            weekStart != null -> {
                val prevWeek = weekStart.minusWeeks(1)
                val newPeriod = TimePeriod(weekStart = prevWeek)
                ivyContext.updateSelectedPeriodInMemory(newPeriod)
                newPeriod
            }
            month != null -> {
                val year = year ?: dateNowUTC().year
                month.incrementMonthPeriod(ivyContext, -1L, year = year)
            }
            else -> this
        }
    }

    private fun displayMonthStartingOn1st(
        month: Month,
        timeProvider: TimeProvider,
    ): String {
        val year = year
        return if (year != null && timeProvider.localNow().year != year) {
            // not this year
            "${month.name.substring(0, MonthNameAbbreviationLength)}, $year"
        } else {
            // this year
            month.name
        }
    }
}
