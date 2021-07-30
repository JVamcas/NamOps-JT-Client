package com.jvmtechs.controllers.home

class UpdateJobController: NewJobController() {


    override fun onDock() {
        super.onDock()
        modalStage?.isResizable = false
        title = "Edit Job Details"
    }
}