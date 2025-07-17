package com.github.dhks77.intellijdoorayplugin.plugin.config

import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class DooraySettingsComponent {

    private val logger = Logger.getInstance(DooraySettingsComponent::class.java)

    val tokenField = JBTextField()
    val projectIdField = JBTextField()
    val domainField = JBTextField()
    
    val panel: JPanel by lazy {
        try {
            logger.info("Creating Dooray Settings UI panel")
            FormBuilder.createFormBuilder()
                .addLabeledComponent(JBLabel("Dooray Token: "), tokenField, 1, false)
                .addLabeledComponent(JBLabel("Dooray Project ID: "), projectIdField, 1, false)
                .addLabeledComponent(JBLabel("Dooray Domain (예: https://your-company.dooray.com): "), domainField, 1, false)
                .addComponentFillVertically(JPanel(), 0)
                .panel
        } catch (e: Exception) {
            logger.error("Failed to create Dooray Settings UI panel", e)
            JPanel() // 빈 패널 반환
        }
    }

    fun getPreferredFocusedComponent(): JComponent {
        return tokenField
    }

    fun getToken(): String {
        return tokenField.text?.trim() ?: ""
    }

    fun getProjectId(): String {
        return projectIdField.text?.trim() ?: ""
    }

    fun getDomain(): String {
        return domainField.text?.trim() ?: ""
    }

    fun setToken(newText: String?) {
        tokenField.text = newText ?: ""
    }

    fun setProjectId(newText: String?) {
        projectIdField.text = newText ?: ""
    }

    fun setDomain(newText: String?) {
        domainField.text = newText ?: ""
    }

}