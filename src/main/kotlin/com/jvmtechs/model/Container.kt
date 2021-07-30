package com.jvmtechs.model

import tornadofx.*


class Container : AbstractModel(){

    var containerNo: String? = null

    var delivered: Boolean = false

    var pickedUp: Boolean = false

    var allocatedDriver: User? = null

    override fun toString(): String {
        return "${containerNo}"
    }
}

class ContainerModel: ItemViewModel<Container>(){
    val containerNo = bind(Container::containerNo)

    init {
        item = Container()
    }
}