package com.riztechacademy.mandibhav.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

private val Base = Typography()

/**
 * The Material type scale with three changes. The system font is kept: it is
 * already on the phone, covers Devanagari, and costs nothing to download.
 */
val MandiBhavTypography = Base.copy(
    // Card titles: a little heavier, so the commodity is the first thing read.
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    // The modal price. Tabular figures give every digit the same width,
    // so prices on cards one above the other line up digit for digit.
    headlineSmall = Base.headlineSmall.copy(
        fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = "tnum",
    ),
    // Min and max under the modal price use the same figures.
    bodyMedium = Base.bodyMedium.copy(fontFeatureSettings = "tnum"),
)
