package com.example.network

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType

class JsonContentHandler<out T> : Api.ContentParser<T>() {

    override fun <T> getContent(clazz: Class<T>, inputStream: InputStream): T {
        val text = Common.readText(inputStream)
        Log.d(TAG, "getContent: $text")
        return if (text.startsWith(BEGIN_ARRAY_TOKEN)) {
            parseArray(clazz, JSONArray(text))
        } else {
            parseObject(clazz, JSONObject(text))
        } as T
    }

    private fun parseObject(clazz: Class<*>, json: JSONObject): Any? {
        val obj = clazz.newInstance()
        val fields = clazz.declaredFields
        for (i in fields.indices) {
            val field = fields[i]
            Log.d(TAG, "parseObject: field=${field.name}, type=${field.type.kotlin}")
            val value = parseField(field, json)
            Log.d(TAG, "parseObject: field=${field.name}, value=$value")
            field.isAccessible = true
            field.set(obj, value)
        }
        return obj
    }

    private fun parseArray(clazz: Class<*>, json: JSONArray): Array<*> {
        val t = clazz.componentType!!
        return when {
            t.isArray -> {
                Array(json.length()) {
                    parseArray(t, json.getJSONArray(it))
                }
            }
            else -> {
                typedArray(t, json)
            }
        }
    }

    private fun parseList(type: Type, json: JSONArray): List<*> {
        if (type is WildcardType) {
            val wildcardType = type.upperBounds[0] as ParameterizedType
            if (wildcardType.rawType == List::class.java) {
                return MutableList(json.length()) {
                    parseList(wildcardType.actualTypeArguments[0], json.getJSONArray(it))
                }
            }
        }
        return typedList(type as Class<*>, json)
    }


    private fun parseField(field: Field, json: JSONObject): Any? {
        Log.d(TAG, "parseField: field=${field.name}")
        val type = field.type
        return when {
            isJsonValueType(type) -> {
                typedValue(type, field.name, json)
            }
            type == List::class.java -> {
                val jArray = json.getJSONArray(field.name)
                val t = (field.genericType as ParameterizedType).actualTypeArguments[0]
                parseList(t, jArray)
            }
            type.isArray -> {
                val jArray = json.getJSONArray(field.name)
                val t = (field.genericType as Class<*>).componentType!!
                typedArray(t, jArray)
            }
            else -> {
                parseObject(type, json.getJSONObject(field.name))
            }
        }
    }

    companion object {
        private const val TAG = "JsonContentHandler"
        private const val BEGIN_ARRAY_TOKEN = "["

        private fun isJsonValueType(clazz: Class<*>): Boolean =
            clazz.kotlin.javaPrimitiveType != null || clazz == String::class.java
                    || clazz == JSONObject::class.java || clazz == JSONArray::class.java

        private fun typedList(clazz: Class<*>, json: JSONArray): List<*> {
            return when (clazz.kotlin) {
                String::class -> createList(json) { index -> json.optString(index) }
                Int::class -> createList(json) { index -> json.optInt(index) }
                Long::class -> createList(json) { index -> json.optLong(index) }
                Double::class -> createList(json) { index -> json.optDouble(index) }
                Boolean::class -> createList(json) { index -> json.optBoolean(index) }
                JSONObject::class -> createList(json) { index -> json.optJSONObject(index) }
                JSONArray::class -> createList(json) { index -> json.optJSONArray(index) }
                else -> createList(json) { index -> json.opt(index) }
            }
        }

        private fun typedArray(clazz: Class<*>, json: JSONArray): Array<*> {
            return when (clazz.kotlin) {
                String::class -> createArray(json) { index -> json.optString(index) }
                Int::class -> createArray(json) { index -> json.optInt(index) }
                Long::class -> createArray(json) { index -> json.optLong(index) }
                Double::class -> createArray(json) { index -> json.optDouble(index) }
                Boolean::class -> createArray(json) { index -> json.optBoolean(index) }
                JSONObject::class -> createArray(json) { index -> json.optJSONObject(index) }
                JSONArray::class -> createArray(json) { index -> json.optJSONArray(index) }
                else -> createArray(json) { index -> json.opt(index) }
            }
        }

        private fun typedValue(clazz: Class<*>, key: String, json: JSONObject): Any? {
            return when (clazz.kotlin) {
                String::class -> json.optString(key)
                Int::class -> json.optInt(key)
                Long::class -> json.optLong(key)
                Double::class -> json.optDouble(key)
                Boolean::class -> json.optBoolean(key)
                JSONObject::class -> json.optJSONObject(key)
                JSONArray::class -> json.optJSONArray(key)
                else -> json.opt(key)
            }
        }

        private inline fun <reified T> createArray(
            json: JSONArray,
            typedGetter: (index: Int) -> T
        ): Array<T> = Array(json.length()) { typedGetter(it) }

        private inline fun <reified T> createList(
            json: JSONArray,
            typedGetter: (index: Int) -> T
        ): List<T> = MutableList(json.length()) { typedGetter(it) }
    }
}