package com.happytech.colorpickerview.compose

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HueSliderTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun tappingTheMiddleOfTheTrackSelectsTheMiddleHue() {
        var hue = 0f

        rule.setContent {
            HueSlider(
                hue = hue,
                onHueChange = { hue = it },
                modifier = Modifier.testTag("hue").width(300.dp),
            )
        }

        rule.onNodeWithTag("hue").performTouchInput {
            click(Offset(width / 2f, height / 2f))
        }

        rule.runOnIdle { assertEquals(180f, hue, 10f) }
    }

    @Test
    fun tappingTheFarLeftSelectsHueZero() {
        var hue = 200f

        rule.setContent {
            HueSlider(
                hue = hue,
                onHueChange = { hue = it },
                modifier = Modifier.testTag("hue").width(300.dp),
            )
        }

        rule.onNodeWithTag("hue").performTouchInput {
            click(Offset(0f, height / 2f))
        }

        rule.runOnIdle { assertEquals(0f, hue, 1f) }
    }

    @Test
    fun theFinishedCallbackFiresExactlyOncePerGesture() {
        var finishedCount = 0

        rule.setContent {
            HueSlider(
                hue = 0f,
                onHueChange = {},
                modifier = Modifier.testTag("hue").width(300.dp),
                onHueChangeFinished = { finishedCount++ },
            )
        }

        rule.onNodeWithTag("hue").performTouchInput { swipeRight() }

        rule.runOnIdle { assertEquals(1, finishedCount) }
    }

    @Test
    fun theFinishedCallbackReceivesTheHueAtTheEndOfTheGesture() {
        var finishedHue = -1f

        rule.setContent {
            var hue by remember { mutableFloatStateOf(0f) }
            HueSlider(
                hue = hue,
                onHueChange = { hue = it },
                modifier = Modifier.testTag("hue").width(300.dp),
                onHueChangeFinished = { finishedHue = it },
            )
        }

        rule.onNodeWithTag("hue").performTouchInput {
            click(Offset(width / 2f, height / 2f))
        }

        rule.runOnIdle { assertEquals(180f, finishedHue, 10f) }
    }

    @Test
    fun theStateOverloadWritesHueBackIntoTheState() {
        lateinit var state: ColorPickerState

        rule.setContent {
            state = rememberColorPickerState()
            HueSlider(
                state = state,
                modifier = Modifier.testTag("hue").width(300.dp),
            )
        }

        rule.onNodeWithTag("hue").performTouchInput {
            click(Offset(width / 2f, height / 2f))
        }

        rule.runOnIdle { assertEquals(180f, state.hue, 10f) }
    }
}
