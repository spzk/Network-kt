package com.example.network.annotation

/**
 * @param key key 지정, 이 값이 "" 일 경우 해당 필드는 포함되지 않음
 * @param force true: value가 null이어도 key 포함
 */
@Target(AnnotationTarget.FIELD)
annotation class Key(val key: String, val force: Boolean = false)