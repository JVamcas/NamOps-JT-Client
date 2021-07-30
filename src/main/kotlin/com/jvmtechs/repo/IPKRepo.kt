package com.jvmtechs.repo

import com.google.gson.reflect.TypeToken
import com.jvmtechs.model.IPK
import com.jvmtechs.model.User
import java.lang.reflect.Type

class IPKRepo(
    updateUrl: String = "",
    loadUrl: String = "",
    queryUrl: String = "",
    modelName: String = "",
    val user: User,
    type: Type = object : TypeToken<List<IPK>>() {}.type
) : AbstractRepo<IPK>(
    updateUrl = updateUrl,
    loadUrl = loadUrl,
    queryUrl = queryUrl,
    modelName = modelName,
    user = user,
    colType = type,
    kClass = IPK::class.java,
    batchUrl = ""
)