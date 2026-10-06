// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.editor.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.ukpratik.folio.core.model.Quad
import io.github.ukpratik.folio.feature.editor.R
import io.github.ukpratik.folio.feature.editor.detail.CropMath.point
import io.github.ukpratik.folio.feature.editor.detail.CropMath.with
import kotlin.math.roundToInt

private val HandleTouch = 48.dp
private val HandleRadius = 12.dp
private val LoupeRadius = 52.dp
private const val LOUPE_ZOOM = 2f
private val Accent = Color(0xFF3FC7B5)

/**
 * S4a crop: the unedited source with 4 draggable corners (48 dp touch), dimmed outside the crop, and a 2×
 * loupe while dragging. Corners commit once on finger-up (D-29). Each corner is also a focusable TalkBack node
 * with 1 % nudge actions (UX §7).
 */
@Composable
internal fun CropEditor(
    image: ImageBitmap?,
    corners: Quad?,
    onCommit: (Quad) -> Unit,
    modifier: Modifier = Modifier,
) {
    var working by remember(corners) { mutableStateOf(corners ?: CropMath.FULL_IMAGE) }
    var active by remember { mutableStateOf<Corner?>(null) }
    val density = LocalDensity.current
    val touchPx = with(density) { HandleTouch.toPx() } * 1.5f

    BoxWithConstraints(modifier.fillMaxSize()) {
        val canvasW = with(density) { maxWidth.toPx() }
        val canvasH = with(density) { maxHeight.toPx() }
        val rect = image?.let { FitRect.fit(it.width, it.height, canvasW, canvasH) } ?: FitRect(0f, 0f, canvasW, canvasH)

        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(rect) {
                    detectDragGestures(
                        onDragStart = { start -> active = CropMath.nearestCorner(working, start.x, start.y, rect, touchPx) },
                        onDrag = { change, _ ->
                            active?.let { corner ->
                                change.consume()
                                working = working.with(corner, CropMath.toImage(change.position.x, change.position.y, rect))
                            }
                        },
                        onDragEnd = {
                            if (active != null) onCommit(working)
                            active = null
                        },
                        onDragCancel = { active = null },
                    )
                },
        ) {
            image?.let { drawFitted(it, rect) }
            drawCrop(working, rect)
            active?.let { corner -> image?.let { drawLoupe(it, rect, working.point(corner), LoupeRadius.toPx()) } }
        }

        // Invisible, focusable handles for TalkBack and keyboard users.
        Corner.entries.forEach { corner ->
            HandleNode(corner, working, rect) { moved ->
                working = moved
                onCommit(moved)
            }
        }
    }
}

@Composable
private fun HandleNode(corner: Corner, quad: Quad, rect: FitRect, onMove: (Quad) -> Unit) {
    val density = LocalDensity.current
    val (x, y) = CropMath.toScreen(quad.point(corner), rect)
    val half = with(density) { HandleTouch.toPx() } / 2
    val label = stringResource(
        when (corner) {
            Corner.TL -> R.string.detail_corner_tl
            Corner.TR -> R.string.detail_corner_tr
            Corner.BR -> R.string.detail_corner_br
            Corner.BL -> R.string.detail_corner_bl
        },
    )
    val up = stringResource(R.string.detail_nudge_up)
    val down = stringResource(R.string.detail_nudge_down)
    val left = stringResource(R.string.detail_nudge_left)
    val right = stringResource(R.string.detail_nudge_right)
    Box(
        Modifier
            .offset { IntOffset((x - half).roundToInt(), (y - half).roundToInt()) }
            .size(HandleTouch)
            .semantics {
                contentDescription = label
                customActions = listOf(
                    CustomAccessibilityAction(up) { onMove(CropMath.nudge(quad, corner, 0f, -CropMath.NUDGE)); true },
                    CustomAccessibilityAction(down) { onMove(CropMath.nudge(quad, corner, 0f, CropMath.NUDGE)); true },
                    CustomAccessibilityAction(left) { onMove(CropMath.nudge(quad, corner, -CropMath.NUDGE, 0f)); true },
                    CustomAccessibilityAction(right) { onMove(CropMath.nudge(quad, corner, CropMath.NUDGE, 0f)); true },
                )
            },
    )
}

private fun DrawScope.drawFitted(image: ImageBitmap, rect: FitRect) {
    drawImage(
        image,
        dstOffset = IntOffset(rect.left.roundToInt(), rect.top.roundToInt()),
        dstSize = IntSize(rect.width.roundToInt(), rect.height.roundToInt()),
    )
}

private fun quadPath(quad: Quad, rect: FitRect) = Path().apply {
    listOf(quad.tl, quad.tr, quad.br, quad.bl).forEachIndexed { i, p ->
        val (x, y) = CropMath.toScreen(p, rect)
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private fun DrawScope.drawCrop(quad: Quad, rect: FitRect) {
    val inner = quadPath(quad, rect)
    val outside = Path().apply {
        fillType = PathFillType.EvenOdd
        addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
        addPath(inner)
    }
    drawPath(outside, Color.Black.copy(alpha = 0.5f))
    drawPath(inner, Accent, style = Stroke(width = 2.5.dp.toPx()))
    listOf(quad.tl, quad.tr, quad.br, quad.bl).forEach { p ->
        val (x, y) = CropMath.toScreen(p, rect)
        drawCircle(Color.White, radius = HandleRadius.toPx(), center = Offset(x, y))
        drawCircle(Accent, radius = HandleRadius.toPx(), center = Offset(x, y), style = Stroke(width = 3.dp.toPx()))
    }
}

/** A 2× magnified view of the image around the dragged corner, placed in the opposite top corner. */
private fun DrawScope.drawLoupe(image: ImageBitmap, rect: FitRect, corner: io.github.ukpratik.folio.core.model.PointF01, radius: Float) {
    val margin = 16.dp.toPx()
    val center = if (corner.x < 0.5f) Offset(size.width - radius - margin, radius + margin) else Offset(radius + margin, radius + margin)
    val (cx, cy) = CropMath.toScreen(corner, rect)
    val circle = Path().apply { addOval(androidx.compose.ui.geometry.Rect(center, radius)) }
    clipPath(circle, ClipOp.Intersect) {
        drawRect(Color.Black, topLeft = center - Offset(radius, radius), size = Size(radius * 2, radius * 2))
        val scaleToImage = image.width / rect.width
        val srcHalf = radius / LOUPE_ZOOM * scaleToImage
        val srcSide = (srcHalf * 2).roundToInt().coerceAtMost(minOf(image.width, image.height))
        // Keep the source window inside the image (corners often sit on the image edge).
        val srcX = ((cx - rect.left) * scaleToImage - srcHalf).roundToInt().coerceIn(0, image.width - srcSide)
        val srcY = ((cy - rect.top) * scaleToImage - srcHalf).roundToInt().coerceIn(0, image.height - srcSide)
        drawImage(
            image,
            srcOffset = IntOffset(srcX, srcY),
            srcSize = IntSize(srcSide, srcSide),
            dstOffset = IntOffset((center.x - radius).roundToInt(), (center.y - radius).roundToInt()),
            dstSize = IntSize((radius * 2).roundToInt(), (radius * 2).roundToInt()),
        )
        drawLine(Color.White, center - Offset(14f, 0f), center + Offset(14f, 0f), strokeWidth = 1.5.dp.toPx())
        drawLine(Color.White, center - Offset(0f, 14f), center + Offset(0f, 14f), strokeWidth = 1.5.dp.toPx())
    }
    drawCircle(Color.White, radius = radius, center = center, style = Stroke(width = 3.dp.toPx()))
}
