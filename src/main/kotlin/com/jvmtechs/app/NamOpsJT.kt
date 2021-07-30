package com.jvmtechs.app

import com.jvmtechs.controllers.LoginController
import tornadofx.*

class NamOpsJT : App(Workspace::class, Styles::class) {


    override fun onBeforeShow(view: UIComponent) {
        super.onBeforeShow(view)
        workspace.dockInNewScope<LoginController>(workspace)
        println("Login workspace $workspace")
    }
}