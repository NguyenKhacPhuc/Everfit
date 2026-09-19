package com.example.everfit.assignment.data.base

import com.example.everfit.assignment.data.network.ApiError
import com.example.everfit.assignment.data.network.BaseResponse
import com.example.everfit.assignment.data.network.ResponseApiError
import com.example.everfit.assignment.ext.json.JsonHelper
import com.example.everfit.assignment.model.common.ErrorModel
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

sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>

    data class Error(
        val code: Int,
        val message: String,
        val exception: Exception? = null,
        val data: Any? = null,
    ) : Result<Nothing>

    data object Loading : Result<Nothing>
}

suspend fun Exception?.toResult(): Result.Error {
    return try {
        when (this) {
            is ClientRequestException, is ServerResponseException -> {
                val error = JsonHelper.toObject<ErrorModel>(this.response.bodyAsText())
                if (null == error) {
                    // Keep the HTTP status rather than collapsing to UNKNOWN: a
                    // 500 with a non-JSON body still tells the caller something.
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

suspend inline fun <reified T> HttpResponse.toResultOrNothing(): Result<Boolean> {
    return if (this.status.value in 200..299) {
        this.body<BaseResponse<T>>()
        Result.Success(true)
    } else {
        Result.Error(this.status.value, this.bodyAsText())
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

/**
 * Record a non-HTTP exception for debugging.
 *
 * ADAPTED: the reference implementation reports to Firebase Crashlytics. Firebase
 * is not configured in this project (it needs a google-services.json and a
 * Firebase project), so reporting goes through [CrashReporter] instead. Swapping
 * in Crashlytics means changing that one object, not these call sites.
 */
fun Exception.logAsNonFatal() {
    printStackTrace()
    CrashReporter.recordException(this)
}

fun Throwable.logAsNonFatal() {
    printStackTrace()
    CrashReporter.recordException(this)
}

/** Seam for a crash reporter. Replace the body with Crashlytics when it is configured. */
object CrashReporter {
    fun recordException(error: Throwable) {
        // no-op until a reporter is configured
    }
}
