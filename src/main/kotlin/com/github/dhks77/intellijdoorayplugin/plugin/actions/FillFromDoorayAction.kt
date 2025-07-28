package com.github.dhks77.intellijdoorayplugin.plugin.actions

import com.github.dhks77.intellijdoorayplugin.dooray.facade.getPost
import com.github.dhks77.intellijdoorayplugin.plugin.cache.DoorayPostCache
import com.github.dhks77.intellijdoorayplugin.plugin.config.DooraySettingsState
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.editor.impl.EditorComponentImpl
import com.intellij.openapi.command.WriteCommandAction
import git4idea.GitUtil
import java.awt.*
import java.awt.datatransfer.StringSelection
import javax.swing.*

class FillFromDoorayAction : AnAction("Fill from Dooray") {
    
    private val logger = Logger.getInstance(FillFromDoorayAction::class.java)
    
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
    
    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null
    }
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        logger.info("FillFromDoorayAction 실행 시작")
        
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Dooray 정보 가져오는 중...", false) {
            override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) {
                try {
                    val repository = GitUtil.getRepositoryManager(project).repositories.firstOrNull()
                    if (repository == null) {
                        logger.warn("Git repository를 찾을 수 없습니다.")
                        ApplicationManager.getApplication().invokeLater {
                            Messages.showWarningDialog(project, "Git repository를 찾을 수 없습니다.", "Dooray")
                        }
                        return
                    }
                    
                    val currentBranch = repository.currentBranch?.name
                    if (currentBranch == null) {
                        logger.warn("현재 브랜치를 찾을 수 없습니다.")
                        ApplicationManager.getApplication().invokeLater {
                            Messages.showWarningDialog(project, "현재 브랜치를 찾을 수 없습니다.", "Dooray")
                        }
                        return
                    }
                    
                    logger.info("현재 브랜치: $currentBranch")
                    
                    val taskNumber = extractTaskNumber(currentBranch)
                    if (taskNumber == null) {
                        logger.warn("브랜치명에서 태스크 번호를 추출할 수 없습니다: $currentBranch")
                        ApplicationManager.getApplication().invokeLater {
                            Messages.showWarningDialog(project, "브랜치명에서 태스크 번호를 추출할 수 없습니다.", "Dooray")
                        }
                        return
                    }
                    
                    logger.info("추출된 태스크 번호: $taskNumber")
                    
                    // 캐시에서 먼저 확인
                    val cache = project.getService(DoorayPostCache::class.java)
                    var post = cache.getPost(taskNumber)
                    
                    if (post == null) {
                        logger.info("캐시에 없음. Dooray API 호출")
                        // API 호출
                        val settings = DooraySettingsState.getInstance()
                        if (settings.domain.isBlank() || settings.token.isBlank() || settings.projectId.isBlank()) {
                            logger.warn("Dooray 설정이 완료되지 않았습니다.")
                            ApplicationManager.getApplication().invokeLater {
                                Messages.showWarningDialog(project, "Dooray 설정을 먼저 완료해주세요.", "Dooray")
                            }
                            return
                        }
                        
                        post = getPost(settings.token, settings.projectId, taskNumber.toLong())
                        
                        if (post != null) {
                            cache.putPost(taskNumber, post)
                            logger.info("Dooray API에서 포스트 정보 가져옴: ${post.subject}")
                        } else {
                            logger.warn("Dooray API에서 포스트 정보를 가져올 수 없습니다.")
                            ApplicationManager.getApplication().invokeLater {
                                Messages.showWarningDialog(project, "Dooray에서 태스크 정보를 찾을 수 없습니다.", "Dooray")
                            }
                            return
                        }
                    } else {
                        logger.info("캐시에서 포스트 정보 사용: ${post.subject}")
                    }
                    
                    val settings = DooraySettingsState.getInstance()
                    val prTitle = settings.prTitleTemplate
                        .replace("{taskNumber}", taskNumber)
                        .replace("#{taskNumber}", "#$taskNumber")
                        .replace("{subject}", post.subject)
                    
                    val prDescription = "${settings.domain}/project/tasks/${post.id}"
                    
                    logger.info("생성된 PR 제목: $prTitle")
                    logger.info("생성된 PR 설명: $prDescription")
                    
                    ApplicationManager.getApplication().invokeLater {
                        fillPullRequestFields(project, prTitle, prDescription)
                    }
                    
                } catch (e: Exception) {
                    logger.error("FillFromDoorayAction 실행 중 오류 발생", e)
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(project, "오류가 발생했습니다: ${e.message}", "Dooray")
                    }
                }
            }
        })
    }
    
    private fun fillPullRequestFields(project: Project, title: String, description: String) {
        logger.info("GitHub Pull Request 필드 채우기 시작")
        
        // GitHub Pull Request 필드 검색
        var titleFilled = false
        var descriptionFilled = false
        
        // 모든 윈도우와 컴포넌트 검색
        val allComponents = getAllVisibleComponents()
        logger.info("총 ${allComponents.size}개의 컴포넌트 발견")
        
        // EditorComponentImpl 검색 (GitHub PR은 에디터 컴포넌트 사용)
        val editorComponents = allComponents.filterIsInstance<EditorComponentImpl>()
        logger.info("발견된 EditorComponentImpl 개수: ${editorComponents.size}")
        
        for ((index, editorComponent) in editorComponents.withIndex()) {
            try {
                val editor = editorComponent.editor
                val document = editor.document
                val currentText = document.text
                
                logger.info("EditorComponent[$index] - text: '$currentText', " +
                        "isViewer: ${editor.isViewer}, " +
                        "lineCount: ${document.lineCount}")
                
                // Title 필드 추정 (짧은 텍스트, 한 줄)
                if (!titleFilled && document.lineCount <= 1 && currentText.length < 200) {
                    WriteCommandAction.runWriteCommandAction(project) {
                        document.setText(title)
                    }
                    titleFilled = true
                    logger.info("Title 필드 채우기 성공 (Editor)")
                }
                // Description 필드 추정 (긴 텍스트, 여러 줄 가능)
                else if (!descriptionFilled && (document.lineCount > 1 || currentText.isEmpty())) {
                    WriteCommandAction.runWriteCommandAction(project) {
                        document.setText(description)
                    }
                    descriptionFilled = true
                    logger.info("Description 필드 채우기 성공 (Editor)")
                }
            } catch (e: Exception) {
                logger.warn("EditorComponent[$index] 처리 실패", e)
            }
        }
        
        // 기존 JTextField/JTextArea도 확인 (fallback)
        if (!titleFilled || !descriptionFilled) {
            val textFields = allComponents.filterIsInstance<JTextField>()
            val textAreas = allComponents.filterIsInstance<JTextArea>()
            logger.info("Fallback - JTextField: ${textFields.size}, JTextArea: ${textAreas.size}")
            
            for (field in textFields) {
                if (!titleFilled && isLikelyTitleField(field)) {
                    try {
                        field.text = title
                        titleFilled = true
                        logger.info("Title 필드 채우기 성공 (JTextField)")
                    } catch (e: Exception) {
                        logger.warn("Title 필드 채우기 실패", e)
                    }
                }
            }
            
            for (area in textAreas) {
                if (!descriptionFilled && isLikelyDescriptionField(area)) {
                    try {
                        area.text = description
                        descriptionFilled = true
                        logger.info("Description 필드 채우기 성공 (JTextArea)")
                    } catch (e: Exception) {
                        logger.warn("Description 필드 채우기 실패", e)
                    }
                }
            }
        }
        
        if (!titleFilled || !descriptionFilled) {
            logger.warn("GitHub Pull Request 필드를 찾지 못했습니다. 클립보드에 복사합니다.")
            
            // 클립보드에 복사
            val clipboardText = "Title: $title\n\nDescription: $description"
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(StringSelection(clipboardText), null)
        } else {
            logger.info("PR 정보가 성공적으로 입력되었습니다.")
        }
    }
    
    private fun getAllVisibleComponents(): List<Component> {
        val components = mutableListOf<Component>()
        
        // 모든 윈도우 검색
        for (window in Window.getWindows()) {
            if (window.isVisible) {
                collectComponents(window, components)
            }
        }
        
        return components
    }
    
    private fun collectComponents(container: Container, components: MutableList<Component>) {
        for (component in container.components) {
            components.add(component)
            if (component is Container) {
                collectComponents(component, components)
            }
        }
    }
    
    private fun isLikelyTitleField(field: JTextField): Boolean {
        val name = field.name?.lowercase() ?: ""
        val accessibleName = field.accessibleContext?.accessibleName?.lowercase() ?: ""
        val toolTip = field.toolTipText?.lowercase() ?: ""
        val className = field.javaClass.simpleName.lowercase()
        
        return name.contains("title") ||
                accessibleName.contains("title") ||
                toolTip.contains("title") ||
                toolTip.contains("enter title") ||
                className.contains("title") ||
                (field.isVisible && field.isEnabled && field.text.isEmpty())
    }
    
    private fun isLikelyDescriptionField(area: JTextArea): Boolean {
        val name = area.name?.lowercase() ?: ""
        val accessibleName = area.accessibleContext?.accessibleName?.lowercase() ?: ""
        val toolTip = area.toolTipText?.lowercase() ?: ""
        val className = area.javaClass.simpleName.lowercase()
        
        return name.contains("description") ||
                name.contains("body") ||
                accessibleName.contains("description") ||
                accessibleName.contains("body") ||
                toolTip.contains("description") ||
                toolTip.contains("add a description") ||
                className.contains("description") ||
                className.contains("body") ||
                (area.isVisible && area.isEnabled && area.text.isEmpty() && area.rows > 1)
    }
    
    private fun extractTaskNumber(branchName: String): String? {
        // 브랜치명에서 숫자 추출 (예: feature/123, hotfix/456-description)
        val regex = Regex("""(\d+)""")
        return regex.find(branchName)?.value
    }
} 