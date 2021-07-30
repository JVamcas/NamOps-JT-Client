package com.jvmtechs.repo

import com.google.gson.reflect.TypeToken
import com.jvmtechs.model.Job
import com.jvmtechs.model.JobSeq
import com.jvmtechs.model.User
import java.lang.reflect.Type


class JobRepo(
    updateUrl: String,
    loadUrl: String,
    queryUrl: String,
    modelName: String,
    val user: User,
    type: Type = object : TypeToken<List<Job>>() {}.type
) : AbstractRepo<Job>(
    updateUrl = updateUrl,
    loadUrl = loadUrl,
    queryUrl = queryUrl,
    modelName = "job",
    user = user,
    colType = type,
    kClass = Job::class.java,
    batchUrl = "job_batch_update"
)

class JobSeqRepo(
    updateUrl: String,
    loadUrl: String,
    queryUrl: String,
    modelName: String,
    val user: User,
    type: Type = object : TypeToken<List<JobSeq>>() {}.type
) : AbstractRepo<JobSeq>(
    updateUrl = updateUrl,
    loadUrl = loadUrl,
    queryUrl = queryUrl,
    modelName = modelName,
    user = user,
    colType = type,
    kClass = JobSeq::class.java,
    batchUrl = ""
)