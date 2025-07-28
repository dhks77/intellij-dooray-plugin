package com.github.dhks77.intellijdoorayplugin.plugin.actions

import com.github.dhks77.intellijdoorayplugin.dooray.facade.getPost
import com.github.dhks77.intellijdoorayplugin.plugin.cache.DoorayPostCache
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
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import git4idea.GitUtil
import git4idea.GitBranch
import git4idea.commands.Git
import git4idea.repo.GitRepository
import javax.swing.*
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.vcs.VcsDataKeys

class OpenDoorayTaskByBranchAction : AnAction() {

    companion object {
        fun extractTaskNumber(branchName: String): Long? {
            return branchName.split("/").last().toLongOrNull()
        }

        fun getDisplayText(branch: GitBranch, cache: DoorayPostCache): String {
            val taskNumber = extractTaskNumber(branch.name)
            val post = cache.getPost(branch.name)

            return if (post != null) {
                "${branch.name} (${post.number}: ${post.subject})"
            } else {
                "${branch.name} (Task #$taskNumber)"
            }
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        val repository = project?.let { GitUtil.getRepositoryManager(it).repositories.firstOrNull() }

        val isVisible = project != null && repository != null
        e.presentation.isVisible = isVisible
        e.presentation.isEnabled = isVisible
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val repository = GitUtil.getRepositoryManager(project).repositories.firstOrNull() ?: return

        val settings = DooraySettingsState.getInstance()
        if (settings.domain.isEmpty() || settings.token.isEmpty() || settings.projectId.isEmpty()) {
            Messages.showWarningDialog(
                project,
                "Dooray 설정을 먼저 구성해주세요.\n(Settings → Dooray Settings)",
                "설정 필요"
            )
            return
        }

        val localBranches = repository.branches.localBranches
        val remoteBranches = repository.branches.remoteBranches

        val taskBranches = (localBranches + remoteBranches)
            .filter { branch -> extractTaskNumber(branch.name) != null }

        if (taskBranches.isEmpty()) {
            Messages.showInfoMessage(
                project,
                "Task number가 있는 브랜치를 찾을 수 없습니다.\n(브랜치명 마지막에 숫자가 있어야 합니다)",
                "브랜치 없음"
            )
            return
        }

        val branchGroups = groupBranchesByPrefix(taskBranches)

        val popup = JBPopupFactory.getInstance().createListPopup(
            TopLevelPopupStep(project, repository, branchGroups, taskBranches)
        )

        popup.showCenteredInCurrentWindow(project)
    }

    private fun groupBranchesByPrefix(branches: List<GitBranch>): List<BranchGroup> {
        return branches.groupBy { getBranchPrefix(it.name) }
            .toSortedMap()
            .map { (prefix, branchList) ->
                val capitalizedPrefix = if (prefix.isNotEmpty()) {
                    prefix.first().toString().uppercase() + prefix.drop(1)
                } else {
                    prefix
                }
                BranchGroup(
                    name = capitalizedPrefix,
                    branches = branchList.sortedBy { it.name }
                )
            }
    }

    private fun getBranchPrefix(branchName: String): String {
        val parts = branchName.split("/")
        return if (parts.size > 1) {
            parts[0] + "/"
        } else {
            "other"
        }
    }
}

data class BranchGroup(
    val name: String,
    val branches: List<GitBranch>
)

enum class TopLevelAction {
    LOAD_ALL_TASKS,
    VIEW_GROUPS
}

class TopLevelPopupStep(
    private val project: com.intellij.openapi.project.Project,
    private val repository: GitRepository,
    private val branchGroups: List<BranchGroup>,
    private val allTaskBranches: List<GitBranch>
) : BaseListPopupStep<Any>("액션 선택", listOf(TopLevelAction.LOAD_ALL_TASKS) + branchGroups) {

    override fun getTextFor(value: Any): String {
        return when (value) {
            is TopLevelAction -> "모든 업무 불러오기"
            is BranchGroup -> "${value.name} (${value.branches.size}개)"
            else -> ""
        }
    }

    override fun hasSubstep(value: Any): Boolean = value is BranchGroup

    override fun onChosen(selectedValue: Any, finalChoice: Boolean): PopupStep<*>? {
        if (finalChoice) {
            if (selectedValue is TopLevelAction) {
                loadAllTasks()
            }
            return null
        }

        if (selectedValue is BranchGroup) {
            return BranchGroupActionPopupStep(project, repository, selectedValue)
        }
        return null
    }
    
    private fun loadAllTasks() {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "모든 업무 정보 불러오는 중...", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val settings = DooraySettingsState.getInstance()
                    val cache = DoorayPostCache.getInstance(project)
                    val totalBranches = allTaskBranches.size
                    
                    allTaskBranches.forEachIndexed { index, branch ->
                        if (indicator.isCanceled) return
                        
                        indicator.text = "업무 정보 불러오는 중... (${index + 1}/$totalBranches)"
                        indicator.fraction = (index + 1).toDouble() / totalBranches
                        
                        val existingPost = cache.getPost(branch.name)
                        if (existingPost == null) {
                            val taskNumber = OpenDoorayTaskByBranchAction.extractTaskNumber(branch.name)
                            if (taskNumber != null) {
                                try {
                                    val post = getPost(settings.token, settings.projectId, taskNumber)
                                    if (post != null) {
                                        cache.putPost(branch.name, post)
                                    }
                                } catch (e: Exception) {
                                    // 개별 브랜치 오류는 무시하고 계속 진행
                                }
                            }
                        }
                    }
                    
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showInfoMessage(
                            project,
                            "모든 브랜치의 업무 정보를 불러왔습니다.",
                            "완료"
                        )
                    }
                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project,
                            "업무 정보 불러오기 중 오류가 발생했습니다: ${e.message}",
                            "오류"
                        )
                    }
                }
            }
        })
    }
}


