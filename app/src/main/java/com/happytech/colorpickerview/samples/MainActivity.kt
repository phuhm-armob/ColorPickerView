package com.happytech.colorpickerview.samples

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.happytech.colorpickerview.samples.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.colorPickerView.hueSliderView = binding.hueSlider
        binding.colorPickerView.alphaSliderView = binding.colorAlphaSlider

        binding.openComposeSample.setOnClickListener {
            startActivity(Intent(this, ComposeSampleActivity::class.java))
        }
    }
}