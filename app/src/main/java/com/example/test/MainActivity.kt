package com.example.test

import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.network.*
import com.example.network.content.ClassConverter
import com.example.test.databinding.ActivityMainBinding
import com.example.test.ui.login.LoginActivity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.net.ContentHandler
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.reflect.KParameter
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
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
                Api.get<CustomResponse>("http://192.168.1.127:3000").onResult {
//                    val obj = GsonBuilder().create().fromJson(it, CustomResponse::class.java)
                    Log.d(TAG, "onClick: obj=$it")
//                    val json = GsonBuilder().create().toJson(obj)
//                    Log.d(TAG, "onClick: json=$json")
                }.onFail { code, error ->
                    Log.e(TAG, "onClick: $code / $error")
                }
            }
            R.id.btnTest2 -> {
                Api.get<DClass>("http://192.168.1.127:3000/list")
                    .onResult {
//                        val obj = GsonBuilder().create().fromJson(it, List<TClass>::class.java)
                        Log.d(TAG, "onClick: obj=$it")
//                        val json = GsonBuilder().create().toJson(it)
//                        Log.d(TAG, "onClick: json=$json")
//                        Log.d(TAG, "onResult: ${it.joinToString()}")
                    }.onFail{ code, error ->
                        Log.e(TAG, "onFail: $code / $error")
                    }


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
                val param = ClassConverter.toJSON(TClass(22))
                Log.d(TAG, "ClassConverter.toJSONString(): $param")
            }
            R.id.btnTest4 -> {

            }
        }
    }

    data class DClass(val a: Int, val b: String, val c: String = "c", var aa: String){
        var ee: String? = null
    }

    class TClass(val a: Int, val ss:String = "11"){
        val arr = arrayOf(InnerClass(1), InnerClass((2)))

        override fun toString(): String {
            return "a:$a, ss:$ss, arr:${arr.joinToString()}"
        }
    }

    class InnerClass(val inner1: Int){
        override fun toString(): String {
            return "inner1:$inner1"
        }
    }
}