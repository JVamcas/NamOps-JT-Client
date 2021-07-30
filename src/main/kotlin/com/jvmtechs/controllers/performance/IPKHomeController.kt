package com.jvmtechs.controllers.performance

import com.jfoenix.controls.JFXButton
import com.jvmtechs.controllers.AbstractModelTableController
import com.jvmtechs.model.*
import com.jvmtechs.repo.IPKRepo
import com.jvmtechs.utils.DateTimePicker
import com.jvmtechs.utils.ParseUtil.Companion.bindPicker
import com.jvmtechs.utils.ParseUtil.Companion.localDateTimeFormat
import com.jvmtechs.utils.ParseUtil.Companion.moneyFormat
import com.jvmtechs.utils.ParseUtil.Companion.toSingleDecimal
import com.jvmtechs.utils.Results
import javafx.application.Platform
import javafx.collections.ObservableList
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.controlsfx.control.SearchableComboBox
import tornadofx.*

class IPKHomeController : AbstractModelTableController<IPK>("Income Per KM") {

    private val ipkRepo = IPKRepo(
        user = Account.currentUser.get(),
        loadUrl = "ipk_fetch_all",
        modelName = "ipk",
        queryUrl = "ipk_query"

    )
    override val root: BorderPane by fxml("/IPKView.fxml")

    private val iPKQryModel = IPKQueryModel()

    private val truckCombo: SearchableComboBox<Truck> by fxid("truckCombo")
    private val fromDateHBox: HBox by fxid("fromDateHBox")
    private val toDateHBox: HBox by fxid("toDateHBox")
    private val iPKQryBtn: JFXButton by fxid("iPKQryBtn")
    private val iPKClearBtn: JFXButton by fxid("iPKClearBtn")
    private var fromDatePicker: DateTimePicker
    private var toDatePicker: DateTimePicker

    init {

        root.apply {
            center {
                scrollpane {

                    tableView = tableview(modelList) {

                        smartResize()
                        prefWidthProperty().bind(this@scrollpane.widthProperty())
                        prefHeightProperty().bind(this@scrollpane.heightProperty())
                        placeholder = progressbar { }

                        columns.add(indexColumn)

                        column("Date", IPK::dtmCreated).contentWidth(padding = 5.0, useAsMin = true)
                            .localDateTimeFormat()
                        column("Truck", IPK::truck).contentWidth(padding = 5.0, useAsMin = true)
                        column("Starting (KM)", IPK::startKm).contentWidth(padding = 5.0, useAsMin = true)
                        column("Ending (KM)", IPK::endKm).contentWidth(padding = 5.0, useAsMin = true)
                        column("Distance Covered (KM)", IPK::distance).contentWidth(padding = 5.0, useAsMin = true)
                            .toSingleDecimal()
                        column("Income Today (NAD)", IPK::totalIncome).contentWidth(padding = 5.0, useAsMin = true)
                            .moneyFormat()
                        column("IPK", IPK::ipk).contentWidth(padding = 5.0, useAsMin = true).moneyFormat()
                        placeholder = Label("Nothing to display yet.")

                        columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
                        vgrow = Priority.ALWAYS
                    }
                }
            }
        }

        truckCombo.apply { bind(iPKQryModel.truck) }

        fromDateHBox.apply {
            fromDatePicker = DateTimePicker().apply {
                prefWidthProperty().bind(fromDateHBox.widthProperty())
                bindPicker(iPKQryModel.fromDate)
            }
            add(fromDatePicker)
        }

        toDateHBox.apply {
            toDatePicker = DateTimePicker().apply {
                prefWidthProperty().bind(toDateHBox.widthProperty())
                bindPicker(iPKQryModel.toDate)
            }
            add(toDatePicker)
        }
        iPKQryBtn.apply {
            action {
                iPKQryModel.commit()
                val query = iPKQryModel.item
                if (query.truck == null
                    && query.fromDate == null
                    && query.toDate == null
                ) {
                    showNoParamWarning()
                    return@action
                }
                else if ((query.fromDate != null && query.toDate == null) || (query.toDate != null && query.fromDate == null)) {
                    showMissingDateWarning()
                    return@action
                }

                GlobalScope.launch {
                    val results = ipkRepo.queryModel(query)
                    if (results is Results.Success<*>)
                        modelList.asyncItems { results.data as List<IPK> }
                    else parseResults(results)
                }
            }
        }

        iPKClearBtn.apply {
            action {
                iPKQryModel.item = IPKQuery()
                fromDatePicker.valueProperty().set(null)
                toDatePicker.valueProperty().set(null)
            }
        }
    }

    override suspend fun loadModels(user: User): ObservableList<IPK> {

        val results = truckRepo.loadAll()
        val truckList = if (results is Results.Success<*>) {
            results.data as? ObservableList<Truck> ?: observableListOf()
        } else observableListOf()
        truckCombo.asyncItems { truckList }

        val loadResults = ipkRepo.loadAll()
        val data = if (loadResults is Results.Success<*>) {
            val ipkList = loadResults.data as ObservableList<IPK>
            ipkList.forEach {
                it.totalIncome = it.distance * it.ipk
            }
            ipkList
        } else {
            parseResults(loadResults)
            observableListOf()
        }

        Platform.runLater {
            tableView.placeholder = label("No IPK found.")
        }
        return data
    }

    override fun onDock() {
        super.onDock()
        disableCreate()
        disableDelete()
    }
}