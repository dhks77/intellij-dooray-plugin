package com.github.dhks77.intellijdoorayplugin.plugin.config

import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

class DooraySettingsConfigurable : Configurable {

    private val dooraySettingsComponent = DooraySettingsComponent()

    override fun createComponent(): JComponent {
        return dooraySettingsComponent.panel
    }

    override fun isModified(): Boolean {
        val settings = DooraySettingsState.getInstance()
        return !dooraySettingsComponent.tokenField.text.equals(settings.token) ||
                !dooraySettingsComponent.projectIdField.text.equals(settings.projectId) ||
                !dooraySettingsComponent.domainField.text.equals(settings.domain)
    }

    override fun apply() {
        val settings = DooraySettingsState.getInstance()
        settings.token = dooraySettingsComponent.getToken()
        settings.projectId = dooraySettingsComponent.getProjectId()
        settings.domain = dooraySettingsComponent.getDomain()
    }

    override fun reset() {
        val settings = DooraySettingsState.getInstance()
        dooraySettingsComponent.setToken(settings.token)
        dooraySettingsComponent.setProjectId(settings.projectId)
        dooraySettingsComponent.setDomain(settings.domain)
    }

    @Nls
    override fun getDisplayName(): String {
        return "Dooray Settings"
    }

}