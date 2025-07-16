package com.github.dhks77.intellijdoorayplugin.common.converter

import com.github.dhks77.intellijdoorayplugin.common.mapper.ObjectMapper
import com.github.dhks77.intellijdoorayplugin.plugin.model.PostMap
import com.intellij.util.xmlb.Converter


class PostMapConverter : Converter<PostMap>() {

    override fun toString(value: PostMap): String {
        return ObjectMapper.serialize(value)
    }

    override fun fromString(value: String): PostMap? {
        if (value.isEmpty()) {
            return PostMap()
        }
        return ObjectMapper.deserialize(value)
    }

}