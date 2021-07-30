package com.jvmtechs.utils

import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException


sealed class Results {

    companion object {
        fun loading() = Loading
    }

    object Loading : Results()

    class Success<T>(
        val data: T? = null,
        val code: CODE
    ) : Results() {
        enum class CODE {
            WRITE_SUCCESS,
            UPDATE_SUCCESS,
            LOAD_SUCCESS
        }
    }

    class Error(e: Exception) : Results() {
        enum class CODE {
            JOB_CARD_NOT_PERSISTED,
            NO_CONNECTION,
            CONNECTION_TIMEOUT,
            UNKNOWN
        }

        val code: CODE = when (e) {
            is JobCardNotPersistedException -> CODE.JOB_CARD_NOT_PERSISTED
            is SocketException -> CODE.NO_CONNECTION
            is ConnectException -> CODE.NO_CONNECTION
            is SocketTimeoutException -> CODE.CONNECTION_TIMEOUT
            else -> {
                println("Error message was ${e.printStackTrace()}")
                CODE.UNKNOWN
            }
        }
    }

    class JobCardNotPersistedException : Exception()
}