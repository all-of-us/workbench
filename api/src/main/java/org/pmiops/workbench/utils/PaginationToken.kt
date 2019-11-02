package org.pmiops.workbench.utils

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.nio.charset.Charset
import java.util.Base64
import java.util.Objects
import org.pmiops.workbench.exceptions.BadRequestException

class PaginationToken(@field:JsonProperty("o")
                      val offset: Long, @field:JsonProperty("h")
                      val parameterHash: Int) {

    fun toBase64(): String {
        return Base64.getEncoder().encodeToString(Gson().toJson(this).toByteArray(UTF_8))
    }

    fun matchesParameters(vararg parameters: Any): Boolean {
        return Objects.hash(*parameters) == parameterHash
    }

    companion object {

        private val UTF_8 = Charset.forName("UTF-8")

        fun of(offset: Long, vararg parameters: Any): PaginationToken {
            return PaginationToken(offset, Objects.hash(*parameters))
        }

        fun fromBase64(str: String): PaginationToken {
            val json = String(Base64.getDecoder().decode(str), UTF_8)
            try {
                val result = Gson().fromJson(json, PaginationToken::class.java)
                if (result.offset < 0) {
                    throw BadRequestException(
                            String.format("Invalid pagination offset: %d", result.offset))
                }
                return result
            } catch (e: JsonSyntaxException) {
                throw BadRequestException(String.format("Invalid pagination token: %s", str))
            }

        }
    }
}
