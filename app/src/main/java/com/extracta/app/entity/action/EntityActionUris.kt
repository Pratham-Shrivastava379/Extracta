package com.extracta.app.entity.action

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object EntityActionUris {
    fun dial(phoneNumber: String): String = "tel:${encode(phoneNumber)}"
    fun email(address: String): String = "mailto:${encode(address)}"
    fun map(address: String): String = "geo:0,0?q=${encode(address)}"

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
}