class BranchGroupActionPopupStep(
    private val project: com.intellij.openapi.project.Project,
    private val repository: GitRepository,
    private val branchGroup: BranchGroup
) : BaseListPopupStep<String>("${branchGroup.name} 액션 선택", listOf("브랜치 목록 보기", "여러 브랜치 삭제")) {

    override fun getTextFor(value: String): String = value

    override fun hasSubstep(selectedValue: String): Boolean {
        return selectedValue == "브랜치 목록 보기"
    }

    override fun onChosen(selectedValue: String, finalChoice: Boolean): PopupStep<*>? {
        if (selectedValue == "브랜치 목록 보기") {
            return BranchListPopupStep(project, repository, branchGroup)
        } else if (selectedValue == "여러 브랜치 삭제") {
            ApplicationManager.getApplication().invokeLater {
                BranchMultiDeletePopupStep(project, repository, branchGroup).showMultiDeleteDialog()
            }
        }
        return null
    }
}

class BranchListPopupStep(
    private val project: com.intellij.openapi.project.Project,
    private val repository: GitRepository,
    private val branchGroup: BranchGroup
) : BaseListPopupStep<GitBranch>("${branchGroup.name} 브랜치 선택", branchGroup.branches) {

    override fun getTextFor(branch: GitBranch): String {
        val cache = DoorayPostCache.getInstance(project)
        return OpenDoorayTaskByBranchAction.getDisplayText(branch, cache)
    }

    override fun hasSubstep(branch: GitBranch): Boolean = true

    override fun onChosen(selectedBranch: GitBranch, finalChoice: Boolean): PopupStep<*>? {
        if (finalChoice) {
            return null
        }
        return BranchActionPopupStep(project, repository, selectedBranch)
    }
}

enum class BranchAction {
    OPEN_DOORAY,
    DELETE_BRANCH
}

