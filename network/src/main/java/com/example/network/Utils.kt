package com.example.network

import java.io.InputStream

object Utils {

    fun readText(inputStream: InputStream): String {
        return inputStream.bufferedReader().readText()
    }
}