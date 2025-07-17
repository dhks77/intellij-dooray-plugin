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

    private val logger = Logger.getInstance(DooraySettingsState::class.java)

    var token = ""
    var projectId = ""
    var domain = ""

    companion object {
        private val logger = Logger.getInstance(DooraySettingsState::class.java)

        fun getInstance(): DooraySettingsState {
            return try {
                logger.info("Getting DooraySettingsState instance")
                ApplicationManager.getApplication().getService(DooraySettingsState::class.java)
                    ?: throw IllegalStateException("DooraySettingsState service not found")
            } catch (e: Exception) {
                logger.error("Failed to get DooraySettingsState instance", e)
                throw e
            }
        }
    }

    override fun getState(): DooraySettingsState {
        logger.debug("Getting DooraySettingsState state")
        return this
    }

    override fun loadState(state: DooraySettingsState) {
        try {
            logger.info("Loading DooraySettingsState state")
            this.token = state.token ?: ""
            this.projectId = state.projectId ?: ""
            this.domain = state.domain ?: ""
            logger.info("DooraySettingsState state loaded successfully")
        } catch (e: Exception) {
            logger.error("Failed to load DooraySettingsState state", e)
            // 기본값으로 초기화
            this.token = ""
            this.projectId = ""
            this.domain = ""
        }
    }

}