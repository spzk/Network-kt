package com.example.network.content.handler

import java.io.InputStream

interface ContentHandler<T : Any> {
    fun getContent(inputStream: InputStream): T
}