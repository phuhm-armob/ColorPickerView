package com.happytech.colorpickerview.samples

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.happytech.colorpickerview.compose.ColorAlphaSlider
import com.happytech.colorpickerview.compose.ColorPicker
import com.happytech.colorpickerview.compose.HueSlider
import com.happytech.colorpickerview.compose.rememberColorPickerState

class ComposeSampleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ComposeSampleScreen()
            }
        }
    }
}

@Composable
private fun ComposeSampleScreen() {
    val state = rememberColorPickerState(Color.Red)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ColorPicker(
            state = state,
            modifier = Modifier.height(240.dp),
        )

        HueSlider(state = state)

        ColorAlphaSlider(state = state)

        Text(
            text = "#%08X".format(state.color.toArgb()),
            style = MaterialTheme.typography.titleMedium,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(state.color)
        )
    }
}
