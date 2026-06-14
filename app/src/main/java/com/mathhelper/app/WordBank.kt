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
}
