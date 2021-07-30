package com.jvmtechs.repo

import com.google.gson.reflect.TypeToken
import com.jvmtechs.model.User
import java.lang.reflect.Type

class UserRepo(
    updateUrl: String = "",
    loadUrl: String = "",
    modelName: String = "",
    user: User = User(),
    type: Type = object : TypeToken<List<User>>() {}.type
) : AbstractRepo<User>(
    updateUrl = updateUrl,
    loadUrl = loadUrl,
    modelName = modelName,
    user = user,
    colType = type,
    kClass = User::class.java,
    batchUrl = ""
)
