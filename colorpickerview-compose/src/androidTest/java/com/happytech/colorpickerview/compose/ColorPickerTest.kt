package com.happytech.colorpickerview.compose

import androidx.compose.foundation.layout.size
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

class ColorPickerTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun tappingTheCentreSelectsMidSaturationAndMidValue() {
        var saturation = 0f
        var value = 0f

        rule.setContent {
            ColorPicker(
                hue = 200f,
                saturation = saturation,
                value = value,
                onChange = { s, v -> saturation = s; value = v },
                modifier = Modifier.testTag("picker").size(300.dp),
            )
        }

        rule.onNodeWithTag("picker").performTouchInput {
            click(Offset(width / 2f, height / 2f))
        }

        rule.runOnIdle {
            assertEquals(0.5f, saturation, 0.05f)
            assertEquals(0.5f, value, 0.05f)
        }
    }

    @Test
    fun tappingTheTopLeftSelectsNoSaturationAndFullValue() {
        var saturation = 1f
        var value = 0f

        rule.setContent {
            ColorPicker(
                hue = 200f,
                saturation = saturation,
                value = value,
                onChange = { s, v -> saturation = s; value = v },
                modifier = Modifier.testTag("picker").size(300.dp),
            )
        }

        rule.onNodeWithTag("picker").performTouchInput { click(Offset(0f, 0f)) }

        rule.runOnIdle {
            assertEquals(0f, saturation, 0.01f)
            assertEquals(1f, value, 0.01f)
        }
    }

    @Test
    fun theFinishedCallbackFiresExactlyOncePerGesture() {
        var finishedCount = 0

        rule.setContent {
            ColorPicker(
                hue = 200f,
                saturation = 0.5f,
                value = 0.5f,
                onChange = { _, _ -> },
                modifier = Modifier.testTag("picker").size(300.dp),
                onColorChangeFinished = { finishedCount++ },
            )
        }

        rule.onNodeWithTag("picker").performTouchInput { swipeRight() }

        rule.runOnIdle { assertEquals(1, finishedCount) }
    }

    @Test
    fun theStateOverloadWritesSaturationAndValueBackIntoTheState() {
        lateinit var state: ColorPickerState

        rule.setContent {
            state = rememberColorPickerState()
            ColorPicker(
                state = state,
                modifier = Modifier.testTag("picker").size(300.dp),
            )
        }

        rule.onNodeWithTag("picker").performTouchInput {
            click(Offset(width / 2f, height / 2f))
        }

        rule.runOnIdle {
            assertEquals(0.5f, state.saturation, 0.05f)
            assertEquals(0.5f, state.value, 0.05f)
        }
    }
}
