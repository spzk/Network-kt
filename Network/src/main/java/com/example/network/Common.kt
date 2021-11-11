package com.example.network

import org.json.JSONObject
import java.io.InputStream
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.primaryConstructor

object Common {

    fun readText(inputStream: InputStream): String {
        return inputStream.bufferedReader().readText()
    }
}