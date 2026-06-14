package com.mathhelper.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Accent = Color(0xFF3D6EA5)

// ===== 마스코트 (별 모으면 성장) =====

data class MascotStage(val emoji: String, val name: String, val msg: String, val min: Int)

private val MASCOT = listOf(
    MascotStage("🥚", "공부알", "조금만 더 별을 모아봐!", 0),
    MascotStage("🐣", "삐약이", "안녕 JANE! 같이 공부하자!", 10),
    MascotStage("🐥", "병아리", "우와, 잘하고 있어!", 30),
    MascotStage("🐔", "꼬꼬", "대단해 JANE! 멋져!", 60),
    MascotStage("🦄", "유니콘", "JANE은 공부 천재야!", 100),
)

internal fun mascotFor(stars: Int): MascotStage = MASCOT.last { stars >= it.min }
internal fun nextMascot(stars: Int): MascotStage? = MASCOT.firstOrNull { it.min > stars }

@Composable
internal fun MascotCard(store: Store) {
    val stars = store.stars
    val m = mascotFor(stars)
    val next = nextMascot(stars)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFFF7E8),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(m.emoji, fontSize = 46.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(m.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(m.msg, fontSize = 13.sp, color = Color.DarkGray)
                Spacer(Modifier.height(4.dp))
                if (next != null) {
                    Text("다음 친구 ${next.emoji} 까지  ⭐${next.min - stars}", fontSize = 12.sp, color = Accent)
                } else {
                    Text("최고 단계 달성! 🎉", fontSize = 12.sp, color = Accent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ===== 스티커북 (별 모으면 열림) =====

data class Sticker(val emoji: String, val name: String, val need: Int)

private val STICKERS = listOf(
    Sticker("🌟", "반짝별", 3), Sticker("🍭", "막대사탕", 6), Sticker("🎈", "풍선", 10),
    Sticker("🐶", "강아지", 15), Sticker("🦄", "유니콘", 20), Sticker("🚀", "로켓", 28),
    Sticker("🌈", "무지개", 36), Sticker("🍰", "케이크", 45), Sticker("👑", "왕관", 55),
    Sticker("🐉", "용", 70), Sticker("🏆", "트로피", 90), Sticker("💎", "다이아", 120),
)

internal val stickerTotal = STICKERS.size
internal fun stickerCollected(stars: Int): Int = STICKERS.count { stars >= it.need }

@Composable
fun StickerScreen(store: Store, onBack: () -> Unit) {
    val stars = store.stars
    val got = stickerCollected(stars)
    Column(Modifier.fillMaxSize().background(Color(0xFFFAFBFD))) {
        Surface(color = Accent) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "뒤로", tint = Color.White) }
                Text("스티커북", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("⭐ $stars", color = Color.White, fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("별을 모으면 새 스티커가 열려요! ⭐", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("모은 스티커  $got / $stickerTotal", color = Color.Gray, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            STICKERS.chunked(3).forEach { rowItems ->
                Row(Modifier.fillMaxWidth()) {
                    rowItems.forEach { s ->
                        val unlocked = stars >= s.need
                        Card(
                            modifier = Modifier.weight(1f).padding(6.dp).aspectRatio(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = if (unlocked) Color.White else Color(0xFFEFEFEF)
                            ),
                        ) {
                            Column(
                                Modifier.fillMaxSize().padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                if (unlocked) {
                                    Text(s.emoji, fontSize = 40.sp)
                                    Text(s.name, fontSize = 12.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                                } else {
                                    Text("🔒", fontSize = 30.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text("⭐${s.need}", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
