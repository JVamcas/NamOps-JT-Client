package com.jvmtechs.model

import tornadofx.*

class Truck : AbstractModel() {

    var fleetNo: String? = null

    var plateNo: String? = null

    override fun toString(): String {
        return "${fleetNo?.toUpperCase()} | ${plateNo?.toUpperCase()}"
    }
}

class TruckModel : ItemViewModel<Truck>() {
    val fleetNo = bind(Truck::fleetNo)
    val plateNo = bind(Truck::plateNo)
}