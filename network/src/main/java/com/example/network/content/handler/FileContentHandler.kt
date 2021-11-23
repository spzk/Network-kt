package com.example.network.content.handler

import android.util.Log
import java.io.File
import java.io.IOException
import java.io.InputStream

class FileContentHandler(private val file: File) : ContentHandler<File> {
    override fun getContent(inputStream: InputStream, contentLength: Long, contentType: String): File {
        if (contentLength <= 0L){
            throw IOException("stream is empty.")
        }
        if (!file.exists() && !file.createNewFile()) {
            throw IOException("file create fail.")
        }

        inputStream.buffered(DEFAULT_BUFFER_SIZE).use {
            val output = file.outputStream().buffered(DEFAULT_BUFFER_SIZE)
            do {
                val readBytes = it.readBytes()
                output.write(readBytes)
            } while (readBytes.size == DEFAULT_BUFFER_SIZE)
            output.close()
        }
        return file
    }
}