package com.andres.saldo.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.rounded.EuroSymbol
import androidx.compose.ui.graphics.vector.ImageVector
import com.andres.saldo.data.IconKeys

object AppIcons {
    private val map: Map<String, ImageVector> = mapOf(
        "cart" to Icons.Rounded.ShoppingCart,
        "restaurant" to Icons.Rounded.Restaurant,
        "coffee" to Icons.Rounded.LocalCafe,
        "bar" to Icons.Rounded.LocalBar,
        "train" to Icons.Rounded.Train,
        "car" to Icons.Rounded.DirectionsCar,
        "fuel" to Icons.Rounded.LocalGasStation,
        "flight" to Icons.Rounded.Flight,
        "hotel" to Icons.Rounded.Hotel,
        "home" to Icons.Rounded.Home,
        "bolt" to Icons.Rounded.Bolt,
        "wifi" to Icons.Rounded.Wifi,
        "phone" to Icons.Rounded.Smartphone,
        "subscriptions" to Icons.Rounded.Subscriptions,
        "games" to Icons.Rounded.SportsEsports,
        "movie" to Icons.Rounded.Movie,
        "music" to Icons.Rounded.MusicNote,
        "shirt" to Icons.Rounded.Checkroom,
        "gym" to Icons.Rounded.FitnessCenter,
        "health" to Icons.Rounded.LocalHospital,
        "pharmacy" to Icons.Rounded.LocalPharmacy,
        "beauty" to Icons.Rounded.Spa,
        "gift" to Icons.Rounded.CardGiftcard,
        "pets" to Icons.Rounded.Pets,
        "kids" to Icons.Rounded.ChildCare,
        "education" to Icons.Rounded.School,
        "book" to Icons.Rounded.AutoStories,
        "tools" to Icons.Rounded.Build,
        "savings" to Icons.Rounded.Savings,
        "other" to Icons.Rounded.Category,
        "work" to Icons.Rounded.Work,
        "payments" to Icons.Rounded.Payments,
        "insights" to Icons.Rounded.Insights,
        "refund" to Icons.Rounded.Replay,
        "sell" to Icons.Rounded.LocalOffer,
        "plus" to Icons.Rounded.AddCircle,
        "card" to Icons.Rounded.CreditCard,
        "wallet" to Icons.Rounded.AccountBalanceWallet,
        "bank" to Icons.Rounded.AccountBalance,
        "cash" to Icons.Rounded.EuroSymbol,
        "invest" to Icons.Rounded.Insights,
    )

    fun category(key: String?): ImageVector =
        map[key ?: "other"] ?: map[IconKeys.normalizeCategory(key ?: "")] ?: Icons.Rounded.Category

    fun account(key: String?): ImageVector =
        map[key ?: "wallet"] ?: map[IconKeys.normalizeAccount(key ?: "")] ?: Icons.Rounded.AccountBalanceWallet
}
