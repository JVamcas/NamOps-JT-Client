package com.jvmtechs.controllers.home

import com.jfoenix.controls.JFXButton
import com.jvmtechs.controllers.AbstractModelTableController
import com.jvmtechs.model.*
import com.jvmtechs.model.Job
import com.jvmtechs.repo.JobRepo
import com.jvmtechs.utils.ComboBoxEditingCell
import com.jvmtechs.utils.DateTimePicker
import com.jvmtechs.utils.DateUtil.Companion._24
import com.jvmtechs.utils.DateUtil.Companion.localDateTimeToday
import com.jvmtechs.utils.DateUtil.Companion.today
import com.jvmtechs.utils.ParseUtil.Companion.bindPicker
import com.jvmtechs.utils.ParseUtil.Companion.listToString
import com.jvmtechs.utils.ParseUtil.Companion.localDateTimeFormat
import com.jvmtechs.utils.ParseUtil.Companion.moneyFormat
import com.jvmtechs.utils.ParseUtil.Companion.toExcelSpreedSheet
import com.jvmtechs.utils.ParseUtil.Companion.toUppercase
import com.jvmtechs.utils.Results
import javafx.application.Platform
import javafx.collections.ObservableList
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import jxl.Workbook
import kotlinx.coroutines.*
import org.controlsfx.control.SearchableComboBox
import org.controlsfx.control.WorldMapView
import org.controlsfx.control.textfield.CustomTextField
import org.controlsfx.glyphfont.Glyph
import tornadofx.*
import java.io.File
import java.time.temporal.ChronoUnit


class JobHomeController : AbstractModelTableController<Job>("Pending Jobs") {

    override val root: BorderPane by fxml("/JobHomeView.fxml")
    private val qryModel = JobQueryModel()

    private val jobRepo = JobRepo(
        user = Account.currentUser.get(),
        updateUrl = "job_add_update",
        loadUrl = "jobs_fetch_all",
        modelName = "job",
        queryUrl = "jobs_query"
    )

    private val scrollPane: ScrollPane by fxid("tableViewScrollPane")

    lateinit var tableModel: TableViewEditModel<Job>

    private val worldMapView: WorldMapView by fxid("worldMapView")

    private val jobNo: TextField by fxid("jobNo")
    private val qbNo: TextField by fxid("qbNo")
    private val allocatedDriverCombo: SearchableComboBox<User> by fxid("allocatedDriverCombo")
    private val containerNo: CustomTextField by fxid("containerNo")
    private val fromDateHbox: HBox by fxid("fromDateHBox")
    private val toDateHbox: HBox by fxid("toDateHBox")

    private val saveBtn: JFXButton by fxid("saveBtn")
    private val clearBtn: JFXButton by fxid("clearBtn")
    private var toDatePicker: DateTimePicker
    private var fromDatePicker: DateTimePicker

