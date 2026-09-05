# 🎨 ColorPickerView
[![](https://jitpack.io/v/mihphu/ColorPickerView.svg)](https://jitpack.io/#mihphu/ColorPickerView)

# 🎬 Preview
<img width="400" height="400" alt="Frame 1597880347" src="https://github.com/user-attachments/assets/dfed14ce-da64-4a86-bcfb-cf97f8f81f37" />

# 🚀 Features 
- ColorPicker: changes values of brightness and satruation in HSV color model
- HueSlider: changes hue values of HSV color model and passes it into ColorPicker
- ColorAlphaSlider: changes alpha value of provided color by ColorPicker
- State of components are preserved during configuration changes (screen rotation etc...)
- View size change events are implemented correctly to provide nice animations and layout changes
- Components of this library both work together and separately
- Change the value of ColorPicker and Sliders via code

| Artifact | minSdk | Pulls in Compose |
|---|---|---|
| `colorpickerview` | 18 | no |
| `colorpickerview-compose` | 21 | yes |

# 🏄‍♂️ Gradle dependency
`build.gradle`
```gradle
repositories {
        ..
        maven { url 'https://jitpack.io' }
    }
```
`settings.gradle`
```gradle
dependencyResolutionManagement {
    ..
    repositories {
      maven { url 'https://jitpack.io' }
    }
}
```
> **Note:** The coordinates below are the expected form for a multi-module JitPack build
> (`com.github.<owner>.<repo>:<module>:<tag>`). They have not yet been confirmed against an
> actual JitPack build log for the `1.1.0` tag — check
> https://jitpack.io/#mihphu/ColorPickerView/1.1.0 and use whatever the log prints if it differs.

```gradle
// XML views — minSdk 18, does not pull in Compose
implementation("com.github.mihphu.ColorPickerView:colorpickerview:1.1.0")

// Jetpack Compose — minSdk 21
implementation("com.github.mihphu.ColorPickerView:colorpickerview-compose:1.1.0")
```
# 📦 Usage
The library has 3 main views called `ColorPicker` `HueSlider` `ColorAlphaSlider` To add them to xml layout do the following:
```xml
    <com.happytech.colorpickerview.ColorPicker
        android:id="@+id/colorPickerView"
        android:layout_width="match_parent"
        android:layout_height="@dimen/_200sdp"
        android:layout_marginHorizontal="@dimen/_16sdp"
        app:cpv_pickerBorderRadius="@dimen/_8sdp"
        app:cpv_pickerOutlineColor="#0D000000"
        app:cpv_pickerOutlineSize="@dimen/_1sdp"
        app:cpv_thumbOutlineColor="#E6E6E6"
        app:cpv_thumbOutlineSize="1dp"
        app:cpv_thumbStrokeColor="@color/white"
        app:cpv_thumbStrokeSize="@dimen/_4sdp" />

    <com.happytech.colorpickerview.HueSlider
        android:id="@+id/hueSlider"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginHorizontal="@dimen/_16sdp"
        app:cpv_thumbOutlineColor="#E6E6E6"
        app:cpv_thumbOutlineSize="1dp"
        app:cpv_thumbStrokeColor="@color/white"
        app:cpv_thumbStrokeSize="@dimen/_4sdp"/>

    <com.happytech.colorpickerview.ColorAlphaSlider
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginHorizontal="@dimen/_16sdp"
        app:cpv_thumbOutlineColor="#E6E6E6"
        app:cpv_thumbOutlineSize="1dp"
        app:cpv_thumbStrokeColor="@color/white"
        app:cpv_thumbStrokeSize="@dimen/_4sdp"/>

```
To connect views to eachother in your code find the views by id then connect them to eachother:
```kotlin
val colorPicker = findViewById<ColorPicker>(R.id.colorPickerView)
val hueSlider = findViewById<HueSlider>(R.id.hueSlider)
val colorAlphaSlider = findViewById<ColorAlphaSlider>(R.id.colorAlphaSlider)

colorPicker.alphaSliderView = colorAlphaSlider
colorPicker.hueSliderView = hueSlider
```
To extract color from color picker do the following on `ColorPicker`
```kotlin
// Kotlin
colorPicker.color
// Java
colorPicker.getColor();
```

# 🧩 Jetpack Compose

The `colorpickerview-compose` artifact is a native rewrite using Compose Canvas. It is
completely independent of the XML artifact — add either one, or both.

```kotlin
@Composable
fun ColorPickerScreen() {
    val state = rememberColorPickerState(Color.Red)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ColorPicker(state = state, modifier = Modifier.height(240.dp))
        HueSlider(state = state)
        ColorAlphaSlider(state = state)
    }

    // state.color is the currently selected color, including alpha
}
```

All three composables share one `ColorPickerState`, so they stay in sync automatically —
replacing the `hueSliderView` / `alphaSliderView` assignment in the XML version. State survives
screen rotation.

If you want to manage state yourself, use the stateless overload:

```kotlin
var hue by remember { mutableFloatStateOf(30f) }

HueSlider(hue = hue, onHueChange = { hue = it })
```

## Customization

```kotlin
ColorAlphaSlider(
    state = state,
    colors = ColorPickerDefaults.colors(checkerDark = Color.LightGray),
    thumb = ColorPickerDefaults.thumb(strokeSize = 4.dp),
    checkerRows = 4,
    showChecker = true,
)
```

## Differences from the XML version

- Bar thickness is declared directly via `trackThickness` instead of being derived from view
  height.
- One `onXxxChangeFinished` callback replaces the pair of changed/changeEnd listeners.
- The hue band is built from a 7-stop gradient instead of a 360 px bitmap. Not noticeable to
  the naked eye.
- `ColorPicker` has no default height — pass one through `modifier`.

# 🤖 Color Listener
```kotlin
// ColorPicker
colorPicker.setOnColorChangedListener { color ->

}
colorPicker.setOnColorChangeEndListener { color ->
           
}

// HueSlider
hueSlider.setOnHueChangedListener { hue, argbColor ->
    // Hue value is between [0..360]
    // argbColor is just the color int representation of hue value with full brightness and saturation.
}
hueSlider.setOnHueChangeEndListener { hue, argbColor ->
    // Hue value is between [0..360]
    // argbColor is just the color int representation of hue value with full brightness and saturation.
}

// ColorAlphaSlider
colorAlphaSlider.setOnAlphaChangedListener { alpha ->
    // Alpha value between [0..1]
}
colorAlphaSlider.setOnAlphaChangeEndListener { alpha ->
    // Alpha value between [0..1]
}
```
# 🌈 Attributes

| Attribute                | Description                                                                                   | Type        | Default                                                | Applies to                                      |
|--------------------------|-----------------------------------------------------------------------------------------------|-------------|--------------------------------------------------------|-------------------------------------------------|
| `cpv_pickerBorderRadius` | Rounds the corners of the color picker area. Larger values = more rounded corners.            | `dimension` | `8dp`                                                  | `ColorPicker`                                   |
| `cpv_pickerOutlineSize`  | Thickness of the outer border of the color picker.                                            | `dimension` | `1dp`                                                  | `ColorPicker`                                   |
| `cpv_pickerOutlineColor` | Color of the outer border of the color picker.                                                | `color`     | `ColorUtils.blendARGB(Color.WHITE, Color.BLACK, 0.1f)` | `ColorPicker`                                   |
| `cpv_thumbOutlineSize`   | Thickness of the outer border of the thumb (color selection handle).                          | `dimension` | `1dp`                                                  | `ColorPicker`, `HueSlider`, `ColorAlphaSlider` |
| `cpv_thumbOutlineColor`  | Color of the outer border of the thumb.                                                       | `color`     | `#FFFFFF`                                              | `ColorPicker`, `HueSlider`, `ColorAlphaSlider` |
| `cpv_thumbStrokeSize`    | Thickness of the inner stroke of the thumb.                                                   | `dimension` | `2dp`                                                  | `ColorPicker`, `HueSlider`, `ColorAlphaSlider` |
| `cpv_thumbStrokeColor`   | Color of the inner stroke of the thumb.                                                       | `color`     | `#FFFFFF`                                              | `ColorPicker`, `HueSlider`, `ColorAlphaSlider` |
| `sliderBarStrokeCap`     | Defines the stroke cap style of the slider bar: `Butt = 0`, `Round = 1`, `Square = 2`.        | `enum`      | `Round`                                                | `HueSlider`, `ColorAlphaSlider`                |

---

## 💻 Set in Code (Kotlin)

```kotlin
// ColorPicker
picker.pickerBorderRadius = 20f
picker.pickerOutlineSize = 1f
picker.pickerOutlineColor = Color.BLACK

// Thumb (applies to ColorPicker, HueSlider, ColorAlphaSlider)
slider.thumbOutlineSize = 1f
slider.thumbOutlineColor = Color.BLACK
slider.thumbStrokeSize = 1f
slider.thumbStrokeColor = Color.BLACK

// Slider bar stroke cap style
slider.lineStrokeCap = Paint.Cap.ROUND
```
You can change the color of `ColorPicker' via 'color' setter
```kotlin
colorPicker.color = Color.parseColor("#962626")
colorPicker.color = Color.argb(128,255,255,255)
// Changing the color property in ColorPicker also changes the values of other sliders if the color was set after setting hue and alpha slider to KavehColorPicker.
// This code changes the color of ColorPicker that causes other sliders to change value also.
colorPicker.alphaSliderView = alphaSlider
colorPicker.hueSliderView = hueSlider
colorPicker.color = Color.argb(128,255,255,255)
// But if you change color before setting the other sliders to ColorPicker then the other sliders won't change.
colorPicker.color = Color.argb(128,255,255,255)
colorPicker.alphaSliderView = alphaSlider
colorPicker.hueSliderView = hueSlider
```
# License
```
MIT License

Copyright (c) 2025 Mohammad Hossein Naderi
https://github.com/Mohammad3125/KavehColorPicker

All modifications and enhancements Copyright (c) 2025
```

