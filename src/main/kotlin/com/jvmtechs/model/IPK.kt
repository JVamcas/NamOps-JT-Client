package com.jvmtechs.model


import tornadofx.*
import java.time.LocalDateTime

class IPK : AbstractModel() {

    var dtmCreated: LocalDateTime? = null

    var startKm: String? = null

    var endKm: String? = null

    var truck: Truck? = null

    var distance: Double = 0.0

    var totalIncome: Double = 0.0

    var ipk: Double = 0.0
}

class IPKQuery : AbstractModel() {

    var toDate: LocalDateTime? = null
    var fromDate: LocalDateTime? = null
    var truck: Truck? = null

}

class IPKQueryModel : ItemViewModel<IPKQuery>() {

    val toDate = bind(IPKQuery::toDate)
    val fromDate = bind(IPKQuery::fromDate)
    val truck = bind(IPKQuery::truck)

    init {
        item = IPKQuery()
    }
}

