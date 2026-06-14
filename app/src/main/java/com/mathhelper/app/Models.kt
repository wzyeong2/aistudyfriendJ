package com.mathhelper.app

import org.json.JSONObject

/** 학년 — 설정에서 고르면 그 눈높이로 설명한다. */
enum class Grade(val label: String) {
    E1("초1"), E2("초2"), E3("초3"), E4("초4"),
    E5("초5"), E6("초6"), M1("중1"), M2("중2"), M3("중3");

    companion object {
        fun from(name: String?): Grade = entries.firstOrNull { it.name == name } ?: E2
    }
}

/** 과목 — 설정에서 고르면 그 과목 방식으로 분석한다. */
enum class Subject(val label: String, val emoji: String) {
    MATH("수학", "🔢"), ENGLISH("영어", "🔤");

    companion object {
        fun from(name: String?): Subject = entries.firstOrNull { it.name == name } ?: MATH
    }
}

/** 한 문제에 대한 분석 결과 */
data class ProblemResult(
    val label: String,          // "1번" 등
    val problem: String,        // 인식한 문제
    val childAnswer: String?,   // 아이가 쓴 답 (없으면 null)
    val correct: String,        // 올바른 답
    val isCorrect: Boolean?,    // 맞았는지 (childAnswer 없으면 null)
    val why: String?,           // 틀린 이유 (아이 눈높이, 틀렸을 때만)
    val steps: List<String>,    // 쉬운 단계별 풀이
)

/** 유사 연습문제 */
data class PracticeProblem(
    val q: String,
    val a: String,
    val hint: String?,
)

/** 사진 한 장 분석 전체 결과 */
data class Analysis(
    val overall: String,
    val problems: List<ProblemResult>,
    val practice: List<PracticeProblem>,
    val timeMillis: Long = 0L,
    val gradeLabel: String = "",
    val subjectLabel: String = "",
    /** 파싱 실패 시 원문을 그대로 보여주기 위한 폴백 */
    val rawFallback: String? = null,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("overall", overall)
        put("time", timeMillis)
        put("grade", gradeLabel)
        put("subject", subjectLabel)
        if (rawFallback != null) put("raw", rawFallback)
        put("problems", org.json.JSONArray().apply {
            problems.forEach { p ->
                put(JSONObject().apply {
                    put("label", p.label)
                    put("problem", p.problem)
                    put("childAnswer", p.childAnswer ?: JSONObject.NULL)
                    put("correct", p.correct)
                    put("isCorrect", p.isCorrect ?: JSONObject.NULL)
                    put("why", p.why ?: JSONObject.NULL)
                    put("steps", org.json.JSONArray(p.steps))
                })
            }
        })
        put("practice", org.json.JSONArray().apply {
            practice.forEach { pr ->
                put(JSONObject().apply {
                    put("q", pr.q); put("a", pr.a); put("hint", pr.hint ?: JSONObject.NULL)
                })
            }
        })
    }

    companion object {
        /** AI가 돌려준 JSON 문자열을 Analysis로 파싱. 실패하면 원문을 폴백으로. */
        fun parse(text: String): Analysis {
            // 코드펜스·앞뒤 군더더기 제거 후, 첫 '{' ~ 마지막 '}' 만 추출해 파싱
            val t = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val start = t.indexOf('{')
            val end = t.lastIndexOf('}')
            val jsonStr = if (start in 0 until end) t.substring(start, end + 1) else t
            return try {
                fromJson(JSONObject(jsonStr))
            } catch (e: Exception) {
                Analysis(
                    overall = "",
                    problems = emptyList(),
                    practice = emptyList(),
                    rawFallback = text.trim(),
                )
            }
        }

        fun fromJson(o: JSONObject): Analysis {
            val problems = mutableListOf<ProblemResult>()
            o.optJSONArray("problems")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val p = arr.getJSONObject(i)
                    val steps = mutableListOf<String>()
                    p.optJSONArray("steps")?.let { s -> for (j in 0 until s.length()) steps.add(s.getString(j)) }
                    problems.add(
                        ProblemResult(
                            label = p.optString("label", "${i + 1}번"),
                            problem = p.optString("problem", ""),
                            childAnswer = p.optStringOrNull("childAnswer"),
                            correct = p.optString("correct", ""),
                            isCorrect = if (p.isNull("isCorrect")) null else p.optBoolean("isCorrect"),
                            why = p.optStringOrNull("why"),
                            steps = steps,
                        )
                    )
                }
            }
            val practice = mutableListOf<PracticeProblem>()
            o.optJSONArray("practice")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val p = arr.getJSONObject(i)
                    practice.add(
                        PracticeProblem(
                            q = p.optString("q", ""),
                            a = p.optString("a", ""),
                            hint = p.optStringOrNull("hint"),
                        )
                    )
                }
            }
            return Analysis(
                overall = o.optString("overall", ""),
                problems = problems,
                practice = practice,
                timeMillis = o.optLong("time", 0L),
                gradeLabel = o.optString("grade", ""),
                subjectLabel = o.optString("subject", ""),
                rawFallback = o.optStringOrNull("raw"),
            )
        }

        private fun JSONObject.optStringOrNull(key: String): String? =
            if (isNull(key)) null else optString(key, "").ifBlank { null }
    }
}
