package com.mathhelper.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Accent = Color(0xFF3D6EA5)

// ===== 꾸미기 아이템 =====
data class Hat(val emoji: String, val name: String, val need: Int)
data class MascotBg(val color: Long, val name: String, val need: Int)

internal val HATS = listOf(
    Hat("", "없음", 0),
    Hat("🎀", "리본", 5),
    Hat("🧢", "야구모자", 10),
    Hat("🎩", "마술모자", 20),
    Hat("🎓", "학사모", 35),
    Hat("👑", "왕관", 55),
    Hat("🦄", "유니콘뿔", 80),
)
internal val BGS = listOf(
    MascotBg(0xFFFFF7E8, "기본", 0),
    MascotBg(0xFFDCEBFF, "하늘", 6),
    MascotBg(0xFFFFE3C7, "노을", 18),
    MascotBg(0xFFDDF3DE, "숲", 30),
    MascotBg(0xFFE9E0FF, "우주", 50),
    MascotBg(0xFFFFE0EC, "솜사탕", 70),
)

/** 마스코트 + 모자(머리 위) 표시 */
@Composable
internal fun MascotView(emoji: String, hat: String, size: Int) {
    Box(contentAlignment = Alignment.Center) {
        Text(emoji, fontSize = size.sp)
        if (hat.isNotBlank()) {
            Text(
                hat, fontSize = (size * 0.5).sp,
                modifier = Modifier.align(Alignment.TopCenter).offset(y = (-size * 0.32).dp),
            )
        }
    }
}

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
internal fun MascotCard(store: Store, onDecorate: () -> Unit) {
    val stars = store.stars
    val m = mascotFor(stars)
    val next = nextMascot(stars)
    val bg = BGS.getOrElse(store.mascotBg) { BGS[0] }
    Surface(
        onClick = onDecorate,
        shape = RoundedCornerShape(14.dp),
        color = Color(bg.color),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            MascotView(m.emoji, store.mascotHat, size = 44)
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Brush, "꾸미기", tint = Accent)
                Text("꾸미기", fontSize = 11.sp, color = Accent)
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

// ===== 마스코트 꾸미기 =====

@Composable
fun DecorateScreen(store: Store, onBack: () -> Unit) {
    val stars = store.stars
    val mascot = mascotFor(stars).emoji
    var hat by remember { mutableStateOf(store.mascotHat) }
    var bgIdx by remember { mutableStateOf(store.mascotBg) }
    val bg = BGS.getOrElse(bgIdx) { BGS[0] }

    Column(Modifier.fillMaxSize().background(Color(0xFFFAFBFD))) {
        Surface(color = Accent) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "뒤로", tint = Color.White) }
                Text("마스코트 꾸미기", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("⭐ $stars", color = Color.White, fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            // 미리보기
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(bg.color),
                modifier = Modifier.fillMaxWidth().height(150.dp),
            ) {
                Box(contentAlignment = Alignment.Center) { MascotView(mascot, hat, size = 72) }
            }
            Spacer(Modifier.height(18.dp))

            Text("🎩 모자", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            HATS.chunked(4).forEach { rowItems ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    rowItems.forEach { h ->
                        val unlocked = stars >= h.need
                        val selected = hat == h.emoji
                        DecorCell(
                            label = if (h.emoji.isBlank()) "❌" else h.emoji,
                            name = h.name, unlocked = unlocked, selected = selected, need = h.need,
                            modifier = Modifier.weight(1f),
                        ) { if (unlocked) { hat = h.emoji; store.mascotHat = h.emoji } }
                    }
                    repeat(4 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("🎨 배경", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            BGS.chunked(4).forEachIndexed { rowI, rowItems ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    rowItems.forEachIndexed { colI, b ->
                        val i = rowI * 4 + colI
                        val unlocked = stars >= b.need
                        val selected = bgIdx == i
                        DecorCell(
                            label = "🎨", name = b.name, unlocked = unlocked, selected = selected,
                            need = b.need, swatch = Color(b.color), modifier = Modifier.weight(1f),
                        ) { if (unlocked) { bgIdx = i; store.mascotBg = i } }
                    }
                    repeat(4 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("별을 모으면 더 많은 꾸미기가 열려요! ⭐", color = Color.Gray, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DecorCell(
    label: String,
    name: String,
    unlocked: Boolean,
    selected: Boolean,
    need: Int,
    modifier: Modifier = Modifier,
    swatch: Color? = null,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            color = if (swatch != null && unlocked) swatch else if (unlocked) Color.White else Color(0xFFEFEFEF),
            border = if (selected) androidx.compose.foundation.BorderStroke(2.5.dp, Accent) else null,
            modifier = Modifier.size(56.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (unlocked) { if (swatch == null) Text(label, fontSize = 26.sp) }
                else Text("🔒", fontSize = 18.sp)
            }
        }
        Text(
            if (unlocked) name else "⭐$need",
            fontSize = 11.sp,
            color = if (unlocked) Color.DarkGray else Color.Gray,
            maxLines = 1,
        )
    }
}
