// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.ukpratik.folio.core.ui

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import io.github.ukpratik.folio.core.ui.components.showUndo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UndoSnackbarTest {
    @get:Rule val compose = createComposeRule()

    private fun show(onResult: (Boolean) -> Unit) {
        val host = SnackbarHostState()
        compose.mainClock.autoAdvance = false
        compose.setContent {
            SnackbarHost(host)
            LaunchedEffect(Unit) { onResult(host.showUndo("Page deleted", "UNDO")) }
        }
        compose.mainClock.advanceTimeBy(500)
    }

    @Test fun tappingUndoInTimeReturnsTrue() {
        var result: Boolean? = null
        show { result = it }
        compose.onNodeWithText("UNDO").performClick()
        compose.mainClock.advanceTimeBy(500)
        assertThat(result).isTrue()
    }

    @Test fun expiresAfterAboutFiveSeconds() {
        var result: Boolean? = null
        show { result = it }
        compose.mainClock.advanceTimeBy(4_000)
        assertThat(result).isNull() // still showing at ~4.5 s
        compose.mainClock.advanceTimeBy(1_000)
        assertThat(result).isFalse()
    }
}
