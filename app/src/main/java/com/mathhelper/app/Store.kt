package com.mathhelper.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

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

    // ----- 보상: 별 / 스트릭 / 오늘의 미션 -----
    val stars: Int get() = prefs.getInt("stars", 0)
    fun addStars(n: Int) = prefs.edit().putInt("stars", stars + n).apply()

    /** 정답 보상: 별 적립 + 오늘 공부 기록(스트릭) 한 번에 */
    fun reward(n: Int) { addStars(n); recordStudyToday() }

    val streak: Int get() = prefs.getInt("streak", 0)
    private val lastStudyDate: String get() = prefs.getString("last_study", "") ?: ""

    /** 오늘 공부 기록 → 연속 학습일 갱신 (하루 한 번만 증가) */
    fun recordStudyToday() {
        val today = LocalDate.now().toString()
        if (lastStudyDate == today) return
        val yesterday = LocalDate.now().minusDays(1).toString()
        val newStreak = if (lastStudyDate == yesterday) streak + 1 else 1
        prefs.edit().putInt("streak", newStreak).putString("last_study", today).apply()
    }

    /** 오늘의 미션 완료 문제 수 (날짜 바뀌면 0) */
    var missionDone: Int
        get() = if ((prefs.getString("mission_date", "") ?: "") == LocalDate.now().toString())
            prefs.getInt("mission_done", 0) else 0
        set(v) = prefs.edit()
            .putString("mission_date", LocalDate.now().toString())
            .putInt("mission_done", v).apply()

    // ----- 외운 영어 단어 -----
    fun learnedWords(): Set<String> =
        (prefs.getStringSet("learned_words", emptySet()) ?: emptySet()).toSet()

    fun setLearned(en: String, learned: Boolean) {
        val cur = learnedWords().toMutableSet()
        if (learned) cur.add(en) else cur.remove(en)
        prefs.edit().putStringSet("learned_words", cur).apply()
    }
}
