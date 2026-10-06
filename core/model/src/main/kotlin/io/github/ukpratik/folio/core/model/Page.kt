// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.model

/** A point normalised to 0..1 in source-image space. */
data class PointF01(val x: Float, val y: Float) {
    init {
        require(x in 0f..1f && y in 0f..1f) { "Point must be normalised: ($x, $y)" }
    }
}

/** Document corners, clockwise from top-left. */
data class Quad(val tl: PointF01, val tr: PointF01, val br: PointF01, val bl: PointF01)

enum class Rotation(val degrees: Int) {
    R0(0), R90(90), R180(180), R270(270);

    fun clockwise(): Rotation = entries[(ordinal + 1) % entries.size]
}

enum class EnhancementMode { ORIGINAL, AUTO, GRAYSCALE, BW }

/** Brightness and contrast, each -1..1 (0 = unchanged). */
data class Adjustments(val brightness: Float = 0f, val contrast: Float = 0f)

enum class PageStatus { IMPORTING, READY, FAILED }

/**
 * One page of a document. Edits are stored as parameters; the source file is never modified (FR-11, ADR-0007).
 * [editVersion] increases on every edit and keys the thumbnail cache (ADR-0016).
 */
data class Page(
    val id: PageId,
    val documentId: DocumentId,
    val order: Int,
    val sourceId: String,
    val corners: Quad? = null,
    val autoCorners: Quad? = null,
    val rotation: Rotation = Rotation.R0,
    val mode: EnhancementMode = EnhancementMode.AUTO,
    val adjustments: Adjustments = Adjustments(),
    val status: PageStatus = PageStatus.IMPORTING,
    val editVersion: Long = 0,
    /** Epoch millis. Lets startup recovery ignore pages created in the current session. */
    val createdAt: Long = 0,
)
