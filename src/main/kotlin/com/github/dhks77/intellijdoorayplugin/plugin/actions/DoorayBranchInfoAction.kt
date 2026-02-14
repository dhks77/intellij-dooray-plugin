package com.github.dhks77.intellijdoorayplugin.plugin.actions

import com.github.dhks77.intellijdoorayplugin.common.BranchUtils
import com.github.dhks77.intellijdoorayplugin.dooray.facade.getPost
import com.github.dhks77.intellijdoorayplugin.plugin.cache.DoorayPostCache
import com.github.dhks77.intellijdoorayplugin.plugin.config.DooraySettingsState
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import git4idea.GitBranch
import git4idea.actions.branch.GitSingleBranchAction
import git4idea.repo.GitRepository
import java.util.concurrent.ConcurrentHashMap

class DoorayBranchInfoAction : GitSingleBranchAction() {

    private val fetching = ConcurrentHashMap.newKeySet<String>()

    override fun isEnabledForRef(ref: GitBranch, repositories: List<GitRepository>): Boolean {
        return BranchUtils.extractTaskNumber(ref.name) != null
    }

    override fun updateIfEnabledAndVisible(
        e: AnActionEvent,
        project: Project,
        repositories: List<GitRepository>,
        reference: GitBranch
    ) {
        val branchName = reference.name
        val taskNumber = BranchUtils.extractTaskNumber(branchName) ?: return

        val cache = DoorayPostCache.getInstance(project)
        val post = cache.getPost(branchName)

        if (post != null) {
            e.presentation.text = "${post.number}: ${post.subject}"
        } else {
            e.presentation.text = "Dooray #$taskNumber"
            fetchAsync(project, branchName, taskNumber)
        }
    }

    override fun actionPerformed(
        e: AnActionEvent,
        project: Project,
        repositories: List<GitRepository>,
        reference: GitBranch
    ) {
        val branchName = reference.name
        val taskNumber = BranchUtils.extractTaskNumber(branchName) ?: return
        val settings = DooraySettingsState.getInstance()
        if (settings.domain.isEmpty()) return

        val cache = DoorayPostCache.getInstance(project)
        val post = cache.getPost(branchName)

        if (post != null) {
            BrowserUtil.browse("${settings.domain}/project/tasks/${post.id}")
        } else {
            BrowserUtil.browse("${settings.domain}/project/posts/$taskNumber")
        }
    }

    private fun fetchAsync(project: Project, branchName: String, taskNumber: Long) {
        if (!fetching.add(branchName)) return

        val settings = DooraySettingsState.getInstance()
        if (settings.token.isEmpty() || settings.projectId.isEmpty()) {
            fetching.remove(branchName)
            return
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val post = getPost(settings.token, settings.projectId, taskNumber)
                if (post != null) {
                    DoorayPostCache.getInstance(project).putPost(branchName, post)
                }
            } catch (_: Exception) {
            } finally {
                fetching.remove(branchName)
            }
        }
    }
}
