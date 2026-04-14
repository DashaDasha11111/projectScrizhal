package com.example.scrizhal

import java.io.Serializable

data class Cleric(
    val id: Int,
    val name: String,
    val isMonk: Boolean,
    val birthday: String,
    val date: String,
    val eventType: String,
    val eventValue: String,
    val awardType: String,
    val priestOrdination: String,
    val description: String,
    val church: String = "",
    val phone: String = "",
    val email: String = "",
    val nameDay: String = "",
    val diaconalOrdination: String = ""
) : Serializable {

    fun getHumanStatus(): String {
        val days = DateUtils.getDaysUntil(date)
        return when {
            days == 0L -> "сегодня"
            days == 1L -> "завтра"
            days in 2..4 -> "через $days дня"
            days in 5..30 -> "через $days дней"
            days < 0 -> "прошло ${-days} дн."
            else -> "в ожидании"
        }
    }

}