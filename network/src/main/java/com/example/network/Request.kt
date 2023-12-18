package com.example.network

import com.example.network.content.ClassConverter
import com.example.network.content.handler.ContentHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLConnection

class Request<T : Any> @PublishedApi internal constructor(
    private val clazz: Class<T>,
    val url: String,
    val method: String,
) {
    internal var mContentHandler: ContentHandler<T>? = null
    internal var mResult: ((response: T) -> Unit)? = null
    internal var mPostResult: ((response: T) -> Unit)? = null
    internal var mError: ((code: Int, error: String) -> Unit)? = null
    internal var isExecuted = false

    var isCanceled = false
    var tag: String? = null

    //config
    var options: RequestOption? = null
    var contentType: String? = null

    var data: Any? = null

    fun cancel() {
        isCanceled = true
    }

    internal fun handleContent(conn: URLConnection): Any {
        return mContentHandler?.getContent(conn.inputStream, conn.contentLengthLong, conn.contentType)
            ?: ClassConverter.fromJSON(
            clazz,
            Utils.readText(conn.inputStream)
        )
    }

    @Suppress("UNCHECKED_CAST")
    internal suspend fun onResponse(res: Response) {
        when (res.isSuccess) {
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

    internal fun onException(e: Throwable) {
        mError?.invoke(-1, e.toString())
    }

    class RequestOption(
        var connectTimeout: Int = DEFAULT_CONNECT_TIME_OUT,
        var readTimeout: Int = DEFAULT_READ_TIME_OUT,
        var useCaches: Boolean = false,
        header: MutableMap<String, MutableList<String>>? = null
    ) {

        init {
            header?.let {
                setRequestProperty(it)
            }
        }

        private lateinit var mRequestProperty: MutableMap<String, MutableList<String>>

        fun hasRequestProperty(): Boolean {
            return ::mRequestProperty.isInitialized && mRequestProperty.isNotEmpty()
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

    companion object {
        const val DEFAULT_CONNECT_TIME_OUT = 15000
        const val DEFAULT_READ_TIME_OUT = 15000

        inline fun <reified T : Any> create(
            url: String,
            method: String,
            param: Any? = null,
            options: RequestOption?
        ): Request<T> {
            return Request(T::class.java, url, method).apply {
                this.options = options
                data = param
            }
        }
    }
}