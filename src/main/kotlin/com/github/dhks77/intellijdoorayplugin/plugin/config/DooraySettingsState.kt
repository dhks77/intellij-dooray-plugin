package com.github.dhks77.intellijdoorayplugin.plugin.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service
@State(
    name = "com.github.dhks77.doorayintellijplugin.config.DooraySettingsState",
    storages = [Storage("dooray-settings.xml")]
)
class DooraySettingsState : PersistentStateComponent<DooraySettingsState> {

    var token = ""
    var projectId = ""

    companion object {

        fun getInstance(): DooraySettingsState {
            return ApplicationManager.getApplication().getService(DooraySettingsState::class.java)
        }

    }

    override fun getState(): DooraySettingsState {
        return this
    }

    override fun loadState(state: DooraySettingsState) {
        this.token = state.token
        this.projectId = state.projectId
    }

}