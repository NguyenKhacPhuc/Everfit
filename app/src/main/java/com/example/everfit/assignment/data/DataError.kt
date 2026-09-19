package com.example.everfit.assignment.data

/**
 * Every failure the data layer can produce, typed at the boundary rather than
 * thrown across it. Nothing above `data` sees a Ktor or serialization exception,
 * so replacing the HTTP client touches one file.
 *
 * Extends Throwable purely so it can travel inside a [Result]; it is a value, and
 * carries no stack trace of its own.
 */
sealed class DataError(message: String, cause: Throwable? = null) : Throwable(message, cause) {

    /** No connectivity, DNS failure, timeout. */
    class Network(cause: Throwable? = null) : DataError("network unavailable", cause)

    /** Reached the server, which refused. */
    class Server(val code: Int, cause: Throwable? = null) : DataError("server returned $code", cause)

    /** Reached the server, which returned something unreadable. */
    class Parsing(cause: Throwable? = null) : DataError("response could not be parsed", cause)

    class Unknown(cause: Throwable? = null) : DataError("unexpected failure", cause)
}
