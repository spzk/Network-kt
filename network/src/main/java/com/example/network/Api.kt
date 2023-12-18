package com.example.network

import android.util.Log
import com.example.network.Request.Companion.DEFAULT_CONNECT_TIME_OUT
import com.example.network.Request.Companion.DEFAULT_READ_TIME_OUT
import com.example.network.content.ClassConverter
import com.example.network.content.handler.ContentHandler
import kotlinx.coroutines.*
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.*

object Api {
    private const val TAG = "Api"
    const val GET = "GET"
    const val POST = "POST"

    const val CONTENT_TYPE = "Content-Type"
    const val APPLICATION_JSON = "application/json"

    private val mRequestMap = ConcurrentHashMap<String, BlockingQueue<Request<*>>>()
    private var mScope: CoroutineScope? = null

    inline fun <reified T : Any> get(
        url: String,
        options: Request.RequestOption? = null
    ): Request<T> {
        return Request.create(url, GET, options = options)
    }

    inline fun <reified T : Any> post(
        url: String,
        data: Any,
        options: Request.RequestOption? = null
    ): Request<T> {
        return Request.create(url, POST, param = data, options = options)
    }

    inline fun <reified T : Any> request(
        url: String,
        method: String,
        data: Any? = null,
        options: Request.RequestOption? = null
    ): Request<T> {
        return Request.create(url, method = method, param = data, options = options)
    }

    fun cancel(tag: String? = null) {
        when (tag) {
            null -> {
                mScope?.cancel()
                mRequestMap.clear()
            }
            else -> {
                mRequestMap[tag]?.forEach {
                    it.cancel()
                }
            }
        }
    }

    internal fun send(req: Request<*>) {
        if (req.isExecuted) return
        req.isExecuted = true
        if (mScope?.isActive != true) {
            mScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        }
        req.tag?.let { tag ->
            mRequestMap.getOrPut(tag) { LinkedBlockingDeque() }.add(req)
        }
        mScope!!.launch {
            runCatching {
                val url = URL(req.url)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = req.method
                conn.doInput = true

                req.options?.let {
                    conn.connectTimeout = it.connectTimeout
                    conn.readTimeout = it.readTimeout
                    conn.useCaches = it.useCaches
                    if (it.hasRequestProperty()) {
                        conn.requestProperties.putAll(it.getRequestProperty())
                    }
                } ?: let {
                    conn.connectTimeout = DEFAULT_CONNECT_TIME_OUT
                    conn.readTimeout = DEFAULT_READ_TIME_OUT
                }

                if (req.method == POST) {
                    req.data?.let {
                        conn.doOutput = true
                        val param = ClassConverter.toJSON(it)
                        if (req.contentType != null) {
                            conn.setRequestProperty(CONTENT_TYPE, req.contentType)
                        } else {
                            conn.setRequestProperty(CONTENT_TYPE, APPLICATION_JSON)
                        }
                        val bufferedWriter = conn.outputStream.bufferedWriter()
                        bufferedWriter.write(param.toString())
                        bufferedWriter.flush()
                        bufferedWriter.close()
                    }
                }

                if (req.isCanceled) return@runCatching null
                conn.connect()
                if (req.isCanceled) return@runCatching null

                val response = when (conn.responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        Log.d(TAG, "send: ${conn.contentLength}")
                        val resultObj = req.handleContent(conn)
                        Response(conn.responseCode, resultObj)
                    }
                    else -> {
                        val error = conn.responseCode
                        val errorMessage = conn.responseMessage
                        Response(error, errorMessage)
                    }
                }
                runCatching { conn.inputStream.close() }
                conn.disconnect()
                response
            }.onSuccess {
                yield()
                if (it == null || req.isCanceled) return@onSuccess
                req.onResponse(it)
            }.onFailure {
                yield()
                if (req.isCanceled) return@onFailure
                it.printStackTrace()
                req.onException(it)
            }
            req.tag?.let { tag ->
                mRequestMap[tag]?.remove(req)
                if (mRequestMap[tag]?.isEmpty() == true) {
                    mRequestMap.remove(tag)
                }
            }
        }
    }
}

fun <T : Any> Request<T>.setContentHandler(callback: (inputStream: InputStream, contentLength: Long, contentType: String) -> T): Request<T> {
    mContentHandler = object : ContentHandler<T> {
        override fun getContent(inputStream: InputStream, contentLength: Long, contentType: String): T {
            return callback(inputStream, contentLength, contentType)
        }
    }
    Api.send(this)
    return this
}

fun <T : Any> Request<T>.setContentHandler(callback: ContentHandler<T>): Request<T> {
    mContentHandler = callback
    Api.send(this)
    return this
}

fun <T : Any> Request<T>.onResult(callback: (response: T) -> Unit): Request<T> {
    mResult = callback
    Api.send(this)
    return this
}

fun <T : Any> Request<T>.onFail(callback: (code: Int, error: String) -> Unit): Request<T> {
    mError = callback
    Api.send(this)
    return this
}

fun <T : Any> Request<T>.onPostResult(callback: (response: T) -> Unit): Request<T> {
    mPostResult = callback
    Api.send(this)
    return this
}
