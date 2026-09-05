package com.happytech.colorpickerview.compose

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ColorAlphaSliderTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun tappingTheMiddleOfTheTrackSelectsHalfAlpha() {
        var alpha = 1f

        rule.setContent {
            ColorAlphaSlider(
                color = Color.Red,
                alpha = alpha,
                onAlphaChange = { alpha = it },
                modifier = Modifier.testTag("alpha").width(300.dp),
            )
        }

        rule.onNodeWithTag("alpha").performTouchInput {
            click(Offset(width / 2f, height / 2f))
        }

        rule.runOnIdle { assertEquals(0.5f, alpha, 0.05f) }
    }

    @Test
    fun tappingTheFarLeftSelectsZeroAlpha() {
        var alpha = 1f

        rule.setContent {
            ColorAlphaSlider(
                color = Color.Red,
                alpha = alpha,
                onAlphaChange = { alpha = it },
                modifier = Modifier.testTag("alpha").width(300.dp),
            )
        }

        rule.onNodeWithTag("alpha").performTouchInput {
            click(Offset(0f, height / 2f))
        }

        rule.runOnIdle { assertEquals(0f, alpha, 0.01f) }
    }

    @Test
    fun theFinishedCallbackFiresExactlyOncePerGesture() {
        var finishedCount = 0

        rule.setContent {
            ColorAlphaSlider(
                color = Color.Red,
                alpha = 1f,
                onAlphaChange = {},
                modifier = Modifier.testTag("alpha").width(300.dp),
                onAlphaChangeFinished = { finishedCount++ },
            )
        }

        rule.onNodeWithTag("alpha").performTouchInput { swipeLeft() }

        rule.runOnIdle { assertEquals(1, finishedCount) }
    }

    @Test
    fun theFinishedCallbackReceivesTheAlphaAtTheEndOfTheGesture() {
        var finishedAlpha = -1f

        rule.setContent {
            var alpha by remember { mutableFloatStateOf(1f) }
            ColorAlphaSlider(
                color = Color.Red,
                alpha = alpha,
                onAlphaChange = { alpha = it },
                modifier = Modifier.testTag("alpha").width(300.dp),
                onAlphaChangeFinished = { finishedAlpha = it },
            )
        }

        rule.onNodeWithTag("alpha").performTouchInput {
            click(Offset(width / 2f, height / 2f))
        }

        rule.runOnIdle { assertEquals(0.5f, finishedAlpha, 0.05f) }
    }

    @Test
    fun theStateOverloadWritesAlphaBackIntoTheState() {
        lateinit var state: ColorPickerState

        rule.setContent {
            state = rememberColorPickerState()
            ColorAlphaSlider(
                state = state,
                modifier = Modifier.testTag("alpha").width(300.dp),
            )
        }

        rule.onNodeWithTag("alpha").performTouchInput {
            click(Offset(width / 2f, height / 2f))
        }

        rule.runOnIdle { assertEquals(0.5f, state.alpha, 0.05f) }
    }

    @Test
    fun hidingTheCheckerStillTracksTouches() {
        var alpha = 1f

        rule.setContent {
            ColorAlphaSlider(
                color = Color.Red,
                alpha = alpha,
                onAlphaChange = { alpha = it },
                modifier = Modifier.testTag("alpha").width(300.dp),
                showChecker = false,
            )
        }

        rule.onNodeWithTag("alpha").performTouchInput {
            click(Offset(width / 2f, height / 2f))
        }

        rule.runOnIdle { assertEquals(0.5f, alpha, 0.05f) }
    }
}
