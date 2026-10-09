package com.rapidquiz.app.data.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

val ApiJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Serializable
private data class ErrorEnvelope(val error: ErrorBody? = null)

@Serializable
private data class ErrorBody(val code: String? = null, val message: String? = null)

/** Repository katmanının tek hata türü. [code] sunucudaki error.code ya da istemci kodlarıdır. */
class ApiError(val code: String, val status: Int? = null, message: String? = null) : Exception(message ?: code) {
    companion object {
        const val NETWORK = "network_error"
        const val SERVER = "server_error"
        const val UNKNOWN = "unknown"
    }
}

fun Throwable.toApiError(): ApiError = when (this) {
    is ApiError -> this
    is HttpException -> {
        val body = runCatching { response()?.errorBody()?.string() }.getOrNull()
        val parsed = body?.let { runCatching { ApiJson.decodeFromString<ErrorEnvelope>(it).error }.getOrNull() }
        val code = parsed?.code ?: if (code() >= 500) ApiError.SERVER else ApiError.UNKNOWN
        ApiError(code, code(), parsed?.message)
    }
    is IOException -> ApiError(ApiError.NETWORK, null, message)
    else -> ApiError(ApiError.UNKNOWN, null, message)
}
