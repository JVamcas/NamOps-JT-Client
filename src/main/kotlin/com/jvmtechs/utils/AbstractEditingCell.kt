package com.jvmtechs.utils

import javafx.beans.value.ObservableValue
import javafx.scene.control.TableCell
import javafx.scene.control.TextField

abstract class AbstractEditingCell<K> : TableCell<K, String?>() {
    private lateinit var textField: TextField

    override fun startEdit() {
        if (!isEmpty) {
            super.startEdit()
            createTextField()
            text = null
            graphic = textField
            textField.selectAll()
        }
    }

    override fun cancelEdit() {
        super.cancelEdit()
        text = item
        graphic = null
    }

    override fun updateItem(item: String?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty) {
            text = item
            graphic = null
        } else {
            if (isEditing) {
                textField.text = string
                text = null
                graphic = textField
            } else {
                text = string
                graphic = null
            }
        }
    }

    private fun createTextField() {
        textField = TextField(string)
        textField.minWidth = this.width - this.graphicTextGap * 2
        textField.setOnAction { commitEdit(textField.text) }
        textField.focusedProperty()
            .addListener { _: ObservableValue<out Boolean?>?, _: Boolean?, newValue: Boolean? ->
                if (!newValue!!) {
                    commitEdit(textField.text)
                }
            }
    }

    private val string: String
        get() = item ?: ""
}
