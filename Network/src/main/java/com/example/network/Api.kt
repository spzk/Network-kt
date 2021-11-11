package com.example.network

import android.util.Log
import com.example.network.Request.Companion.TIME_OUT
import com.example.network.content.ClassConverter
import com.example.network.content.handler.ContentHandler
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object Api {
    private const val TAG = "Api"
    const val GET = "GET"
    const val POST = "POST"

    const val CONTENT_TYPE = "Content-Type"
    const val APPLICATION_JSON = "application/json"

    private val mExecutor =
        ThreadPoolExecutor(0, 4, Long.MAX_VALUE, TimeUnit.NANOSECONDS, LinkedBlockingDeque())
    private var mScope: CoroutineScope? = null

    inline fun <reified T : Any> get(url: String): Request<T> {
        return Request.create(url, GET)
    }

    inline fun <reified T : Any> post(url: String, data: String): Request<T> {
        return Request.create(url, POST, data)
    }

    fun cancel() {
        mScope?.cancel()
    }

    internal fun send(req: Request<*>) {
        if (req.isExecuted) return
        req.isExecuted = true
        if (mScope?.isActive != true) {
            mScope = CoroutineScope(SupervisorJob() + mExecutor.asCoroutineDispatcher())
        }
        mScope!!.launch {
            runCatching {
                val url = URL(req.url)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = req.method
                conn.doInput = true

                if (req.method == POST) {
                    conn.doOutput = true
                    conn.setRequestProperty(CONTENT_TYPE, APPLICATION_JSON)
                    val bufferedWriter = conn.outputStream.bufferedWriter()
                    bufferedWriter.write(req.data)
                    bufferedWriter.flush()
                    bufferedWriter.close()
                }

                conn.connect()

                val response = when (conn.responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        val startTime = System.currentTimeMillis()
                        val resultObj = req.handleContent(conn.inputStream)
                        Log.d(TAG, "delay: ${System.currentTimeMillis() - startTime}")
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
                req.onResponse(it)
            }.onFailure {
                yield()
                it.printStackTrace()
                req.onException(it)
            }
        }
    }

    class RequestOption {
        var timeout: Long = TIME_OUT
        var useCaches: Boolean = false

        private lateinit var mRequestProperty: MutableMap<String, MutableList<String>>

        fun isEmptyHeader(): Boolean {
            return !::mRequestProperty.isInitialized || mRequestProperty.isNullOrEmpty()
        }

        fun getRequestProperty(): MutableMap<String, MutableList<String>> {
            if (!::mRequestProperty.isInitialized) mRequestProperty = mutableMapOf()
            return mRequestProperty
        }

        fun setRequestProperty(header: MutableMap<String, MutableList<String>>) {
            mRequestProperty = header
        }

        fun setRequestProperty(key: String, value: String) {
            getRequestProperty()[key] = mutableListOf(value)
        }

        fun addRequestProperty(key: String, value: String) {
            val map = getRequestProperty()
            map[key]?.also { it.add(value) }
                ?: let {
                    map[key] = mutableListOf(value)
                }
        }
    }
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

fun <T : Any> Request<T>.onPost(callback: (response: T) -> Unit): Request<T> {
    mPostResult = callback
    Api.send(this)
    return this
}
