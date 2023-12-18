package com.example.test

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.network.*
import com.example.network.content.ClassConverter
import com.example.network.content.handler.ContentHandler
import com.example.test.databinding.ActivityMainBinding
import org.json.JSONObject
import java.io.InputStream

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val TAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.onClick = this
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btnTest1 -> {
                Api.get<JSONObject>("http://10.0.2.2:8081").onResult {
                    Log.d(TAG, "onResult: ${it.getString("a")}")
//                    Log.d(TAG, "onResult to json: ${ClassConverter.toJSONObject(it)}")
                }.onFail { code, error ->
                    Log.e(TAG, "onFail: $code / $error")
                }
            }
            R.id.btnTest2 -> {
                Api.get<List<ElementClass>>("http://localhost/list")
                    .apply {
                        //tag = ""
                        //contentType = "Application/json" //default
                    }.onResult {
                        Log.d(TAG, "onResult: $it")
                    }.onFail { code, error ->
                        Log.e(TAG, "onFail: $code / $error")
                    }

                Api.get<CustomResponse>("http://localhost", Request.RequestOption())
                    .apply {
                        tag = "b"
                    }
                    .onResult {
                    }
                    .onFail { code, msg ->
                    }
            }
            R.id.btnTest3 -> {
                Api.post<CustomResponse>("http://localhost", Request.RequestOption())
                    .apply {
                        tag = "a"
                    }
                    .setContentHandler(object : ContentHandler<CustomResponse> {
                        override fun getContent(
                            inputStream: InputStream,
                            contentLength: Long,
                            contentType: String
                        ): CustomResponse {
                            TODO("Not yet implemented")
                        }
                    })
                    .onResult {
                    }
                    .onFail { code, msg ->
                    }
            }
            R.id.btnTest4 -> {
                Api.cancel()
            }
        }
    }

    data class ElementClass(val value: String)
}