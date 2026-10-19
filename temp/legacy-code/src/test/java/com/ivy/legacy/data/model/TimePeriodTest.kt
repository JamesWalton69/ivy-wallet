package com.ivy.legacy.data.model

import com.ivy.base.time.TimeProvider
import com.ivy.base.time.impl.TestTimeConverter
import com.ivy.legacy.IvyWalletCtx
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

class TimePeriodTest {

    private val timeConverter = TestTimeConverter()
    private val fixedLocalDate = LocalDate.of(2026, 10, 19) // Monday
    private val fixedLocalDateTime = fixedLocalDate.atTime(12, 0, 0)
    private val fixedInstant = fixedLocalDateTime.atZone(ZoneId.of("UTC")).toInstant()

    private val testTimeProvider = object : TimeProvider {
        override fun getZoneId(): ZoneId = ZoneId.of("UTC")
        override fun utcNow(): Instant = fixedInstant
        override fun localNow(): LocalDateTime = fixedLocalDateTime
        override fun localDateNow(): LocalDate = fixedLocalDate
        override fun localTimeNow(): LocalTime = fixedLocalDateTime.toLocalTime()
    }

    private lateinit var ivyContext: IvyWalletCtx

    @Before
    fun setUp() {
        ivyContext = IvyWalletCtx()
    }

    @Test
    fun `today creates a Day TimePeriod with today's date`() {
        val period = TimePeriod.today(testTimeProvider)

        period.periodType shouldBe PeriodType.DAY
        period.day shouldBe fixedLocalDate
        period.isValid() shouldBe true
    }

    @Test
    fun `currentWeek creates a Week TimePeriod starting on Monday`() {
        val period = TimePeriod.currentWeek(testTimeProvider)

        period.periodType shouldBe PeriodType.WEEK
        period.weekStart shouldBe fixedLocalDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        period.isValid() shouldBe true
    }

    @Test
    fun `Day toRange covers full 24 hours of that day`() {
        val period = TimePeriod(day = fixedLocalDate)

        val range = period.toRange(
            startDateOfMonth = 1,
            timeConverter = timeConverter,
            timeProvider = testTimeProvider,
        )

        range.from shouldBe fixedLocalDate.atStartOfDay().atZone(ZoneId.of("UTC")).toInstant()
        range.to shouldBe fixedLocalDate.atTime(LocalTime.MAX).atZone(ZoneId.of("UTC")).toInstant()
    }

    @Test
    fun `Week toRange covers 7 days from weekStart`() {
        val monday = fixedLocalDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = monday.plusDays(6)
        val period = TimePeriod(weekStart = monday)

        val range = period.toRange(
            startDateOfMonth = 1,
            timeConverter = timeConverter,
            timeProvider = testTimeProvider,
        )

        range.from shouldBe monday.atStartOfDay().atZone(ZoneId.of("UTC")).toInstant()
        range.to shouldBe sunday.atTime(LocalTime.MAX).atZone(ZoneId.of("UTC")).toInstant()
    }

    @Test
    fun `Day next and previous move by exactly one day`() {
        val period = TimePeriod(day = fixedLocalDate)

        val nextPeriod = period.next(ivyContext, 1)
        nextPeriod.day shouldBe fixedLocalDate.plusDays(1)
        nextPeriod.periodType shouldBe PeriodType.DAY

        val prevPeriod = period.previous(ivyContext, 1)
        prevPeriod.day shouldBe fixedLocalDate.minusDays(1)
        prevPeriod.periodType shouldBe PeriodType.DAY
    }

    @Test
    fun `Week next and previous move by exactly one week`() {
        val monday = fixedLocalDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val period = TimePeriod(weekStart = monday)

        val nextPeriod = period.next(ivyContext, 1)
        nextPeriod.weekStart shouldBe monday.plusWeeks(1)
        nextPeriod.periodType shouldBe PeriodType.WEEK

        val prevPeriod = period.previous(ivyContext, 1)
        prevPeriod.weekStart shouldBe monday.minusWeeks(1)
        prevPeriod.periodType shouldBe PeriodType.WEEK
    }
}
