package com.mathhelper.app

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Gemini(무료) / Claude(유료) 둘 다 지원하는 비전 AI 클라이언트.
 * (운동 코치 앱 AiCoach.kt의 검증된 호출 방식을 그대로 재사용)
 */
object AiClient {

    private val GEMINI_MODELS = listOf("gemini-2.5-flash", "gemini-2.5-flash-lite")
    private const val CLAUDE_MODEL = "claude-sonnet-4-6"

    suspend fun generate(
        provider: String,
        apiKey: String,
        prompt: String,
        imageJpeg: ByteArray? = null,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val text = if (provider == "claude") callClaude(apiKey, prompt, imageJpeg)
            else callGemini(apiKey, prompt, imageJpeg)
            Result.success(text.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------- Gemini ----------
    private fun callGemini(apiKey: String, prompt: String, image: ByteArray?): String {
        val parts = JSONArray().put(JSONObject().put("text", prompt))
        if (image != null) {
            parts.put(JSONObject().put("inline_data", JSONObject()
                .put("mime_type", "image/jpeg")
                .put("data", Base64.encodeToString(image, Base64.NO_WRAP))))
        }
        val body = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", parts)))
            // 구조화된 JSON 응답을 더 안정적으로 받기 위한 힌트
            .put("generationConfig", JSONObject().put("temperature", 0.4))

        var lastErr = "알 수 없는 오류"
        for (model in GEMINI_MODELS) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val (code, resp) = httpPost(url, mapOf("Content-Type" to "application/json"), body.toString())
            if (code in 200..299) {
                return JSONObject(resp).getJSONArray("candidates").getJSONObject(0)
                    .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
            }
            lastErr = parseError(resp, code)
            if (code != 429) throw RuntimeException(lastErr) // 429면 다음 모델로 폴백
        }
        throw RuntimeException(lastErr)
    }

    // ---------- Claude ----------
    private fun callClaude(apiKey: String, prompt: String, image: ByteArray?): String {
        val content = JSONArray()
        if (image != null) {
            content.put(JSONObject().put("type", "image").put("source", JSONObject()
                .put("type", "base64").put("media_type", "image/jpeg")
                .put("data", Base64.encodeToString(image, Base64.NO_WRAP))))
        }
        content.put(JSONObject().put("type", "text").put("text", prompt))
        val body = JSONObject()
            .put("model", CLAUDE_MODEL).put("max_tokens", 2000)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", content)))

        val (code, resp) = httpPost(
            "https://api.anthropic.com/v1/messages",
            mapOf(
                "x-api-key" to apiKey,
                "anthropic-version" to "2023-06-01",
                "content-type" to "application/json",
            ),
            body.toString()
        )
        if (code !in 200..299) throw RuntimeException(parseError(resp, code))
        return JSONObject(resp).getJSONArray("content").getJSONObject(0).getString("text")
    }

    // ---------- 공통 HTTP ----------
    private fun httpPost(url: String, headers: Map<String, String>, body: String): Pair<Int, String> {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
            doOutput = true
            connectTimeout = 30_000
            readTimeout = 90_000
        }
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
        return code to text
    }

    private fun parseError(resp: String, code: Int): String = try {
        JSONObject(resp).getJSONObject("error").getString("message")
    } catch (e: Exception) { "HTTP $code" }
}

/** 과목별 문제 사진 → 분석 프롬프트 빌더 */
object StudyPrompt {

    fun build(subject: Subject, grade: Grade): String = when (subject) {
        Subject.MATH -> math(grade)
        Subject.ENGLISH -> english(grade)
    }

    private fun math(grade: Grade): String = buildString {
        appendLine("너는 ${grade.label} 학생을 가르치는 다정하고 친절한 수학 선생님이야.")
        appendLine("아이가 찍어서 보낸 수학 문제 사진을 보고 분석해줘.")
        appendLine()
        appendLine("[중요 규칙]")
        appendLine("1. 사진에 아이가 직접 쓴 답/풀이가 보이면: 채점하고, 틀린 문제는 '왜 틀렸는지'를 ${grade.label} 아이가 이해할 수 있는 아주 쉬운 말로 설명해줘. (예: 받아올림을 깜빡했어, 자릿수를 잘못 맞췄어 등)")
        appendLine("2. 아이가 쓴 답이 안 보이면: 그냥 문제만 인식해서 쉬운 풀이를 알려줘. (isCorrect, childAnswer, why 는 null)")
        appendLine("3. 모든 설명은 ${grade.label} 아이가 혼자 읽고 이해할 수 있게, 짧고 쉬운 문장으로. 어려운 용어 금지.")
        appendLine("4. 풀이(steps)는 한 단계씩 따라 할 수 있게 순서대로 나눠줘. (스스로 풀도록 힌트처럼)")
        appendLine("5. concept에는 이 문제에서 꼭 알아야 할 '핵심 공식이나 개념'을 ${grade.label} 눈높이로 한 줄 적어줘. (예: '받아올림: 10이 넘으면 윗자리로 1을 올려요')")
        appendLine("6. 사진 속 문제와 같은 유형의 '연습문제'를 2~3개 새로 만들어서 practice에 넣어줘. 너무 똑같지 않게, 숫자를 바꿔서.")
        appendLine("7. 사진이 흐리거나 수학 문제가 아니면 overall 에 그 사실을 알려주고 problems는 빈 배열로.")
        appendLine()
        append(jsonSpec())
    }

