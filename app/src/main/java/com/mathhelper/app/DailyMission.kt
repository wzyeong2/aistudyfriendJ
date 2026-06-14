package com.mathhelper.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Accent = Color(0xFF3D6EA5)
private val Good = Color(0xFF2E7D32)
private val Bad = Color(0xFFC62828)
const val MISSION_GOAL = 5

@Composable
fun DailyMissionScreen(store: Store, grade: Grade, onBack: () -> Unit) {
    val context = LocalContext.current
    val questions = remember { List(MISSION_GOAL) { MathGen.generate(grade) } }
    var idx by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var picked by remember(idx) { mutableStateOf<Int?>(null) }
    var finished by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Color(0xFFFAFBFD))) {
        // 헤더
        Surface(color = Accent) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "뒤로", tint = Color.White) }
                Text("오늘의 미션", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("⭐ ${store.stars}", color = Color.White, fontSize = 14.sp)
                Spacer(Modifier.width(10.dp))
            }
        }

        if (finished) {
            MissionResult(store, score, onBack)
            return
        }

        val q = questions[idx]
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp)) {
            Text("${idx + 1} / ${questions.size}   ·   ⭐ $score", color = Color.Gray, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                        Text(q.text, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(14.dp))
                q.choices.forEach { opt ->
                    val show = picked != null
                    val isCorrect = opt == q.answer
                    val color = when {
                        !show -> Color.White
                        isCorrect -> Good.copy(alpha = 0.15f)
                        opt == picked -> Bad.copy(alpha = 0.15f)
                        else -> Color.White
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        colors = CardDefaults.cardColors(containerColor = color),
                        onClick = {
                            if (picked == null) {
                                picked = opt
                                if (isCorrect) { score++; store.addStars(1); celebrateVibrate(context) }
                            }
                        },
                    ) {
                        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("$opt", fontSize = 22.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            if (show && isCorrect) Icon(Icons.Default.Check, null, tint = Good)
                            else if (show && opt == picked) Icon(Icons.Default.Close, null, tint = Bad)
                        }
                    }
                }
                picked?.let { p ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (p == q.answer) "잘했어! 정답이야 ⭐" else "아쉬워! 정답은 ${q.answer}",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        color = if (p == q.answer) Good else Bad,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(8.dp))
            Button(
                enabled = picked != null,
                onClick = {
                    if (idx + 1 >= questions.size) {
                        store.recordStudyToday()
                        store.missionDone = MISSION_GOAL
                        store.addStars(5) // 완주 보너스
                        finished = true
                    } else idx++
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    when {
                        picked == null -> "정답을 골라줘"
                        idx + 1 == questions.size -> "미션 끝내기"
                        else -> "다음 문제"
                    },
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun MissionResult(store: Store, score: Int, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(if (score >= MISSION_GOAL) "🏆" else "🎉", fontSize = 70.sp)
        Spacer(Modifier.height(12.dp))
        Text("오늘의 미션 완료!", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("$MISSION_GOAL 문제 중 ${score}개 정답  ·  완주 보너스 ⭐5", color = Color.Gray)
        Spacer(Modifier.height(16.dp))
        Surface(shape = RoundedCornerShape(14.dp), color = Accent.copy(alpha = 0.10f)) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⭐ 모은 별  ${store.stars}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Accent)
                Spacer(Modifier.height(6.dp))
                Text("🔥 ${store.streak}일 연속 공부 중!", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("JANE, 내일 또 만나자! 👋", color = Accent, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(20.dp))
        Button(onClick = onBack, modifier = Modifier.height(50.dp)) {
            Icon(Icons.Default.Home, null); Spacer(Modifier.width(6.dp)); Text("홈으로")
        }
    }
}
