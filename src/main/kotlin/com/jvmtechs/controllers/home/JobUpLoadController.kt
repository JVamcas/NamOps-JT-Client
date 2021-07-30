package com.jvmtechs.controllers.home

import com.jfoenix.controls.JFXButton
import com.jvmtechs.controllers.AbstractModelTableController
import com.jvmtechs.model.*
import com.jvmtechs.repo.JobRepo
import com.jvmtechs.utils.DateUtil.Companion.localDateTimeToday
import com.jvmtechs.utils.ParseUtil.Companion.flagDueToday
import com.jvmtechs.utils.ParseUtil.Companion.listToString
import com.jvmtechs.utils.ParseUtil.Companion.localDateTimeFormat
import com.jvmtechs.utils.ParseUtil.Companion.moneyFormat
import com.jvmtechs.utils.ParseUtil.Companion.toUppercase
import com.jvmtechs.utils.Results
import javafx.application.Platform
import javafx.collections.ObservableList
import javafx.scene.control.ScrollPane
import javafx.scene.control.TableView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Priority
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.util.CellReference
import org.apache.poi.xssf.usermodel.XSSFCell
import org.apache.poi.xssf.usermodel.XSSFRow
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import tornadofx.*
import java.io.File
import java.time.LocalDateTime


class JobUpLoadController : AbstractModelTableController<Job>("Job Upload") {

    override val root: BorderPane by fxml("/JobUpLoadView.fxml")
    private val tableScope = super.scope as AbstractModelTableController<Job>.ModelEditScope
    private val uploadedFileName = params["uploadedFileName"] as String

    private val scrollPane: ScrollPane by fxid("tableViewScrollPane")
    private val saveBtn: JFXButton by fxid("saveBtn")
    private val cancelBtn: JFXButton by fxid("cancelBtn")

    private val entryIdx = CellReference.convertColStringToIndex("X")
    private val lgIdx = CellReference.convertColStringToIndex("Y")
    private val qtyIdx = CellReference.convertColStringToIndex("AA")
    private val codeIdx = CellReference.convertColStringToIndex("AD")
    private val qbIdx = CellReference.convertColStringToIndex("AE")
    private val uomIdx = CellReference.convertColStringToIndex("AG")
    private val descriptionIdx = CellReference.convertColStringToIndex("AH")
    private val memoIdx = CellReference.convertColStringToIndex("BH")
    private val pickUpIdx = CellReference.convertColStringToIndex("BO")
    private val dropOffIdx = CellReference.convertColStringToIndex("BP")
    private val startDtIdx = CellReference.convertColStringToIndex("BQ")
    private val dueDtIdx = CellReference.convertColStringToIndex("BR")
    private val priceLessTaxIdx = CellReference.convertColStringToIndex("DF")

    private var jobColumnList: List<Int>
    private val contStartIdx = CellReference.convertColStringToIndex("DQ")

    private val jobRepo = JobRepo(
        user = Account.currentUser.get(),
        updateUrl = "job_add_update",
        loadUrl = "job_fetch_all",
        modelName = "job",
        queryUrl = "job_query"
    )

