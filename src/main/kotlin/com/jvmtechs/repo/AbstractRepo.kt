package com.jvmtechs.repo

import com.google.gson.JsonParser
import com.jvmtechs.model.AbstractModel
import com.jvmtechs.model.User
import com.jvmtechs.utils.Const
import com.jvmtechs.utils.ParseUtil.Companion.convert
import com.jvmtechs.utils.ParseUtil.Companion.toJson
import com.jvmtechs.utils.Results
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import tornadofx.*
import java.lang.reflect.Type

abstract class AbstractRepo<T : AbstractModel>(
    private val updateUrl: String,
    private val loadUrl: String,
    private val queryUrl: String = "",
    private val modelName: String,
    private val user: User,
    private val colType: Type,
    private val kClass: Class<T>,
    private val batchUrl: String
) {
    val client = OkHttpClient.Builder().build()

    suspend fun loadAll(): Results {
        val url = "${Const.baseUrl}/${loadUrl}?username=${user.username}&password=${user.password}"
        val request = Request.Builder()
            .url(url)
            .build()

        return try {
            withContext(Dispatchers.IO) {
                val results = client.newCall(request).execute()//wait for the results from api
                val data = results.body?.string()

                val jsonTree = JsonParser.parseString(data).asJsonObject

                when (jsonTree.get("Status").toString().replace("\"", "")) {
                    "Success" -> {
                        val jsonData = jsonTree.get("data").toString()
                        val modelList = if (jsonData.isEmpty())
                            listOf() else jsonData.convert<List<T>>(colType)
                        Results.Success(
                            data = modelList.asObservable(),
                            code = Results.Success.CODE.LOAD_SUCCESS
                        )
                    }
                    "Invalid Auth" -> Results.Error(AbstractModel.InvalidAuthCredException())

                    else -> Results.Error(AbstractModel.ServerException())
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Results.Error(e)
        }
    }

    suspend fun addOrUpdateModel(toUpdate: T): Results {
        return try {

            val requestBody = FormBody.Builder()
                .add("password", user.password)
                .add("username", user.username)
                .add(modelName, toUpdate.toJson())
                .build()

            val request = Request.Builder()
                .url("${Const.baseUrl}/$updateUrl")
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
                        val jsonData = jsonTree.get("data").toString()
                        val model = if (jsonData.isEmpty())
                            null else jsonData.convert(kClass)

                        Results.Success(
                            data = model,
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

    suspend fun batchUpdate(list: List<T>): Results {
        return try {
            val requestBody = FormBody.Builder()
                .add("password", user.password)
                .add("username", user.username)
                .add(modelName, list.toJson())
                .build()

            val request = Request.Builder()
                .url("${Const.baseUrl}/$batchUrl")
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
                        val jsonData = jsonTree.get("data").toString()
                        val dataList = if (jsonData.isEmpty())
                            null else jsonData.convert<List<T>>(colType)

                        Results.Success(
                            data = dataList,
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

    suspend fun <Q : AbstractModel> queryModel(query: Q): Results {
        if (queryUrl.isEmpty())
            throw Exception("Query Url is not specified.")

        val requestBody = FormBody.Builder()
            .add("password", user.password)
            .add("username", user.username)
            .add(modelName, query.toJson())
            .build()

        val request = Request.Builder()
            .url("${Const.baseUrl}/$queryUrl")
            .post(requestBody)
            .build()

        return try {
            withContext(Dispatchers.IO) {
                val results = client.newCall(request).execute()//wait for the results from api
                val data = results.body?.string()

                val jsonTree = JsonParser.parseString(data).asJsonObject

                when (jsonTree.get("Status").toString().replace("\"", "")) {
                    "Success" -> {
                        val jsonData = jsonTree.get("data").toString()
                        val modelList = if (jsonData.isEmpty())
                            listOf() else jsonData.convert<List<Q>>(colType)
                        Results.Success(
                            data = modelList.asObservable(),
                            code = Results.Success.CODE.LOAD_SUCCESS
                        )
                    }
                    "Invalid Auth" -> Results.Error(AbstractModel.InvalidAuthCredException())

                    else -> Results.Error(AbstractModel.ServerException())
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Results.Error(e)
        }
    }

    suspend fun deleteModel(toUpdate: T): Results {
        return try {
            withContext(Dispatchers.IO) {
                toUpdate.deleted = true
                addOrUpdateModel(toUpdate)
            }
        } catch (e: Exception) {
            Results.Error(e)
        }
    }
}