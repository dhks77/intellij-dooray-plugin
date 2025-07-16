package com.github.dhks77.intellijdoorayplugin.plugin.config

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent

class DooraySettingsComponent {

    val tokenField = JBTextField()
    val projectIdField = JBTextField()
    val domainField = JBTextField()
    val panel = FormBuilder.createFormBuilder()
        .addLabeledComponent(JBLabel("Dooray Token: "), tokenField, 1, false)
        .addLabeledComponent(JBLabel("Dooray Project ID: "), projectIdField, 1, false)
        .addLabeledComponent(JBLabel("Dooray Domain (예: https://nhnent.dooray.com): "), domainField, 1, false)
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

    fun getDomain(): String {
        return domainField.text
    }

    fun setToken(newText: String) {
        tokenField.text = newText
    }

    fun setProjectId(newText: String) {
        projectIdField.text = newText
    }

    fun setDomain(newText: String) {
        domainField.text = newText
    }

}