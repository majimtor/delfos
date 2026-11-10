package com.manuelalejandro.delfos.api

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.springframework.stereotype.Component
import tools.jackson.core.JsonGenerator
import tools.jackson.core.JsonParser
import tools.jackson.core.JsonToken
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.module.SimpleModule

/**
 * The shared module carries field-error parameters as kotlinx [JsonPrimitive] values so the same
 * classes work on every platform; this module lets Jackson read and write them as plain JSON scalars.
 */
@Component
class JsonPrimitiveModule : SimpleModule("delfos-json-primitive") {

    init {
        addSerializer(JsonPrimitive::class.java, Serializer())
        addDeserializer(JsonPrimitive::class.java, Deserializer())
    }

    private class Serializer : ValueSerializer<JsonPrimitive>() {
        override fun serialize(value: JsonPrimitive, gen: JsonGenerator, ctxt: SerializationContext) {
            val content = value.content
            when {
                value is JsonNull -> gen.writeNull()
                value.isString -> gen.writeString(content)
                content == "true" || content == "false" -> gen.writeBoolean(content.toBoolean())
                content.toLongOrNull() != null -> gen.writeNumber(content.toLong())
                content.toDoubleOrNull() != null -> gen.writeNumber(content.toDouble())
                else -> gen.writeString(content)
            }
        }
    }

    private class Deserializer : ValueDeserializer<JsonPrimitive>() {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): JsonPrimitive = when (p.currentToken()) {
            JsonToken.VALUE_STRING -> JsonPrimitive(p.string)
            JsonToken.VALUE_NUMBER_INT -> JsonPrimitive(p.longValue)
            JsonToken.VALUE_NUMBER_FLOAT -> JsonPrimitive(p.doubleValue)
            JsonToken.VALUE_TRUE -> JsonPrimitive(true)
            JsonToken.VALUE_FALSE -> JsonPrimitive(false)
            JsonToken.VALUE_NULL -> JsonNull
            else -> ctxt.handleUnexpectedToken(JsonPrimitive::class.java, p) as JsonPrimitive
        }
    }
}
