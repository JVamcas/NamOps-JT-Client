package com.jvmtechs.controllers.trucks

import com.jfoenix.controls.JFXButton
import com.jvmtechs.controllers.AbstractModelTableController
import com.jvmtechs.controllers.AbstractView
import com.jvmtechs.model.Truck
import com.jvmtechs.model.TruckModel
import com.jvmtechs.repo.TruckRepo
import com.jvmtechs.utils.ParseUtil.Companion.clearFields
import com.jvmtechs.utils.ParseUtil.Companion.isValidPlateNo
import com.jvmtechs.utils.ParseUtil.Companion.isValidVehicleNo
import com.jvmtechs.utils.ParseUtil.Companion.toUppercase
import com.jvmtechs.utils.ParseUtil.Companion.updateItem
import com.jvmtechs.utils.Results
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.controlsfx.control.textfield.CustomTextField
import tornadofx.*

open class TruckRegistrationController : AbstractView("") {

    val tableScope = super.scope as AbstractModelTableController<Truck>.ModelEditScope
    val truckModel = tableScope.viewModel as TruckModel

    val truckRepo = TruckRepo(
        user = Account.currentUser.get(),
        updateUrl = "truck_add_update",
        loadUrl = "trucks_fetch_all",
        modelName = "truck"
    )

    override val root: BorderPane by fxml("/NewTruckView.fxml")
    private val fleetNo: CustomTextField by fxid("fleetNo")
    private val plateNo: CustomTextField by fxid("plateNo")
    val truckGPane: GridPane by fxid("truckGPane")

    val saveBtn: JFXButton by fxid("saveBtn")
    private val clearBtn: JFXButton by fxid("clearBtn")


    init {
        fleetNo.apply {
            bind(truckModel.fleetNo)
            validator(ValidationTrigger.OnChange()) { if (it.isValidVehicleNo()) null else error("Invalid fleet number.") }
        }.toUppercase()

        plateNo.apply {
            bind(truckModel.plateNo)
            validator(ValidationTrigger.OnChange()) { if (it.isValidPlateNo()) null else error("Invalid plate number.") }
        }.toUppercase()

        saveBtn.apply {
            enableWhen { truckModel.valid }
            action {
                truckModel.commit()
                GlobalScope.launch {
                    val truck = truckModel.item
                    val results = truckRepo.addOrUpdateModel( toUpdate = truck)
                    if (results is Results.Success<*>) {
                        val updatedTruck = results.data as? Truck
                        tableScope.tableData.updateItem(updatedTruck)
                        closeView()
                        return@launch
                    }
                    parseResults(results)
                }
            }
        }

        clearBtn.apply {
            enableWhen { truckModel.dirty }
            action { truckModel.rollback()}
        }

        truckModel.validate(decorateErrors = false)
    }

    override fun onDock() {
        super.onDock()
        modalStage?.isResizable = false
        title = "Truck Registration"
    }
}