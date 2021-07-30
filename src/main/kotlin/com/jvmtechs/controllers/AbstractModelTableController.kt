package com.jvmtechs.controllers

import com.jvmtechs.model.AbstractModel
import com.jvmtechs.model.User
import com.jvmtechs.repo.TruckRepo
import com.jvmtechs.repo.UserRepo
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.control.Alert
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.text.Text
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import tornadofx.*
import kotlin.reflect.KClass

abstract class AbstractModelTableController<T : AbstractModel>(title: String) : AbstractView(title) {

    val modelList = observableListOf<T>()
    val indexColumn = TableColumn<T, String>("No.")
    var tableView: TableView<T> by singleAssign()

    val userRepo = UserRepo(
        user = Account.currentUser.get(),
        updateUrl = "user_add_update",
        loadUrl = "users_fetch_all",
        modelName = "user"
    )

    val truckRepo = TruckRepo(
        user = Account.currentUser.get(),
        updateUrl = "truck_add_update",
        loadUrl = "trucks_fetch_all",
        modelName = "truck"
    )

    init {
        indexColumn.apply {
            contentWidth(padding = 5.0, useAsMin = true)
            style = "-fx-alignment: CENTER;"
            cellValueFactory = PropertyValueFactory("")
            setCellFactory {
                object : TableCell<T?, String>() {
                    override fun updateIndex(index: Int) {
                        super.updateIndex(index)
                        if (isEmpty || index < 0) {
                            setText(null)
                        } else {
                            setText((index + 1).toString())
                        }
                    }
                }
            }
        }
        modelList.addListener(ListChangeListener {
            tableView.refresh()
        })
    }

    fun TableView<T>.enableAutoResize() {
        modelList.addListener(ListChangeListener {
            if (modelList.isNotEmpty()) {
                tableView.autoResizeColumns()
            }
        })
    }

    private fun <T> TableView<T>.autoResizeColumns() {
        columnResizePolicy = TableView.UNCONSTRAINED_RESIZE_POLICY
        columns.forEach { column ->
            column.setPrefWidth(
                (((0 until items.size).mapNotNull {
                    column.getCellData(it)
                }.map {
                    Text(it.toString()).layoutBounds.width
                }.toMutableList()).maxOrNull() ?: 40.0) + 40.0
            )
        }
    }

    fun showMissingDateWarning() {
        alert(
            header = "Warning!!",
            type = Alert.AlertType.WARNING,
            title = "Missing date params!!",
            content = "The start and end dates are required.",
            owner = this@AbstractModelTableController.currentWindow
        )
    }

    fun showNoParamWarning() {
        alert(
            header = "Whooo-Hooo",
            type = Alert.AlertType.WARNING,
            title = "Missing Query Params",
            content = "Yeah, we all hate typing but please enter at least one query param.",
            owner = this@AbstractModelTableController.currentWindow
        )
    }

    override fun onDock() {
        super.onDock()
        disableSave()
        onRefresh()
    }

    override fun onRefresh() {
        super.onRefresh()
//        tableView.placeholder = progressbar {}
        //load user data here from db
        GlobalScope.launch {
            val models = loadModels(Account.currentUser.get())
            modelList.asyncItems { models }
        }
    }

    fun <J : View> editModel(editScope: ModelEditScope, model: T, tClass: KClass<J>) {
        editScope.viewModel.item = model// the model to be edited

        setInScope(editScope.viewModel, editScope)

        find(tClass, editScope).openModal()
    }

    abstract suspend fun loadModels(user: User): ObservableList<T>

    inner class ModelEditScope(val viewModel: ItemViewModel<T>) : Scope() {
        //default user
        val tableData by lazy { modelList }
    }
}

