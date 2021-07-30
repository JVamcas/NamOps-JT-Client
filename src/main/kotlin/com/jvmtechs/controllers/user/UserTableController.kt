package com.jvmtechs.controllers.user

import com.jvmtechs.controllers.AbstractModelTableController
import com.jvmtechs.model.User
import com.jvmtechs.model.UserGroup
import com.jvmtechs.model.UserModel
import com.jvmtechs.utils.Results
import javafx.collections.ObservableList
import javafx.scene.Parent
import javafx.scene.control.Label
import javafx.scene.control.TableView
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import tornadofx.*

class UserTableController: AbstractModelTableController<User>("User Details") {


    override val root: Parent = scrollpane {

        tableView = tableview(modelList) {

            smartResize()
            prefWidthProperty().bind(this@scrollpane.widthProperty())
            prefHeightProperty().bind(this@scrollpane.heightProperty())

            columns.add(indexColumn)
            column("First Name", User::firstName).contentWidth(padding = 20.0, useAsMin = true)
            column("Last Name", User::lastName).contentWidth(padding = 20.0, useAsMin = true)
            column("User Group", User::userGroup)
//                .cellFormat {
//                style {
//                    if (it == UserGroup.Admin.name) {
//                        backgroundColor += c("#8b0000")
//                        textFill = Color.WHITE
//                    } else {
//                        backgroundColor += Color.WHITE
//                        textFill = Color.BLACK
//                    }
//                }
//            }
            onUserSelect {
                val currentUser = Account.currentUser.get()
//                if (currentUser == it //can edit own account
                //is authorized to edit account
//                    || currentUser.isAuthorised(AccessType.EDIT_USER)) {
                val scope = ModelEditScope(UserModel())
                editModel(scope, it, EditUserDetailsController::class)
//                }
            }

            placeholder = Label("No users here yet.")

            columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
            vgrow = Priority.ALWAYS
        }
    }


    override fun onCreate() {
        super.onCreate()
        val scope = ModelEditScope(UserModel())
        editModel(scope, User(), NewUserController::class)
    }

    override suspend fun loadModels(user: User): ObservableList<User> {
        val loadResults = userRepo.loadAll()
        return if (loadResults is Results.Success<*>) {
            loadResults.data as? ObservableList<User> ?: observableListOf()
        } else observableListOf()
    }

    override fun onDelete() {
        super.onDelete()
        GlobalScope.launch {
            val results = tableView.selectedItem?.let { userRepo.deleteModel(toUpdate = it) }
            if (results is Results.Success<*>)
                onRefresh()
            else results?.let { parseResults(it) }
        }
    }

    override fun onUndock() {
        super.onUndock()
        workspace.viewStack.remove(this)
    }
}