package com.github.dhks77.intellijdoorayplugin.plugin.provider

import com.github.dhks77.intellijdoorayplugin.dooray.facade.getPost
import com.github.dhks77.intellijdoorayplugin.dooray.vo.Post
import com.github.dhks77.intellijdoorayplugin.plugin.config.DooraySettingsState
import com.github.dhks77.intellijdoorayplugin.service.GithubService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.status.TextPanel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.Disposable
import com.intellij.util.Consumer
import com.intellij.ide.BrowserUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.event.MouseEvent
import java.util.concurrent.ConcurrentHashMap

class BranchStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = "DoorayBranchInfo"
    override fun getDisplayName(): String = "Dooray Branch Info"
    override fun isAvailable(project: Project): Boolean = true
    override fun createWidget(project: Project): StatusBarWidget = BranchStatusBarWidget(project)
    override fun disposeWidget(widget: StatusBarWidget) = widget.dispose()
    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true
}

class BranchStatusBarWidget(private val project: Project) : StatusBarWidget, StatusBarWidget.TextPresentation {
    private val cache = ConcurrentHashMap<String, Post?>()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var currentText = "Dooray: -"
            private var statusBar: StatusBar? = null
    private var alarm: com.intellij.util.Alarm? = null
    
    override fun ID(): String = "DoorayBranchInfo"
    
    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this
    
    override fun getText(): String = currentText
    
    override fun getAlignment(): Float = 0.0f
    
    override fun getTooltipText(): String? = "클릭하여 업무 페이지 열기"
    
    override fun getClickConsumer(): Consumer<MouseEvent>? = Consumer { mouseEvent ->
        // 우클릭(또는 Ctrl+클릭)이면 새로고침
        if (mouseEvent.isPopupTrigger || mouseEvent.isControlDown) {
            updateBranchInfo()
            return@Consumer
        }
        
        // 일반 클릭이면 Dooray 업무 페이지 열기
        val githubService = project.service<GithubService>()
        val branchName = githubService.getBranchName()
        if (branchName == null) {
            return@Consumer
        }
        
        // 캐시에서 Post 객체 가져오기
        val post = cache[branchName]
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
        
        // 브랜치 변경 감지 - 5초마다 체크 (가장 확실한 방법)
        alarm = com.intellij.util.Alarm(com.intellij.util.Alarm.ThreadToUse.SWING_THREAD, project)
        var lastBranchName: String? = null
        
        val checkBranchTask = object : Runnable {
            override fun run() {
                val currentBranch = project.service<GithubService>().getBranchName()
                if (currentBranch != lastBranchName) {
                    lastBranchName = currentBranch
                    updateBranchInfo()
                }
                // 5초 후 다시 체크
                alarm?.addRequest(this, 5000)
            }
        }
        
        // 초기 체크 시작
        alarm?.addRequest(checkBranchTask, 1000) // 1초 후 시작
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
        val githubService = project.service<GithubService>()
        val branchName = githubService.getBranchName()
        
        if (branchName.isNullOrEmpty()) {
            updateText("Dooray: No Branch")
            return
        }
        
        // 캐시에서 확인
        if (cache.containsKey(branchName)) {
            val post = cache[branchName]
            val text = if (post != null) {
                "${post.number}: ${post.subject}"
            } else {
                "Dooray: 캐시된 오류"
            }
            updateText(text)
            return
        }
        
        // 브랜치 이름에서 task number 추출
        val taskNumber = extractTaskNumber(branchName)
        if (taskNumber == null) {
            updateText("Dooray: No Task")
            return
        }
        
        updateText("Dooray: 로딩 중...")
        
        // 백그라운드에서 API 호출
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Dooray 업무 정보 가져오는 중...", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val settings = DooraySettingsState.getInstance()
                    if (settings.token.isEmpty() || settings.projectId.isEmpty()) {
                        ApplicationManager.getApplication().invokeLater {
                            updateText("Dooray: 설정 필요")
                            cache[branchName] = null
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
                        cache[branchName] = post
                    }
                    
                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        updateText("Dooray: 오류")
                        cache[branchName] = null
                    }
                }
            }
        })
    }
    
    private fun extractTaskNumber(branchName: String): Long? {
        return branchName.split("/").last().toLongOrNull()
    }
} 