package com.jvmtechs.controllers.user

import com.jfoenix.controls.JFXButton
import com.jvmtechs.controllers.AbstractModelTableController
import com.jvmtechs.controllers.AbstractView
import com.jvmtechs.model.User
import com.jvmtechs.model.UserGroup
import com.jvmtechs.model.UserModel
import com.jvmtechs.repo.UserRepo
import com.jvmtechs.utils.ParseUtil.Companion.generalTxtFieldValidation
import com.jvmtechs.utils.ParseUtil.Companion.optionalTxtFieldValidation
import com.jvmtechs.utils.ParseUtil.Companion.updateItem
import com.jvmtechs.utils.Results
import javafx.scene.control.Tooltip
import javafx.scene.layout.GridPane
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.controlsfx.control.SearchableComboBox
import org.controlsfx.control.textfield.CustomPasswordField
import org.controlsfx.control.textfield.CustomTextField
import tornadofx.*

open class NewUserController : AbstractView("") {

    override val root: GridPane by fxml("/NewUserView.fxml")
    private val tableScope = super.scope as AbstractModelTableController<User>.ModelEditScope
    private val userModel = tableScope.viewModel as UserModel

    private val userRepo = UserRepo(
        user = Account.currentUser.get(),
        updateUrl = "user_add_update",
        loadUrl = "users_fetch_all",
        modelName = "user"
    )

    private val fname: CustomTextField by fxid("fname")
    private val lname: CustomTextField by fxid("lname")
    private val username: CustomTextField by fxid("username")
    private val password: CustomPasswordField by fxid("password")
    private val userGroup: SearchableComboBox<String> by fxid("userGroup")

    val saveBtn: JFXButton by fxid("saveBtn")
    private val clearBtn: JFXButton by fxid("clearBtn")

    init {

        fname.apply {
            bind(userModel.firstName)
            generalTxtFieldValidation("First name must at least have 4 chars.", 3)
        }
        lname.apply {
            bind(userModel.lastName)
            generalTxtFieldValidation("Last name must at least have 4 chars.", 3)
        }
        userGroup.apply {
            bindCombo(userModel.userGroup)
            tooltip = Tooltip("Select user group.")
            val groups = UserGroup.values().map { it.name }.toList().asObservable()
            items = groups
        }
        username.apply {
            bind(userModel.username)
            optionalTxtFieldValidation("Username must at least have 5 chars.", 5)
        }

        password.apply {
            bind(userModel.password)
            optionalTxtFieldValidation("Password must at least have 4 chars.", 4)
        }

        saveBtn.apply {
            enableWhen { userModel.valid }
            action {
                userModel.commit()
                GlobalScope.launch {
                    val userToAdd = userModel.item
                    val results = userRepo.addOrUpdateModel(toUpdate = userToAdd)
                    if (results is Results.Success<*>) {
                        val updatedUser = results.data as? User
                        tableScope.tableData.updateItem(updatedUser)
                        closeView()
                        return@launch
                    }
                    parseResults(results)
                }
            }
        }

        clearBtn.apply {
            enableWhen { userModel.dirty }
            action { userModel.rollback() }
        }
        userModel.validate(decorateErrors = false)
    }


    override fun onDock() {
        super.onDock()
        modalStage?.isResizable = false
        title = "Add User"
    }
}