    init {
        /**
         * Start of [JobQuery]
         * */

        qbNo.apply {
            bind(qryModel.qbNumber)
            toUppercase()
        }

        jobNo.apply {
            bind(qryModel.jobNo)
            toUppercase()
        }
        allocatedDriverCombo.apply {
            bindCombo(qryModel.allocatedDriver)
        }
        containerNo.apply {
            bind(qryModel.containerNo)
        }

        fromDateHbox.apply {
            fromDatePicker = DateTimePicker().apply {
                prefWidthProperty().bind(fromDateHbox.widthProperty())
                bindPicker(qryModel.fromDate)
            }
            add(fromDatePicker)
        }

        toDateHbox.apply {
            toDatePicker = DateTimePicker().apply {
                bindPicker(qryModel.toDate)
                prefWidthProperty().bind(toDateHbox.widthProperty())
            }
            add(toDatePicker)
        }

        clearBtn.apply {
            action {
                qryModel.item = JobQuery()
                fromDatePicker.valueProperty().set(null)
                toDatePicker.valueProperty().set(null)
            }
        }

        saveBtn.apply {
            enableWhen { qryModel.valid }
            action {
                qryModel.commit()
                val query = qryModel.item
                if (query.jobNo.isNullOrEmpty()
                    && query.qbNumber.isNullOrEmpty()
                    && query.allocatedDriver == null
                    && query.fromDate == null
                    && query.toDate == null
                ) {
                    showNoParamWarning()
                    return@action
                } else if ((query.fromDate != null && query.toDate == null) || (query.toDate != null && query.fromDate == null)) {
                    alert(
                        header = "Warning!!",
                        type = Alert.AlertType.WARNING,
                        title = "Missing date params!!",
                        content = "The start and end dates are required.",
                        owner = this@JobHomeController.currentWindow
                    )
                    return@action
                }

                GlobalScope.launch {
                    val results = jobRepo.queryModel(query)
                    if (results is Results.Success<*>)
                        modelList.asyncItems { results.data as List<Job> }
                    else parseResults(results)
                }
            }
        }
        qryModel.validate(decorateErrors = false)

        /**
         * End of [JobCardQuery]
         * */

        tableView = tableview(modelList) {
            //ensure table dimensions match the enclosing ScrollPane
            enableAutoResize()
            smartResize()
            prefWidthProperty().bind(scrollPane.widthProperty())
            prefHeightProperty().bind(scrollPane.heightProperty())
            columnResizePolicy = TableView.UNCONSTRAINED_RESIZE_POLICY
            items = modelList
            placeholder = progressbar { title = "Loading pending jobs. Please wait..." }
            columns.add(indexColumn)

            column("Status", Job::status).apply {
                contentWidth(
                    padding = 20.0,
                    useAsMin = true
                )
                setCellFactory {
                    ComboBoxEditingCell(
                        coll = JobStatus.values().map { it.name }.asObservable(),
                        repo = jobRepo,
                        view = this@JobHomeController
                    )
                }
            }
            column("Job No", Job::jobNo).apply {
                contentWidth(
                    padding = 20.0,
                    useAsMin = true
                )
            }.toUppercase()

            column("Code", Job::code).apply {
                contentWidth(
                    padding = 20.0,
                    useAsMin = true
                )
            }.toUppercase()

            column("Posted On", Job::dtmPosted).apply {
                contentWidth(
                    padding = 20.0,
                    useAsMin = true
                )
            }.localDateTimeFormat()

            column("Start On", Job::dtmStart).apply {
                contentWidth(
                    padding = 20.0,
                    useAsMin = true
                )
            }.localDateTimeFormat()

            column("Due On", Job::dtmDue).apply {
                contentWidth(
                    padding = 20.0,
                    useAsMin = true
                )
            }.localDateTimeFormat()
                .cellFormat {
                    text = it?._24()
                    style {
                        textFill = Color.BLACK
                        backgroundColor += when {
                            item == null -> Color.WHITE
                            ChronoUnit.HOURS.between(localDateTimeToday(), item) in 0..24
                                    || ChronoUnit.HOURS.between(localDateTimeToday(), item) <= 0 -> Color.RED
                            ChronoUnit.HOURS.between(localDateTimeToday(), item) in 24..60 -> Color.ORANGE
                            else -> Color.GREEN
                        }
                    }
                }

            column("Quick Books Code", Job::qBCode).apply {
                contentWidth(
                    padding = 20.0,
                    useAsMin = true
                )
            }
            column("Drivers", Job::driversList).apply {
                contentWidth(
                    padding = 20.0,
                    useAsMin = true
                )
            }.listToString()

            column("Containers", Job::containerList).apply {
                contentWidth(
                    padding = 20.0,
                    useAsMin = true
                )
            }.listToString()

            column("Description", Job::description).apply {
                contentWidth(
                    padding = 20.0,
                    useAsMin = true
                )
            }

            column("Memo", Job::memo).apply {
                contentWidth(
                    padding = 20.0,
                    useAsMin = true
                )
            }

            column("Pick Up From", Job::pickUpLocation).apply {
                contentWidth(
                    padding = 20.0,
                    useAsMin = true
                )
            }

            column("Deliver To", Job::deliveryLocation).apply {
                contentWidth(
                    padding = 20.0,
                    useAsMin = true
                )
            }

            column("Price Less Tax", Job::priceLessTax).apply {
                contentWidth(
                    padding = 20.0,
                    useAsMin = true
                )
            }.moneyFormat()

            onUserSelect {
                val currentUser = Account.currentUser.get()
//                if (currentUser == it //can edit own account
                //is authorized to edit account
//                    || currentUser.isAuthorised(AccessType.EDIT_USER)) {
                val scope = ModelEditScope(JobModel())
                editModel(scope, it, UpdateJobController::class)
//                }
            }
            tableModel = editModel

        }
        scrollPane.apply {
            add(tableView)
        }
    }

