package com.jvmtechs.controllers

import com.jvmtechs.app.Styles
import com.jvmtechs.controllers.home.JobHomeController
import com.jvmtechs.controllers.performance.IPKHomeController
import com.jvmtechs.controllers.performance.TripHomeController
import com.jvmtechs.controllers.trucks.TruckHomeController
import com.jvmtechs.controllers.user.UserTableController
import javafx.event.EventHandler
import javafx.scene.control.Label
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem
import tornadofx.*

class HomeMenu : AbstractView("") {

    override val root: MenuBar = menubar {
        addClass(Styles.cmenu)
        style {
            backgroundColor += c("#BDC3C7")
        }
        menu {
            graphic = Label("Jobs").apply {
                onMouseClicked = EventHandler {
                    workspace.dock<JobHomeController>()
                }
            }
        }
        menu {
            graphic = Label("Performance")
            items.apply {
                add(
                    MenuItem("Trips").apply {
                        action {
                            workspace.dock<TripHomeController>()
                        }
                    }
                )
                add(
                    MenuItem("Income Per KM").apply {
                        action {
                            workspace.dock<IPKHomeController>()
                        }
                    }
                )
            }
        }

        menu {
            graphic = Label("Trucks").apply {
                onMouseClicked = EventHandler {
                    workspace.dock<TruckHomeController>()
                }
            }
        }

        menu {
            graphic = Label("Users").apply {
                onMouseClicked = EventHandler {
                    workspace.dock<UserTableController>()
                }
            }
        }

        menu {
            graphic = Label("Logout").apply {
                onMouseClicked = EventHandler {
                    logout()
                }
            }
        }
    }
}