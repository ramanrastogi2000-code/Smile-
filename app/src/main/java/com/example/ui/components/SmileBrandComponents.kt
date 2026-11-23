package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeliveryStatus
import com.example.ui.theme.SmileDeliveredGreen
import com.example.ui.theme.SmileInk
import com.example.ui.theme.SmileOnlineGreen
import com.example.ui.theme.SmileWhite
import com.example.ui.theme.SmileYellow
import java.security.MessageDigest

/**
 * Exact brand mark for SMILE:
 * Yellow (#FFD600) rounded square, crisp white (#FFFFFF) chat bubble outline,
 * and cheerful black (#111111) smiling face.
 */
@Composable
fun SmileLogo(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    showRoundedSquareBg: Boolean = true
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        if (showRoundedSquareBg) {
            drawRoundRect(
                color = SmileYellow,
                topLeft = Offset.Zero,
                size = Size(w, h),
                cornerRadius = CornerRadius(w * 0.24f, h * 0.24f)
            )
        }

        // White chat bubble outline with bottom-left tail
        val cx = w * 0.5f
        val cy = h * 0.47f
        val rx = w * 0.32f
        val ry = h * 0.27f

        val bubblePath = Path().apply {
            moveTo(cx, cy - ry)
            cubicTo(cx + rx * 0.72f, cy - ry, cx + rx, cy - ry * 0.52f, cx + rx, cy)
            cubicTo(cx + rx, cy + ry * 0.56f, cx + rx * 0.65f, cy + ry, cx, cy + ry)
            cubicTo(cx - rx * 0.25f, cy + ry, cx - rx * 0.45f, cy + ry * 0.92f, cx - rx * 0.56f, cy + ry * 0.86f)
            // Tail
            lineTo(cx - rx * 0.92f, cy + ry * 1.16f)
            lineTo(cx - rx * 0.80f, cy + ry * 0.62f)
            cubicTo(cx - rx * 0.96f, cy + ry * 0.36f, cx - rx, cy + ry * 0.16f, cx - rx, cy)
            cubicTo(cx - rx, cy - ry * 0.55f, cx - rx * 0.72f, cy - ry, cx, cy - ry)
            close()
        }

        drawPath(
            path = bubblePath,
            color = SmileWhite.copy(alpha = 0.22f)
        )
        drawPath(
            path = bubblePath,
            color = SmileWhite,
            style = Stroke(width = w * 0.068f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Black smiling face inside the chat bubble
        val eyeRadius = w * 0.042f
        val eyeY = cy - ry * 0.18f
        drawCircle(
            color = SmileInk,
            radius = eyeRadius,
            center = Offset(cx - rx * 0.32f, eyeY)
        )
        drawCircle(
            color = SmileInk,
            radius = eyeRadius,
            center = Offset(cx + rx * 0.32f, eyeY)
        )

        val smilePath = Path().apply {
            moveTo(cx - rx * 0.40f, cy + ry * 0.15f)
            quadraticTo(cx, cy + ry * 0.58f, cx + rx * 0.40f, cy + ry * 0.15f)
        }
        drawPath(
            path = smilePath,
            color = SmileInk,
            style = Stroke(width = w * 0.062f, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun SmileAvatar(
    name: String,
    colorHex: String = "#FFD600",
    avatarDataBase64: String? = null,
    size: Dp = 48.dp,
    showOnlineDot: Boolean = false,
    online: Boolean = false
) {
    val bgColor = remember(colorHex) {
        try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (_: Exception) {
            SmileYellow
        }
    }
    val bitmap: Bitmap? = remember(avatarDataBase64) {
        if (avatarDataBase64.isNullOrBlank()) null
        else try {
            val bytes = Base64.decode(avatarDataBase64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }
    val initials = remember(name) {
        name.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { "S" }
    }

    Box(modifier = Modifier.size(size)) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "$name avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(1.5.dp, SmileYellow, CircleShape)
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(bgColor)
                    .border(1.dp, SmileInk.copy(alpha = 0.08f), CircleShape)
            ) {
                Text(
                    text = initials,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.36f).sp,
                    color = SmileInk
                )
            }
        }
        if (showOnlineDot && online) {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(SmileOnlineGreen)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

@Composable
fun DeliveryStatusTicks(
    status: DeliveryStatus,
    modifier: Modifier = Modifier
) {
    when (status) {
        DeliveryStatus.SENT -> Icon(
            imageVector = Icons.Default.Done,
            contentDescription = "Sent",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.size(15.dp)
        )
        DeliveryStatus.DELIVERED -> Icon(
            imageVector = Icons.Default.DoneAll,
            contentDescription = "Delivered",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.size(15.dp)
        )
        DeliveryStatus.READ -> Icon(
            imageVector = Icons.Default.DoneAll,
            contentDescription = "Read",
            tint = SmileDeliveredGreen,
            modifier = modifier.size(15.dp)
        )
    }
}

@Composable
fun E2eePill(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(50),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Deterministic Identity QR Badge encoding smile://u/{smileId} with 21x21 finder patterns
 * and SHA-256 data payload matrix.
 */
@Composable
fun QrCodeCard(
    displayName: String,
    smileId: String,
    modifier: Modifier = Modifier
) {
    val matrix = remember(smileId) { buildIdentityMatrix("smile://u/$smileId") }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SmileWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SmileLogo(size = 32.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "SMILE ID",
                    style = MaterialTheme.typography.titleMedium,
                    color = SmileInk,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(188.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SmileWhite)
                    .border(3.dp, SmileYellow, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val n = matrix.size
                    val cell = size.width / n
                    for (r in 0 until n) {
                        for (c in 0 until n) {
                            if (matrix[r][c]) {
                                drawRect(
                                    color = SmileInk,
                                    topLeft = Offset(c * cell, r * cell),
                                    size = Size(cell * 0.96f, cell * 0.96f)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleMedium,
                color = SmileInk,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "@$smileId",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF555555)
            )
        }
    }
}

private fun buildIdentityMatrix(payload: String): Array<BooleanArray> {
    val n = 21
    val grid = Array(n) { BooleanArray(n) }
    fun drawFinder(top: Int, left: Int) {
        for (r in 0..6) {
            for (c in 0..6) {
                val border = r == 0 || r == 6 || c == 0 || c == 6
                val inner = r in 2..4 && c in 2..4
                grid[top + r][left + c] = border || inner
            }
        }
    }
    drawFinder(0, 0)
    drawFinder(0, n - 7)
    drawFinder(n - 7, 0)

    val hash = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray())
    val hash2 = MessageDigest.getInstance("SHA-256").digest(hash)
    val bytes = hash + hash2
    var bitIdx = 0
    for (r in 0 until n) {
        for (c in 0 until n) {
            val inFinder = (r < 8 && c < 8) || (r < 8 && c >= n - 8) || (r >= n - 8 && c < 8)
            if (!inFinder) {
                val b = bytes[(bitIdx / 8) % bytes.size].toInt()
                grid[r][c] = ((b shr (bitIdx % 8)) and 1) == 1
                bitIdx++
            }
        }
    }
    return grid
}
