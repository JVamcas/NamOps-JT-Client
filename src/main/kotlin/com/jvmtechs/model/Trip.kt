package com.jvmtechs.model


import tornadofx.*
import java.time.LocalDateTime

class Trip : AbstractModel() {

    var dtmStarted: LocalDateTime? = null

    var dtmCompleted: LocalDateTime? = null

    var truck: Truck? = null

    var driver: User? = null

    var containerList = observableListOf<Container>()

    var description: String? = null

    var memo: String? = null

    var pickUpLocation: String? = null

    var deliveryLocation: String? = null

    var priceLessTax: Float? = null

}

class TripQuery : AbstractModel() {

    var toDate: LocalDateTime? = null
    var fromDate: LocalDateTime? = null
    var truck: Truck? = null
    var driver: User? = null
    var containerNo: String? = null

}

class TripQueryModel : ItemViewModel<TripQuery>() {

    val toDate = bind(TripQuery::toDate)
    val fromDate = bind(TripQuery::fromDate)
    val truck = bind(TripQuery::truck)
    val driver = bind(TripQuery::driver)
    val containerNo = bind(TripQuery::containerNo)

    init {
        item = TripQuery()
    }
}
