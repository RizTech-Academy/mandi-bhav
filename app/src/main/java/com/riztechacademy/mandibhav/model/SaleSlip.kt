package com.riztechacademy.mandibhav.model

import java.io.File
import java.time.LocalDate

/**
 * A photo of the sale slip (patti) the commission agent gave the farmer,
 * for one commodity at one market: the last one she attached. [date] is the
 * day of the price it was attached to, so the slip can be read against that
 * day's modal price. [file] is the app's own copy, in its private storage.
 */
data class SaleSlip(
    val market: String,
    val commodity: String,
    val date: LocalDate,
    val file: File,
)
