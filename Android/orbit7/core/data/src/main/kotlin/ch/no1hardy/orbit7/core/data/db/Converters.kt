package ch.no1hardy.orbit7.core.data.db

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

/**
 * Room converters.
 *
 * Dates are stored as ISO-8601 text so the database is readable in a SQL browser and stable across
 * timezones; instants as epoch milliseconds.
 */
class Converters {
    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun instantToEpochMilli(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMilliToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
}
