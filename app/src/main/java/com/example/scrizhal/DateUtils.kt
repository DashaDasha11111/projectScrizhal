package com.example.scrizhal

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object DateUtils {
    private val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun parse(dateStr: String): LocalDate = LocalDate.parse(dateStr, formatter)

    fun getDaysUntil(dateStr: String): Long {
        val target = parse(dateStr)
        val today = LocalDate.now()
        return ChronoUnit.DAYS.between(today, target)
    }

    fun getYearsBetween(start: String, end: String = LocalDate.now().format(formatter)): Int {
        return ChronoUnit.YEARS.between(parse(start), parse(end)).toInt()
    }

    fun parseIso(dateStr: String): LocalDate = LocalDate.parse(dateStr, isoFormatter)

    fun parseFlexible(dateStr: String): LocalDate {
        return try {
            parse(dateStr)
        } catch (_: Exception) {
            parseIso(dateStr)
        }
    }

    fun format(date: LocalDate): String = date.format(formatter)

    fun formatFlexibleToDisplay(dateStr: String): String {
        return runCatching { format(parseFlexible(dateStr)) }.getOrElse { dateStr }
    }
}