class BranchActionPopupStep(
    private val project: com.intellij.openapi.project.Project,
    private val repository: GitRepository,
    private val branch: GitBranch
) : BaseListPopupStep<BranchAction>("${branch.name} 액션 선택", BranchAction.values().toList()) {

    override fun getTextFor(action: BranchAction): String {
        return when (action) {
            BranchAction.OPEN_DOORAY -> "Dooray 업무 페이지 열기"
            BranchAction.DELETE_BRANCH -> "Git 브랜치 삭제"
        }
    }

    override fun onChosen(selectedAction: BranchAction, finalChoice: Boolean): PopupStep<*>? {
        if (finalChoice) {
            when (selectedAction) {
                BranchAction.OPEN_DOORAY -> openDoorayTask()
                BranchAction.DELETE_BRANCH -> {
                    ApplicationManager.getApplication().invokeLater {
                        deleteBranch()
                    }
                }
            }
        }
        return null
    }

    private fun openDoorayTask() {
        val branchName = branch.name
        val taskNumber = OpenDoorayTaskByBranchAction.extractTaskNumber(branchName) ?: return

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Dooray 업무 정보 가져오는 중...", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val settings = DooraySettingsState.getInstance()
                    val post = getPost(settings.token, settings.projectId, taskNumber)

                    ApplicationManager.getApplication().invokeLater {
                        if (post != null) {
                            val cache = DoorayPostCache.getInstance(project)
                            cache.putPost(branchName, post)

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
    
    private fun deleteBranch() {
        if (branch.isRemote) {
            Messages.showWarningDialog(
                project,
                "원격 브랜치는 삭제할 수 없습니다.\n로컬 브랜치만 삭제 가능합니다.",
                "브랜치 삭제 실패"
            )
            return
        }

        val result = Messages.showYesNoDialog(
            project,
            "브랜치 '${branch.name}'을 삭제하시겠습니까?\n\n" +
                    "⚠️ 주의: 병합되지 않은 커밋이 있어도 강제로 삭제됩니다.\n" +
                    "이 작업은 되돌릴 수 없습니다.",
            "브랜치 삭제 확인",
            Messages.getQuestionIcon()
        )

        if (result == Messages.YES) {
            ProgressManager.getInstance().run(object : Task.Backgroundable(project, "브랜치 삭제 중...", false) {
                override fun run(indicator: ProgressIndicator) {
                    try {
                        val git = Git.getInstance()
                        val isCurrentBranch = repository.currentBranch == branch

                        if (isCurrentBranch) {
                            ApplicationManager.getApplication().invokeLater {
                                Messages.showWarningDialog(
                                    project,
                                    "현재 체크아웃된 브랜치는 삭제할 수 없습니다.\n다른 브랜치로 체크아웃 후 삭제해주세요.",
                                    "브랜치 삭제 실패"
                                )
                            }
                            return
                        }

                        val result = git.branchDelete(repository, branch.name, true)

                        ApplicationManager.getApplication().invokeLater {
                            if (result.success()) {
                                Messages.showInfoMessage(
                                    project,
                                    "브랜치 '${branch.name}'이 성공적으로 삭제되었습니다.",
                                    "삭제 완료"
                                )
                            } else {
                                Messages.showErrorDialog(
                                    project,
                                    "브랜치 삭제 중 오류가 발생했습니다:\n${result.errorOutputAsJoinedString}",
                                    "삭제 실패"
                                )
                            }
                        }
                    } catch (e: Exception) {
                        ApplicationManager.getApplication().invokeLater {
                            Messages.showErrorDialog(
                                project,
                                "브랜치 삭제 중 오류가 발생했습니다: ${e.message}",
                                "삭제 실패"
                            )
                        }
                    }
                }
            })
        }
    }
}

class BranchMultiDeletePopupStep(
    private val project: com.intellij.openapi.project.Project,
    private val repository: GitRepository,
    private val branchGroup: BranchGroup
) {
    fun showMultiDeleteDialog() {
        val localBranches = branchGroup.branches.filter { !it.isRemote }
        
        if (localBranches.isEmpty()) {
            Messages.showWarningDialog(
                project,
                "삭제할 수 있는 로컬 브랜치가 없습니다.",
                "브랜치 삭제"
            )
            return
        }
        
        val dialog = JDialog()
        dialog.title = "${branchGroup.name} 브랜치 삭제"
        dialog.isModal = true
        dialog.layout = BorderLayout()
        
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = JBUI.Borders.empty(10)
        
        val checkboxes = mutableListOf<JBCheckBox>()
        val cache = DoorayPostCache.getInstance(project)
        
        localBranches.forEach { branch ->
            val displayText = OpenDoorayTaskByBranchAction.getDisplayText(branch, cache)
            val checkbox = JBCheckBox(displayText)
            checkbox.actionCommand = branch.name // 실제 브랜치 이름을 저장
            checkboxes.add(checkbox)
            panel.add(checkbox)
        }
        
        val scrollPane = JBScrollPane(panel)
        scrollPane.preferredSize = Dimension(400, 300)
        
        val buttonPanel = JPanel()
        val deleteButton = JButton("선택한 브랜치 삭제")
        val cancelButton = JButton("취소")
        
        deleteButton.addActionListener {
            val selectedBranches = checkboxes.filter { it.isSelected }.map { checkbox ->
                localBranches.find { it.name == checkbox.actionCommand }
            }.filterNotNull()
            
            if (selectedBranches.isEmpty()) {
                Messages.showWarningDialog(
                    project,
                    "삭제할 브랜치를 선택해주세요.",
                    "브랜치 삭제"
                )
                return@addActionListener
            }
            
            val result = Messages.showYesNoDialog(
                project,
                "선택한 ${selectedBranches.size}개의 브랜치를 삭제하시겠습니까?\n\n" +
                        "⚠️ 주의: 병합되지 않은 커밋이 있어도 강제로 삭제됩니다.\n" +
                        "이 작업은 되돌릴 수 없습니다.",
                "브랜치 삭제 확인",
                Messages.getQuestionIcon()
            )
            
            if (result == Messages.YES) {
                dialog.dispose()
                deleteSelectedBranches(selectedBranches)
            }
        }
        
        cancelButton.addActionListener {
            dialog.dispose()
        }
        
        buttonPanel.add(deleteButton)
        buttonPanel.add(cancelButton)
        
        dialog.add(scrollPane, BorderLayout.CENTER)
        dialog.add(buttonPanel, BorderLayout.SOUTH)
        
        dialog.pack()
        dialog.setLocationRelativeTo(null)
        dialog.isVisible = true
    }
    
    private fun deleteSelectedBranches(branches: List<GitBranch>) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "브랜치 삭제 중...", false) {
            override fun run(indicator: ProgressIndicator) {
                val git = Git.getInstance()
                val totalBranches = branches.size
                var successCount = 0
                val failedBranches = mutableListOf<String>()
                
                branches.forEachIndexed { index, branch ->
                    indicator.text = "브랜치 삭제 중... (${index + 1}/$totalBranches)"
                    indicator.fraction = (index + 1).toDouble() / totalBranches
                    
                    try {
                        val isCurrentBranch = repository.currentBranch == branch
                        
                        if (isCurrentBranch) {
                            failedBranches.add("${branch.name} (현재 브랜치)")
                        } else {
                            val result = git.branchDelete(repository, branch.name, true)
                            
                            if (result.success()) {
                                successCount++
                            } else {
                                failedBranches.add("${branch.name} (${result.errorOutputAsJoinedString})")
                            }
                        }
                    } catch (e: Exception) {
                        failedBranches.add("${branch.name} (${e.message})")
                    }
                }
                
                ApplicationManager.getApplication().invokeLater {
                    val message = buildString {
                        append("브랜치 삭제 완료\n\n")
                        append("성공: ${successCount}개\n")
                        if (failedBranches.isNotEmpty()) {
                            append("실패: ${failedBranches.size}개\n\n")
                            append("실패한 브랜치:\n")
                            failedBranches.forEach { append("- $it\n") }
                        }
                    }
                    
                    if (failedBranches.isNotEmpty()) {
                        Messages.showWarningDialog(project, message, "브랜치 삭제 결과")
                    } else {
                        Messages.showInfoMessage(project, message, "브랜치 삭제 완료")
                    }
                }
            }
        })
    }
}