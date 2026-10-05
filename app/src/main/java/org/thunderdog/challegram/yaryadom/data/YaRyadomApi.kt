package org.thunderdog.challegram.yaryadom.data

import org.thunderdog.challegram.yaryadom.data.models.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Простой HTTP-клиент для backend «Я рядом».
 *
 * ВАЖНО: текущий backend ожидает initData от Mini App.
 * Для нативного клиента рекомендуется добавить на backend
 * поддержку заголовка X-Telegram-User-Id + подписи.
 *
 * Пока используется временный режим: передаём userId в теле
 * и специальный флаг nativeClient=true (backend нужно доработать).
 */
class YaRyadomApi(
    private val baseUrl: String,
    private val userId: Long,
    private val firstName: String,
    private val username: String? = null
) {

    companion object {
        private const val TIMEOUT_MS = 15_000
    }

    fun createOrder(request: CreateOrderRequest): CreateOrderResponse {
        val body = JSONObject().apply {
            put("nativeClient", true)
            put("userId", userId)
            put("firstName", firstName)
            put("username", username)
            put("category", request.category)
            put("description", request.description)
            put("latitude", request.latitude)
            put("longitude", request.longitude)
            put("radiusMeters", request.radiusMeters)
            put("expiresInMinutes", request.expiresInMinutes)
            request.destinationText?.let { put("destinationText", it) }
        }
        val json = post("/api/orders", body)
        return CreateOrderResponse(
            id = json.getString("id"),
            status = json.getString("status"),
            expiresAt = json.getString("expiresAt")
        )
    }

    fun nearby(latitude: Double, longitude: Double, radiusMeters: Int = 5000): NearbyResponse {
        val body = JSONObject().apply {
            put("nativeClient", true)
            put("userId", userId)
            put("latitude", latitude)
            put("longitude", longitude)
            put("radiusMeters", radiusMeters)
        }
        val json = post("/api/orders/nearby", body)
        val items = mutableListOf<NearbyItem>()
        val arr = json.optJSONArray("items") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            items.add(
                NearbyItem(
                    id = o.getString("id"),
                    category = o.getString("category"),
                    description = o.getString("description"),
                    destinationText = o.optString("destinationText").takeIf { it.isNotEmpty() },
                    distanceMeters = if (o.has("distanceMeters")) o.getInt("distanceMeters") else null,
                    status = o.getString("status"),
                    creatorName = o.getString("creatorName")
                )
            )
        }
        return NearbyResponse(items)
    }

    fun takeOrder(orderId: String): TakeResponse {
        val body = JSONObject().apply {
            put("nativeClient", true)
            put("userId", userId)
            put("firstName", firstName)
            put("username", username)
            put("orderId", orderId)
        }
        val json = post("/api/orders/take", body)
        val notifications = json.optJSONObject("notifications")?.let {
            Notifications(
                creatorNotified = it.optBoolean("creatorNotified"),
                takerNotified = it.optBoolean("takerNotified")
            )
        }
        return TakeResponse(
            id = json.getString("id"),
            status = json.getString("status"),
            message = json.optString("message", "Заявка взята"),
            notifications = notifications
        )
    }

    fun myTaken(): MineResponse {
        val body = JSONObject().apply {
            put("nativeClient", true)
            put("userId", userId)
        }
        val json = post("/api/orders/mine", body)
        val items = mutableListOf<MineItem>()
        val arr = json.optJSONArray("items") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            items.add(
                MineItem(
                    id = o.getString("id"),
                    category = o.getString("category"),
                    description = o.getString("description"),
                    destinationText = o.optString("destinationText").takeIf { it.isNotEmpty() },
                    status = o.getString("status"),
                    creatorName = o.getString("creatorName"),
                    creatorUsername = o.optString("creatorUsername").takeIf { it.isNotEmpty() }
                )
            )
        }
        return MineResponse(items)
    }

    fun completeOrder(orderId: String): CompleteResponse {
        val body = JSONObject().apply {
            put("nativeClient", true)
            put("userId", userId)
            put("orderId", orderId)
        }
        val json = post("/api/orders/complete", body)
        return CompleteResponse(
            id = json.getString("id"),
            status = json.getString("status"),
            message = json.optString("message", "Заявка выполнена")
        )
    }

    private fun post(path: String, body: JSONObject): JSONObject {
        val url = URL(baseUrl.trimEnd('/') + path)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("X-Ya-Ryadom-Client", "android-native")
        }

        OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { writer ->
            writer.write(body.toString())
            writer.flush()
        }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val responseText = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }

        if (code !in 200..299) {
            val errMsg = try {
                JSONObject(responseText).optString("message")
                    .ifEmpty { JSONObject(responseText).optString("error") }
            } catch (_: Exception) {
                responseText
            }
            throw YaRyadomApiException(code, errMsg.ifEmpty { "HTTP $code" })
        }

        return JSONObject(responseText)
    }
}

class YaRyadomApiException(val statusCode: Int, message: String) : Exception(message)
