package com.github.dhks77.intellijdoorayplugin.plugin.config

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

class DooraySettingsConfigurable : Configurable {

    private val logger = Logger.getInstance(DooraySettingsConfigurable::class.java)
    private var dooraySettingsComponent: DooraySettingsComponent? = null

    override fun createComponent(): JComponent? {
        return try {
            logger.info("Creating Dooray Settings component")
            dooraySettingsComponent = DooraySettingsComponent()
            dooraySettingsComponent?.panel
        } catch (e: Exception) {
            logger.error("Failed to create Dooray Settings component", e)
            null
        }
    }

    override fun isModified(): Boolean {
        return try {
            val component = dooraySettingsComponent ?: return false
            val settings = DooraySettingsState.getInstance()
            
            val tokenChanged = component.tokenField.text != settings.token
            val projectIdChanged = component.projectIdField.text != settings.projectId
            val domainChanged = component.domainField.text != settings.domain
            val prTitleTemplateChanged = component.prTitleTemplateField.text != settings.prTitleTemplate
            
            tokenChanged || projectIdChanged || domainChanged || prTitleTemplateChanged
        } catch (e: Exception) {
            logger.error("Failed to check if settings are modified", e)
            false
        }
    }

    override fun apply() {
        try {
            val component = dooraySettingsComponent ?: throw ConfigurationException("Settings component not initialized")
            val settings = DooraySettingsState.getInstance()
            
            settings.token = component.getToken()
            settings.projectId = component.getProjectId()
            settings.domain = component.getDomain()
            settings.prTitleTemplate = component.getPrTitleTemplate()
            
            logger.info("Dooray settings applied successfully")
        } catch (e: Exception) {
            logger.error("Failed to apply Dooray settings", e)
            throw ConfigurationException("Failed to save Dooray settings: ${e.message}")
        }
    }

    override fun reset() {
        try {
            val component = dooraySettingsComponent ?: return
            val settings = DooraySettingsState.getInstance()
            
            component.setToken(settings.token)
            component.setProjectId(settings.projectId)
            component.setDomain(settings.domain)
            component.setPrTitleTemplate(settings.prTitleTemplate)
            
            logger.info("Dooray settings reset successfully")
        } catch (e: Exception) {
            logger.error("Failed to reset Dooray settings", e)
        }
    }

    override fun disposeUIResources() {
        dooraySettingsComponent = null
    }

    @Nls
    override fun getDisplayName(): String {
        return "Dooray Settings"
    }

}