    init {
        tableView = tableview(modelList) {
            enableAutoResize()
            smartResize()
            prefWidthProperty().bind(scrollPane.widthProperty())
            prefHeightProperty().bind(scrollPane.heightProperty())
            columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
            vgrow = Priority.ALWAYS

            placeholder = progressbar {
                title = "Parsing MS Excel file. Please wait..."
            }

            columns.add(indexColumn)
            column("Status", Job::status).apply {
                cellFormat {
                    graphic = if (item == null) null
                    else
                        checkbox("Is Draft?") {
                            isSelected = item == JobStatus.Ready.name
                            selectedProperty().addListener { _, _, newValue ->
                                rowItem.status = if (newValue) JobStatus.Ready.name else JobStatus.Draft.name
                            }
                        }
                }
            }
            column("Entry No", Job::entryNo).apply {
                contentWidth(
                    padding = 1.0,
                    useAsMin = true
                )
            }.toUppercase()

            column("Code", Job::code).apply {
                contentWidth(
                    padding = 1.0,
                    useAsMin = true
                )
            }.toUppercase()

            column("Posted On", Job::dtmPosted).apply {
                contentWidth(
                    padding = 1.0,
                    useAsMin = true
                )
            }.localDateTimeFormat()

            column("Start On", Job::dtmStart).apply {
                contentWidth(
                    padding = 1.0,
                    useAsMin = true
                )
            }.localDateTimeFormat()

            column("Due On", Job::dtmDue).apply {
                contentWidth(
                    padding = 1.0,
                    useAsMin = true
                )
            }.flagDueToday().localDateTimeFormat()

            column("Quick Books Code", Job::qBCode).apply {
                contentWidth(
                    padding = 1.0,
                    useAsMin = true
                )
            }.toUppercase()

            column("Description", Job::description).apply {
                contentWidth(
                    padding = 1.0,
                    useAsMin = true
                )
            }.remainingWidth()

            column("Memo", Job::memo).apply {
                contentWidth(
                    padding = 1.0,
                    useAsMin = true
                )
            }.remainingWidth()

            column("Pick Up From", Job::pickUpLocation).apply {
                contentWidth(
                    padding = 1.0,
                    useAsMin = true
                )
            }

            column("Deliver To", Job::deliveryLocation).apply {
                contentWidth(
                    padding = 1.0,
                    useAsMin = true
                )
            }

            column("Price Less Tax", Job::priceLessTax).apply {
                contentWidth(
                    padding = 1.0,
                    useAsMin = true
                )
            }.moneyFormat()

            column("Containers", Job::containerList).apply {
                contentWidth(
                    padding = 1.0,
                    useAsMin = true
                )
            }.listToString()


            onUserSelect {
                val currentUser = Account.currentUser.get()
//                if (currentUser == it //can edit own account
                //is authorized to edit account
//                    || currentUser.isAuthorised(AccessType.EDIT_USER)) {
                val scope = ModelEditScope(JobModel())
                editModel(scope, it, UpdateJobController::class)
//                }
            }
        }
        jobColumnList = listOf(
            entryIdx,
            lgIdx,
            qtyIdx,
            codeIdx,
            qbIdx,
            uomIdx,
            descriptionIdx,
            memoIdx,
            pickUpIdx,
            dropOffIdx,
            startDtIdx,
            dueDtIdx,
            priceLessTaxIdx
        )

        runAsync { excelToJob() }

        root.center {
            add(tableView)
        }

        saveBtn.apply {
            action {
                GlobalScope.launch {
                    val batchResults = jobRepo.batchUpdate(modelList)
                    if (batchResults is Results.Success<*>)
                        workspace.dock<JobHomeController>()
                    else parseResults(batchResults)
                }
            }
        }

        cancelBtn.apply {
            action {
                workspace.dock<JobHomeController>()
            }
        }
    }

