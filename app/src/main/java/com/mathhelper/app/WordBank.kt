package com.mathhelper.app

/** 영어 단어 한 개 */
data class Word(val en: String, val ko: String, val emoji: String)

/** 초등학교 2학년 수준 기초 영어 단어 (바로 내장) */
object WordBank {
    val GRADE2: List<Word> = listOf(
        // 동물
        Word("cat", "고양이", "🐱"),
        Word("dog", "강아지", "🐶"),
        Word("bird", "새", "🐦"),
        Word("fish", "물고기", "🐟"),
        Word("rabbit", "토끼", "🐰"),
        Word("lion", "사자", "🦁"),
        Word("bear", "곰", "🐻"),
        Word("pig", "돼지", "🐷"),
        // 색깔
        Word("red", "빨강", "🔴"),
        Word("blue", "파랑", "🔵"),
        Word("green", "초록", "🟢"),
        Word("yellow", "노랑", "🟡"),
        // 음식
        Word("apple", "사과", "🍎"),
        Word("banana", "바나나", "🍌"),
        Word("milk", "우유", "🥛"),
        Word("bread", "빵", "🍞"),
        Word("egg", "달걀", "🥚"),
        Word("water", "물", "💧"),
        // 가족·사람
        Word("mom", "엄마", "👩"),
        Word("dad", "아빠", "👨"),
        Word("baby", "아기", "👶"),
        Word("friend", "친구", "🧒"),
        // 학교
        Word("book", "책", "📕"),
        Word("pencil", "연필", "✏️"),
        Word("bag", "가방", "🎒"),
        Word("school", "학교", "🏫"),
        // 몸
        Word("hand", "손", "✋"),
        Word("eye", "눈", "👁️"),
        Word("nose", "코", "👃"),
        Word("ear", "귀", "👂"),
        // 자연
        Word("sun", "해", "☀️"),
        Word("moon", "달", "🌙"),
        Word("tree", "나무", "🌳"),
        Word("star", "별", "⭐"),
        // 동작·상태
        Word("run", "달리다", "🏃"),
        Word("happy", "행복한", "😊"),
    )

    /** 초1 — 아주 기초 */
    val GRADE1: List<Word> = listOf(
        Word("cat", "고양이", "🐱"), Word("dog", "강아지", "🐶"), Word("fish", "물고기", "🐟"),
        Word("bird", "새", "🐦"), Word("cow", "소", "🐮"), Word("duck", "오리", "🦆"),
        Word("red", "빨강", "🔴"), Word("blue", "파랑", "🔵"), Word("green", "초록", "🟢"),
        Word("apple", "사과", "🍎"), Word("milk", "우유", "🥛"), Word("water", "물", "💧"),
        Word("mom", "엄마", "👩"), Word("dad", "아빠", "👨"), Word("sun", "해", "☀️"),
        Word("moon", "달", "🌙"), Word("star", "별", "⭐"), Word("tree", "나무", "🌳"),
        Word("hand", "손", "✋"), Word("eye", "눈", "👁️"), Word("ear", "귀", "👂"),
        Word("car", "자동차", "🚗"), Word("ball", "공", "⚽"), Word("hat", "모자", "🧢"),
    )

    /** 초3 이상 — 단어 확장 */
    val GRADE3: List<Word> = GRADE2 + listOf(
        Word("teacher", "선생님", "👩‍🏫"), Word("student", "학생", "🧑‍🎓"),
        Word("morning", "아침", "🌅"), Word("night", "밤", "🌃"),
        Word("rain", "비", "🌧️"), Word("snow", "눈", "❄️"), Word("wind", "바람", "🌬️"),
        Word("eat", "먹다", "🍽️"), Word("sleep", "자다", "😴"), Word("read", "읽다", "📖"),
        Word("write", "쓰다", "✍️"), Word("play", "놀다", "🤾"), Word("study", "공부하다", "📚"),
        Word("big", "큰", "🐘"), Word("small", "작은", "🐜"), Word("fast", "빠른", "⚡"),
        Word("slow", "느린", "🐢"), Word("hot", "뜨거운", "🔥"), Word("cold", "차가운", "🧊"),
        Word("river", "강", "🏞️"), Word("mountain", "산", "⛰️"), Word("flower", "꽃", "🌸"),
    )

    /** 학년에 맞는 단어 목록 */
    fun forGrade(grade: Grade): List<Word> = when (grade) {
        Grade.E1 -> GRADE1
        Grade.E2 -> GRADE2
        else -> GRADE3
    }
}
