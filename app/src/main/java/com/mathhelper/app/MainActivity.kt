package com.mathhelper.app

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BluePrimary = Color(0xFF3D6EA5)
private val GreenOk = Color(0xFF2E7D32)
private val RedNo = Color(0xFFC62828)

// 개발자 후원 링크 (비어 있으면 "마음만 받을게요" 팝업). 링크 받으면 여기에 넣으면 됨.
private const val DONATE_URL = ""

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = BluePrimary,
                    secondary = BluePrimary,
                )
            ) {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    SplashScreen(onDone = { showSplash = false })
                } else {
                    MathApp()
                }
            }
        }
    }
}

/** 앱 시작 시 잠깐 보이는 간단한 스플래시 (로고 + 이름 + 한 줄 소개) */
@Composable
private fun SplashScreen(onDone: () -> Unit) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.8f) }
    LaunchedEffect(Unit) {
        // 동시에 페이드 인 + 살짝 커지기
        kotlinx.coroutines.coroutineScope {
            launch { alpha.animateTo(1f, tween(500)) }
            launch { scale.animateTo(1f, tween(500)) }
        }
        delay(1400)
        onDone()
    }
    Box(
        Modifier.fillMaxSize().background(BluePrimary),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alpha.value).scale(scale.value),
        ) {
            // 동그란 흰 배경 위에 계산기 아이콘
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                modifier = Modifier.size(104.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Calculate, null,
                        tint = BluePrimary,
                        modifier = Modifier.size(60.dp),
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            Text("AI 공부친구 JANE", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "사진 찍으면 AI가 풀이를 알려줘요",
                color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MathApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as ComponentActivity
    val store = remember { Store(context) }

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Analysis?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showWrong by remember { mutableStateOf(false) }
    var showVocab by remember { mutableStateOf(false) }
    var showDaily by remember { mutableStateOf(false) }
    var showStickers by remember { mutableStateOf(false) }
    var showDecor by remember { mutableStateOf(false) }
    var grade by remember { mutableStateOf(store.grade) }
    var subject by remember { mutableStateOf(store.subject) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    fun loadBitmap(uri: Uri): Bitmap? = try {
        val src = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = true
        }
    } catch (e: Exception) {
        message = "사진을 불러오지 못했어요: ${e.message}"
        null
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) pendingUri?.let { bitmap = loadBitmap(it); result = null }
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { bitmap = loadBitmap(it); result = null }
    }

    fun launchCamera() {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        val file = File(dir, "shot_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingUri = uri
        takePicture.launch(uri)
    }

    fun analyze() {
        val bmp = bitmap ?: return
        val provider = store.provider
        val key = store.keyFor(provider)
        if (key.isBlank()) {
            message = "먼저 ⚙️ 설정에서 ${if (provider == "claude") "Claude" else "Gemini"} API 키를 넣어줘."
            showSettings = true
            return
        }
        loading = true
        message = null
        activity.lifecycleScope.launch {
            val res = AiClient.generate(provider, key, StudyPrompt.build(subject, grade), jpegBytes(bmp))
            loading = false
            res.onSuccess { text ->
                val a = Analysis.parse(text).copy(
                    timeMillis = System.currentTimeMillis(),
                    gradeLabel = grade.label,
                    subjectLabel = subject.label,
                )
                result = a
                store.addHistory(a)
            }.onFailure { message = "분석 실패: ${it.message}" }
        }
    }

    // 시스템 뒤로가기: 하위 화면이면 홈으로, 아니면 앱 종료(기본)
    BackHandler(enabled = showVocab || showDaily || showStickers || showDecor || result != null) {
        when {
            showVocab -> showVocab = false
            showDaily -> showDaily = false
            showStickers -> showStickers = false
            showDecor -> showDecor = false
            result != null -> { result = null; bitmap = null }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 공부친구 JANE", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BluePrimary, titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
                actions = {
                    IconButton(onClick = { showWrong = true }) {
                        Icon(Icons.Default.ErrorOutline, "오답노트")
                    }
                    IconButton(onClick = { showHistory = true }) {
                        Icon(Icons.Default.History, "기록")
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, "설정")
                    }
                }
            )
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            val r = result
            when {
                showVocab -> VocabScreen(store = store, grade = grade, onBack = { showVocab = false })
                showDaily -> DailyMissionScreen(store = store, grade = grade, onBack = { showDaily = false })
                showStickers -> StickerScreen(store = store, onBack = { showStickers = false })
                showDecor -> DecorateScreen(store = store, onBack = { showDecor = false })
                r != null -> ResultContent(r, store = store, onNew = { result = null; bitmap = null })
                else -> HomeContent(
                    bitmap = bitmap,
                    grade = grade,
                    subject = subject,
                    store = store,
                    loading = loading,
                    onPickGrade = { grade = it; store.grade = it },
                    onPickSubject = { subject = it; store.subject = it },
                    onOpenVocab = { showVocab = true },
                    onOpenDaily = { showDaily = true },
                    onOpenStickers = { showStickers = true },
                    onDecorate = { showDecor = true },
                    onCamera = { launchCamera() },
                    onGallery = { pickImage.launch("image/*") },
                    onAnalyze = { analyze() },
                    onClear = { bitmap = null },
                )
            }

            if (loading) {
                Box(
                    Modifier.fillMaxSize().background(Color(0x66000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
                        Column(
                            Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = BluePrimary)
                            Spacer(Modifier.height(14.dp))
                            Text("선생님이 문제를 보고 있어요...", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }

    message?.let { msg ->
        LaunchedEffect(msg) {}
        AlertDialog(
            onDismissRequest = { message = null },
            confirmButton = { TextButton(onClick = { message = null }) { Text("확인") } },
            text = { Text(msg) },
        )
    }

    if (showSettings) {
        SettingsDialog(store = store, onClose = { showSettings = false })
    }
    if (showHistory) {
        HistoryDialog(
            store = store,
            onOpen = { result = it; showHistory = false },
            onClose = { showHistory = false },
        )
    }
    if (showWrong) {
        WrongNoteDialog(store = store, onClose = { showWrong = false })
    }
}

@Composable
private fun HomeContent(
    bitmap: Bitmap?,
    grade: Grade,
    subject: Subject,
    store: Store,
    loading: Boolean,
    onPickGrade: (Grade) -> Unit,
    onPickSubject: (Subject) -> Unit,
    onOpenVocab: () -> Unit,
    onOpenDaily: () -> Unit,
    onOpenStickers: () -> Unit,
    onDecorate: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onAnalyze: () -> Unit,
    onClear: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // JANE 인사 + 별/스트릭
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = BluePrimary.copy(alpha = 0.08f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("👋", fontSize = 22.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    "안녕, JANE!",
                    fontSize = 15.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Text("⭐ ${store.stars}", fontWeight = FontWeight.Bold, color = BluePrimary)
                if (store.streak > 0) {
                    Spacer(Modifier.width(10.dp))
                    Text("🔥 ${store.streak}", fontWeight = FontWeight.Bold, color = Color(0xFFE8590C))
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        // 마스코트 (별 모으면 성장, 탭하면 꾸미기)
        MascotCard(store, onDecorate = onDecorate)
        Spacer(Modifier.height(10.dp))

        // 오늘의 미션
        val missionDone = store.missionDone >= MISSION_GOAL
        Surface(
            onClick = { if (!missionDone) onOpenDaily() },
            shape = RoundedCornerShape(14.dp),
            color = if (missionDone) Color(0xFFE9F6EC) else BluePrimary,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (missionDone) "✅" else "🎯", fontSize = 26.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (missionDone) "오늘의 미션 완료!" else "오늘의 미션",
                        fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        color = if (missionDone) Color(0xFF2E7D32) else Color.White,
                    )
                    Text(
                        if (missionDone) "내일 또 만나요 👋" else "$MISSION_GOAL 문제 풀고 별 모으기 ⭐",
                        fontSize = 13.sp,
                        color = if (missionDone) Color.Gray else Color.White.copy(alpha = 0.9f),
                    )
                }
                if (!missionDone) Icon(Icons.Default.ChevronRight, null, tint = Color.White)
            }
        }
        Spacer(Modifier.height(10.dp))

        // 스티커북
        OutlinedButton(
            onClick = onOpenStickers,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text("🎁 스티커북  (${stickerCollected(store.stars)}/$stickerTotal)", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))

        // 과목 선택
        SubjectSelector(subject, onPickSubject)
        Spacer(Modifier.height(10.dp))
        // 학년 선택
        GradeSelector(grade, onPickGrade)
        Spacer(Modifier.height(12.dp))

        // 영어 단어 공부 진입 (영어 과목일 때만)
        if (subject == Subject.ENGLISH) {
            Button(
                onClick = onOpenVocab,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B5EA7)),
            ) {
                Icon(Icons.Default.MenuBook, null)
                Spacer(Modifier.width(8.dp))
                Text("📚 영어 단어 공부하기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
        }

        if (bitmap == null) {
            Spacer(Modifier.height(24.dp))
            Icon(
                Icons.Default.PhotoCamera, null,
                modifier = Modifier.size(72.dp), tint = BluePrimary
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "${subject.label} 문제를 사진으로 찍어줘!",
                fontSize = 18.sp, fontWeight = FontWeight.Bold,
            )
            Text(
                "푼 답이 같이 보이면 왜 틀렸는지도 알려줄게.",
                fontSize = 14.sp, color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onCamera,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Default.PhotoCamera, null)
                Spacer(Modifier.width(8.dp))
                Text("카메라로 찍기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onGallery,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Default.Image, null)
                Spacer(Modifier.width(8.dp))
                Text("갤러리에서 고르기", fontSize = 16.sp)
            }
        } else {
            Surface(
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDDDDD)),
            ) {
                Image(
                    bitmap.asImageBitmap(), "찍은 문제",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onAnalyze,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Default.AutoAwesome, null)
                Spacer(Modifier.width(8.dp))
                Text("분석하기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Row {
                TextButton(onClick = onCamera) {
                    Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(4.dp)); Text("다시 찍기")
                }
                TextButton(onClick = onClear) {
                    Icon(Icons.Default.Close, null); Spacer(Modifier.width(4.dp)); Text("지우기")
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        var showThanks by remember { mutableStateOf(false) }
        TextButton(onClick = {
            if (DONATE_URL.isBlank()) {
                showThanks = true
            } else {
                try {
                    context.startActivity(
                        android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(DONATE_URL))
                    )
                } catch (e: Exception) { showThanks = true }
            }
        }) {
            Text("☕ 개발자에게 커피 한 잔", color = Color.Gray, fontSize = 13.sp)
        }
        if (showThanks) {
            AlertDialog(
                onDismissRequest = { showThanks = false },
                confirmButton = { TextButton(onClick = { showThanks = false }) { Text("고마워요 ㅎㅎ") } },
                text = { Text("☕ 마음만 받을게요! JANE 열심히 공부하는 게 최고의 선물이에요 😊") },
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SubjectSelector(subject: Subject, onPick: (Subject) -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFEEF3F9),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.MenuBook, null, tint = BluePrimary)
            Spacer(Modifier.width(8.dp))
            Text("과목", fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Subject.entries.forEach { s ->
                FilterChip(
                    selected = subject == s,
                    onClick = { onPick(s) },
                    label = { Text("${s.emoji} ${s.label}") },
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun GradeSelector(grade: Grade, onPick: (Grade) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFEEF3F9),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.School, null, tint = BluePrimary)
            Spacer(Modifier.width(8.dp))
            Text("몇 학년이야?", fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Box {
                AssistChip(
                    onClick = { expanded = true },
                    label = { Text(grade.label, fontWeight = FontWeight.Bold) },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    Grade.entries.forEach { g ->
                        DropdownMenuItem(
                            text = { Text(g.label) },
                            onClick = { onPick(g); expanded = false },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultContent(a: Analysis, store: Store, onNew: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        if (a.rawFallback != null) {
            // JSON 파싱 실패 → 친절한 안내 + 원문 그대로 보여주기
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF6E5))) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "결과를 예쁘게 정리하진 못했지만, 선생님 설명은 아래에 있어! 👇\n(문제가 너무 많으면 2~3개씩 나눠 찍으면 더 잘 돼요)",
                        fontSize = 13.sp, color = Color(0xFF8A6D00),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(a.rawFallback)
                }
            }
        } else {
            if (a.overall.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF3F9)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiObjects, null, tint = BluePrimary)
                        Spacer(Modifier.width(10.dp))
                        Text(a.overall, fontSize = 15.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            a.problems.forEach { p ->
                ProblemCard(p)
                Spacer(Modifier.height(12.dp))
            }
            if (a.practice.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FitnessCenter, null, tint = BluePrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("비슷한 문제로 연습해요!", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                a.practice.forEachIndexed { i, pr ->
                    PracticeCard(i + 1, pr, store)
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onNew,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(Icons.Default.PhotoCamera, null)
            Spacer(Modifier.width(8.dp))
            Text("새 문제 찍기", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ProblemCard(p: ProblemResult) {
    val correct = p.isCorrect == true
    val wrong = p.isCorrect == false
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.label, fontWeight = FontWeight.Bold, color = BluePrimary)
                Spacer(Modifier.width(8.dp))
                when (p.isCorrect) {
                    true -> StatusPill("맞았어요", GreenOk, Icons.Default.Check)
                    false -> StatusPill("틀렸어요", RedNo, Icons.Default.Close)
                    null -> {}
                }
            }
            Spacer(Modifier.height(8.dp))
            if (p.problem.isNotBlank()) {
                Text(p.problem, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
            }
            if (p.childAnswer != null) {
                Text(
                    buildString {
                        append("내가 쓴 답: ${p.childAnswer}")
                    },
                    color = if (wrong) RedNo else if (correct) GreenOk else Color.DarkGray,
                    fontSize = 14.sp,
                )
            }
            Text("정답: ${p.correct}", fontWeight = FontWeight.Bold, fontSize = 15.sp)

            if (wrong && !p.why.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFFDECEA)) {
                    Row(Modifier.padding(12.dp)) {
                        Icon(Icons.Default.Lightbulb, null, tint = RedNo)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("왜 틀렸을까?", fontWeight = FontWeight.Bold, color = RedNo, fontSize = 14.sp)
                            Text(p.why, fontSize = 14.sp)
                        }
                    }
                }
            }
            if (!p.concept.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFEAF1FB)) {
                    Row(Modifier.padding(12.dp)) {
                        Icon(Icons.Default.School, null, tint = BluePrimary)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("📌 알아두기", fontWeight = FontWeight.Bold, color = BluePrimary, fontSize = 13.sp)
                            Text(p.concept, fontSize = 14.sp)
                        }
                    }
                }
            }
            if (p.steps.isNotEmpty()) {
                var revealed by remember(p) { mutableStateOf(0) }
                Spacer(Modifier.height(10.dp))
                Text("이렇게 풀어요 (한 단계씩 힌트)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BluePrimary)
                Spacer(Modifier.height(4.dp))
                for (i in 0 until revealed) {
                    Row(Modifier.padding(vertical = 2.dp)) {
                        Text("${i + 1}. ", fontWeight = FontWeight.Bold)
                        Text(p.steps[i], fontSize = 14.sp)
                    }
                }
                if (revealed < p.steps.size) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { revealed++ }) {
                            Icon(Icons.Default.Lightbulb, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (revealed == 0) "💡 힌트 보기" else "다음 힌트 (${revealed}/${p.steps.size})")
                        }
                        if (p.steps.size > 1) {
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { revealed = p.steps.size }) { Text("다 보기") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(shape = RoundedCornerShape(20.dp), color = color.copy(alpha = 0.12f)) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PracticeCard(index: Int, pr: PracticeProblem, store: Store) {
    var revealed by remember { mutableStateOf(false) }
    var graded by remember { mutableStateOf<HandwriteResult?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F8FB)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("연습 $index", fontWeight = FontWeight.Bold, color = BluePrimary, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(pr.q, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            if (!pr.hint.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("힌트: ${pr.hint}", fontSize = 13.sp, color = Color.Gray)
            }

            graded?.let { g ->
                Spacer(Modifier.height(10.dp))
                HandwriteResultCard(g, pr.a, GreenOk)
            }

            Spacer(Modifier.height(10.dp))
            // 버튼들
            Row(verticalAlignment = Alignment.CenterVertically) {
                WriteAndGradeButton(
                    label = if (graded == null) "직접 풀기" else "다시 풀기",
                    prompt = pr.q,
                    q = pr.q, expected = pr.a, store = store,
                    onResult = { graded = it },
                )
                Spacer(Modifier.width(8.dp))
                if (revealed) {
                    Surface(shape = RoundedCornerShape(8.dp), color = GreenOk.copy(alpha = 0.12f)) {
                        Text(
                            "정답: ${pr.a}",
                            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            color = GreenOk, fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    OutlinedButton(onClick = { revealed = true }) {
                        Icon(Icons.Default.Visibility, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp)); Text("정답 보기")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDialog(store: Store, onClose: () -> Unit) {
    var provider by remember { mutableStateOf(store.provider) }
    var geminiKey by remember { mutableStateOf(store.geminiKey) }
    var claudeKey by remember { mutableStateOf(store.claudeKey) }

    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            TextButton(onClick = {
                store.provider = provider
                store.geminiKey = geminiKey.trim()
                store.claudeKey = claudeKey.trim()
                onClose()
            }) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("취소") } },
        title = { Text("AI 설정") },
        text = {
            Column {
                Text("AI 엔진", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                Row {
                    FilterChip(
                        selected = provider == "gemini",
                        onClick = { provider = "gemini" },
                        label = { Text("Gemini (무료)") },
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = provider == "claude",
                        onClick = { provider = "claude" },
                        label = { Text("Claude (유료)") },
                    )
                }
                Spacer(Modifier.height(14.dp))
                if (provider == "gemini") {
                    OutlinedTextField(
                        value = geminiKey, onValueChange = { geminiKey = it },
                        label = { Text("Gemini API 키") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "aistudio.google.com 에서 무료로 발급받아요.",
                        fontSize = 12.sp, color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                } else {
                    OutlinedTextField(
                        value = claudeKey, onValueChange = { claudeKey = it },
                        label = { Text("Claude API 키") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    )
}

@Composable
private fun HistoryDialog(store: Store, onOpen: (Analysis) -> Unit, onClose: () -> Unit) {
    val items = remember { store.history() }
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { TextButton(onClick = onClose) { Text("닫기") } },
        title = { Text("최근 기록") },
        text = {
            if (items.isEmpty()) {
                Text("아직 분석한 문제가 없어요.")
            } else {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    items.forEach { a ->
                        Surface(
                            onClick = { onOpen(a) },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF2F5F9),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                val emoji = when (a.subjectLabel) {
                                    "영어" -> "🔤"; "수학" -> "🔢"; else -> "📝"
                                }
                                Text(
                                    "$emoji  " + fmtTime(a.timeMillis) + "  ·  " +
                                        listOf(a.subjectLabel, a.gradeLabel).filter { it.isNotBlank() }.joinToString(" "),
                                    fontSize = 12.sp, color = Color.Gray,
                                )
                                val title = a.problems.firstOrNull()?.problem?.ifBlank { null }
                                    ?: a.overall.ifBlank { null }
                                    ?: a.rawFallback?.lineSequence()?.firstOrNull { it.isNotBlank() }?.take(24)
                                    ?: "분석 결과"
                                Text(title, fontWeight = FontWeight.Medium, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun WrongNoteDialog(store: Store, onClose: () -> Unit) {
    // 기록 전체에서 '틀린 문제'만 모으기 (최신순)
    val items = remember {
        store.history().flatMap { a ->
            a.problems.filter { it.isCorrect == false }
                .map { Triple(a.subjectLabel, a.timeMillis, it) }
        }
    }
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { TextButton(onClick = onClose) { Text("닫기") } },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ErrorOutline, null, tint = RedNo)
                Spacer(Modifier.width(8.dp)); Text("오답노트")
            }
        },
        text = {
            if (items.isEmpty()) {
                Text("아직 틀린 문제가 없어요. 잘하고 있어, JANE! 👏")
            } else {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    items.forEach { (subj, time, p) ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFDECEA),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    listOf(subj, fmtTime(time)).filter { it.isNotBlank() }.joinToString("  ·  "),
                                    fontSize = 12.sp, color = Color.Gray,
                                )
                                if (p.problem.isNotBlank()) {
                                    Text(p.problem, fontWeight = FontWeight.Medium)
                                }
                                Row(Modifier.padding(top = 2.dp)) {
                                    if (p.childAnswer != null) {
                                        Text("내 답: ${p.childAnswer}", color = RedNo, fontSize = 13.sp)
                                        Spacer(Modifier.width(12.dp))
                                    }
                                    Text("정답: ${p.correct}", color = GreenOk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                if (!p.why.isNullOrBlank()) {
                                    Text("💡 ${p.why}", fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

// ---------- 유틸 ----------

private val timeFmt = SimpleDateFormat("M월 d일 a h:mm", Locale.KOREAN)
private fun fmtTime(ms: Long): String = if (ms <= 0) "" else timeFmt.format(Date(ms))
