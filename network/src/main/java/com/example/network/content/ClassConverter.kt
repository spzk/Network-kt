package com.example.network.content

import android.util.Log
import com.example.network.annotation.Key
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaField

object ClassConverter {

    fun toJSON(instance: Any): Any {
        val clazz = instance::class.java
        return when {
            isJsonValueType(clazz.kotlin) -> instance
            clazz.isArray || List::class.java.isInstance(instance) -> toJSONArray(instance)
            else -> toJSONObject(instance)
        }
    }

    fun toJSONObject(instance: Any): JSONObject {
        val json = JSONObject()
        val fields = instance::class.java.declaredFields
        for (i in fields.indices) {
            val field = fields[i]
            val key = field.getKeyAnnotation()
            if (key != null && key.key.isEmpty()) continue
            field.isAccessible = true
            val value = field.get(instance)?.let { toJSON(it) }
            if (key?.force != true && value == null) continue
            json.put(key?.key ?: field.name, value)
        }
        return json
    }

    fun toJSONArray(instance: Any): JSONArray {
        val clazz = instance::class
        return if (clazz.java.isArray)
            JSONArray().from((instance as Array<*>).iterator())
        else
            JSONArray().from((instance as List<*>).iterator())
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> fromJSON(clazz: Class<T>, jsonString: String): T {
        return (if (clazz.isArray)
            parseArray(clazz, JSONArray(jsonString))
        else when {
            String::class == clazz.kotlin -> jsonString
            JSONObject::class == clazz.kotlin -> JSONObject(jsonString)
            JSONArray::class == clazz.kotlin -> JSONArray(jsonString)
            List::class.isSuperclassOf(clazz.kotlin) -> parseList(clazz, JSONArray(jsonString))
            else -> parseObject(clazz, JSONObject(jsonString))
        }) as T
    }

    private fun Field.getKeyAnnotation(): Key? {
        return getDeclaredAnnotation(Key::class.java)
    }

    private fun parseObject(clazz: Class<*>, json: JSONObject): Any {
        val obj = clazz.kotlin.createInstance(json)
        val fields = clazz.kotlin.declaredMemberProperties
        fields.forEach {
            Log.d("ClassConverter", "parseObject: $it")
            val name = it.findAnnotation<Key>()?.key ?: it.name
            if (name.isNotEmpty() && it is KMutableProperty1 && json.has(name)) {
                val value = parseField(it.javaField!!, name, json)
                it.setter.call(obj, value)
            }
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
                typedArray(t.kotlin, json)
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

    private fun parseField(field: Field, name: String, json: JSONObject): Any? {
        val type = field.type
        return when {
            isJsonValueType(type.kotlin) -> {
                typedValue(type.kotlin, name, json)
            }
            List::class.isSuperclassOf(type.kotlin) -> {
                val jArray = json.getJSONArray(name)
                val t = (field.genericType as ParameterizedType).actualTypeArguments[0]
                parseList(t, jArray)
            }
            type.isArray -> {
                val jArray = json.getJSONArray(name)
                val t = (field.genericType as Class<*>).componentType!!
                typedArray(t.kotlin, jArray)
            }
            else -> {
                parseObject(type, json.getJSONObject(name))
            }
        }
    }

    private fun typedList(clazz: Class<*>, json: JSONArray): List<*> {
        return when (clazz.kotlin) {
            String::class -> createList(json) { index -> json.optString(index) }
            Int::class -> createList(json) { index -> json.optInt(index) }
            Long::class -> createList(json) { index -> json.optLong(index) }
            Float::class -> createList(json) { index -> json.optDouble(index).toFloat() }
            Double::class -> createList(json) { index -> json.optDouble(index) }
            Boolean::class -> createList(json) { index -> json.optBoolean(index) }
            JSONObject::class -> createList(json) { index -> json.optJSONObject(index) }
            JSONArray::class -> createList(json) { index -> json.optJSONArray(index) }
            else -> createList(json) { index -> parseObject(clazz, json.optJSONObject(index)) }
        }
    }

    private fun typedArray(clazz: KClass<*>, json: JSONArray): Array<*> {
        return when (clazz) {
            String::class -> createArray(json) { index -> json.optString(index) }
            Int::class -> createArray(json) { index -> json.optInt(index) }
            Long::class -> createArray(json) { index -> json.optLong(index) }
            Float::class -> createArray(json) { index -> json.optDouble(index).toFloat() }
            Double::class -> createArray(json) { index -> json.optDouble(index) }
            Boolean::class -> createArray(json) { index -> json.optBoolean(index) }
            JSONObject::class -> createArray(json) { index -> json.optJSONObject(index) }
            JSONArray::class -> createArray(json) { index -> json.optJSONArray(index) }
            else -> createArray(json) { index -> json.opt(index) }
        }
    }

    private fun typedValue(clazz: KClass<*>, key: String, json: JSONObject): Any? {
        return when (clazz) {
            String::class -> json.optString(key)
            Int::class -> json.optInt(key)
            Long::class -> json.optLong(key)
            Float::class -> json.optDouble(key).toFloat()
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

    private fun isJsonValueType(clazz: KClass<*>): Boolean =
        clazz.javaPrimitiveType != null || clazz == String::class
                || clazz == JSONObject::class || clazz == JSONArray::class

    private fun KClass<*>.createInstance(json: JSONObject): Any {
        return if (primaryConstructor!!.parameters.isEmpty()) {
            createInstance()
        } else {
            primaryConstructor!!.callBy(primaryConstructor!!.parameters.filter {
                val name = it.findAnnotation<Key>()?.key ?: it.name
                json.has(name)
            }
                .associateWith { json.opt(it.findAnnotation<Key>()?.key ?: it.name) })
        }
    }

    private fun JSONArray.from(iterator: Iterator<*>): JSONArray {
        iterator.forEach {
            val value = it?.let { toJSON(it) }
            put(value)
        }
        return this
    }
}