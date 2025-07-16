package com.github.dhks77.intellijdoorayplugin.dooray.facade

import com.github.dhks77.intellijdoorayplugin.dooray.vo.*
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.jackson.responseObject

private const val DOORAY_URL = "https://api.dooray.com"

fun getPost(token: String, projectId: String, postNumber: Long): Post? {
    return Fuel.get("${DOORAY_URL}/project/v1/projects/$projectId/posts",
                    listOf("postNumber" to postNumber))
        .header("Authorization" to "dooray-api $token")
        .responseObject<PostsResponse>()
        .third
        .get()
        .result
        .firstOrNull()
}