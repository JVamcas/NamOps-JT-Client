package com.jvmtechs.controllers.performance

import com.jfoenix.controls.JFXButton
import com.jvmtechs.controllers.AbstractModelTableController
import com.jvmtechs.model.*
import com.jvmtechs.repo.TripRepo
import com.jvmtechs.utils.DateTimePicker
import com.jvmtechs.utils.ParseUtil.Companion.bindPicker
import com.jvmtechs.utils.ParseUtil.Companion.listToString
import com.jvmtechs.utils.Results
import javafx.application.Platform
import javafx.collections.ObservableList
import javafx.scene.control.Alert
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.controlsfx.control.SearchableComboBox
import org.controlsfx.control.textfield.CustomTextField
import tornadofx.*

class TripHomeController : AbstractModelTableController<Trip>("Trips") {

    private val tripRepo = TripRepo(
        user = Account.currentUser.get(),
        loadUrl = "trip_fetch_all",
        modelName = "trip",
        queryUrl = "trip_query",
    )

    override val root: BorderPane by fxml("/TripHomeView.fxml")
    private val truckCombo: SearchableComboBox<Truck> by fxid("truckCombo")
    private val driverCombo: SearchableComboBox<User> by fxid("driverCombo")
    private val containerNo: CustomTextField by fxid("containerNo")
    private val fromDateHBox: HBox by fxid("fromDateHBox")
    private val toDateHBox: HBox by fxid("toDateHBox")

    private var toDatePicker: DateTimePicker
    private var fromDatePicker: DateTimePicker

    private val tripQryBtn: JFXButton by fxid("tripQryBtn")
    private val tripClearBtn: JFXButton by fxid("tripClearBtn")

    private val tripQryModel = TripQueryModel()

    init {
        truckCombo.apply {
            bindCombo(tripQryModel.truck)
        }
        driverCombo.apply {
            bindCombo(tripQryModel.driver)
        }
        containerNo.bind(tripQryModel.containerNo)
        fromDateHBox.apply {
            fromDatePicker = DateTimePicker().apply {
                prefWidthProperty().bind(fromDateHBox.widthProperty())
                bindPicker(tripQryModel.fromDate)
            }
            add(fromDatePicker)
        }

        toDateHBox.apply {
            toDatePicker = DateTimePicker().apply {
                bindPicker(tripQryModel.toDate)
                prefWidthProperty().bind(toDateHBox.widthProperty())
            }
            add(toDatePicker)
        }

        tripClearBtn.apply {
            action {
                tripQryModel.item = TripQuery()
                fromDatePicker.valueProperty().set(null)
                toDatePicker.valueProperty().set(null)
            }
        }
        tripQryBtn.apply {
            enableWhen { tripQryModel.valid }
            action {
                tripQryModel.commit()
                val query = tripQryModel.item
                if (query.containerNo.isNullOrEmpty()
                    && query.driver == null
                    && query.truck == null
                    && query.fromDate == null
                    && query.toDate == null
                ) {
                    showNoParamWarning()
                    return@action
                } else if ((query.fromDate != null && query.toDate == null) || (query.toDate != null && query.fromDate == null)) {
                   showMissingDateWarning()
                    return@action
                }

                GlobalScope.launch {
                    val results = tripRepo.queryModel(query)
                    if (results is Results.Success<*>)
                        modelList.asyncItems { results.data as List<Trip> }
                    else parseResults(results)
                }
            }
        }

        root.apply {
            center {
                scrollpane {

                    tableView = tableview(modelList) {
                        placeholder = progressbar { title = "Loading pending jobs. Please wait..." }
                        enableAutoResize()
                        smartResize()
                        prefWidthProperty().bind(this@scrollpane.widthProperty())
                        prefHeightProperty().bind(this@scrollpane.heightProperty())
                        items = modelList
                        columns.add(indexColumn)
                        column("Started", Trip::dtmStarted).contentWidth(
                            padding = 20.0,
                            useAsMin = true
                        )
                        column("Completed", Trip::dtmCompleted).contentWidth(
                            padding = 20.0,
                            useAsMin = true
                        )
                        column("Truck", Trip::truck).contentWidth(
                            padding = 20.0,
                            useAsMin = true
                        )
                        column("Driver", Trip::driver).contentWidth(
                            padding = 20.0,
                            useAsMin = true
                        )
                        column("PickUp Location", Trip::pickUpLocation).contentWidth(
                            padding = 20.0,
                            useAsMin = true
                        )
                        column("Delivery Location", Trip::deliveryLocation).contentWidth(
                            padding = 20.0,
                            useAsMin = true
                        )
                        column("Containers", Trip::containerList).contentWidth(
                            padding = 20.0,
                            useAsMin = true
                        ).listToString()

                        column("Cost", Trip::priceLessTax).contentWidth(
                            padding = 20.0,
                            useAsMin = true
                        )

                        column("Description", Trip::description).contentWidth(
                            padding = 20.0,
                            useAsMin = true
                        )
                        column("Memo", Trip::memo).contentWidth(
                            padding = 20.0,
                            useAsMin = true
                        )
                    }
                }
            }
        }
    }

    override fun onDock() {
        super.onDock()
        disableCreate()
        disableDelete()
    }


    override suspend fun loadModels(user: User): ObservableList<Trip> {
        val results = truckRepo.loadAll()
        val truckList = if (results is Results.Success<*>) {
            results.data as? ObservableList<Truck> ?: observableListOf()
        } else observableListOf()
        truckCombo.asyncItems { truckList }

        val results1 = userRepo.loadAll()
        val driverList = if (results1 is Results.Success<*>) {
            results1.data as? ObservableList<User> ?: observableListOf()
        } else observableListOf()
        driverCombo.asyncItems {
            driverList.filter { it.userGroup == UserGroup.Driver.name }.asObservable()
        }

        val loadResults = tripRepo.loadAll()
        val data =  if (loadResults is Results.Success<*>) {
            val tripList = loadResults.data as ObservableList<Trip>
            tripList
        } else {
            parseResults(loadResults)
            observableListOf()
        }

        Platform.runLater {
            tableView.placeholder = label("No trips found.")
        }
        return data
    }
}