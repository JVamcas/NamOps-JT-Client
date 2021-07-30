package com.jvmtechs.controllers.trucks

import com.jvmtechs.utils.ParseUtil.Companion.clearFields
import com.jvmtechs.utils.ParseUtil.Companion.updateItem
import com.jvmtechs.utils.Results
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import tornadofx.*

class EditTruckDetailsController : TruckRegistrationController() {


    init {
        saveBtn.apply {
            enableWhen { truckModel.valid }
            action {
                truckModel.commit()
                GlobalScope.launch {
                    val truck = truckModel.item
                    val results = truckRepo.addOrUpdateModel(toUpdate = truck)
                    if (results is Results.Success<*>) {
                        truckGPane.clearFields()
                        closeView()
                        tableScope.tableData.updateItem(truckModel.item)
                        return@launch
                    }
                    parseResults(results)
                }
            }
        }
    }


    override fun onDock() {
        super.onDock()
        modalStage?.isResizable = false
        title = "Edit Truck Details"
    }
}