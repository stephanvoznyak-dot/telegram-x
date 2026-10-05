package org.thunderdog.challegram.yaryadom.data.models

/**
 * Модели данных модуля «Я рядом».
 * Соответствуют API backend (apps/bot).
 */

data class UserInfo(
    val id: Long,
    val firstName: String,
    val lastName: String? = null,
    val username: String? = null
)

data class CreateOrderRequest(
    val category: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val destinationText: String? = null,
    val radiusMeters: Int = 5000,
    val expiresInMinutes: Int = 30
)

data class CreateOrderResponse(
    val id: String,
    val status: String,
    val expiresAt: String
)

data class NearbyItem(
    val id: String,
    val category: String,
    val description: String,
    val destinationText: String? = null,
    val distanceMeters: Int? = null,
    val status: String,
    val creatorName: String
)

data class NearbyResponse(
    val items: List<NearbyItem>
)

data class MineItem(
    val id: String,
    val category: String,
    val description: String,
    val destinationText: String? = null,
    val status: String,
    val creatorName: String,
    val creatorUsername: String? = null
)

data class MineResponse(
    val items: List<MineItem>
)

data class TakeResponse(
    val id: String,
    val status: String,
    val message: String,
    val notifications: Notifications? = null
)

data class Notifications(
    val creatorNotified: Boolean,
    val takerNotified: Boolean
)

data class CompleteResponse(
    val id: String,
    val status: String,
    val message: String
)

/** Категории заявок (синхронизированы с backend) */
object Categories {
    val ALL = listOf(
        "DELIVERY" to "Доставка",
        "RIDE" to "Поездка",
        "HELP" to "Помощь",
        "SHOPPING" to "Купить / принести",
        "REPAIR" to "Ремонт",
        "CLEANING" to "Уборка",
        "COMPUTER" to "Компьютер",
        "RENTAL" to "Аренда",
        "OTHER" to "Другое"
    )

    fun label(code: String): String =
        ALL.find { it.first == code }?.second ?: code
}

/** Доступные радиусы поиска (метры) */
object Radii {
    val ALL = listOf(
        1000 to "1 км",
        2000 to "2 км",
        5000 to "5 км",
        10000 to "10 км",
        20000 to "20 км"
    )
}
