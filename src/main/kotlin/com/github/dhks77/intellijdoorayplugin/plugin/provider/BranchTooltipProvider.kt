package com.github.dhks77.intellijdoorayplugin.plugin.provider

import com.github.dhks77.intellijdoorayplugin.common.BranchUtils
import com.github.dhks77.intellijdoorayplugin.dooray.facade.getPost
import com.github.dhks77.intellijdoorayplugin.plugin.config.DooraySettingsState
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class BranchTooltipProvider(private val project: Project, private val scope: CoroutineScope) {

    private val cache = ConcurrentHashMap<String, String>()

    fun getTooltipForBranch(branchName: String): String? {
        cache[branchName]?.let { return it }

        val taskNumber = BranchUtils.extractTaskNumber(branchName) ?: return null

        fetchTooltipAsync(branchName, taskNumber)

        return "로딩 중..."
    }

    private fun fetchTooltipAsync(branchName: String, taskNumber: Long) {
        scope.launch(Dispatchers.IO) {
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

                ApplicationManager.getApplication().invokeLater {
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
