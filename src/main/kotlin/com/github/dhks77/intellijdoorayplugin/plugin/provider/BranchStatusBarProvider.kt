package com.github.dhks77.intellijdoorayplugin.plugin.provider

import com.github.dhks77.intellijdoorayplugin.common.BranchUtils
import com.github.dhks77.intellijdoorayplugin.dooray.facade.getPost
import com.github.dhks77.intellijdoorayplugin.plugin.cache.DoorayPostCache
import com.github.dhks77.intellijdoorayplugin.plugin.config.DooraySettingsState
import com.github.dhks77.intellijdoorayplugin.service.BranchService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.Consumer
import com.intellij.ide.BrowserUtil

import java.awt.event.MouseEvent

class BranchStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = "DoorayBranchInfo"
    override fun getDisplayName(): String = "Dooray Branch Info"
    override fun isAvailable(project: Project): Boolean = true
    override fun createWidget(project: Project): StatusBarWidget = BranchStatusBarWidget(project)
    override fun disposeWidget(widget: StatusBarWidget) = widget.dispose()
    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true
}

class BranchStatusBarWidget(private val project: Project) : StatusBarWidget, StatusBarWidget.TextPresentation {
    private val cache = DoorayPostCache.getInstance(project)
    private var currentText = "Dooray: -"
    private var statusBar: StatusBar? = null
    private var alarm: com.intellij.util.Alarm? = null

    override fun ID(): String = "DoorayBranchInfo"

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun getText(): String = currentText

    override fun getAlignment(): Float = 0.0f

    override fun getTooltipText(): String? = "클릭하여 업무 페이지 열기"

    override fun getClickConsumer(): Consumer<MouseEvent>? = Consumer { mouseEvent ->
        if (mouseEvent.isPopupTrigger || mouseEvent.isControlDown) {
            updateBranchInfo()
            return@Consumer
        }

        val branchService = project.service<BranchService>()
        val branchName = branchService.getBranchName()
        if (branchName == null) {
            return@Consumer
        }

        val post = cache.getPost(branchName)
        if (post != null) {
            val settings = DooraySettingsState.getInstance()
            if (settings.domain.isNotEmpty()) {
                val url = "${settings.domain}/project/tasks/${post.id}"
                BrowserUtil.browse(url)
            }
        }
    }

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
        updateBranchInfo()

        alarm = com.intellij.util.Alarm(com.intellij.util.Alarm.ThreadToUse.SWING_THREAD, project)
        var lastBranchName: String? = null

        val checkBranchTask = object : Runnable {
            override fun run() {
                val currentBranch = project.service<BranchService>().getBranchName()
                if (currentBranch != lastBranchName) {
                    lastBranchName = currentBranch
                    updateBranchInfo()
                }
                alarm?.addRequest(this, 5000)
            }
        }

        alarm?.addRequest(checkBranchTask, 1000)
    }

    override fun dispose() {
        alarm?.dispose()
        alarm = null
    }

    private fun updateText(text: String) {
        currentText = text
        statusBar?.updateWidget(ID())
    }

    private fun updateBranchInfo() {
        val branchService = project.service<BranchService>()
        val branchName = branchService.getBranchName()

        if (branchName.isNullOrEmpty()) {
            updateText("Dooray: No Branch")
            return
        }

        if (cache.hasPost(branchName)) {
            val post = cache.getPost(branchName)
            val text = if (post != null) {
                "${post.number}: ${post.subject}"
            } else {
                "Dooray: 캐시된 오류"
            }
            updateText(text)
            return
        }

        val taskNumber = BranchUtils.extractTaskNumber(branchName)
        if (taskNumber == null) {
            updateText("Dooray: No Task")
            return
        }

        updateText("Dooray: 로딩 중...")

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Dooray 업무 정보 가져오는 중...", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val settings = DooraySettingsState.getInstance()
                    if (settings.token.isEmpty() || settings.projectId.isEmpty()) {
                        ApplicationManager.getApplication().invokeLater {
                            updateText("Dooray: 설정 필요")
                            cache.putPost(branchName, null)
                        }
                        return
                    }

                    val post = getPost(settings.token, settings.projectId, taskNumber)
                    val tooltip = if (post != null) {
                        "${post.number}: ${post.subject}"
                    } else {
                        "Dooray: 업무 없음"
                    }

                    ApplicationManager.getApplication().invokeLater {
                        updateText(tooltip)
                        cache.putPost(branchName, post)
                    }

                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        updateText("Dooray: 오류")
                        cache.putPost(branchName, null)
                    }
                }
            }
        })
    }
}
