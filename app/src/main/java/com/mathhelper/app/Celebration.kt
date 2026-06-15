package com.mathhelper.app

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Accent = Color(0xFF3D6EA5)

/** 정답·미션 클리어 시 보여주는 축하 화면 (통통 튀는 애니메이션) */
@Composable
internal fun CelebrationContent(message: String) {
    val scale = remember { Animatable(0.3f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        )
    }
    Box(
        Modifier.fillMaxSize().background(Color(0xFFFFFDF5)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale.value),
        ) {
            Text("🎉", fontSize = 92.sp)
            Spacer(Modifier.height(14.dp))
            Text(
                message,
                fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Accent,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text("⭐   ⭐   ⭐", fontSize = 28.sp)
        }
    }
}

/** 정답 피드백: 진동 + 효과음 */
@Suppress("DEPRECATION")
internal fun celebrateVibrate(context: Context) {
    try {
        val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vib?.hasVibrator() == true) {
            vib.vibrate(VibrationEffect.createOneShot(140, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    } catch (e: Exception) {
        // 진동 실패는 무시
    }
    playSuccessSound()
}

/** 정답 효과음 (오디오 파일 없이 톤 생성) */
internal fun playSuccessSound() {
    try {
        val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 90)
        tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 250) // 띠링~ 하는 확인음
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try { tg.release() } catch (e: Exception) {}
        }, 350)
    } catch (e: Exception) {
        // 사운드 실패는 무시
    }
}
