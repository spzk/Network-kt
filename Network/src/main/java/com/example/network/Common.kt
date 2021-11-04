package com.example.network

import java.io.InputStream

object Common {

    fun readText(inputStream: InputStream): String {
        return inputStream.bufferedReader().readText()
    }
}