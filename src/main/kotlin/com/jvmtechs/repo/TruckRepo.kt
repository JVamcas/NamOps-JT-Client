package com.jvmtechs.repo

import com.google.gson.reflect.TypeToken
import com.jvmtechs.model.Truck
import com.jvmtechs.model.User
import java.lang.reflect.Type

class TruckRepo(
    updateUrl: String,
    loadUrl: String,
    modelName: String,
    user: User,
    type: Type = object : TypeToken<List<Truck>>(){}.type
) : AbstractRepo<Truck>(
    updateUrl = updateUrl,
    loadUrl = loadUrl,
    modelName = modelName,
    user = user,
    colType = type,
    kClass = Truck::class.java,
    batchUrl = ""
)