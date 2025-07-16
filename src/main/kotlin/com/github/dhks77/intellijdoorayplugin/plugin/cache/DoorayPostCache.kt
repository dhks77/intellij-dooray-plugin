package com.github.dhks77.intellijdoorayplugin.plugin.cache

import com.github.dhks77.intellijdoorayplugin.dooray.vo.Post
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
@State(
    name = "DoorayPostCache",
    storages = [Storage("dooray-post-cache.xml")]
)
class DoorayPostCache : PersistentStateComponent<DoorayPostCache.State> {
    private val cache = ConcurrentHashMap<String, Post?>()
    
    companion object {
        private const val CACHE_EXPIRY_DAYS = 30L
        private const val CACHE_EXPIRY_MILLIS = CACHE_EXPIRY_DAYS * 24 * 60 * 60 * 1000L
        
        fun getInstance(project: Project): DoorayPostCache {
            return project.getService(DoorayPostCache::class.java)
        }
    }
    
    data class State(
        var posts: MutableMap<String, PostData?> = mutableMapOf()
    )
    
    data class PostData(
        var id: String = "",
        var subject: String = "",
        var taskNumber: String = "",
        var closed: Boolean = false,
        var number: Long = 0,
        var timestamp: Long = 0
    )
    
    fun getPost(branchName: String): Post? {
        return cache[branchName]
    }
    
    fun putPost(branchName: String, post: Post?) {
        cache[branchName] = post
    }
    
    fun hasPost(branchName: String): Boolean {
        return cache.containsKey(branchName)
    }
    
    fun clear() {
        cache.clear()
    }
    
    override fun getState(): State {
        val state = State()
        val currentTime = System.currentTimeMillis()
        
        cache.forEach { (branchName, post) ->
            state.posts[branchName] = post?.let { 
                PostData(
                    id = it.id,
                    subject = it.subject,
                    taskNumber = it.taskNumber,
                    closed = it.closed,
                    number = it.number,
                    timestamp = currentTime
                )
            }
        }
        return state
    }
    
    override fun loadState(state: State) {
        cache.clear()
        val currentTime = System.currentTimeMillis()
        
        state.posts.forEach { (branchName, postData) ->
            // 캐시 만료 체크
            if (postData != null && (currentTime - postData.timestamp) < CACHE_EXPIRY_MILLIS) {
                cache[branchName] = Post(
                    id = postData.id,
                    subject = postData.subject,
                    taskNumber = postData.taskNumber,
                    closed = postData.closed,
                    number = postData.number,
                    milestone = null,
                    tags = null
                )
            }
            // 만료된 캐시는 로드하지 않음 (자동 삭제)
        }
    }
} 