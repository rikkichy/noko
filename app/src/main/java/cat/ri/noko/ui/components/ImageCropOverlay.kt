package cat.ri.noko.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ImageCropOverlay(
    imageUri: Uri,
    onCrop: (Bitmap) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val sourceBitmap = remember(imageUri) {
        context.contentResolver.openInputStream(imageUri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    }

    if (sourceBitmap == null) {
        onCancel()
        return
    }

    val imageBitmap: ImageBitmap = remember(sourceBitmap) { sourceBitmap.asImageBitmap() }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    Scaffold(
        containerColor = Color.Black,
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = Color.White)
                }
                Button(onClick = {
                    val cropped = cropCircularBitmap(
                        source = sourceBitmap,
                        canvasWidth = canvasSize.width,
                        canvasHeight = canvasSize.height,
                        scale = scale,
                        offsetX = offsetX,
                        offsetY = offsetY,
                        outputSize = 512,
                    )
                    onCrop(cropped)
                }) {
                    Text("Crop")
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                },
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                canvasSize = size
                val cw = size.width
                val ch = size.height

                val imgW = imageBitmap.width.toFloat()
                val imgH = imageBitmap.height.toFloat()
                val fitScale = min(cw / imgW, ch / imgH)

                val drawW = imgW * fitScale * scale
                val drawH = imgH * fitScale * scale
                val drawX = (cw - drawW) / 2f + offsetX
                val drawY = (ch - drawH) / 2f + offsetY

                drawImage(
                    image = imageBitmap,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(imageBitmap.width, imageBitmap.height),
                    dstOffset = IntOffset(drawX.roundToInt(), drawY.roundToInt()),
                    dstSize = IntSize(drawW.roundToInt(), drawH.roundToInt()),
                )

                val circleRadius = min(cw, ch) * 0.4f
                val center = Offset(cw / 2f, ch / 2f)

                val circlePath = Path().apply {
                    addOval(
                        Rect(
                            center.x - circleRadius,
                            center.y - circleRadius,
                            center.x + circleRadius,
                            center.y + circleRadius,
                        ),
                    )
                }

                clipPath(circlePath, clipOp = ClipOp.Difference) {
                    drawRect(Color.Black.copy(alpha = 0.6f))
                }

                drawCircle(
                    color = Color.White.copy(alpha = 0.5f),
                    radius = circleRadius,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
                )
            }
        }
    }
}

private fun cropCircularBitmap(
    source: Bitmap,
    canvasWidth: Float,
    canvasHeight: Float,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    outputSize: Int,
): Bitmap {
    val imgW = source.width.toFloat()
    val imgH = source.height.toFloat()
    val fitScale = min(canvasWidth / imgW, canvasHeight / imgH)

    val drawW = imgW * fitScale * scale
    val drawH = imgH * fitScale * scale
    val drawX = (canvasWidth - drawW) / 2f + offsetX
    val drawY = (canvasHeight - drawH) / 2f + offsetY

    val circleRadius = min(canvasWidth, canvasHeight) * 0.4f
    val centerX = canvasWidth / 2f
    val centerY = canvasHeight / 2f

    val srcLeft = ((centerX - circleRadius - drawX) / (drawW / imgW)).coerceIn(0f, imgW)
    val srcTop = ((centerY - circleRadius - drawY) / (drawH / imgH)).coerceIn(0f, imgH)
    val srcRight = ((centerX + circleRadius - drawX) / (drawW / imgW)).coerceIn(0f, imgW)
    val srcBottom = ((centerY + circleRadius - drawY) / (drawH / imgH)).coerceIn(0f, imgH)

    val srcWidth = (srcRight - srcLeft).roundToInt().coerceAtLeast(1)
    val srcHeight = (srcBottom - srcTop).roundToInt().coerceAtLeast(1)

    val cropped = Bitmap.createBitmap(
        source,
        srcLeft.roundToInt().coerceIn(0, source.width - 1),
        srcTop.roundToInt().coerceIn(0, source.height - 1),
        srcWidth.coerceAtMost(source.width - srcLeft.roundToInt().coerceAtLeast(0)),
        srcHeight.coerceAtMost(source.height - srcTop.roundToInt().coerceAtLeast(0)),
    )

    return Bitmap.createScaledBitmap(cropped, outputSize, outputSize, true)
}
