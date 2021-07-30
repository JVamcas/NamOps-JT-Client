package com.jvmtechs.controllers.home

import com.jfoenix.controls.JFXButton
import com.jvmtechs.controllers.AbstractModelTableController
import com.jvmtechs.controllers.AbstractView
import com.jvmtechs.model.*
import com.jvmtechs.repo.JobRepo
import com.jvmtechs.repo.JobSeqRepo
import com.jvmtechs.repo.TripRepo
import com.jvmtechs.repo.UserRepo
import com.jvmtechs.utils.DateUtil
import com.jvmtechs.utils.ParseUtil.Companion.generalTxtFieldValidation
import com.jvmtechs.utils.ParseUtil.Companion.isValidContainerNo
import com.jvmtechs.utils.ParseUtil.Companion.pickerBind
import com.jvmtechs.utils.ParseUtil.Companion.toUppercase
import com.jvmtechs.utils.ParseUtil.Companion.toYesNo
import com.jvmtechs.utils.ParseUtil.Companion.updateItem
import com.jvmtechs.utils.ParseUtil.Companion.validateEntryNo
import com.jvmtechs.utils.ParseUtil.Companion.validateQuickBooksNo
import com.jvmtechs.utils.Results
import javafx.application.Platform
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.TableView
import javafx.scene.control.TextArea
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import jfxtras.scene.control.LocalDateTimeTextField
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.controlsfx.control.CheckComboBox
import org.controlsfx.control.tableview2.FilteredTableView
import org.controlsfx.control.textfield.CustomTextField
import tornadofx.*
import java.time.LocalDateTime

open class NewJobController : AbstractView("") {

    override val root: VBox by fxml("/NewJobView.fxml")
    private val tableScope = super.scope as AbstractModelTableController<Job>.ModelEditScope
    private val jobModel = tableScope.viewModel as JobModel
    private val containerModel = ContainerModel()

    private val entryNo: CustomTextField by fxid("entryNo")
    private val legIdx: CustomTextField by fxid("legIdx")
    private val qty: CustomTextField by fxid("qty")
    private val code: CustomTextField by fxid("code")
    private val uom: CustomTextField by fxid("uom")
    private val qBCode: CustomTextField by fxid("qBCode")
    private val description: TextArea by fxid("description")
    private val memo: TextArea by fxid("memo")
    private val driverListCombo: CheckComboBox<User> by fxid("driverListCombo")

    private val dtmStartHBox: HBox by fxid("dtmStartHBox")
    private val dtmDueHBox: HBox by fxid("dtmDueHBox")
    private val pickUpLocation: CustomTextField by fxid("pickUpLocation")
    private val deliveryLocation: CustomTextField by fxid("deliveryLocation")
    private val priceLessTax: CustomTextField by fxid("priceLessTax")
    private var dtmStartPicker: LocalDateTimeTextField
    private var dtmEndPicker: LocalDateTimeTextField
    private val jobIdxProperty = SimpleIntegerProperty(0)
    private val jobNoPrefixProperty = SimpleStringProperty("")
    private val jobNoLabel: Label by fxid("jobNoLabel")

    private val saveBtn: JFXButton by fxid("saveBtn")
    private val clearBtn: JFXButton by fxid("clearBtn")

    private val containerTable: FilteredTableView<Container> by fxid("containerTable")
    private val addContainerBtn: JFXButton by fxid("addContainerBtn")
    private val containerNo: CustomTextField by fxid("containerNo")
    private val isJobDraft: CheckBox by fxid("isJobDraft")

    private val jobRepo = JobRepo(
        user = Account.currentUser.get(),
        updateUrl = "job_add_update",
        loadUrl = "jobs_fetch_all",
        modelName = "job",
        queryUrl = "job_query"
    )

    private val jobSeqRepo = JobSeqRepo(
        user = Account.currentUser.get(),
        updateUrl = "",
        loadUrl = "next_job_index",
        modelName = "",
        queryUrl = ""
    )

