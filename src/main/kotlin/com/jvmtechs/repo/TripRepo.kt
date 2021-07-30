package com.jvmtechs.repo

import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.jvmtechs.model.AbstractModel
import com.jvmtechs.model.IPK
import com.jvmtechs.model.Trip
import com.jvmtechs.model.User
import com.jvmtechs.utils.Const
import com.jvmtechs.utils.ParseUtil.Companion.convert
import com.jvmtechs.utils.ParseUtil.Companion.toJson
import com.jvmtechs.utils.Results
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import java.lang.reflect.Type

class TripRepo(
    updateUrl: String = "",
    loadUrl: String = "",
    queryUrl: String = "",
    modelName: String = "",
    val user: User
) : AbstractRepo<Trip>(
    updateUrl = updateUrl,
    loadUrl = loadUrl,
    queryUrl = queryUrl,
    modelName = modelName,
    user = user,
    colType = object : TypeToken<List<Trip>>() {}.type,
    kClass = Trip::class.java,
    batchUrl = ""
) {

    suspend fun updateTripCost(jobLog: Int?, newCost: Float?): Results {
        return try {
            val requestBody = FormBody.Builder()
                .add("password", user.password)
                .add("username", user.username)
                .add("jobLog", jobLog.toString())
                .add("newCost", newCost.toString())
                .build()

            val request = Request.Builder()
                .url("${Const.baseUrl}/update_trip_cost")
                .post(requestBody)
                .build()

            withContext(Dispatchers.IO) {

                val results =
                    client.newCall(request).execute()//wait for the results from the SERVER
                val data = results.body?.string()
                val jsonTree = JsonParser.parseString(data).asJsonObject
                val writeResp = jsonTree.get("Status").toString().replace("\"", "")

                when (writeResp) {
                    "Success" -> {
                        Results.Success(
                            data = null,
                            code = Results.Success.CODE.WRITE_SUCCESS
                        )
                    }
                    "Invalid Auth" -> Results.Error(AbstractModel.InvalidAuthCredException())
                    else -> Results.Error(AbstractModel.ServerException())
                }
            }
        } catch (e: Exception) {
            Results.Error(e)
        }
    }

}