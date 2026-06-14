package com.mathhelper.app

import kotlin.random.Random

/** 자동 생성 수학 문제 (4지선다) */
data class MathQuestion(val text: String, val answer: Int, val choices: List<Int>)

/** 학년에 맞춰 연산 문제를 무한 생성 (AI 없이 로컬, 무료) */
object MathGen {

    fun generate(grade: Grade): MathQuestion {
        val (text, answer) = when (grade) {
            Grade.E1 -> addSub(1, 20)
            Grade.E2 -> addSub(1, 100)
            Grade.E3 -> mix(1, 1000, mulMax = 9)
            Grade.E4 -> mix(1, 1000, mulMax = 12)
            Grade.E5, Grade.E6 -> mix(1, 10000, mulMax = 19)
            else -> mix(1, 10000, mulMax = 19)
        }
        return MathQuestion(text, answer, choices(answer))
    }

    private fun addSub(min: Int, max: Int): Pair<String, Int> {
        return if (Random.nextBoolean()) {
            val a = Random.nextInt(min, max); val b = Random.nextInt(min, max)
            "$a + $b = ?" to (a + b)
        } else {
            val a = Random.nextInt(min, max); val b = Random.nextInt(min, a + 1)
            "$a − $b = ?" to (a - b)
        }
    }

    private fun mix(min: Int, max: Int, mulMax: Int): Pair<String, Int> {
        return when (Random.nextInt(3)) {
            0 -> {
                val a = Random.nextInt(2, mulMax + 1); val b = Random.nextInt(2, mulMax + 1)
                "$a × $b = ?" to (a * b)
            }
            1 -> {
                val a = Random.nextInt(min, max); val b = Random.nextInt(min, max)
                "$a + $b = ?" to (a + b)
            }
            else -> {
                val a = Random.nextInt(min, max); val b = Random.nextInt(min, a + 1)
                "$a − $b = ?" to (a - b)
            }
        }
    }

    /** 정답 + 비슷한 오답 3개 → 섞어서 4지선다 */
    private fun choices(answer: Int): List<Int> {
        val set = linkedSetOf(answer)
        val deltas = listOf(-10, -3, -2, -1, 1, 2, 3, 10, 11)
        var guard = 0
        while (set.size < 4 && guard < 60) {
            guard++
            val c = answer + deltas.random()
            if (c >= 0) set.add(c)
        }
        var n = answer + 1
        while (set.size < 4) { if (n >= 0) set.add(n); n++ }
        return set.toList().shuffled()
    }
}
