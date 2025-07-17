package com.github.dhks77.intellijdoorayplugin.plugin.actions

import com.github.dhks77.intellijdoorayplugin.plugin.config.DooraySettingsState
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.Messages

class TestDooraySettingsAction : AnAction() {

    private val logger = Logger.getInstance(TestDooraySettingsAction::class.java)

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        try {
            logger.info("Testing Dooray Settings")
            
            // 설정 상태 확인
            val settings = DooraySettingsState.getInstance()
            
            val diagnosticInfo = buildString {
                append("🔍 Dooray Settings 진단 정보\n\n")
                append("✅ 설정 서비스 상태: 정상\n")
                append("📝 설정 값:\n")
                append("  - Token: ${if (settings.token.isNotEmpty()) "설정됨 (${settings.token.length}자)" else "비어있음"}\n")
                append("  - Project ID: ${if (settings.projectId.isNotEmpty()) settings.projectId else "비어있음"}\n")
                append("  - Domain: ${if (settings.domain.isNotEmpty()) settings.domain else "비어있음"}\n")
                append("\n")
                append("📍 설정 경로: ~/.config/JetBrains/[IDE]/options/dooray-settings.xml\n")
                append("\n")
                append("💡 설정 접근 방법:\n")
                append("  - Settings/Preferences → Dooray Settings\n")
                append("  - 또는 Settings/Preferences → Tools → Dooray Settings\n")
            }
            
            Messages.showInfoMessage(
                project,
                diagnosticInfo,
                "Dooray Settings 진단"
            )
            
            logger.info("Dooray Settings test completed successfully")
            
        } catch (e: Exception) {
            logger.error("Failed to test Dooray Settings", e)
            
            val errorInfo = buildString {
                append("❌ Dooray Settings 오류 발생\n\n")
                append("오류 메시지: ${e.message}\n")
                append("오류 타입: ${e.javaClass.simpleName}\n")
                append("\n")
                append("🔧 해결 방법:\n")
                append("1. IntelliJ 재시작 후 다시 시도\n")
                append("2. 플러그인 재설치\n")
                append("3. 로그 파일 확인 (Help → Show Log in Explorer)\n")
                append("4. GitHub 이슈 등록: https://github.com/dhks77/intellij-dooray-plugin/issues\n")
            }
            
            Messages.showErrorDialog(
                project,
                errorInfo,
                "Dooray Settings 오류"
            )
        }
    }
} 