    private fun english(grade: Grade): String = buildString {
        appendLine("너는 ${grade.label} 학생을 가르치는 다정하고 친절한 영어 선생님이야.")
        appendLine("아이가 찍어서 보낸 영어 문제 사진을 보고 분석해줘. 설명은 전부 '한국어'로 해줘.")
        appendLine()
        appendLine("[중요 규칙]")
        appendLine("1. 사진에 아이가 직접 쓴 답이 보이면: 채점하고, 틀린 문제는 '왜 틀렸는지'를 ${grade.label} 아이가 이해할 쉬운 한국어로 설명해줘. (예: 단수/복수 실수, 시제(과거/현재) 틀림, 스펠링 오류, 단어 뜻 혼동 등)")
        appendLine("2. 아이가 쓴 답이 안 보이면: 문제만 인식해서 쉬운 풀이를 알려줘. (isCorrect, childAnswer, why 는 null)")
        appendLine("3. steps에는 단어 뜻, 문법 포인트, 왜 그 답인지를 한 단계씩 쉬운 한국어로 적어줘.")
        appendLine("4. concept에는 이 문제의 핵심 문법/단어 포인트를 ${grade.label} 눈높이로 한 줄 적어줘. (예: '복수형: 여러 개일 땐 끝에 s를 붙여요')")
        appendLine("5. 사진 속 문제와 같은 유형의 영어 '연습문제'를 2~3개 새로 만들어 practice에 넣어줘. q는 영어 문제, a는 정답, hint는 한국어 힌트.")
        appendLine("6. 사진이 흐리거나 영어 문제가 아니면 overall 에 그 사실을 알려주고 problems는 빈 배열로.")
        appendLine()
        append(jsonSpec())
    }

    private fun jsonSpec(): String = buildString {
        appendLine("[출력 형식] 반드시 아래 JSON '하나만' 출력해. 다른 말, 코드펜스(```), 설명 절대 붙이지 마.")
        append("""{
  "overall": "전체적으로 한두 문장 격려/요약",
  "problems": [
    {
      "label": "1번",
      "problem": "인식한 문제",
      "childAnswer": "아이가 쓴 답 또는 null",
      "correct": "올바른 답",
      "isCorrect": true,
      "why": "틀렸으면 쉬운 말로 이유, 맞았으면 null",
      "steps": ["1단계 설명", "2단계 설명", "..."],
      "concept": "핵심 공식·개념 한 줄"
    }
  ],
  "practice": [
    { "q": "연습문제", "a": "정답", "hint": "한 줄 힌트(선택, 없으면 null)" }
  ]
}""")
    }

    /** 사진에서 단어를 추출하는 프롬프트 (영어만/한글만/둘다 모두 처리) */
    fun extractWords(): String =
        """첨부한 사진에서 공부할 단어들을 찾아서 '영어-한국어' 짝으로 만들어줘.
사진은 아래 셋 중 하나일 수 있어:
- 영어 단어만 적혀 있음 → 한국어 뜻(ko)을 네가 채워줘.
- 한국어 단어만 적혀 있음 → 알맞은 영어 단어(en)를 네가 채워줘.
- 영어와 한국어가 같이 적혀 있음 → 그대로 짝지어줘.
초등학생 수준의 가장 흔한 뜻으로, 단어가 아닌 건 빼고 최대 12개.
반드시 아래 JSON '하나만' 출력 (코드펜스·다른 말 금지):
{"words":[{"en":"apple","ko":"사과"},{"en":"dog","ko":"강아지"}]}"""

    /** 손글씨로 푼 답 채점용 프롬프트 (그림을 함께 보냄) */
    fun gradeHandwriting(q: String, a: String): String =
        """첨부한 그림은 학생이 손으로 쓴 답이야.
문제: "$q"
올바른 정답: "$a"
손글씨를 읽고, 학생의 답이 정답과 같은지 판정해줘. 띄어쓰기·대소문자 같은 사소한 차이는 맞은 걸로 봐줘.
반드시 아래 JSON '하나만' 출력해 (코드펜스 금지):
{"read":"네가 읽은 학생의 답","correct":true,"comment":"초등학생에게 한 줄 격려 또는 힌트"}"""
}
