package com.mathhelper.app

import android.graphics.Bitmap
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale

private val Accent = Color(0xFF3D6EA5)
private val Good = Color(0xFF2E7D32)
private val Bad = Color(0xFFC62828)

@Composable
fun VocabScreen(store: Store, onBack: () -> Unit) {
    val context = LocalContext.current
    // 영어 발음용 TTS
    val tts = remember {
        val holder = arrayOfNulls<TextToSpeech>(1)
        holder[0] = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) holder[0]?.language = Locale.US
        }
        holder[0]!!
    }
    DisposableEffect(Unit) { onDispose { tts.stop(); tts.shutdown() } }
    fun speak(en: String) = tts.speak(en, TextToSpeech.QUEUE_FLUSH, null, en)

    val words = remember { WordBank.GRADE2 }
    val learned = remember { mutableStateListOf<String>().apply { addAll(store.learnedWords()) } }
    fun toggle(en: String, on: Boolean) {
        if (on) { if (!learned.contains(en)) learned.add(en) } else learned.remove(en)
        store.setLearned(en, on)
    }
    var mode by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize().background(Color(0xFFFAFBFD))) {
        // 헤더
        Surface(color = Accent) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "뒤로", tint = Color.White)
                }
                Text("영어 단어", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("외움 ${learned.size}/${words.size}", color = Color.White, fontSize = 13.sp)
                Spacer(Modifier.width(8.dp))
            }
        }
        // 모드 선택 (가로 스크롤)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            listOf("단어장", "암기카드", "퀴즈", "📷 사진퀴즈").forEachIndexed { i, label ->
                FilterChip(
                    selected = mode == i,
                    onClick = { mode = i },
                    label = { Text(label) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }

        when (mode) {
            0 -> WordListMode(words, learned, ::speak, ::toggle)
            1 -> FlashcardMode(words, learned, ::speak, ::toggle)
            2 -> QuizMode(words, store, ::speak)
            else -> PhotoQuizMode(store, ::speak)
        }
    }
}

