package com.riztechacademy.mandibhav.model

import java.time.LocalDate

/** The modal price of one commodity at one market on one day: a point in its history. */
data class DayPrice(
    val date: LocalDate,
    val modalPrice: Long,
)
