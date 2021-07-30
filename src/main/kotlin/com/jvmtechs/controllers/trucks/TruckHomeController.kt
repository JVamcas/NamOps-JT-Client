package com.jvmtechs.controllers.trucks

import com.jvmtechs.controllers.AbstractModelTableController
import com.jvmtechs.model.Truck
import com.jvmtechs.model.TruckModel
import com.jvmtechs.model.User
import com.jvmtechs.repo.TruckRepo
import com.jvmtechs.utils.Results
import javafx.collections.ObservableList
import javafx.scene.Parent
import javafx.scene.control.Label
import javafx.scene.control.TableView
import javafx.scene.layout.Priority
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import tornadofx.*

class TruckHomeController : AbstractModelTableController<Truck>("Trucks Details") {

    override val root: Parent = scrollpane {

        tableView = tableview(modelList) {

            smartResize()
            prefWidthProperty().bind(this@scrollpane.widthProperty())
            prefHeightProperty().bind(this@scrollpane.heightProperty())

            columns.add(indexColumn)
            column("FleetNo", Truck::fleetNo).contentWidth(padding = 20.0, useAsMin = true)
            column("PlateNo", Truck::plateNo).contentWidth(padding = 20.0, useAsMin = true)

            onUserSelect {
                val currentUser = Account.currentUser.get()
//                if (currentUser == it //can edit own account
                //is authorized to edit account
//                    || currentUser.isAuthorised(AccessType.EDIT_USER)) {
                val scope = ModelEditScope(TruckModel())
                editModel(scope, it, EditTruckDetailsController::class)
//                }
            }

            placeholder = Label("No trucks here yet.")

            columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
            vgrow = Priority.ALWAYS
        }
    }


    override fun onCreate() {
        super.onCreate()
        val scope = ModelEditScope(TruckModel())
        editModel(scope, Truck(), TruckRegistrationController::class)
    }


    override suspend fun loadModels(user: User): ObservableList<Truck> {
        val loadResults = truckRepo.loadAll()
        return if (loadResults is Results.Success<*>) {
            loadResults.data as? ObservableList<Truck> ?: observableListOf()
        } else observableListOf()
    }

    override fun onDelete() {
        super.onDelete()
        GlobalScope.launch {
            val results = tableView.selectedItem?.let { truckRepo.deleteModel(toUpdate = it) }
            if (results is Results.Success<*>)
                onRefresh()
            else results?.let { parseResults(it) }
        }
    }
}