package com.jvmtechs.model

import tornadofx.*

enum class UserGroup { Driver, Officer, Admin }

class User : AbstractModel() {

    var firstName = ""

    var lastName = ""

    var userGroup = UserGroup.Officer.name

    var username = ""

    var password = ""

//    var jobList = observableListOf<Job>()

    override fun toString(): String {
        return "${firstName.capitalize()} ${lastName.capitalize()}"
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is User)
            return false
        return other.id == id
    }

    override fun hashCode(): Int {
        var result = id ?: 0
        result = 31 * result + firstName.hashCode()
        result = 31 * result + lastName.hashCode()
        return result
    }
}


class UserModel : ItemViewModel<User>() {

    val firstName = bind(User::firstName)
    val lastName = bind(User::lastName)
    val userGroup = bind(User::userGroup)
    val username = bind(User::username)
    val password = bind(User::password)

}