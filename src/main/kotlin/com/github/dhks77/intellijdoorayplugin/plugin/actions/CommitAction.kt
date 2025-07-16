package com.github.dhks77.intellijdoorayplugin.plugin.actions

import com.github.dhks77.intellijdoorayplugin.dooray.facade.getPost
import com.github.dhks77.intellijdoorayplugin.plugin.config.DooraySettingsState
import com.github.dhks77.intellijdoorayplugin.service.GithubService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.vcs.CommitMessageI
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.ui.Refreshable

class CommitAction : AnAction() {

    override fun actionPerformed(actionEvent: AnActionEvent) {
        val settings = DooraySettingsState.getInstance()

        val project = actionEvent.project ?: return setCommitMessage(actionEvent, "project가 없을 수 없는데...!")

        val githubService = project.service<GithubService>()
        val branchName = githubService.getBranchName() ?: return setCommitMessage(actionEvent, "branch name 이 없을 수 없는데...!")

        val postNumber = branchName.split("/").last().toLongOrNull()

        if (settings.token.isEmpty() || settings.projectId.isEmpty()) {
            setCommitMessage(actionEvent, "project 와 token 설정이 안되어있어요!")
            return
        }

        if (postNumber == null) {
            setCommitMessage(actionEvent, "브렌치에 task number가 없어요!")
            return
        }

        val post = getPost(settings.token, settings.projectId, postNumber)
        if (post == null) {
            setCommitMessage(actionEvent, "업무가 없어요..ㅠㅠ!")
        } else {
            setCommitMessage(actionEvent, "${post.number} ${post.subject}")
        }
    }

    private fun setCommitMessage(actionEvent: AnActionEvent, newCommitMessage: String) {
        val commitPanel = getCommitPanel(actionEvent)
        commitPanel?.setCommitMessage(newCommitMessage)
    }

    private fun getCommitPanel(actionEvent: AnActionEvent): CommitMessageI? {
        val data = Refreshable.PANEL_KEY.getData(actionEvent.dataContext)
        if (data is CommitMessageI) {
            return data
        }

        return VcsDataKeys.COMMIT_MESSAGE_CONTROL.getData(actionEvent.dataContext)
    }

}