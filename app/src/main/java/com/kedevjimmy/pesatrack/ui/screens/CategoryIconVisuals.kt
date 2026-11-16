package com.kedevjimmy.pesatrack.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.ui.theme.CategoryIconPalette as C

/** One icon and colour language for category selection and the Activity ledger. */
internal data class CategoryIconVisual(val icon: ImageVector, val tint: Color)

internal fun categoryIconVisual(label: String): CategoryIconVisual {
    val name = label.trim().lowercase()
    return when {
        name.contains("internet") || name.contains("wi-fi") || name.contains("wifi") ->
            CategoryIconVisual(Icons.Rounded.Wifi, C.internet)
        name.contains("phone") || name.contains("airtime") || name.contains("data") ->
            CategoryIconVisual(Icons.Rounded.PhoneAndroid, C.phone)
        name.contains("elect") || name.contains("power") ->
            CategoryIconVisual(Icons.Rounded.Bolt, C.electricity)
        name.contains("water") ->
            CategoryIconVisual(Icons.Rounded.WaterDrop, C.water)
        name.contains("rent") ->
            CategoryIconVisual(Icons.Rounded.House, C.rent)
        name.contains("household") || name.contains("home") ->
            CategoryIconVisual(Icons.Rounded.Home, C.household)
        name.contains("grocer") ->
            CategoryIconVisual(Icons.Rounded.ShoppingBasket, C.groceries)
        name.contains("shop") ->
            CategoryIconVisual(Icons.Rounded.ShoppingCart, C.shopping)
        name.contains("food") || name.contains("dining") || name.contains("eat") ->
            CategoryIconVisual(Icons.Rounded.Restaurant, C.food)
        name.contains("health") || name.contains("medic") ->
            CategoryIconVisual(Icons.Rounded.Favorite, C.health)
        name.contains("educat") || name.contains("school") ->
            CategoryIconVisual(Icons.Rounded.School, C.education)
        name.contains("entertain") || name.contains("game") ->
            CategoryIconVisual(Icons.Rounded.SportsEsports, C.entertainment)
        name.contains("personal") || name.contains("beaut") ->
            CategoryIconVisual(Icons.Rounded.Spa, C.personalCare)
        name.contains("family") ->
            CategoryIconVisual(Icons.Rounded.Groups, C.family)
        name.contains("fee") ->
            CategoryIconVisual(Icons.Rounded.ReceiptLong, C.fees)
        name.contains("debt") || name.contains("loan") || name.contains("repayment") ->
            CategoryIconVisual(Icons.Rounded.CurrencyExchange, C.debt)
        name.contains("saving") ->
            CategoryIconVisual(Icons.Rounded.Savings, C.savings)
        name.contains("invest") ->
            CategoryIconVisual(Icons.Rounded.TrendingUp, C.investments)
        name.contains("income") || name.contains("salary") ->
            CategoryIconVisual(Icons.Rounded.Work, C.income)
        name.contains("transfer") || name.contains("transf") ->
            CategoryIconVisual(Icons.Rounded.SyncAlt, C.transfer)
        name.contains("transport") || name.contains("travel") ->
            CategoryIconVisual(Icons.Rounded.DirectionsCar, C.transport)
        name.contains("uncategorized") || name.contains("uncategorised") ->
            CategoryIconVisual(Icons.Rounded.Label, C.uncategorized)
        else -> CategoryIconVisual(Icons.Rounded.LocalOffer, C.other)
    }
}

/** Keep real transfer icons distinct, but do not hide savings/investment categories. */
internal fun activityCategoryIconVisual(
    label: String, direction: MoneyDirection
): CategoryIconVisual = when {
    direction == MoneyDirection.TRANSFER &&
        !label.contains("saving", ignoreCase = true) &&
        !label.contains("invest", ignoreCase = true) -> categoryIconVisual("Transfer")
    direction == MoneyDirection.INCOME &&
        (label.equals("Other", ignoreCase = true) ||
            label.equals("Uncategorized", ignoreCase = true)) -> categoryIconVisual("Income")
    else -> categoryIconVisual(label)
}