@Composable
private fun WordListMode(
    words: List<Word>,
    learned: SnapshotStateList<String>,
    speak: (String) -> Unit,
    toggle: (String, Boolean) -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
        words.forEach { w ->
            val isLearned = learned.contains(w.en)
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isLearned) Good.copy(alpha = 0.10f) else Color.White
                ),
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(w.emoji, fontSize = 26.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(w.en, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(w.ko, fontSize = 14.sp, color = Color.Gray)
                    }
                    IconButton(onClick = { speak(w.en) }) {
                        Icon(Icons.Default.VolumeUp, "발음", tint = Accent)
                    }
                    IconButton(onClick = { toggle(w.en, !isLearned) }) {
                        Icon(
                            if (isLearned) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            "외움", tint = if (isLearned) Good else Color.LightGray,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun FlashcardMode(
    words: List<Word>,
    learned: SnapshotStateList<String>,
    speak: (String) -> Unit,
    toggle: (String, Boolean) -> Unit,
) {
    val deck = remember { words.shuffled() }
    var idx by remember { mutableStateOf(0) }
    var flipped by remember { mutableStateOf(false) }
    val w = deck[idx]

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("${idx + 1} / ${deck.size}", color = Color.Gray, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        // 카드 (탭하면 뒤집기)
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            onClick = { flipped = !flipped },
        ) {
            Column(
                Modifier.fillMaxSize().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(w.emoji, fontSize = 64.sp)
                Spacer(Modifier.height(16.dp))
                if (!flipped) {
                    Text(w.en, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("카드를 탭하면 뜻이 보여요", fontSize = 13.sp, color = Color.LightGray)
                } else {
                    Text(w.ko, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Accent)
                    Spacer(Modifier.height(8.dp))
                    Text(w.en, fontSize = 16.sp, color = Color.Gray)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { speak(w.en) }) {
                    Icon(Icons.Default.VolumeUp, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp)); Text("발음 듣기")
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { flipped = false; idx = (idx + 1) % deck.size },
                modifier = Modifier.weight(1f).height(52.dp),
            ) { Text("다시 (모르겠어)") }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = { toggle(w.en, true); flipped = false; idx = (idx + 1) % deck.size },
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Good),
            ) {
                Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp)); Text("외웠어요!")
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun QuizMode(words: List<Word>, store: Store, speak: (String) -> Unit) {
    val context = LocalContext.current
    val quiz = remember { words.shuffled().take(10) }
    var qIdx by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var picked by remember { mutableStateOf<Word?>(null) }

    if (qIdx >= quiz.size) {
        // 결과
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(if (score >= 8) "🏆" else "🎉", fontSize = 64.sp)
            Spacer(Modifier.height(12.dp))
            Text("${quiz.size}문제 중 ${score}개 맞았어요!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("JANE, 정말 잘했어! 👏", color = Accent)
            Spacer(Modifier.height(20.dp))
            Button(onClick = { qIdx = 0; score = 0; picked = null }) {
                Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("다시 풀기")
            }
        }
        return
    }

    val current = quiz[qIdx]
    val options = remember(qIdx) {
        (listOf(current) + words.filter { it != current }.shuffled().take(3)).shuffled()
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp)) {
        Text("${qIdx + 1} / ${quiz.size}   ·   점수 $score", color = Color.Gray, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))

        // 문제 + 보기 (남는 공간에서 스크롤)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(current.emoji, fontSize = 40.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(current.en, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text("이 단어의 뜻은?", fontSize = 14.sp, color = Color.Gray)
                    IconButton(onClick = { speak(current.en) }) {
                        Icon(Icons.Default.VolumeUp, "발음", tint = Accent)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            options.forEach { opt ->
                val show = picked != null
                val isCorrect = opt == current
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
                            if (isCorrect) { score++; celebrateVibrate(context); store.reward(1) }
                        }
                    },
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(opt.ko, fontSize = 17.sp, modifier = Modifier.weight(1f))
                        if (show && isCorrect) Icon(Icons.Default.Check, null, tint = Good)
                        else if (show && opt == picked) Icon(Icons.Default.Close, null, tint = Bad)
                    }
                }
            }
            picked?.let { p ->
                Spacer(Modifier.height(10.dp))
                Text(
                    if (p == current) "잘했어! 정답이야 🎉" else "아쉬워! 정답은 '${current.ko}'",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = if (p == current) Good else Bad,
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // 하단 고정 버튼 (항상 보임)
        Spacer(Modifier.height(8.dp))
        Button(
            enabled = picked != null,
            onClick = { picked = null; qIdx++ },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(
                when {
                    picked == null -> "정답을 골라줘"
                    qIdx + 1 == quiz.size -> "결과 보기"
                    else -> "다음 문제"
                },
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ===== 사진 → 랜덤 손글씨 퀴즈 =====

@Composable
private fun PhotoQuizMode(store: Store, speak: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var stage by remember { mutableStateOf("intro") } // intro | loading | quiz | done
    var words by remember { mutableStateOf<List<Word>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var finalScore by remember { mutableStateOf(0) }

    fun extract(bmp: Bitmap) {
        val provider = store.provider
        val key = store.keyFor(provider)
        if (key.isBlank()) { error = "⚙️ 설정에서 API 키를 먼저 넣어줘."; return }
        stage = "loading"; error = null
        scope.launch {
            val res = AiClient.generate(provider, key, StudyPrompt.extractWords(), jpegBytes(bmp))
            res.onSuccess {
                val ws = parseWords(it)
                if (ws.size < 2) { error = "단어를 충분히 못 찾았어. 또렷한 사진으로 다시!"; stage = "intro" }
                else { words = ws; stage = "quiz" }
            }.onFailure { error = "실패: ${it.message}"; stage = "intro" }
        }
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) pendingUri?.let { uri -> loadBitmapFromUri(context, uri)?.let { extract(it) } }
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { loadBitmapFromUri(context, it)?.let { b -> extract(b) } }
    }
    fun camera() { val uri = newCameraUri(context); pendingUri = uri; takePicture.launch(uri) }

    when (stage) {
        "loading" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Accent)
                Spacer(Modifier.height(12.dp))
                Text("단어를 읽고 있어요...")
            }
        }
        "quiz" -> PhotoQuizPlay(words, store, speak, onDone = { finalScore = it; stage = "done" })
        "done" -> Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(if (finalScore >= words.size) "🏆" else "🎉", fontSize = 60.sp)
            Spacer(Modifier.height(12.dp))
            Text("${words.size}문제 중 ${finalScore}개 맞았어!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp)); Text("JANE 최고! 👏", color = Accent)
            Spacer(Modifier.height(20.dp))
            Button(onClick = { stage = "intro"; words = emptyList() }) {
                Icon(Icons.Default.PhotoCamera, null); Spacer(Modifier.width(6.dp)); Text("새 사진으로 또 하기")
            }
        }
        else -> Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(20.dp))
            Icon(Icons.Default.PhotoCamera, null, Modifier.size(64.dp), tint = Accent)
            Spacer(Modifier.height(12.dp))
            Text("단어가 적힌 걸 사진으로 찍어봐!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "AI가 단어를 읽고 랜덤 손글씨 퀴즈를 내줄게.\n(뜻 보고 영어 쓰기 / 영어 보고 뜻 쓰기)",
                fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { camera() },
                modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp),
            ) { Icon(Icons.Default.PhotoCamera, null); Spacer(Modifier.width(8.dp)); Text("카메라로 찍기", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { pickImage.launch("image/*") },
                modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp),
            ) { Icon(Icons.Default.Image, null); Spacer(Modifier.width(8.dp)); Text("갤러리에서 고르기") }
            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = Bad, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun PhotoQuizPlay(
    words: List<Word>,
    store: Store,
    speak: (String) -> Unit,
    onDone: (Int) -> Unit,
) {
    // 문제마다 타입 랜덤: true=뜻 보고 영어 쓰기, false=영어 보고 뜻 쓰기
    val items = remember { words.shuffled().map { it to (Math.random() < 0.5) } }
    var idx by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    val (w, writeEnglish) = items[idx]

    var graded by remember(idx) { mutableStateOf<HandwriteResult?>(null) }

    val promptShown = if (writeEnglish) "뜻: ${w.ko}" else "${w.emoji} ${w.en}"
    val instruction = if (writeEnglish) "영어 단어를 손으로 써봐 ✍️" else "뜻(한국어)을 손으로 써봐 ✍️"
    val expected = if (writeEnglish) w.en else w.ko
    val q = if (writeEnglish) "뜻이 '${w.ko}'인 영어 단어" else "'${w.en}'의 한국어 뜻"

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp)) {
        Text("${idx + 1} / ${items.size}   ·   점수 $score", color = Color.Gray, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(promptShown, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    if (!writeEnglish) {
                        IconButton(onClick = { speak(w.en) }) {
                            Icon(Icons.Default.VolumeUp, "발음", tint = Accent)
                        }
                    }
                    Text(instruction, fontSize = 14.sp, color = Accent)
                }
            }
            Spacer(Modifier.height(16.dp))
            if (graded == null) {
                WriteAndGradeButton(
                    label = "✍️ 손글씨로 답쓰기",
                    prompt = promptShown,
                    subtitle = instruction,
                    q = q, expected = expected, store = store,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    onResult = { graded = it; if (it.correct) score++ },
                )
            } else {
                HandwriteResultCard(graded!!, expected, Good)
            }
            Spacer(Modifier.height(8.dp))
        }
        // 하단 고정 버튼
        if (graded != null) {
            Button(
                onClick = { if (idx + 1 >= items.size) onDone(score) else idx++ },
                modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp),
            ) { Text(if (idx + 1 >= items.size) "결과 보기" else "다음 문제", fontWeight = FontWeight.Bold) }
        }
    }
}

private fun parseWords(text: String): List<Word> {
    val cleaned = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
    return try {
        val arr = JSONObject(cleaned).optJSONArray("words") ?: return emptyList()
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val en = o.optString("en", "").trim()
            val ko = o.optString("ko", "").trim()
            if (en.isBlank() || ko.isBlank()) null else Word(en, ko, "📝")
        }
    } catch (e: Exception) {
        emptyList()
    }
}
