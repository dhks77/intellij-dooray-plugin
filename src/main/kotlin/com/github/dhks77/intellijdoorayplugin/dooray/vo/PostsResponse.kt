package com.github.dhks77.intellijdoorayplugin.dooray.vo

data class PostsResponse(val result: List<Post>)

data class Post(val id: String,
                val subject: String,
                val taskNumber: String,
                val closed: Boolean,
                val number: Long,
                val milestone: Milestone?,
                val tags: List<Tag>?)


data class Tag(val id: String)

data class Milestone(val id: String, val name: String)