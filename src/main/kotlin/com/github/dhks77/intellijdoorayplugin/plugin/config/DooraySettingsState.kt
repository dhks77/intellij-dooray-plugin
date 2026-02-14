package com.github.dhks77.intellijdoorayplugin.plugin.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger

@Service
@State(
    name = "com.github.dhks77.intellijdoorayplugin.config.DooraySettingsState",
    storages = [Storage("dooray-settings.xml")]
)
class DooraySettingsState : PersistentStateComponent<DooraySettingsState> {

    var token = ""
    var projectId = ""
    var domain = ""
    var prTitleTemplate = "[#{taskNumber}] {subject}"

    companion object {
        private val logger = Logger.getInstance(DooraySettingsState::class.java)

        fun getInstance(): DooraySettingsState {
            return try {
                ApplicationManager.getApplication().getService(DooraySettingsState::class.java)
                    ?: throw IllegalStateException("DooraySettingsState service not found")
            } catch (e: Exception) {
                logger.error("Failed to get DooraySettingsState instance", e)
                throw e
            }
        }
    }

    override fun getState(): DooraySettingsState {
        return this
    }

    override fun loadState(state: DooraySettingsState) {
        try {
            this.token = state.token ?: ""
            this.projectId = state.projectId ?: ""
            this.domain = state.domain ?: ""
            this.prTitleTemplate = state.prTitleTemplate ?: "[#{taskNumber}] {subject}"
        } catch (e: Exception) {
            logger.error("Failed to load DooraySettingsState state", e)
            this.token = ""
            this.projectId = ""
            this.domain = ""
        }
    }

}