    private val userRepo = UserRepo(
        user = Account.currentUser.get(),
        updateUrl = "user_add_update",
        loadUrl = "users_fetch_all",
        modelName = "user"
    )
    private val tripRepo = TripRepo(
        user = Account.currentUser.get(),
        loadUrl = "trip_fetch_all",
        modelName = "trip",
        queryUrl = "trip_query",
    )

    init {

        // ---------------------------------------- Job Start -----------------------------------------------------------

        jobIdxProperty.addListener { _, _, newValue -> jobNoPrefixProperty.value = String.format("JC%04d", newValue) }

        jobModel.entryNo.addListener { _, _, newValue ->
            val jobNo = "${jobNoPrefixProperty.get()}$newValue"
            jobModel.jobNo.value = jobNo
        }

        //when updating, the prefix will be constructed from the id
        jobNoPrefixProperty.value = String.format("JC%04d", jobModel.item.id)

        jobNoLabel.apply { bind(jobModel.jobNo) }

        entryNo.apply {
            bind(jobModel.entryNo)
            validateEntryNo()
        }

        //listen for tick on checkbox
        isJobDraft.apply {
            val status = jobModel.item.status
            selectedProperty().set(
                status == JobStatus.Ready.name
            )
            selectedProperty().addListener { _, _, newValue ->
                jobModel.item.status = if (newValue) JobStatus.Ready.name else JobStatus.Draft.name
            }
        }

        legIdx.apply {
            bind(jobModel.legIdx)
            generalTxtFieldValidation(msg = "Enter Leg No.", len = 1)
        }

        qty.apply {
            bind(jobModel.qty)
//            generalTxtFieldValidation(msg = "Enter quantity.", len = 1)
        }

        uom.apply {
            bind(jobModel.uom)
//            generalTxtFieldValidation(msg = "Enter unit of measurement.", len = 1)
        }

        code.apply { bind(jobModel.code) }

        qBCode.apply {
            bind(jobModel.qBCode)
            validateQuickBooksNo()
        }

        driverListCombo.apply {
            //initially use the selected drivers
            checkModel.checkedItems.addListener(ListChangeListener {
                jobModel.driverList.setValue(it.list.asObservable())
            })
        }

        description.apply {
            bind(jobModel.description)
            generalTxtFieldValidation(msg = "Description must be at least 10 characters.", len = 10)
        }
        memo.apply { bind(jobModel.memo) }

        pickUpLocation.apply {
            bind(jobModel.pickUpLocation)
            generalTxtFieldValidation(msg = "Description must be at least 4 characters.", len = 2)
        }
        deliveryLocation.apply {
            bind(jobModel.deliveryLocation)
            generalTxtFieldValidation(msg = "Description must be at least 4 characters.", len = 2)
        }
        priceLessTax.apply {
            bind(jobModel.priceLessTax)
            validator(ValidationTrigger.OnChange()) {
                if (text?.toFloatOrNull() ?: -1.0f > 50.0f)
                    null else error("Price must at least be greater than NAD 50.00")
            }
        }

        dtmStartHBox.apply {
            dtmStartPicker = LocalDateTimeTextField().apply {
                jobModel.dtmStart.pickerBind(localDateTimeProperty() as SimpleObjectProperty<LocalDateTime?>)
                prefWidthProperty().bind(dtmStartHBox.widthProperty())
                promptText = "Select date and time on which job is to start."
            }
            add(dtmStartPicker)
        }

        dtmDueHBox.apply {
            dtmEndPicker = LocalDateTimeTextField().apply {
                jobModel.dtmDue.pickerBind(localDateTimeProperty() as SimpleObjectProperty<LocalDateTime?>)
                prefWidthProperty().bind(dtmDueHBox.widthProperty())
                promptText = "Select date for this job."
            }
            add(dtmEndPicker)
        }

        jobModel.item.containerList.addListener(
            ListChangeListener {
                val jobLog = jobModel.item.id
                val newCost = (jobModel.item.priceLessTax ?: 0.0f) / jobModel.item.containerList.size
                GlobalScope.launch {
                    val results = tripRepo.updateTripCost(jobLog = jobLog, newCost = newCost)
                    if (results is Results.Error) {
                        parseResults(results)
                    }
                }
            }
        )

        saveBtn.apply {
            enableWhen { jobModel.valid }
            action {
                jobModel.commit()
                GlobalScope.launch {
                    val job = jobModel.item
                    if (job.id == null) {
                        job.apply {
                            dtmPosted = DateUtil.localDateTimeToday()
                        }
                    }
                    val results = jobRepo.addOrUpdateModel(toUpdate = job)
                    if (results is Results.Success<*>) {
                        val updatedJC = results.data as? Job
                        tableScope.tableData.updateItem(updatedJC)
                        jobModel.item = Job()
                        closeView()
                        return@launch
                    }
                    parseResults(results)
                }
            }
        }

        clearBtn.apply {
            enableWhen { jobModel.dirty }
            action { jobModel.item = Job() }
        }
        jobModel.validate(decorateErrors = false)

        // -------------------------- Start of Job Containers -----------------------------------
        val containersOnTable = jobModel.item.containerList
        containerTable.apply {
            items = containersOnTable
            smartResize()
            columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
            vgrow = Priority.ALWAYS
            placeholder = label("No containers here. Enter the container number in the box below.")

            column("Container No", Container::containerNo)
            column("Picked Up", Container::pickedUp).toYesNo()
            column("Delivered", Container::delivered).toYesNo()

            onUserSelect { containerModel.item = it }

            contextmenu {
                item("Remove Container") {
                    action {
                        GlobalScope.launch {
                            val results = selectedItem?.let { leg ->
                                jobModel.item.containerList.removeIf { it.id == leg.id }
                                jobRepo.addOrUpdateModel(jobModel.item)
                            }
                            if (results is Results.Success<*>) {
                                val updatedJC = results.data as? Job
                                jobModel.itemProperty.set(updatedJC)
                                tableScope.tableData.updateItem(updatedJC)
                                containersOnTable.setAll(updatedJC?.containerList)
                            } else results?.let { parseResults(it) }
                        }
                    }
                }
            }
        }
        containerNo.apply {
            bind(containerModel.containerNo)
            validator(ValidationTrigger.OnChange()) {
                if (text.isValidContainerNo())
                    null else error("Invalid container number.")
            }
        }.toUppercase()

        addContainerBtn.apply {
            enableWhen { containerModel.valid }
            action {
                containerModel.commit()
                val job = jobModel.item
                val container = containerModel.item

                job.containerList.updateItem(container)

                GlobalScope.launch {
                    val results = jobRepo.addOrUpdateModel(toUpdate = job)
                    if (results is Results.Success<*>) {
                        val updatedJC = results.data as? Job
                        jobModel.itemProperty.set(updatedJC)
                        tableScope.tableData.updateItem(updatedJC)
                        containersOnTable.setAll(updatedJC?.containerList)
                        containerModel.item = Container()
                        return@launch
                    }
                    parseResults(results)
                }
            }
        }
        containerModel.validate(decorateErrors = false)
    }


    override fun onDock() {
        super.onDock()
        modalStage?.isResizable = false
        title = "Add New Job"

        if (this !is UpdateJobController) {
            GlobalScope.launch {
                val results = jobSeqRepo.loadAll()
                if (results is Results.Success<*>) {
                    val seq = (results.data as ObservableList<JobSeq>).firstOrNull()
                    val idx = seq?.lastValue ?: -1
                    jobIdxProperty.set(if (idx == -1) 0 else idx + 1)
                }
            }
        }

        GlobalScope.launch {
            val results1 = userRepo.loadAll()
            val driverList = if (results1 is Results.Success<*>) {
                results1.data as? ObservableList<User> ?: observableListOf()
            } else observableListOf()
            driverListCombo.apply {
                items.setAll(driverList.filter { it.userGroup == UserGroup.Driver.name }.asObservable())
                Platform.runLater {
                    jobModel.item.driversList.forEach { checkModel.check(it) }
                }
            }
        }
    }
}