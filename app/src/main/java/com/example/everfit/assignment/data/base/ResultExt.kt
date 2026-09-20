package com.example.everfit.assignment.data.base

import com.example.everfit.assignment.core.ext.json.JsonHelper
import com.example.everfit.assignment.core.model.Result
import com.example.everfit.assignment.data.network.ApiError
import com.example.everfit.assignment.data.network.BaseResponse
import com.example.everfit.assignment.data.network.ErrorModel
import com.example.everfit.assignment.data.network.ResponseApiError
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

suspend fun Exception?.toResult(): Result.Error {
    return try {
        when (this) {
            is ClientRequestException, is ServerResponseException -> {
                val error = JsonHelper.toObject<ErrorModel>(this.response.bodyAsText())
                if (null == error) {
                    Result.Error(this.response.status.value, "", this)
                } else {
                    Result.Error(error.code, error.message, this)
                }
            }

            is ResponseApiError -> Result.Error(
                this.code ?: ApiError.UNKNOWN,
                this.message.orEmpty(),
                this,
                data = this.data,
            )

            is UnknownHostException,
            is SSLException,
            is InterruptedIOException,
            -> Result.Error(ApiError.NETWORK, "", this)

            is ConnectException -> Result.Error(ApiError.SERVER, "", this)
            else -> Result.Error(ApiError.UNKNOWN, "", this)
        }
    } catch (e: Exception) {
        Result.Error(ApiError.UNKNOWN, "", e)
    }
}

suspend inline fun <reified T> HttpResponse.toResult(): Result<T> {
    return if (this.status.value in 200..299) {
        val objectData = this.body<BaseResponse<T>>()
        Result.Success(objectData.data!!)
    } else {
        Result.Error(this.status.value, this.bodyAsText())
    }
}

inline fun <reified T> Flow<T>.asResult(): Flow<Result<T>> {
    return this
        .map<T, Result<T>> { Result.Success(it) }
        .onStart { emit(Result.Loading) }
        .catch {
            it.logAsNonFatal()
            emit((it as? Exception ?: Exception()).toResult())
        }
}

fun Throwable.logAsNonFatal() {
    printStackTrace()
    CrashReporter.recordException(this)
}

/** Seam for a crash reporter. Replace the body with Crashlytics when configured. */
object CrashReporter {
    fun recordException(error: Throwable) {
        // no-op until a reporter is configured
    }
}
