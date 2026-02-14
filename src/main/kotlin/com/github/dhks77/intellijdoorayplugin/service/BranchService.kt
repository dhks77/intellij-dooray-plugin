package com.github.dhks77.intellijdoorayplugin.service

import com.intellij.dvcs.repo.VcsRepositoryManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class BranchService(private val project: Project) {

    fun getBranchName(): String? {
        val vcsRepositoryManager = VcsRepositoryManager.getInstance(project)
        val repositories = vcsRepositoryManager.getRepositories()
        if (repositories.isEmpty()) {
            return null
        }
        val repository = repositories.first()
        return repository.currentBranchName
    }

}
