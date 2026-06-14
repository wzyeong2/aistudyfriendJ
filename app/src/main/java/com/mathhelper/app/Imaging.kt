package com.mathhelper.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File

private val PadAccent = Color(0xFF3D6EA5)

/** 비트맵을 적당한 크기로 줄여 JPEG 바이트로 (AI 전송용) */
internal fun jpegBytes(bmp: Bitmap): ByteArray {
    val max = 1280
    val scale = minOf(1f, max.toFloat() / maxOf(bmp.width, bmp.height))
    val b = if (scale < 1f)
        Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true)
    else bmp
    val out = ByteArrayOutputStream()
    b.compress(Bitmap.CompressFormat.JPEG, 88, out)
    return out.toByteArray()
}

/** Uri → Bitmap (소프트웨어 비트맵, 실패 시 null) */
internal fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? = try {
    val src = ImageDecoder.createSource(context.contentResolver, uri)
    ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        decoder.isMutableRequired = true
    }
} catch (e: Exception) {
    null
}

/** 카메라 촬영 결과를 받을 임시 파일 Uri */
internal fun newCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "captures").apply { mkdirs() }
    val file = File(dir, "shot_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/** 손글씨 채점 결과 */
internal data class HandwriteResult(val correct: Boolean, val read: String, val comment: String)

internal fun parseHandwrite(text: String): HandwriteResult {
    val t = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
    val s = t.indexOf('{'); val e = t.lastIndexOf('}')
    val cleaned = if (s in 0 until e) t.substring(s, e + 1) else t
    return try {
        val o = JSONObject(cleaned)
        HandwriteResult(
            correct = o.optBoolean("correct", false),
            read = o.optString("read", ""),
            comment = o.optString("comment", ""),
        )
    } catch (e: Exception) {
        HandwriteResult(false, "", text.trim())
    }
}

/** 손가락으로 그린 획들을 흰 배경 비트맵으로 래스터화 */
internal fun strokesToBitmap(strokes: List<List<Offset>>, w: Int, h: Int): Bitmap {
    val bmp = Bitmap.createBitmap(maxOf(w, 1), maxOf(h, 1), Bitmap.Config.ARGB_8888)
    val c = android.graphics.Canvas(bmp)
    c.drawColor(android.graphics.Color.WHITE)
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 9f
        style = android.graphics.Paint.Style.STROKE
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
        isAntiAlias = true
    }
    strokes.forEach { stroke ->
        if (stroke.isEmpty()) return@forEach
        val path = android.graphics.Path()
        path.moveTo(stroke[0].x, stroke[0].y)
        for (i in 1 until stroke.size) path.lineTo(stroke[i].x, stroke[i].y)
        c.drawPath(path, paint)
    }
    return bmp
}

/** 손가락으로 그리는 캔버스 */
@Composable
internal fun HandwritingPad(
    strokes: SnapshotStateList<SnapshotStateList<Offset>>,
    onSize: (IntSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.5.dp, PadAccent.copy(alpha = 0.4f)),
        modifier = modifier,
    ) {
        Canvas(
            Modifier.fillMaxSize()
                .onSizeChanged(onSize)
                .pointerInput(strokes) {
                    detectDragGestures(
                        onDragStart = { ofs -> strokes.add(mutableStateListOf(ofs)) },
                        onDrag = { change, _ ->
                            strokes.lastOrNull()?.add(change.position)
                            change.consume()
                        },
                    )
                }
        ) {
            strokes.forEach { stroke ->
                for (i in 1 until stroke.size) {
                    drawLine(
                        color = Color(0xFF222222),
                        start = stroke[i - 1], end = stroke[i],
                        strokeWidth = 9f, cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}
