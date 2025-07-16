package com.github.dhks77.intellijdoorayplugin.plugin.config

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent

class DooraySettingsComponent {

    val tokenField = JBTextField()
    val projectIdField = JBTextField()
    val panel = FormBuilder.createFormBuilder()
        .addLabeledComponent(JBLabel("Dooray Token: "), tokenField, 1, false)
        .addLabeledComponent(JBLabel("Eooray Project ID: "), projectIdField, 1, false)
        .panel

    fun getPreferredFocusedComponent(): JComponent {
        return tokenField
    }

    fun getToken(): String {
        return tokenField.text
    }

    fun getProjectId(): String {
        return projectIdField.text
    }

    fun setToken(newText: String) {
        tokenField.text = newText
    }

    fun setProjectId(newText: String) {
        projectIdField.text = newText
    }

}