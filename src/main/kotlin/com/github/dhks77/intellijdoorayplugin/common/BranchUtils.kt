package com.github.dhks77.intellijdoorayplugin.common

object BranchUtils {
    fun extractTaskNumber(branchName: String): Long? {
        return branchName.split("/").last().toLongOrNull()
    }
}
