package com.example.network

import android.util.JsonReader
import android.util.JsonToken
import android.util.Log
import com.example.network.Api.Request.Companion.TIME_OUT
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONStringer
import org.json.JSONTokener
import java.io.InputStream
import java.net.ContentHandler
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.nio.charset.Charset
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object Api {
    private const val TAG = "Api"
    const val GET = "GET"
    const val POST = "POST"

    const val CONTENT_TYPE = "Content-Type"
    const val APPLICATION_JSON = "application/json"
    const val TEXT = "text/"

//    var defaultContentHandler: IContentHandler<Any> = DefaultContentHandler()

    private val mExecutor =
        ThreadPoolExecutor(0, 4, Long.MAX_VALUE, TimeUnit.NANOSECONDS, LinkedBlockingDeque())
    private var mScope: CoroutineScope? = null

    init {
        URLConnection.setContentHandlerFactory {
            Log.d(TAG, "setContentHandlerFactory: $it")
            when(it){
                APPLICATION_JSON -> {
                    JSONContentHandler()
                }
                else -> TextContentHandler()
            }
        }
    }

    inline fun <reified T> get(url: String): Request<T> {
        return Request.create(url, GET)
    }

    inline fun <reified T> post(url: String, data: String): Request<T> {
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
                        val resultObj = req.mContentHandler?.getContent(req.clazz, conn.inputStream) ?: let {
//                            val bufferedReader = conn.inputStream.reader()
//                            parse(req.clazz, bufferedReader.readText())
                        }
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

//    private fun parseArray(clazz: Class<*>, arr: JSONArray): Any{

//    }

    private fun parse(clazz: Class<*>, response: String): Any {
        return when {
            clazz.isArray -> {
                val jsonArray = JSONArray(response)
//                parseArray(clazz.componentType!!, jsonArray)
            }
            clazz.isAssignableFrom(String::class.java) -> {
                response
            }
            clazz.isAssignableFrom(JSONObject::class.java) -> {
                JSONObject(response)
            }
            clazz.isAssignableFrom(JSONArray::class.java) -> {
                JSONArray(response)
            }
            else -> {
                val obj = clazz.newInstance()
                val json = JSONObject(response)
                clazz.declaredFields.forEach {
                    if (json.has(it.name)) {
                        val value = json.get(it.name)
                        //                    val value = when(it.type){
                        //                        Int::class.java -> {
                        //                            json.getInt(it.name)
                        //                        }
                        //                        String::class.java -> {
                        //                            json.getString(it.name)
                        //                        }
                        //                        else -> {
                        //                            json.get(it.name)
                        //                        }
                        //                    }
                        it.isAccessible = true
                        it.set(obj, value)
                    }
                }
                obj!!
            }
        }
    }

    abstract class ContentParser<out T> {
        abstract fun <T> getContent(clazz: Class<T>, inputStream: InputStream): T
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

    class TextContentHandler : ContentHandler() {
        override fun getContent(conn: URLConnection): Any {
            Log.d(TAG, "getContent: ${conn.contentType} / ${conn.contentLength} / ${conn.contentEncoding}")
            val bufferedReader = conn.inputStream.reader()
            return bufferedReader.readText()
        }
    }

    class JSONContentHandler : ContentHandler() {
        override fun getContent(conn: URLConnection): Any {
            Log.d(TAG, "getContent: ${conn.contentType} / ${conn.contentLength} / ${conn.contentEncoding}")
            val bufferedReader = conn.inputStream.reader()
            val response = bufferedReader.readText()
            return when {
                response.startsWith(BEGIN_ARRAY_TOKEN) -> {
                    JSONArray(response)
                }
                else -> JSONObject(response)
            }
        }

        companion object {
            private const val BEGIN_OBJECT_TOKEN = "{"
            private const val BEGIN_ARRAY_TOKEN = "["
        }
    }

    class Request<T> @PublishedApi internal constructor(
        internal val clazz: Class<T>,
        val url: String,
        val method: String
    ) {
        internal var mContentHandler: ContentParser<T>? = JsonContentHandler()
        internal var mResult: ((response: T) -> Unit)? = null
        internal var mPostResult: ((response: T) -> Unit)? = null
        internal var mError: ((code: Int, error: String) -> Unit)? = null
        internal var isExecuted = false

        //config
        var timeout: Long = TIME_OUT
        var header: MutableMap<String, String>? = null

        var data: String? = null

        suspend fun onResponse(res: Response) {
            when(res.isSuccess){
                true -> {
                    val obj = res.data as T
                    mResult?.invoke(obj)
                    mPostResult?.also { callback ->
                        withContext(Dispatchers.Main) {
                            callback.invoke(obj)
                        }
                    }
                }
                else -> {
                    mError?.invoke(res.responseCode, res.data as String)
                }
            }
        }

        fun onException(e: Throwable) {
            mError?.invoke(-1, e.toString())
        }

        companion object {
            const val TIME_OUT = 30000L

            inline fun <reified T> create(
                url: String,
                method: String,
                param: String? = null
            ): Request<T> {
                return Request(T::class.java, url, method).apply {
                    data = param
                }
            }
        }
    }

    class Response internal constructor(val responseCode: Int, val data: Any) {
        val isSuccess = responseCode == HttpURLConnection.HTTP_OK
    }
}

fun <T> Api.Request<T>.setContentHandler(callback: Api.ContentParser<T>): Api.Request<T> {
    mContentHandler = callback
    Api.send(this)
    return this
}

//fun <T> Api.Request<T>.setContentHandler(callback: (inputStream: InputStream) -> T): Api.Request<T> {
//    return setContentHandler(object : Api.ContentParser<T>{
//        override fun getContent(inputStream: InputStream): T {
//            return callback(inputStream)
//        }
//    })
//}

fun <T> Api.Request<T>.onResult(callback: (response: T) -> Unit): Api.Request<T> {
    mResult = callback
    Api.send(this)
    return this
}

fun <T> Api.Request<T>.onFail(callback: (code: Int, error: String) -> Unit): Api.Request<T> {
    mError = callback
    Api.send(this)
    return this
}

fun <T> Api.Request<T>.onPost(callback: (response: T) -> Unit): Api.Request<T> {
    mPostResult = callback
    Api.send(this)
    return this
}
