package com.jvmtechs.controllers

import com.jvmtechs.controllers.AbstractView.Account.currentUser
import com.jvmtechs.model.User
import com.jvmtechs.model.UserModel
import com.jvmtechs.repo.UserRepo
import javafx.application.Platform
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import tornadofx.*

class LoginController : AbstractView("") {

    override val root: BorderPane by fxml("/LoginView.fxml")
    private val userName: TextField by fxid("username")
    private val password: PasswordField by fxid("password")
    private val loginBtn: Button by fxid("loginBtn")
    private val invalidLoginLabel: Label by fxid("invalidLoginLabel")
    private val progressIndicator: ProgressIndicator by fxid("progressIndicator")

    private val userRepo = UserRepo()

    private val userModel = UserModel().also { it.item = User() }

    init {

        invalidLoginLabel.isVisible = false
        progressIndicator.isVisible = false

        userName.apply {
            bind(userModel.username)
            required(ValidationTrigger.OnChange(), "Username cannot be empty.")
        }
        password.apply {
            bind(userModel.password)
            required(ValidationTrigger.OnChange(), "Password cannot be empty.")
        }


        loginBtn.apply {
            enableWhen { userModel.valid }

//            action {
//                userModel.commit()
//                GlobalScope.launch {
//
//                    var user = userModel.item
//
//                    progressIndicator.isVisible = true
//                    val results = userRepo.authenticate(user.usernameProperty.get(), user.passwordProperty.get())
//                    progressIndicator.isVisible = false
//
//                    if (results is Results.Success<*>) {
//                        user = (results.data as List<User>).firstOrNull()
//                        user?.let {
                Platform.runLater {
                    val user = User().also {
                        it.id = 1
                        it.firstName = "Petrus"
                        it.lastName = "Kambala"
                        it.username="jvamcas"
                        it.password = "3Mili2,87"
                    }
                    currentUser.set(user)
                }
//                            return@launch
//                        }
//                        invalidLoginLabel.isVisible = true
//
//                    } else
//                        parseResults(results)
//                }
            }
//        }
        userModel.validate(decorateErrors = false)
    }


    override fun onDock() {
        super.onDock()
        currentStage?.isMaximized = true
        workspace.header.hide()
    }

    override fun onUndock() {
        super.onUndock()
        //clean up back stack so as not to go back to login screen
        workspace.viewStack.remove(this)
    }
}
