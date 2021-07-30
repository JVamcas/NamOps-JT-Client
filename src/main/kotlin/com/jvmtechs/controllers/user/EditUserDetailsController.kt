package com.jvmtechs.controllers.user

class EditUserDetailsController: NewUserController() {


    override fun onDock() {
        super.onDock()
        modalStage?.isResizable = false
        title = "Edit User Details"
    }
}