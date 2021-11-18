# Network-kt

// 기본
Api.get<CustomResponse>("http://localhost").onResult {
}.onFail { code, error ->
}

val data: JSONObject || JSONArray || Other Class...
Api.post<CustomResponse>("http://localhost", data).onResult {
}.onFail { code, error ->
}

//Option
val option: Request.RequestOption(
  connectTimeout: Int = DEFAULT_CONNECT_TIME_OUT,
  readTimeout: Int = DEFAULT_READ_TIME_OUT,
  useCaches: Boolean = false,
  header: MutableMap<String, MutableList<String>>? = null
  )
Api.get<CustomResponse>("http://localhost", option)


//Tag 지정 및 취소
Api.get<CustomResponse>("http://localhost", option).apply { tag = "tag" }

Api.cancel("tag") // "tag" 로 지정된 요청을 취소합니다.
Api.cancel() // 모든 요청을 취소합니다.


//응답 (각각 생략 가능)
.onResult {
 // 메인 쓰레드가 아닙니다.
}
onPostResult {
 // onResult와 같지만 메인 쓰레드로 호출됩니다.
}
.onFail { code, error ->
}


//응답 Generic Type
Api.get<Type> //이때 지정한 type으로 응답을 받게 됩니다.




//ContentHandeler
Api.get<CustomResponse>("url").setContentHandler(object: ContentHandler<CustomResponse> {
    override fun getContent(inputStream: InputStream): CustomResponse {
        TODO("Not yet implemented")
        // 직접 inputStream read를 구현 할 수 있습니다.
    }
})
.onResult {
}


//ClassConverter
fun toJSON(instance: Any): Any
기본 자료형 또는 JSONObject, JSONArray를 instance에 맞춰 반환

fun toJSONObject(instance: Any): JSONObject
fun toJSONArray(instance: Any): JSONArray
fun <T : Any> fromJSON(clazz: Class<T>, jsonString: String): T

//예
data class CustomClass(val key: String) 

toJSON(CustomClass("value"))
> {"key":"value"}

class ElementClass(var name: String = "default")

val list = arrayListOf(ElementClass(), ElementClass("foo"))
toJSON(list)
> [{"name":"default"}, {"name":"foo"}]