    private fun excelToJob() {
        val pkg: OPCPackage?
        val workbook: XSSFWorkbook?
        try {
            pkg = OPCPackage.open(File(uploadedFileName))
            workbook = XSSFWorkbook(pkg)
        } catch (e: Exception) {
            showError(
                header = "MS Excel Version Error",
                msg = " Make sure the file uploaded is at least MS Excel 2007 or later."
            )
            Platform.runLater {
                workspace.dock<JobHomeController>()
            }
            return
        }

        val sheet = workbook.getSheet("ImportData")

        if (sheet == null) {
            showError(header = "`ImportData` worksheet missing", msg = "No worksheet with name `ImportData` found.")
            Platform.runLater {
                workspace.dock<JobHomeController>()
            }
            return
        }

        var row: XSSFRow?
        try {
            var count = 0
            sheet.rowIterator().forEach { r ->
                row = r as XSSFRow
                row?.let {
                    if (count > 2 && !row.isEmpty()) {
                        val job = Job().apply {
                            dtmPosted = localDateTimeToday()
                            entryNo = it.getCell(entryIdx).toValue()
                            legIdx = it.getCell(lgIdx).toValue()
                            qty = it.getCell(qtyIdx).toValue()?.toIntOrNull()
                            code = it.getCell(codeIdx).toValue()
                            qBCode = it.getCell(qbIdx).toValue()
                            uom = it.getCell(uomIdx).toValue()
                            description = it.getCell(descriptionIdx).toValue()
                            memo = it.getCell(memoIdx).toValue()
                            pickUpLocation = it.getCell(pickUpIdx).toValue()
                            deliveryLocation = it.getCell(dropOffIdx).toValue()
                            dtmStart.set(it.getCell(startDtIdx).toDateValue())
                            dtmDue.set(it.getCell(dueDtIdx).toDateValue())
                            priceLessTax = it.getCell(priceLessTaxIdx).toValue()?.toFloatOrNull()
                        }
                        modelList.add(job)

                        val contList = mutableListOf<Container>()
                        (contStartIdx until contStartIdx + 100).forEach { idx ->
                            row?.getCell(idx)?.toValue()?.let { contNo ->
                                contList.add(Container().also { it.containerNo = contNo })
                            }
                        }
                        if (contList.isNotEmpty())
                            job.containerList.setAll(contList)
                    }
                }
                count++
            }
        } catch (e: Exception) {

            e.printStackTrace()
            showError(
                header = "Parse Error!!",
                msg = "Unable to parse the file. \n1.\tMake sure that the column headers are in row #3"
            )
        } finally {
            workbook.close()
            pkg?.close()
            Platform.runLater {
                tableView.placeholder = label("Failed to upload jobs...but you should not be seeing this.")
            }
        }
    }


    private fun XSSFCell?.toDateValue(): LocalDateTime? {
        return when {
            this == null -> null
            this.cellType == CellType.FORMULA -> {
                when (this.cachedFormulaResultType) {
                    CellType.NUMERIC -> {
                        if (this.rawValue == "0")
                            null
                        else this.localDateTimeCellValue
                    }
                    else -> null
                }
            }
            else -> null
        }
    }

    private fun XSSFCell?.toValue(): String? {
        return when {
            this == null -> null
            this.cellType == CellType.FORMULA -> {
                val value = when (this.cachedFormulaResultType) {
                    CellType.BOOLEAN -> this.booleanCellValue.toString()
                    CellType.NUMERIC -> this.numericCellValue.toString()
                    CellType.STRING -> this.stringCellValue
                    CellType.BLANK -> null
                    CellType.ERROR -> null
                    else -> null
                }
                if (value == "0.0") null else value
            }
            else -> null
        }
    }

    private fun XSSFRow?.isEmpty(): Boolean {
        return try {
            if (this != null) {
                jobColumnList.forEach {
                    val cellValue = this.getCell(it).toValue()
                    if (!cellValue.isNullOrEmpty())
                        return false
                }
            }
            return true
        } catch (e: Exception) {
            true
        }
    }

    override suspend fun loadModels(user: User): ObservableList<Job> {

        return observableListOf()
    }

    override fun onDock() {
        super.onDock()
        disableRefresh()
        disableCreate()
    }

//    override fun onCreate() {
//        super.onCreate()
//        val scope = ModelEditScope(JobModel())
////        setInScope(this, scope)
//        editModel(scope, Job(), NewJobController::class)
//    }

    override fun onUndock() {
        super.onUndock()
        workspace.viewStack.remove(this)
    }

    override fun onDelete() {
        super.onDelete()
        val selected = tableView.selectionModel.selectedItem
        modelList.removeIf { it.entryNo == selected.entryNo }
    }
}
