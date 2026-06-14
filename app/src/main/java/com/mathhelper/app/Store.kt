package com.mathhelper.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** API 키·학년·분석 기록을 기기에 저장 */
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("math_helper", Context.MODE_PRIVATE)

    var provider: String
        get() = prefs.getString("provider", "gemini") ?: "gemini"
        set(v) = prefs.edit().putString("provider", v).apply()

    var geminiKey: String
        get() = prefs.getString("key_gemini", "") ?: ""
        set(v) = prefs.edit().putString("key_gemini", v).apply()

    var claudeKey: String
        get() = prefs.getString("key_claude", "") ?: ""
        set(v) = prefs.edit().putString("key_claude", v).apply()

    fun keyFor(p: String): String = if (p == "claude") claudeKey else geminiKey

    var grade: Grade
        get() = Grade.from(prefs.getString("grade", Grade.E2.name))
        set(v) = prefs.edit().putString("grade", v.name).apply()

    var subject: Subject
        get() = Subject.from(prefs.getString("subject", Subject.MATH.name))
        set(v) = prefs.edit().putString("subject", v.name).apply()

    /** 최근 분석 기록 (최신이 앞). 최대 20개 보관. */
    fun history(): List<Analysis> {
        val raw = prefs.getString("history", "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { Analysis.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addHistory(a: Analysis) {
        val list = mutableListOf<JSONObject>()
        list.add(a.toJson())
        history().forEach { list.add(it.toJson()) }
        val trimmed = list.take(20)
        prefs.edit().putString("history", JSONArray(trimmed).toString()).apply()
    }

    fun clearHistory() = prefs.edit().remove("history").apply()

    // ----- 외운 영어 단어 -----
    fun learnedWords(): Set<String> =
        (prefs.getStringSet("learned_words", emptySet()) ?: emptySet()).toSet()

    fun setLearned(en: String, learned: Boolean) {
        val cur = learnedWords().toMutableSet()
        if (learned) cur.add(en) else cur.remove(en)
        prefs.edit().putStringSet("learned_words", cur).apply()
    }
}
