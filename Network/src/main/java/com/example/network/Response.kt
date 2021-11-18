package com.example.network

import java.net.HttpURLConnection

class Response internal constructor(val responseCode: Int, val data: Any) {
    val isSuccess = responseCode == HttpURLConnection.HTTP_OK
}