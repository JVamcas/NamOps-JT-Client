package com.jvmtechs.model

abstract class AbstractModel {
    class InvalidAuthCredException : Exception()
    class ServerException : Exception()

    fun data() = arrayListOf<Pair<String, String>>()

    var id: Int? = null
    var deleted = false
}