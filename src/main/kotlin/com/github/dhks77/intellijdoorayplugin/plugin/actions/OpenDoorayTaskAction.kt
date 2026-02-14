package com.github.dhks77.intellijdoorayplugin.plugin.actions

import com.github.dhks77.intellijdoorayplugin.common.BranchUtils
import com.github.dhks77.intellijdoorayplugin.dooray.facade.getPost
import com.github.dhks77.intellijdoorayplugin.plugin.config.DooraySettingsState
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.ui.Messages
import git4idea.GitUtil

class OpenDoorayTaskAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        val repository = project?.let { GitUtil.getRepositoryManager(it).repositories.firstOrNull() }

        val branchName = repository?.currentBranch?.name

        val isVisible = project != null && branchName != null && BranchUtils.extractTaskNumber(branchName) != null
        e.presentation.isVisible = isVisible
        e.presentation.isEnabled = isVisible

        if (isVisible) {
            val taskNumber = BranchUtils.extractTaskNumber(branchName!!)
            e.presentation.text = "Dooray 업무 #$taskNumber 페이지 열기"
        } else {
            e.presentation.text = "Dooray 업무 페이지 열기"
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val repository = GitUtil.getRepositoryManager(project).repositories.firstOrNull() ?: return
        val branchName = repository.currentBranch?.name ?: return
        val taskNumber = BranchUtils.extractTaskNumber(branchName) ?: return

        val settings = DooraySettingsState.getInstance()
        if (settings.domain.isEmpty() || settings.token.isEmpty() || settings.projectId.isEmpty()) {
            Messages.showWarningDialog(
                project,
                "Dooray 설정을 먼저 구성해주세요.\n(Settings → Dooray Settings)",
                "설정 필요"
            )
            return
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Dooray 업무 정보 가져오는 중...", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val post = getPost(settings.token, settings.projectId, taskNumber)

                    ApplicationManager.getApplication().invokeLater {
                        if (post != null) {
                            val url = "${settings.domain}/project/tasks/${post.id}"
                            BrowserUtil.browse(url)
                        } else {
                            Messages.showInfoMessage(
                                project,
                                "브랜치 '$branchName'에 해당하는 Dooray 업무를 찾을 수 없습니다.",
                                "업무 없음"
                            )
                        }
                    }
                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project,
                            "Dooray API 호출 중 오류가 발생했습니다: ${e.message}",
                            "오류"
                        )
                    }
                }
            }
        })
    }
}
