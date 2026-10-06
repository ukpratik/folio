// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.feature.editor

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.ui.theme.FolioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RenameDialogTest {
    @get:Rule val compose = createComposeRule()

    private var renamed: String? = null
    private val confirm = hasText("Rename") and hasClickAction()

    private fun open(current: String = "Folio_20261007_0010") {
        compose.setContent { FolioTheme { RenameDialog(current, onRename = { renamed = it }, onDismiss = {}) } }
    }

    @Test fun showsCounterAndRenames() {
        open()
        compose.onNode(hasSetTextAction()).performTextReplacement("Fees/Receipt")
        compose.onNodeWithText("Symbols like / : * ? become _ · 12 / 100").assertExists()
        compose.onNode(confirm).performClick()
        assertThat(renamed).isEqualTo("Fees/Receipt") // the use case sanitises it (RenameDocument)
    }

    @Test fun blankNameCannotBeSaved() {
        open()
        compose.onNode(confirm).assertIsEnabled()
        compose.onNode(hasSetTextAction()).performTextReplacement("   ")
        compose.onNode(confirm).assertIsNotEnabled()
    }

    @Test fun inputIsCappedAt100Characters() {
        open()
        compose.onNode(hasSetTextAction()).performTextReplacement("x".repeat(150))
        compose.onNodeWithText("Symbols like / : * ? become _ · 100 / 100").assertExists()
    }
}
