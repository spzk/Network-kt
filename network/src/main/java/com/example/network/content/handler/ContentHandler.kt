package com.example.network.content.handler

import java.io.InputStream

interface ContentHandler<out T : Any> {
    fun getContent(inputStream: InputStream, contentLength: Long, contentType: String): T
}