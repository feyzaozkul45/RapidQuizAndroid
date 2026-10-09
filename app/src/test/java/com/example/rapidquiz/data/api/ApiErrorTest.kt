package com.example.rapidquiz.data.api

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ApiErrorTest {
    private fun http(status: Int, body: String) =
        HttpException(Response.error<Any>(status, body.toResponseBody("application/json".toMediaType())))

    @Test fun parsesServerErrorEnvelope() {
        val e = http(409, """{"error":{"code":"category_unavailable","message":"x","details":null}}""").toApiError()
        assertEquals("category_unavailable", e.code)
        assertEquals(409, e.status)
    }

    @Test fun ioExceptionIsNetworkError() = assertEquals("network_error", IOException("boom").toApiError().code)

    @Test fun unparsable5xxIsServerError() = assertEquals("server_error", http(500, "<html>").toApiError().code)

    @Test fun unparsable4xxIsUnknown() = assertEquals("unknown", http(418, "nope").toApiError().code)

    @Test fun apiErrorPassesThrough() {
        val original = ApiError("session_expired", 410)
        assertEquals(original, original.toApiError())
    }
}
