// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.ukpratik.folio.core.ui.R

/** Plus Jakarta Sans variable font, bundled (D-36, SIL OFL — see FONT-LICENSE-OFL.txt). */
private fun jakarta(weight: FontWeight) = Font(
    R.font.plus_jakarta_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val PlusJakartaSans = FontFamily(
    jakarta(FontWeight.Normal),
    jakarta(FontWeight.Medium),
    jakarta(FontWeight.SemiBold),
    jakarta(FontWeight.Bold),
)

private fun style(size: Int, line: Int, weight: FontWeight) =
    TextStyle(fontFamily = PlusJakartaSans, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight)

internal val FolioTypography = Typography(
    headlineMedium = style(28, 34, FontWeight.Bold),
    titleLarge = style(22, 28, FontWeight.Bold),
    titleMedium = style(17, 24, FontWeight.SemiBold),
    bodyLarge = style(15, 22, FontWeight.Normal),
    bodyMedium = style(13, 18, FontWeight.Normal),
    labelLarge = style(15, 20, FontWeight.SemiBold),
    labelMedium = style(13, 18, FontWeight.SemiBold),
)
