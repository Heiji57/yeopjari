package com.yeopjari.core

import com.yeopjari.core.sync.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class JsonTest {
    @Test
    fun roundTripsKoreanAndNesting() {
        val value = mapOf("title" to "첫 \"강변북로\"\n합류", "n" to 3, "list" to listOf(true, null, 1.5), "empty" to emptyMap<String, Any>())
        val parsed = Json.parse(Json.write(value)) as Map<*, *>
        assertEquals("첫 \"강변북로\"\n합류", parsed["title"])
        assertEquals(3.0, parsed["n"])
        assertEquals(listOf(true, null, 1.5), parsed["list"])
        assertEquals(emptyMap<String, Any>(), parsed["empty"])
    }

    @Test
    fun parsesUnicodeEscapes() {
        assertEquals("가", Json.parse("\"\\uac00\""))
    }
}
