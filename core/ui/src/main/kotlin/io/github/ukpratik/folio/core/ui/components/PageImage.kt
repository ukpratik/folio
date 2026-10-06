// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.ukpratik.folio.core.model.Page
import io.github.ukpratik.folio.core.model.PageStatus
import io.github.ukpratik.folio.core.model.PageThumbnail

/**
 * A page rendered with all its edits (via Coil + our renderer). Shows a spinner while importing and a
 * drawn "paper" stand-in in previews/screenshot tests, where no renderer is available.
 */
@Composable
fun PageImage(page: Page, sizePx: Int, modifier: Modifier = Modifier, contentDescription: String? = null) {
    Box(modifier.background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
        when {
            page.status == PageStatus.IMPORTING -> CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
            LocalInspectionMode.current -> PaperPlaceholder()
            else -> AsyncImage(
                model = PageThumbnail(page, sizePx),
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun PaperPlaceholder() {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.fillMaxWidth(0.6f).height(8.dp).background(Color(0xFF8E9196), MaterialTheme.shapes.extraSmall))
        repeat(5) { i ->
            Box(Modifier.fillMaxWidth(if (i == 2) 0.8f else 1f).height(5.dp).background(Color(0xFFD5D8DC), MaterialTheme.shapes.extraSmall))
        }
    }
}

/** Large, high-contrast page number (design: dark pill bottom-left). */
@Composable
fun PageNumberBadge(number: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(number.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.surface)
    }
}

/** Thin outline used around thumbnails. */
fun Modifier.pageOutline(): Modifier = this.border(1.dp, Color(0x1F15181C), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
