# Network-kt

<h3>기본</h3>
<pre>
Api.get&lt;CustomResponse>("http://localhost").onResult {
    }.onFail { code, error ->
}

val data: JSONObject || JSONArray || Other Class...
Api.post&lt;CustomResponse>("http://localhost", data).onResult {
}.onFail { code, error ->
}
</pre>
<h3>Option</h3>
<pre>
val option: Request.RequestOption(
  connectTimeout: Int = DEFAULT_CONNECT_TIME_OUT,
  readTimeout: Int = DEFAULT_READ_TIME_OUT,
  useCaches: Boolean = false,
  header: MutableMap&lt;String, MutableList&lt;String>>? = null
)
Api.get&lt;CustomResponse>("http://localhost", option)
</pre>

<h3>Tag 지정 및 취소</h3>
<pre>
Api.get&lt;CustomResponse>("http://localhost", option).apply { tag = "tag" }

Api.cancel("tag") // "tag" 로 지정된 요청을 취소합니다.
Api.cancel() // 모든 요청을 취소합니다.
</pre>

<h3>응답 (각각 생략 가능)</h3>
<pre>
.onResult {
 // 메인 쓰레드가 아닙니다.
}
onPostResult {
 // onResult와 같지만 메인 쓰레드로 호출됩니다.
}
.onFail { code, error ->
}
</pre>

<h3>응답 Generic Type</h3>
<pre>
Api.get&lt;Type> //이때 지정한 type으로 응답을 받게 됩니다.
</pre>

<h3>ContentHandeler</h3>
<pre>
Api.get&lt;CustomResponse>("url").setContentHandler(object: ContentHandler&lt;CustomResponse> {
    override fun getContent(inputStream: InputStream): CustomResponse {
        TODO("Not yet implemented")
        // 직접 inputStream read를 구현 할 수 있습니다.
    }
})
.onResult {
}
</pre>

<h3>ClassConverter</h3>
<pre>
//기본 자료형 또는 JSONObject, JSONArray를 instance에 맞춰 반환
fun toJSON(instance: Any): Any

fun toJSONObject(instance: Any): JSONObject
fun toJSONArray(instance: Any): JSONArray
fun &lt;T : Any> fromJSON(clazz: Class&lt;T>, jsonString: String): T
</pre>

<h4>예</h4>
<pre>
data class CustomClass(val key: String) 

toJSON(CustomClass("value"))
> {"key":"value"}

class ElementClass(var name: String = "default")

val list = arrayListOf(ElementClass(), ElementClass("foo"))
toJSON(list)
> [{"name":"default"}, {"name":"foo"}]
</pre>
