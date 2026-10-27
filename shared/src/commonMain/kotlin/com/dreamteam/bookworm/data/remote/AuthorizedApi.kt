package com.dreamteam.bookworm.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode

/**
 * Calls the backend as the signed-in student. When the access token has expired
 * (401), it refreshes it once through [auth] and retries; if that fails too, the
 * student is signed out and the call throws [ApiException].
 */
class AuthorizedApi(
    private val client: HttpClient,
    private val auth: RemoteAuthRepository,
) {
    suspend fun request(block: HttpRequestBuilder.() -> Unit): HttpResponse {
        val token = auth.session.value?.token ?: throw ApiException(HttpStatusCode.Unauthorized)
        val response = client.request { block(); bearerAuth(token) }
        if (response.status != HttpStatusCode.Unauthorized) return response
        val fresh = auth.tokenAfterUnauthorized(token) ?: throw ApiException(HttpStatusCode.Unauthorized)
        return client.request { block(); bearerAuth(fresh) }
    }
}
