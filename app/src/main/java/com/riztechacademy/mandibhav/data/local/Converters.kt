package com.riztechacademy.mandibhav.data.local

import androidx.room3.ColumnTypeConverter
import java.time.Instant
import java.time.LocalDate

/**
 * How Room stores the types SQLite does not have. A date is stored as its
 * epoch day, a whole number, so `ORDER BY date` and `BETWEEN` compare numbers.
 */
class Converters {
    @ColumnTypeConverter
    fun localDateToEpochDay(date: LocalDate): Long = date.toEpochDay()

    @ColumnTypeConverter
    fun epochDayToLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    /** A moment is stored as milliseconds since 1970 UTC, the same on every phone. */
    @ColumnTypeConverter
    fun instantToEpochMilli(instant: Instant?): Long? = instant?.toEpochMilli()

    @ColumnTypeConverter
    fun epochMilliToInstant(epochMilli: Long?): Instant? = epochMilli?.let(Instant::ofEpochMilli)
}
