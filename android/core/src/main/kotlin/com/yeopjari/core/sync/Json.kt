package com.yeopjari.core.sync

/**
 * 아주 작은 JSON 읽기/쓰기. 서버와 주고받는 모양이 단순해서 라이브러리 없이 처리한다
 * (core 모듈을 Android 없이 테스트하기 위해). 객체 → Map, 배열 → List, 숫자 → Double.
 */
object Json {

    fun write(value: Any?): String = StringBuilder().also { writeTo(it, value) }.toString()

    private fun writeTo(sb: StringBuilder, value: Any?) {
        when (value) {
            null -> sb.append("null")
            is String -> quote(sb, value)
            is Boolean, is Int, is Long -> sb.append(value.toString())
            is Double -> sb.append(if (value.isFinite()) value.toString() else "null")
            is Number -> sb.append(value.toString())
            is Enum<*> -> quote(sb, value.name)
            is Map<*, *> -> {
                sb.append('{')
                value.entries.forEachIndexed { i, (k, v) ->
                    if (i > 0) sb.append(',')
                    quote(sb, k.toString()); sb.append(':'); writeTo(sb, v)
                }
                sb.append('}')
            }
            is Iterable<*> -> {
                sb.append('[')
                value.forEachIndexed { i, v -> if (i > 0) sb.append(','); writeTo(sb, v) }
                sb.append(']')
            }
            else -> quote(sb, value.toString())
        }
    }

    private fun quote(sb: StringBuilder, s: String) {
        sb.append('"')
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\""); '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n"); '\r' -> sb.append("\\r"); '\t' -> sb.append("\\t")
                else -> if (c < ' ') sb.append("\\u%04x".format(c.code)) else sb.append(c)
            }
        }
        sb.append('"')
    }

    fun parse(text: String): Any? = Parser(text).run { val v = value(); ws(); require(i == s.length) { "JSON 끝에 남은 글자" }; v }

    private class Parser(val s: String) {
        var i = 0

        fun ws() { while (i < s.length && s[i].isWhitespace()) i++ }

        fun value(): Any? {
            ws()
            require(i < s.length) { "JSON 이 갑자기 끝남" }
            return when (val c = s[i]) {
                '{' -> obj()
                '[' -> arr()
                '"' -> str()
                't' -> lit("true", true)
                'f' -> lit("false", false)
                'n' -> lit("null", null)
                else -> if (c == '-' || c.isDigit()) num() else error("JSON 위치 $i 에서 모르는 글자 '$c'")
            }
        }

        fun obj(): Map<String, Any?> {
            val m = LinkedHashMap<String, Any?>()
            i++; ws()
            if (s[i] == '}') { i++; return m }
            while (true) {
                ws(); val k = str(); ws(); expect(':'); m[k] = value(); ws()
                if (s[i] == ',') { i++; continue }
                expect('}'); return m
            }
        }

        fun arr(): List<Any?> {
            val l = ArrayList<Any?>()
            i++; ws()
            if (s[i] == ']') { i++; return l }
            while (true) {
                l += value(); ws()
                if (s[i] == ',') { i++; continue }
                expect(']'); return l
            }
        }

        fun str(): String {
            expect('"')
            val sb = StringBuilder()
            while (s[i] != '"') {
                val c = s[i++]
                if (c != '\\') { sb.append(c); continue }
                when (val e = s[i++]) {
                    'n' -> sb.append('\n'); 't' -> sb.append('\t'); 'r' -> sb.append('\r')
                    'b' -> sb.append('\b'); 'f' -> sb.append('\u000c')
                    'u' -> { sb.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                    else -> sb.append(e)
                }
            }
            i++
            return sb.toString()
        }

        fun num(): Double {
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] in "+-.eE")) i++
            return s.substring(start, i).toDouble()
        }

        fun lit(word: String, v: Any?): Any? {
            require(s.startsWith(word, i)) { "JSON 위치 $i: $word 기대" }
            i += word.length
            return v
        }

        fun expect(c: Char) {
            require(i < s.length && s[i] == c) { "JSON 위치 $i: '$c' 기대" }
            i++
        }
    }
}

// 응답 Map 에서 값 꺼내기 도우미
@Suppress("UNCHECKED_CAST")
internal fun Any?.obj(): Map<String, Any?> = this as Map<String, Any?>
internal fun Any?.list(): List<Any?> = (this as? List<Any?>) ?: emptyList()
internal fun Map<String, Any?>.str(k: String): String = this[k] as? String ?: ""
internal fun Map<String, Any?>.strOrNull(k: String): String? = this[k] as? String
internal fun Map<String, Any?>.int(k: String): Int = (this[k] as? Double)?.toInt() ?: 0
internal fun Map<String, Any?>.intOrNull(k: String): Int? = (this[k] as? Double)?.toInt()
internal fun Map<String, Any?>.bool(k: String): Boolean = this[k] as? Boolean ?: false
