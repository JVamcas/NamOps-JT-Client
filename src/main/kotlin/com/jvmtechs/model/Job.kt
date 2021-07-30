package com.jvmtechs.model

import javafx.beans.property.SimpleObjectProperty
import tornadofx.*
import java.io.File
import java.time.LocalDateTime

enum class JobStatus { Draft, Ready, Completed }

class Job : AbstractModel() {

    var dtmPosted: LocalDateTime? = null

    var entryNo: String? = null

    var legIdx: String? = null

    /***
     * Auto-generated from the [entryNo] and [id] as JA[000][id][entryNo]
     */
    var jobNo: String? = null

    var qty: Int? = null

    var code: String? = null

    var qBCode: String? = null

    var uom: String? = null

    var description: String? = null

    var memo: String? = null

    var pickUpLocation: String? = null

    var deliveryLocation: String? = null

    var completed = false

    val dtmStart = SimpleObjectProperty<LocalDateTime>()

    val dtmDue = SimpleObjectProperty<LocalDateTime>()

    var priceLessTax: Float? = null

    /**
     * List of drivers pre-allocated to job
     */
    var driversList = observableListOf<User>()

    var containerList = observableListOf<Container>()

    var status: String = JobStatus.Draft.name

    @Transient
    var fileUploaded: File? = null

}

class JobModel : ItemViewModel<Job>() {

    val entryNo = bind(Job::entryNo)
    val legIdx = bind(Job::legIdx)
    val jobNo = bind(Job::jobNo)
    val code = bind(Job::code)
    val qBCode = bind(Job::qBCode)
    val uom = bind(Job::uom)
    val description = bind(Job::description)
    val memo = bind(Job::memo)
    val pickUpLocation = bind(Job::pickUpLocation)
    val deliveryLocation = bind(Job::deliveryLocation)
    val priceLessTax = bind(Job::priceLessTax)
    val driverList = bind(Job::driversList)
    val containerList = bind(Job::containerList)
    val qty = bind(Job::qty)
    val dtmStart = bind(Job::dtmStart)
    val dtmDue = bind(Job::dtmDue)

}

class JobQuery : AbstractModel() {

    var jobNo: String? = null
    var qbNumber: String? = null
    var allocatedDriver: User? = null
    var fromDate: LocalDateTime? = null
    var toDate: LocalDateTime? = null
    var containerNo: String? = null
}

class JobQueryModel : ItemViewModel<JobQuery>() {

    val jobNo = bind(JobQuery::jobNo)
    val qbNumber = bind(JobQuery::qbNumber)
    val allocatedDriver = bind(JobQuery::allocatedDriver)
    val fromDate = bind(JobQuery::fromDate)
    val toDate = bind(JobQuery::toDate)
    val containerNo = bind(JobQuery::containerNo)

    init {
        item = JobQuery()
    }
}

class JobSeq : AbstractModel() {
    var lastValue: Int? = null
    var isCalled = false
}