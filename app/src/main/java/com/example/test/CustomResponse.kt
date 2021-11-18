package com.example.test


data class CustomResponse(val cc: String, val aa: Int, val ff: Float = 1.1f) {
    var arr: ArrayList<Inner>? = null

    override fun toString(): String {
        val str = arr?.joinToString()
        return "{cc:$cc, aa:$aa, ff:$ff, arr:[$str]}"
    }

    class Inner {
        var c: String? = null
        var d: Int = 0
        var innerList: List<Inner2>? = null

        override fun toString(): String {
            return "{c:$c, d:$d, innerList:[${innerList?.joinToString()}]}"
        }
    }

    class Inner2 {
        var c: String? = null
        var d: Int = 0
        var innerList: List<String>? = null

        override fun toString(): String {
            return "{c:$c, d:$d, innerList:[${innerList?.joinToString()}]}"
        }
    }
}