package com.example.test

import android.os.Parcel
import android.os.Parcelable
import org.json.JSONObject


class CustomResponse{
//    var a: String? = null
//    var b: Int = 0
    var arr: List<Inner>? = null
//    var jsonObj: List<Inner>? = null
//    var innerClz: Inner? = null

    override fun toString(): String {
//        return "a=$a, b=$b, arr:${arr?.joinToString()}, jsonObj:${jsonObj?.joinToString()}, innerClz:($innerClz)"
        val str = arr?.joinToString()
        return str?:""
    }

    class Inner{
        var c: String? = null
        var d: Int = 0
        var innerList: List<Inner2>? = null

        override fun toString(): String {
            return "c:$c, d:$d, innerList:${innerList?.joinToString()}"
        }
    }

    class Inner2{
        var c: String? = null
        var d: Int = 0
        var innerList: List<String>? = null

        override fun toString(): String {
            return "c:$c, d:$d, innerList:${innerList?.joinToString()}"
        }
    }
}