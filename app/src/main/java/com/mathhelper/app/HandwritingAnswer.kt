package com.mathhelper.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Accent = Color(0xFF3D6EA5)
private val Bad = Color(0xFFC62828)

/**
 * 손글씨로 답을 쓰는 버튼. 누르면 '전체화면' 캔버스가 열려 크게 쓸 수 있고,
 * 채점하면 AI가 읽어 판정한 결과를 onResult로 돌려준다.
 */
@Composable
internal fun WriteAndGradeButton(
    label: String,
    prompt: String,
    q: String,
    expected: String,
    store: Store,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    onResult: (HandwriteResult) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Button(
        onClick = { open = true },
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
    ) {
        Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.Bold)
    }
    if (open) {
        WriteDialog(
            prompt = prompt, subtitle = subtitle, q = q, expected = expected, store = store,
            onClose = { open = false },
            onResult = { onResult(it); open = false },
        )
    }
}

@Composable
private fun WriteDialog(
    prompt: String,
    subtitle: String,
    q: String,
    expected: String,
    store: Store,
    onClose: () -> Unit,
    onResult: (HandwriteResult) -> Unit,
) {
    val context = LocalContext.current
    val strokes = remember { mutableStateListOf<SnapshotStateList<Offset>>() }
    var padSize by remember { mutableStateOf(IntSize.Zero) }
    var grading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var celebrating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = { if (!celebrating) onClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = Color.White) {
            if (celebrating) {
                CelebrationContent("정답이야! 잘했어, JANE! 🎉")
                return@Surface
            }
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                // 풀어야 할 문제(단어)를 위에 크게 고정
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(prompt, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                        Text(
                            if (subtitle.isNotBlank()) subtitle else "아래 칸에 답을 크게 써봐 ✍️",
                            fontSize = 14.sp, color = Accent,
                        )
                    }
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, "닫기") }
                }
                Spacer(Modifier.height(8.dp))
                // 되돌리기 (마지막 획만 지우기)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = { strokes.removeLastOrNull() },
                        enabled = strokes.isNotEmpty(),
                    ) {
                        Icon(Icons.Default.Undo, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp)); Text("되돌리기")
                    }
                }
                HandwritingPad(
                    strokes = strokes,
                    onSize = { padSize = it },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                error?.let {
                    Spacer(Modifier.height(6.dp)); Text(it, color = Bad, fontSize = 13.sp)
                }
                Spacer(Modifier.height(12.dp))
                Row {
                    OutlinedButton(
                        onClick = { strokes.clear() },
                        modifier = Modifier.weight(1f).height(56.dp),
                    ) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp)); Text("전체 지우기")
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        enabled = !grading && strokes.isNotEmpty(),
                        onClick = {
                            val provider = store.provider
                            val key = store.keyFor(provider)
                            if (key.isBlank()) { error = "⚙️ 설정에서 API 키를 넣어줘."; return@Button }
                            grading = true; error = null
                            val bmp = strokesToBitmap(strokes, padSize.width, padSize.height)
                            scope.launch {
                                val res = AiClient.generate(
                                    provider, key, StudyPrompt.gradeHandwriting(q, expected), jpegBytes(bmp)
                                )
                                grading = false
                                res.onSuccess {
                                    val r = parseHandwrite(it)
                                    if (r.correct) {
                                        celebrating = true
                                        celebrateVibrate(context)
                                        store.reward(1)
                                        scope.launch { delay(1300); onResult(r) }
                                    } else {
                                        onResult(r)
                                    }
                                }.onFailure { error = "채점 실패: ${it.message}" }
                            }
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                    ) {
                        if (grading) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(6.dp)); Text("채점하기", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/** 채점 결과를 보여주는 작은 카드 (여러 화면에서 공용) */
@Composable
internal fun HandwriteResultCard(g: HandwriteResult, expected: String, good: Color) {
    val col = if (g.correct) good else Bad
    Surface(shape = RoundedCornerShape(10.dp), color = col.copy(alpha = 0.12f)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (g.correct) "⭕" else "❌", fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    if (g.correct) "정답이야!" else "정답은  $expected",
                    fontWeight = FontWeight.Bold, color = col,
                )
                if (g.read.isNotBlank()) Text("내가 쓴 답: ${g.read}", fontSize = 13.sp)
                if (g.comment.isNotBlank()) Text(g.comment, fontSize = 13.sp)
            }
        }
    }
}
