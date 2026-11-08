package com.riztechacademy.mandibhav.ui

/**
 * Formats whole rupees with Indian digit grouping: 2450 -> "₹2,450",
 * 245000 -> "₹2,45,000".
 *
 * Deliberately not String.format or NumberFormat: both depend on the
 * device locale, and java.text groups "245,000" on the JVM that runs
 * unit tests even when the phone shows "2,45,000".
 */
fun formatRupees(amount: Long): String {
    val digits = amount.toString().removePrefix("-")
    val grouped = if (digits.length <= 3) {
        digits
    } else {
        val lastThree = digits.takeLast(3)
        val rest = digits.dropLast(3)
        rest.reversed().chunked(2).joinToString(",").reversed() + "," + lastThree
    }
    val sign = if (amount < 0) "-" else ""
    return "$sign₹$grouped"
}
