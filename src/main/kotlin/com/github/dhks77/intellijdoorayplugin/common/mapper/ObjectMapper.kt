package com.github.dhks77.intellijdoorayplugin.common.mapper

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef

object ObjectMapper {

    val MAPPER = jacksonObjectMapper()

    init {
        MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }


    fun serialize(value: Any?): String {
        if (value == null) {
            return ""
        }
        if (value is String) {
            return value
        }
        return MAPPER.writeValueAsString(value)
    }

    fun <T> deserialize(content: String, valueType: Class<T>): T {
        return if (valueType == String::class.java) {
            @Suppress("UNCHECKED_CAST")
            content as T
        } else MAPPER.readValue(content, valueType)
    }

    fun <T> deserialize(content: String, typeReference: TypeReference<T>): T {
        return if (typeReference.type == String::class.java) {
            @Suppress("UNCHECKED_CAST")
            content as T
        } else MAPPER.readValue(content, typeReference)
    }

    inline fun <reified T> deserialize(content: String): T {
        return deserialize(content, jacksonTypeRef())
    }

}