    override fun onDock() {
        super.onDock()
        disableSave()
        with(workspace) {
            button("Export") {
                addClass("icon-only")
                graphic = Glyph("FontAwesome", "DOWNLOAD")
                    .also {
                        it.size(16.0)
                        it.color(c("#767675"))
                    }
                action {
                    val fileChooser = FileChooser()
                    fileChooser.title = "Save As"
                    fileChooser.extensionFilters.addAll(
                        FileChooser.ExtensionFilter("Excel Workbook", "*.xlsx")
                    )
                    val selectedFile: File? = fileChooser.showSaveDialog(null)
                    selectedFile?.let {
                        runBlocking(Dispatchers.Default) {
                            launch {
                                toExcelSpreedSheet(
                                    wkb = Workbook.createWorkbook(it),
                                    sheetList = arrayListOf(modelList),
                                    sheetNameList = arrayListOf(
                                        "NamOps IPK Report - ${
                                            today().toLocalDateTime()._24()
                                        }"
                                    )
                                )
                            }
                        }
                    }
                }
            }
            button("Import") {
                addClass("icon-only")
                graphic = Glyph("FontAwesome", "UPLOAD")
                    .also {
                        it.size(16.0)
                        it.color(c("#767675"))
                    }
                action {
                    val fileChooser = FileChooser()
                    fileChooser.title = "Select File"
                    fileChooser.extensionFilters.addAll(FileChooser.ExtensionFilter("Excel Workbook", "*.xlsx"))
                    val selectedFile: File? = fileChooser.showOpenDialog(null)
                    selectedFile?.let { file ->
                        // pass the file to be parsed
                        if (file.exists()) {
                            val scope = ModelEditScope(JobModel())
                            setInScope(workspace, scope)
                            workspace.dock<JobUpLoadController>(
                                scope,
                                mapOf("uploadedFileName" to file.absolutePath)
                            )
                        }
                    }
                }
            }
        }
    }

    override suspend fun loadModels(user: User): ObservableList<Job> {

        val results1 = userRepo.loadAll()
        val driverList = if (results1 is Results.Success<*>) {
            results1.data as? ObservableList<User> ?: observableListOf()
        } else observableListOf()
        allocatedDriverCombo.asyncItems {
            driverList.filter { it.userGroup == UserGroup.Driver.name }.asObservable()
        }

        val loadResults = jobRepo.loadAll()

        val data = if (loadResults is Results.Success<*>)
            loadResults.data as ObservableList<Job>
        else observableListOf()
        Platform.runLater {
            tableView.placeholder = label("No pending jobs found.")
        }
        return data
    }

    override fun onCreate() {
        super.onCreate()
        val scope = ModelEditScope(JobModel())
        editModel(scope, Job(), NewJobController::class)
    }

    override fun onDelete() {
        super.onDelete()
        GlobalScope.launch {
            val results = tableView.selectedItem?.let { jobRepo.deleteModel(toUpdate = it) }
            if (results is Results.Success<*>)
                onRefresh()
            else results?.let { parseResults(it) }
        }
    }
}


