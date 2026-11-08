package com.riztechacademy.mandibhav.ui

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.riztechacademy.mandibhav.data.SamplePrices
import com.riztechacademy.mandibhav.model.CommodityPrice

/** Feeds every sample price into a preview, one preview per price. */
class SamplePriceProvider : PreviewParameterProvider<CommodityPrice> {
    private val prices = SamplePrices.all

    override val values: Sequence<CommodityPrice> = prices.asSequence()

    override fun getDisplayName(index: Int): String? {
        val price = prices.getOrNull(index) ?: return null
        return "${price.commodity}, ${price.market}"
    }
}
