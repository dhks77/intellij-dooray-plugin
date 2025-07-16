package com.github.dhks77.intellijdoorayplugin.plugin.provider

import com.github.dhks77.intellijdoorayplugin.dooray.facade.getPost
import com.github.dhks77.intellijdoorayplugin.plugin.config.DooraySettingsState
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class BranchTooltipProvider(private val project: Project) {
    
    private val cache = ConcurrentHashMap<String, String>()
    private val scope = CoroutineScope(Dispatchers.IO)
    
    fun getTooltipForBranch(branchName: String): String? {
        // 캐시에서 먼저 확인
        cache[branchName]?.let { return it }
        
        // 브랜치 이름에서 task number 추출
        val taskNumber = extractTaskNumber(branchName) ?: return null
        
        // 백그라운드에서 API 호출
        fetchTooltipAsync(branchName, taskNumber)
        
        return "로딩 중..."
    }
    
    private fun extractTaskNumber(branchName: String): Long? {
        // 브랜치 이름에서 숫자 추출 (예: feature/123, bugfix/456-test)
        val patterns = listOf(
            """\d+""".toRegex(),  // 단순 숫자
            """(\d+)""".toRegex(), // 괄호 안의 숫자
            """/(\d+)""".toRegex(), // 슬래시 뒤의 숫자
            """-(\d+)""".toRegex(), // 하이픈 뒤의 숫자
            """_(\d+)""".toRegex()  // 언더스코어 뒤의 숫자
        )
        
        for (pattern in patterns) {
            val match = pattern.find(branchName)
            if (match != null) {
                val numberStr = match.groupValues.getOrNull(1) ?: match.value
                try {
                    return numberStr.toLong()
                } catch (e: NumberFormatException) {
                    continue
                }
            }
        }
        return null
    }
    
    private fun fetchTooltipAsync(branchName: String, taskNumber: Long) {
        scope.launch {
            try {
                val settings = DooraySettingsState.getInstance()
                if (settings.token.isEmpty() || settings.projectId.isEmpty()) {
                    cache[branchName] = "Dooray 설정이 필요합니다"
                    return@launch
                }
                
                val post = getPost(settings.token, settings.projectId, taskNumber)
                val tooltip = if (post != null) {
                    "${post.number}: ${post.subject}"
                } else {
                    "업무를 찾을 수 없습니다"
                }
                
                cache[branchName] = tooltip
                
                // UI 업데이트를 위해 EDT에서 실행
                ApplicationManager.getApplication().invokeLater {
                    // 여기서 UI 갱신 이벤트를 발생시킬 수 있습니다
                    thisLogger().info("Branch tooltip updated for $branchName: $tooltip")
                }
                
            } catch (e: Exception) {
                cache[branchName] = "API 호출 실패: ${e.message}"
                thisLogger().warn("Failed to fetch tooltip for branch $branchName", e)
            }
        }
    }
    
    fun clearCache() {
        cache.clear()
    }
} 