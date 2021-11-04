package com.example.test

import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.network.*
import com.example.test.databinding.ActivityMainBinding
import com.example.test.ui.login.LoginActivity
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.reflect.typeOf

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.onClick = this
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btnTest1 -> {
                Log.d(TAG, "onClick: ")
                Api.get<CustomResponse>("http://192.168.1.127:3000").onResult {
                    Log.d(TAG, "onClick: ${it}, ${Looper.getMainLooper().isCurrentThread}")
                }.onFail { code, error ->
                    Log.e(TAG, "onClick: ${code} / $error")
//                    binding.textView.text = error
                }

//                Api.get<CustomResponse>("http://192.168.1.127:3000").setContentHandler {
//                }.onResult {
//                    Log.d(TAG, "onClick: $it")
//                }

            }
            R.id.btnTest2 -> {
//                val param = JSONObject().put("qq", "qq").put("zz", 22)
//                Api.post<Api.Response>("http://192.168.1.127:3000", param.toString()).onResult {
//                    Log.d(TAG, "onClick: ${it}, ${Looper.getMainLooper().isCurrentThread}")
//                    binding.textView.visibility =
//                        if (binding.textView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
//                    binding.textView.text = it.getResponse()
//                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
//                }.onFail { code, error ->
//                    Log.e(TAG, "onClick: ${code} / $error")
//                }.onPost {
//                    Log.d(TAG, "onClick:post ${it}, ${Looper.getMainLooper().isCurrentThread}")
//                    binding.textView.visibility =
//                        if (binding.textView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
//                    binding.textView.text = it.getResponse()
////                    binding.textView.text = it
//                }


            }
            R.id.btnTest3 -> {
                Api.cancel()
            }
            R.id.btnTest4 -> {
                DClass::class.constructors.first().parameters.forEach {
                    Log.d(TAG, "onClick: ${it.type}, ${it.name}")
                }
            }
        }
    }

    data class DClass(val a: Int, val b: String, val c: String = "c"){
        var ee: String? = null
    }
}