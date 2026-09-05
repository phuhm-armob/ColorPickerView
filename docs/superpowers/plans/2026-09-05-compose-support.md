# Jetpack Compose Support Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Thêm module `colorpickerview-compose` cung cấp 3 composable vẽ native bằng Compose Canvas, để người dùng XML và người dùng Compose đều dùng được thư viện.

**Architecture:** Module Android library độc lập, không phụ thuộc module `colorpickerview`. Ba composable (`ColorPicker`, `HueSlider`, `ColorAlphaSlider`) mỗi cái có 2 overload: bản stateless nhận giá trị + callback, và bản state-holder nhận `ColorPickerState` rồi gọi xuống bản stateless. Toàn bộ phép tính hình học và màu tách thành hàm thuần Kotlin ở package `internal` để unit test được trên JVM.

**Tech Stack:** Kotlin 2.0.21, AGP 8.6.0, Compose BOM 2024.12.01 (Compose 1.7.6), JUnit 4, `compose-ui-test-junit4`.

**Spec:** `docs/superpowers/specs/2026-09-05-compose-support-design.md`

## Global Constraints

- Module mới: `namespace = "com.happytech.colorpickerview.compose"`, `compileSdk = 35`, `minSdk = 21`, Java/`jvmTarget` = **11**.
- Module `colorpickerview` **không được sửa** ở bất kỳ task nào. Giữ nguyên `minSdk 18`, `jvmTarget 1.8`, không thêm dependency.
- Dependency Compose trong module mới phải là `api(...)`, không phải `implementation(...)` — chữ ký public phơi ra `Modifier`, `Color`, `Dp`.
- Publishing: block `publishing` của **cả hai** module không hardcode `groupId` / `artifactId` / `version`.
- Màu checkerboard: `checkerLight = Color.White`, `checkerDark = Color(0xFFD7D7E1)`. Số hàng mặc định: 3.
- Giá trị mặc định phải khớp bản XML: thumb `radius = 12.dp`, `strokeSize = 2.dp`, `outlineSize = 1.dp`; picker `cornerRadius = 8.dp`, `outlineWidth = 1.dp`, `pickerOutline = Color(0x0D000000)`; `trackThickness = 12.dp`, `SliderHeight = 48.dp`.
- Mọi setter giá trị đều clamp, không ném exception.
- Hue ánh xạ trái→phải 0→360.

## Hai chỗ lệch so với spec (cố ý, đã cân nhắc)

1. **Spec §4.3 ghi `@Composable fun colors()` / `thumb()`. Plan này để chúng là hàm thường.** Material3 đánh dấu `@Composable` vì đọc `MaterialTheme`; ta không đọc theme nào cả, nên `@Composable` là thừa và làm chúng không unit-test được trên JVM. Hành vi quan sát được không đổi.
2. **Spec §5.3 ghi checkerboard dùng `ShaderBrush(ImageShader(...))` với `FilterQuality.None`. Plan này vẽ ô trực tiếp trong `clipPath`.** Lý do: chữ ký `ImageShader` có nhận `filterQuality` hay không thay đổi theo phiên bản Compose, và shader tile canh theo gốc toạ độ canvas nên còn phải thêm `translate` để bám mép trái track. Vòng lặp `drawRect` trong `clipPath` không có rủi ro API, cho ô sắc nét tuyệt đối vì toạ độ là pixel nguyên, và đọc dễ hơn. Chi phí: ~110 lệnh `drawRect` mỗi frame với slider 300dp — không đáng kể.

## File Structure

**Tạo mới:**

| File | Trách nhiệm |
|---|---|
| `colorpickerview-compose/build.gradle.kts` | Build config module |
| `.../compose/ColorPickerDefaults.kt` | `ColorPickerColors`, `ThumbStyle`, `ColorPickerDefaults` |
| `.../compose/ColorPickerState.kt` | State holder + `Saver` + `rememberColorPickerState` |
| `.../compose/ColorPicker.kt` | 2 overload composable mặt phẳng SV |
| `.../compose/HueSlider.kt` | 2 overload composable slider hue |
| `.../compose/ColorAlphaSlider.kt` | 2 overload composable slider alpha |
| `.../compose/internal/ColorMath.kt` | `rgbToHsv` thuần Kotlin |
| `.../compose/internal/TrackGeometry.kt` | Hình học track + đổi vị trí ↔ tỉ lệ |
| `.../compose/internal/Checkerboard.kt` | Kích thước ô + vẽ checkerboard |
| `.../compose/internal/Thumb.kt` | `DrawScope.drawThumb` |
| `.../compose/internal/DragGesture.kt` | `Modifier.sliderDrag`, `Modifier.planeDrag` |
| `app/.../samples/ComposeSampleActivity.kt` | Màn demo Compose |

**Sửa:** `settings.gradle.kts`, `build.gradle.kts` (root), `gradle/libs.versions.toml`, `colorpickerview/build.gradle.kts` (chỉ block publishing), `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/layout/activity_main.xml`, `app/src/main/res/values/strings.xml`, `README.md`.

Đường dẫn đầy đủ của `.../compose/` là `colorpickerview-compose/src/main/java/com/happytech/colorpickerview/compose/`.

---

### Task 1: Module scaffolding và `ColorPickerDefaults`

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts` (root)
- Modify: `colorpickerview/build.gradle.kts` (chỉ block `publishing`)
- Create: `colorpickerview-compose/build.gradle.kts`
- Create: `colorpickerview-compose/src/main/java/com/happytech/colorpickerview/compose/ColorPickerDefaults.kt`
- Test: `colorpickerview-compose/src/test/java/com/happytech/colorpickerview/compose/ColorPickerDefaultsTest.kt`

**Interfaces:**
- Consumes: không có.
- Produces: `ColorPickerColors(thumbStroke, thumbOutline, pickerOutline, checkerLight, checkerDark: Color)`; `ThumbStyle(radius, strokeSize, outlineSize: Dp)`; `ColorPickerDefaults.colors(...): ColorPickerColors`, `ColorPickerDefaults.thumb(...): ThumbStyle`, `ColorPickerDefaults.PickerCornerRadius/PickerOutlineWidth/TrackThickness/SliderHeight: Dp`, `ColorPickerDefaults.CheckerRows: Int`.

- [ ] **Step 1: Thêm entry vào version catalog**

Trong `gradle/libs.versions.toml`, thêm vào `[versions]`:

```toml
composeBom = "2024.12.01"
```

Thêm vào `[libraries]`:

```toml
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-foundation = { group = "androidx.compose.foundation", name = "foundation" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activity" }
```

Thêm vào `[plugins]`:

```toml
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

Các artifact không ghi version là cố ý — chúng lấy version từ BOM.

- [ ] **Step 2: Đăng ký module và plugin ở cấp root**

Trong `settings.gradle.kts`, thêm sau dòng `include(":colorpickerview")`:

```kotlin
include(":colorpickerview-compose")
```

Trong `build.gradle.kts` ở root, thêm vào block `plugins`:

```kotlin
alias(libs.plugins.kotlin.compose) apply false
```

- [ ] **Step 3: Tạo build file cho module mới**

Tạo `colorpickerview-compose/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("maven-publish")
}

android {
    namespace = "com.happytech.colorpickerview.compose"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
            }
        }
    }
}
```

Không cần `AndroidManifest.xml` — AGP 8 tự sinh từ `namespace`.

- [ ] **Step 4: Bỏ coordinate hardcode ở module cũ**

Trong `colorpickerview/build.gradle.kts`, thay block `afterEvaluate` hiện tại bằng:

```kotlin
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
            }
        }
    }
}
```

Ba dòng `groupId`, `artifactId`, `version` bị xoá để JitPack tự inject. Đây là thay đổi duy nhất được phép làm ở module này.

- [ ] **Step 5: Viết test thất bại cho defaults**

Tạo `colorpickerview-compose/src/test/java/com/happytech/colorpickerview/compose/ColorPickerDefaultsTest.kt`:

```kotlin
package com.happytech.colorpickerview.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorPickerDefaultsTest {

    @Test
    fun `default colors match the xml view defaults`() {
        val colors = ColorPickerDefaults.colors()

        assertEquals(Color.White, colors.thumbStroke)
        assertEquals(Color.White, colors.thumbOutline)
        assertEquals(Color(0x0D000000), colors.pickerOutline)
        assertEquals(Color.White, colors.checkerLight)
        assertEquals(Color(0xFFD7D7E1), colors.checkerDark)
    }

    @Test
    fun `default thumb matches the xml view defaults`() {
        val thumb = ColorPickerDefaults.thumb()

        assertEquals(12.dp, thumb.radius)
        assertEquals(2.dp, thumb.strokeSize)
        assertEquals(1.dp, thumb.outlineSize)
    }

    @Test
    fun `overriding one value leaves the others at their defaults`() {
        val colors = ColorPickerDefaults.colors(checkerDark = Color.Gray)

        assertEquals(Color.Gray, colors.checkerDark)
        assertEquals(Color.White, colors.checkerLight)
    }

    @Test
    fun `layout defaults match the xml view defaults`() {
        assertEquals(8.dp, ColorPickerDefaults.PickerCornerRadius)
        assertEquals(1.dp, ColorPickerDefaults.PickerOutlineWidth)
        assertEquals(12.dp, ColorPickerDefaults.TrackThickness)
        assertEquals(48.dp, ColorPickerDefaults.SliderHeight)
        assertEquals(3, ColorPickerDefaults.CheckerRows)
    }
}
```

- [ ] **Step 6: Chạy test, xác nhận nó fail**

Run: `./gradlew :colorpickerview-compose:testDebugUnitTest`
Expected: FAIL, lỗi compile `Unresolved reference: ColorPickerDefaults`.

- [ ] **Step 7: Viết implementation tối thiểu**

Tạo `colorpickerview-compose/src/main/java/com/happytech/colorpickerview/compose/ColorPickerDefaults.kt`:

```kotlin
package com.happytech.colorpickerview.compose

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Màu của thumb, outline và ô checkerboard. Dựng qua [ColorPickerDefaults.colors]. */
@Immutable
class ColorPickerColors internal constructor(
    val thumbStroke: Color,
    val thumbOutline: Color,
    val pickerOutline: Color,
    val checkerLight: Color,
    val checkerDark: Color,
)

/**
 * Kích thước các vòng của thumb. Dựng qua [ColorPickerDefaults.thumb].
 *
 * [radius] chỉ áp dụng cho [ColorPicker]; hai slider luôn lấy bán kính thumb bằng
 * `trackThickness` của chúng.
 */
@Immutable
class ThumbStyle internal constructor(
    val radius: Dp,
    val strokeSize: Dp,
    val outlineSize: Dp,
)

object ColorPickerDefaults {

    val PickerCornerRadius: Dp = 8.dp
    val PickerOutlineWidth: Dp = 1.dp
    val TrackThickness: Dp = 12.dp
    val SliderHeight: Dp = 48.dp
    const val CheckerRows: Int = 3

    fun colors(
        thumbStroke: Color = Color.White,
        thumbOutline: Color = Color.White,
        pickerOutline: Color = Color(0x0D000000),
        checkerLight: Color = Color.White,
        checkerDark: Color = Color(0xFFD7D7E1),
    ): ColorPickerColors = ColorPickerColors(
        thumbStroke = thumbStroke,
        thumbOutline = thumbOutline,
        pickerOutline = pickerOutline,
        checkerLight = checkerLight,
        checkerDark = checkerDark,
    )

    fun thumb(
        radius: Dp = 12.dp,
        strokeSize: Dp = 2.dp,
        outlineSize: Dp = 1.dp,
    ): ThumbStyle = ThumbStyle(
        radius = radius,
        strokeSize = strokeSize,
        outlineSize = outlineSize,
    )
}
```

- [ ] **Step 8: Chạy test, xác nhận pass**

Run: `./gradlew :colorpickerview-compose:testDebugUnitTest`
Expected: PASS, 4 test.

Nếu test fail vì `java.lang.RuntimeException: Method ... not mocked`, nghĩa là `Color`/`Dp` chạm vào stub `android.graphics`. Khi đó thêm vào block `android` của module: `testOptions { unitTests.isReturnDefaultValues = true }` rồi chạy lại. Không thêm sẵn — chỉ thêm khi thật sự gặp.

- [ ] **Step 9: Xác nhận module cũ không bị ảnh hưởng**

Run: `./gradlew :colorpickerview:assembleRelease`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 10: Commit**

```bash
git add gradle/libs.versions.toml settings.gradle.kts build.gradle.kts colorpickerview/build.gradle.kts colorpickerview-compose/
git commit -m "feat(compose): scaffold colorpickerview-compose module with styling defaults"
```

---

### Task 2: Hàm toán thuần Kotlin

**Files:**
- Create: `colorpickerview-compose/src/main/java/com/happytech/colorpickerview/compose/internal/ColorMath.kt`
- Create: `colorpickerview-compose/src/main/java/com/happytech/colorpickerview/compose/internal/TrackGeometry.kt`
- Test: `colorpickerview-compose/src/test/java/com/happytech/colorpickerview/compose/internal/ColorMathTest.kt`
- Test: `colorpickerview-compose/src/test/java/com/happytech/colorpickerview/compose/internal/TrackGeometryTest.kt`

**Interfaces:**
- Consumes: không có.
- Produces:
  - `internal fun rgbToHsv(red: Float, green: Float, blue: Float): FloatArray` — trả `[hue 0..360, saturation 0..1, value 0..1]`
  - `internal class TrackGeometry(val start: Float, val end: Float, val centerY: Float, val thickness: Float)`
  - `internal fun trackGeometry(size: Size, thicknessPx: Float, thumbRadiusPx: Float): TrackGeometry`
  - `internal fun fractionForX(x: Float, geometry: TrackGeometry): Float` — kết quả đã clamp 0..1
  - `internal fun xForFraction(fraction: Float, geometry: TrackGeometry): Float`

- [ ] **Step 1: Viết test thất bại cho `rgbToHsv`**

Tạo `colorpickerview-compose/src/test/java/com/happytech/colorpickerview/compose/internal/ColorMathTest.kt`:

```kotlin
package com.happytech.colorpickerview.compose.internal

import org.junit.Assert.assertEquals
import org.junit.Test

class ColorMathTest {

    private fun assertHsv(expected: Triple<Float, Float, Float>, actual: FloatArray) {
        assertEquals(expected.first, actual[0], 0.5f)
        assertEquals(expected.second, actual[1], 0.01f)
        assertEquals(expected.third, actual[2], 0.01f)
    }

    @Test
    fun `pure red is hue zero`() {
        assertHsv(Triple(0f, 1f, 1f), rgbToHsv(1f, 0f, 0f))
    }

    @Test
    fun `pure green is hue 120`() {
        assertHsv(Triple(120f, 1f, 1f), rgbToHsv(0f, 1f, 0f))
    }

    @Test
    fun `pure blue is hue 240`() {
        assertHsv(Triple(240f, 1f, 1f), rgbToHsv(0f, 0f, 1f))
    }

    @Test
    fun `magenta wraps into positive hue`() {
        assertHsv(Triple(300f, 1f, 1f), rgbToHsv(1f, 0f, 1f))
    }

    @Test
    fun `white has no saturation`() {
        assertHsv(Triple(0f, 0f, 1f), rgbToHsv(1f, 1f, 1f))
    }

    @Test
    fun `black has no saturation and no value`() {
        assertHsv(Triple(0f, 0f, 0f), rgbToHsv(0f, 0f, 0f))
    }

    @Test
    fun `mid grey keeps value at half`() {
        assertHsv(Triple(0f, 0f, 0.5f), rgbToHsv(0.5f, 0.5f, 0.5f))
    }
}
```

- [ ] **Step 2: Chạy test, xác nhận fail**

Run: `./gradlew :colorpickerview-compose:testDebugUnitTest --tests "*ColorMathTest"`
Expected: FAIL, `Unresolved reference: rgbToHsv`.

- [ ] **Step 3: Viết `rgbToHsv`**

Tạo `colorpickerview-compose/src/main/java/com/happytech/colorpickerview/compose/internal/ColorMath.kt`:

```kotlin
package com.happytech.colorpickerview.compose.internal

/**
 * Đổi RGB (mỗi kênh 0..1) sang HSV thuần Kotlin.
 *
 * `android.graphics.Color.colorToHSV` không dùng được vì module này phải unit test được
 * trên JVM.
 *
 * @return `[hue 0..360, saturation 0..1, value 0..1]`
 */
internal fun rgbToHsv(red: Float, green: Float, blue: Float): FloatArray {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min

    val rawHue = when {
        delta == 0f -> 0f
        max == red -> 60f * (((green - blue) / delta) % 6f)
        max == green -> 60f * (((blue - red) / delta) + 2f)
        else -> 60f * (((red - green) / delta) + 4f)
    }

    val hue = if (rawHue < 0f) rawHue + 360f else rawHue
    val saturation = if (max == 0f) 0f else delta / max

    return floatArrayOf(hue, saturation, max)
}
```

- [ ] **Step 4: Chạy test, xác nhận pass**

Run: `./gradlew :colorpickerview-compose:testDebugUnitTest --tests "*ColorMathTest"`
Expected: PASS, 7 test.

- [ ] **Step 5: Viết test thất bại cho hình học track**

Tạo `colorpickerview-compose/src/test/java/com/happytech/colorpickerview/compose/internal/TrackGeometryTest.kt`:

```kotlin
package com.happytech.colorpickerview.compose.internal

import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackGeometryTest {

    // slider 300 x 48 px, track dày 12 px, thumb bán kính 12 px
    private val geometry = trackGeometry(
        size = Size(300f, 48f),
        thicknessPx = 12f,
        thumbRadiusPx = 12f,
    )

    @Test
    fun `track is inset by the thumb radius on both ends`() {
        assertEquals(12f, geometry.start, 0.01f)
        assertEquals(288f, geometry.end, 0.01f)
    }

    @Test
    fun `track is centred vertically`() {
        assertEquals(24f, geometry.centerY, 0.01f)
    }

    @Test
    fun `x at the start of the track is fraction zero`() {
        assertEquals(0f, fractionForX(12f, geometry), 0.001f)
    }

    @Test
    fun `x at the end of the track is fraction one`() {
        assertEquals(1f, fractionForX(288f, geometry), 0.001f)
    }

    @Test
    fun `x at the middle of the track is fraction half`() {
        assertEquals(0.5f, fractionForX(150f, geometry), 0.001f)
    }

    @Test
    fun `x outside the track is clamped`() {
        assertEquals(0f, fractionForX(-40f, geometry), 0.001f)
        assertEquals(1f, fractionForX(9999f, geometry), 0.001f)
    }

    @Test
    fun `fraction round trips back to x`() {
        assertEquals(150f, xForFraction(fractionForX(150f, geometry), geometry), 0.01f)
    }

    @Test
    fun `a track narrower than its insets does not divide by zero`() {
        val tiny = trackGeometry(Size(4f, 48f), thicknessPx = 12f, thumbRadiusPx = 12f)

        assertEquals(0f, fractionForX(0f, tiny), 0.001f)
        assertEquals(0f, fractionForX(4f, tiny), 0.001f)
    }
}
```

- [ ] **Step 6: Chạy test, xác nhận fail**

Run: `./gradlew :colorpickerview-compose:testDebugUnitTest --tests "*TrackGeometryTest"`
Expected: FAIL, `Unresolved reference: trackGeometry`.

- [ ] **Step 7: Viết `TrackGeometry`**

Tạo `colorpickerview-compose/src/main/java/com/happytech/colorpickerview/compose/internal/TrackGeometry.kt`:

```kotlin
package com.happytech.colorpickerview.compose.internal

import androidx.compose.ui.geometry.Size

/**
 * Hình học của thanh slider, tính bằng pixel.
 *
 * [start] và [end] là tâm của hai đầu bo tròn, tức là khoảng chạy của thumb. Bản thân
 * thanh còn phủ thêm nửa [thickness] ra ngoài mỗi đầu.
 */
internal class TrackGeometry(
    val start: Float,
    val end: Float,
    val centerY: Float,
    val thickness: Float,
)

internal fun trackGeometry(
    size: Size,
    thicknessPx: Float,
    thumbRadiusPx: Float,
): TrackGeometry = TrackGeometry(
    start = thumbRadiusPx,
    end = (size.width - thumbRadiusPx).coerceAtLeast(thumbRadiusPx),
    centerY = size.height / 2f,
    thickness = thicknessPx,
)

internal fun fractionForX(x: Float, geometry: TrackGeometry): Float {
    val span = geometry.end - geometry.start
    if (span <= 0f) return 0f
    return ((x - geometry.start) / span).coerceIn(0f, 1f)
}

internal fun xForFraction(fraction: Float, geometry: TrackGeometry): Float =
    geometry.start + (geometry.end - geometry.start) * fraction.coerceIn(0f, 1f)
```

- [ ] **Step 8: Chạy toàn bộ unit test, xác nhận pass**

Run: `./gradlew :colorpickerview-compose:testDebugUnitTest`
Expected: PASS, 19 test.

- [ ] **Step 9: Commit**

```bash
git add colorpickerview-compose/src/main/java/com/happytech/colorpickerview/compose/internal colorpickerview-compose/src/test
git commit -m "feat(compose): add pure colour and track geometry maths"
```

---

### Task 3: `ColorPickerState`

**Files:**
- Create: `colorpickerview-compose/src/main/java/com/happytech/colorpickerview/compose/ColorPickerState.kt`
- Test: `colorpickerview-compose/src/test/java/com/happytech/colorpickerview/compose/ColorPickerStateTest.kt`

**Interfaces:**
- Consumes: `rgbToHsv` từ Task 2.
- Produces:
  - `class ColorPickerState` với 4 `var Float`: `hue`, `saturation`, `value`, `alpha`; và `var color: Color`
  - `internal fun ColorPickerState(color: Color): ColorPickerState` — factory
  - `internal val ColorPickerStateSaver: Saver<ColorPickerState, FloatArray>`
  - `@Composable fun rememberColorPickerState(initialColor: Color = Color.Red): ColorPickerState`

- [ ] **Step 1: Viết test thất bại**

Tạo `colorpickerview-compose/src/test/java/com/happytech/colorpickerview/compose/ColorPickerStateTest.kt`:

```kotlin
package com.happytech.colorpickerview.compose

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorPickerStateTest {

    @Test
    fun `state built from red exposes hue zero and full saturation`() {
        val state = ColorPickerState(Color.Red)

        assertEquals(0f, state.hue, 0.5f)
        assertEquals(1f, state.saturation, 0.01f)
        assertEquals(1f, state.value, 0.01f)
        assertEquals(1f, state.alpha, 0.01f)
    }

    @Test
    fun `colour round trips through the state`() {
        val original = Color(0.2f, 0.6f, 0.9f, 1f)
        val state = ColorPickerState(original)

        assertEquals(original.red, state.color.red, 1f / 255f)
        assertEquals(original.green, state.color.green, 1f / 255f)
        assertEquals(original.blue, state.color.blue, 1f / 255f)
    }

    @Test
    fun `alpha survives the round trip`() {
        val state = ColorPickerState(Color(1f, 0f, 0f, 0.25f))

        assertEquals(0.25f, state.alpha, 0.01f)
        assertEquals(0.25f, state.color.alpha, 0.01f)
    }

    @Test
    fun `hue is clamped instead of throwing`() {
        val state = ColorPickerState(Color.Red)

        state.hue = 400f
        assertEquals(360f, state.hue, 0.01f)

        state.hue = -20f
        assertEquals(0f, state.hue, 0.01f)
    }

    @Test
    fun `saturation value and alpha are clamped to zero one`() {
        val state = ColorPickerState(Color.Red)

        state.saturation = 3f
        state.value = -1f
        state.alpha = 7f

        assertEquals(1f, state.saturation, 0.01f)
        assertEquals(0f, state.value, 0.01f)
        assertEquals(1f, state.alpha, 0.01f)
    }

    @Test
    fun `assigning colour updates every component`() {
        val state = ColorPickerState(Color.Red)

        state.color = Color(0f, 0f, 1f, 0.5f)

        assertEquals(240f, state.hue, 0.5f)
        assertEquals(1f, state.saturation, 0.01f)
        assertEquals(1f, state.value, 0.01f)
        assertEquals(0.5f, state.alpha, 0.01f)
    }

    @Test
    fun `saver restores an equivalent state`() {
        val state = ColorPickerState(Color(0.2f, 0.6f, 0.9f, 0.4f))

        val saved = with(ColorPickerStateSaver) {
            TestSaverScope.save(state)
        }
        val restored = ColorPickerStateSaver.restore(saved as FloatArray)!!

        assertEquals(state.hue, restored.hue, 0.01f)
        assertEquals(state.saturation, restored.saturation, 0.01f)
        assertEquals(state.value, restored.value, 0.01f)
        assertEquals(state.alpha, restored.alpha, 0.01f)
    }
}

private object TestSaverScope : androidx.compose.runtime.saveable.SaverScope {
    override fun canBeSaved(value: Any): Boolean = true
}
```

- [ ] **Step 2: Chạy test, xác nhận fail**

Run: `./gradlew :colorpickerview-compose:testDebugUnitTest --tests "*ColorPickerStateTest"`
Expected: FAIL, `Unresolved reference: ColorPickerState`.

- [ ] **Step 3: Viết `ColorPickerState`**

Tạo `colorpickerview-compose/src/main/java/com/happytech/colorpickerview/compose/ColorPickerState.kt`:

```kotlin
package com.happytech.colorpickerview.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import com.happytech.colorpickerview.compose.internal.rgbToHsv

/**
 * Màu đang chọn, chia sẻ giữa [ColorPicker], [HueSlider] và [ColorAlphaSlider].
 *
 * Thay cho cơ chế gán chéo `alphaSliderView` / `hueSliderView` của bản XML.
 *
 * Mọi setter clamp giá trị thay vì ném exception: setter bị gọi trong lúc recomposition
 * nên ném ra sẽ làm sập UI.
 */
@Stable
class ColorPickerState internal constructor(
    hue: Float,
    saturation: Float,
    value: Float,
    alpha: Float,
) {
    private val hueState = mutableFloatStateOf(hue.coerceIn(0f, 360f))
    private val saturationState = mutableFloatStateOf(saturation.coerceIn(0f, 1f))
    private val valueState = mutableFloatStateOf(value.coerceIn(0f, 1f))
    private val alphaState = mutableFloatStateOf(alpha.coerceIn(0f, 1f))

    /** Hue theo độ, 0..360. */
    var hue: Float
        get() = hueState.floatValue
        set(newValue) { hueState.floatValue = newValue.coerceIn(0f, 360f) }

    /** Saturation, 0..1. */
    var saturation: Float
        get() = saturationState.floatValue
        set(newValue) { saturationState.floatValue = newValue.coerceIn(0f, 1f) }

    /** Value (độ sáng) trong mô hình HSV, 0..1. */
    var value: Float
        get() = valueState.floatValue
        set(newValue) { valueState.floatValue = newValue.coerceIn(0f, 1f) }

    /** Alpha, 0..1. */
    var alpha: Float
        get() = alphaState.floatValue
        set(newValue) { alphaState.floatValue = newValue.coerceIn(0f, 1f) }

    /** Màu kết quả. Gán vào đây sẽ tách ngược ra bốn thành phần trên. */
    var color: Color
        get() = Color.hsv(hue, saturation, value, alpha)
        set(newValue) {
            val hsv = rgbToHsv(newValue.red, newValue.green, newValue.blue)
            hue = hsv[0]
            saturation = hsv[1]
            value = hsv[2]
            alpha = newValue.alpha
        }

    /** Màu đầy alpha, dùng làm nền gradient cho [ColorAlphaSlider]. */
    internal val opaqueColor: Color
        get() = Color.hsv(hue, saturation, value, alpha = 1f)
}

internal fun ColorPickerState(color: Color): ColorPickerState {
    val hsv = rgbToHsv(color.red, color.green, color.blue)
    return ColorPickerState(hsv[0], hsv[1], hsv[2], color.alpha)
}

internal val ColorPickerStateSaver: Saver<ColorPickerState, FloatArray> = Saver(
    save = { floatArrayOf(it.hue, it.saturation, it.value, it.alpha) },
    restore = { ColorPickerState(it[0], it[1], it[2], it[3]) },
)

/**
 * Tạo một [ColorPickerState] sống qua config change.
 *
 * @param initialColor màu ban đầu; đổi giá trị này sẽ dựng lại state.
 */
@Composable
fun rememberColorPickerState(initialColor: Color = Color.Red): ColorPickerState =
    rememberSaveable(initialColor, saver = ColorPickerStateSaver) {
        ColorPickerState(initialColor)
    }
```

- [ ] **Step 4: Chạy test, xác nhận pass**

Run: `./gradlew :colorpickerview-compose:testDebugUnitTest`
Expected: PASS, 26 test.

- [ ] **Step 5: Commit**

```bash
git add colorpickerview-compose/src/main/java/com/happytech/colorpickerview/compose/ColorPickerState.kt colorpickerview-compose/src/test/java/com/happytech/colorpickerview/compose/ColorPickerStateTest.kt
git commit -m "feat(compose): add ColorPickerState with saveable hoisting"
```

---

### Task 4: `HueSlider` (kèm helper thumb và gesture)

**Files:**
- Create: `colorpickerview-compose/src/main/java/com/happytech/colorpickerview/compose/internal/Thumb.kt`
- Create: `colorpickerview-compose/src/main/java/com/happytech/colorpickerview/compose/internal/DragGesture.kt`
- Create: `colorpickerview-compose/src/main/java/com/happytech/colorpickerview/compose/HueSlider.kt`
- Test: `colorpickerview-compose/src/androidTest/java/com/happytech/colorpickerview/compose/HueSliderTest.kt`

**Interfaces:**
- Consumes: `ColorPickerColors`, `ThumbStyle`, `ColorPickerDefaults` (Task 1); `TrackGeometry`, `trackGeometry`, `fractionForX`, `xForFraction` (Task 2); `ColorPickerState` (Task 3).
- Produces:
  - `internal fun DrawScope.drawThumb(center: Offset, radius: Float, color: Color, colors: ColorPickerColors, thumb: ThumbStyle)`
  - `@Composable internal fun Modifier.sliderDrag(trackThickness: Dp, onFraction: (Float) -> Unit, onFinished: () -> Unit): Modifier`
  - `@Composable internal fun Modifier.planeDrag(inset: Dp, onFraction: (Float, Float) -> Unit, onFinished: () -> Unit): Modifier`
  - `@Composable fun HueSlider(hue: Float, onHueChange: (Float) -> Unit, modifier: Modifier, colors: ColorPickerColors, thumb: ThumbStyle, trackThickness: Dp, onHueChangeFinished: ((Float) -> Unit)?)`
  - `@Composable fun HueSlider(state: ColorPickerState, modifier: Modifier, colors: ColorPickerColors, thumb: ThumbStyle, trackThickness: Dp, onHueChangeFinished: ((Float) -> Unit)?)`

- [ ] **Step 1: Viết test thất bại**

Tạo `colorpickerview-compose/src/androidTest/java/com/happytech/colorpickerview/compose/HueSliderTest.kt`:

```kotlin
package com.happytech.colorpickerview.compose

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
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
```

- [ ] **Step 2: Chạy test, xác nhận fail**

Cần một emulator hoặc thiết bị thật đang kết nối.

Run: `./gradlew :colorpickerview-compose:connectedDebugAndroidTest`
Expected: FAIL, `Unresolved reference: HueSlider`.

- [ ] **Step 3: Viết helper vẽ thumb**

Tạo `colorpickerview-compose/src/main/java/com/happytech/colorpickerview/compose/internal/Thumb.kt`:

```kotlin
package com.happytech.colorpickerview.compose.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.happytech.colorpickerview.compose.ColorPickerColors
import com.happytech.colorpickerview.compose.ThumbStyle

/**
 * Vẽ thumb là bốn vòng tròn đồng tâm, khớp thứ tự của bản XML:
 * outline, stroke, một vòng màu tối hơn 10% làm viền trong, rồi màu thật.
 */
internal fun DrawScope.drawThumb(
    center: Offset,
    radius: Float,
    color: Color,
    colors: ColorPickerColors,
    thumb: ThumbStyle,
) {
    val outline = thumb.outlineSize.toPx()
    val stroke = thumb.strokeSize.toPx()
    val hairline = 1.dp.toPx()

    drawCircle(colors.thumbOutline, radius, center)
    drawCircle(colors.thumbStroke, radius - outline, center)
    drawCircle(lerp(color, Color.Black, 0.1f), radius - outline - stroke, center)
    drawCircle(color, radius - outline - stroke - hairline, center)
}
```

- [ ] **Step 4: Viết helper gesture**

Tạo `colorpickerview-compose/src/main/java/com/happytech/colorpickerview/compose/internal/DragGesture.kt`:

```kotlin
package com.happytech.colorpickerview.compose.internal

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp

/**
 * Chạm là nhảy tới vị trí đó luôn rồi kéo tiếp, giống `ACTION_DOWN` của bản XML.
 *
 * Hai callback đi qua [rememberUpdatedState] vì khối `pointerInput` chỉ chạy lại khi key
 * đổi — không có nó, lambda bị giữ lại từ lần composition đầu và ghi vào state cũ.
 */
@Composable
internal fun Modifier.sliderDrag(
    trackThickness: Dp,
    onFraction: (Float) -> Unit,
    onFinished: () -> Unit,
): Modifier {
    val currentOnFraction by rememberUpdatedState(onFraction)
    val currentOnFinished by rememberUpdatedState(onFinished)

    return pointerInput(trackThickness) {
        val thicknessPx = trackThickness.toPx()

        awaitEachGesture {
            val geometry = trackGeometry(
                size = Size(size.width.toFloat(), size.height.toFloat()),
                thicknessPx = thicknessPx,
                thumbRadiusPx = thicknessPx,
            )

            val down = awaitFirstDown(requireUnconsumed = false)
            currentOnFraction(fractionForX(down.position.x, geometry))
            down.consume()

            drag(down.id) { change ->
                currentOnFraction(fractionForX(change.position.x, geometry))
                change.consume()
            }

            currentOnFinished()
        }
    }
}

/**
 * Bản hai chiều cho mặt phẳng saturation/value. Phát ra `(fractionX, fractionY)` đã clamp
 * 0..1, gốc ở góc trên-trái của vùng vẽ (đã trừ [inset] mỗi cạnh).
 */
@Composable
internal fun Modifier.planeDrag(
    inset: Dp,
    onFraction: (Float, Float) -> Unit,
    onFinished: () -> Unit,
): Modifier {
    val currentOnFraction by rememberUpdatedState(onFraction)
    val currentOnFinished by rememberUpdatedState(onFinished)

    return pointerInput(inset) {
        val insetPx = inset.toPx()

        awaitEachGesture {
            val spanX = (size.width - insetPx * 2f).coerceAtLeast(1f)
            val spanY = (size.height - insetPx * 2f).coerceAtLeast(1f)

            fun emit(x: Float, y: Float) = currentOnFraction(
                ((x - insetPx) / spanX).coerceIn(0f, 1f),
                ((y - insetPx) / spanY).coerceIn(0f, 1f),
            )

            val down = awaitFirstDown(requireUnconsumed = false)
            emit(down.position.x, down.position.y)
            down.consume()

            drag(down.id) { change ->
                emit(change.position.x, change.position.y)
                change.consume()
            }

            currentOnFinished()
        }
    }
}
```

- [ ] **Step 5: Viết `HueSlider`**

Tạo `colorpickerview-compose/src/main/java/com/happytech/colorpickerview/compose/HueSlider.kt`:

```kotlin
package com.happytech.colorpickerview.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.happytech.colorpickerview.compose.internal.drawThumb
import com.happytech.colorpickerview.compose.internal.sliderDrag
import com.happytech.colorpickerview.compose.internal.trackGeometry
import com.happytech.colorpickerview.compose.internal.xForFraction

/**
 * Bảy mốc hue. Bản XML dùng bitmap 360 px một-pixel-một-độ; ở đây nội suy tuyến tính
 * giữa bảy mốc, sai khác không nhìn ra được và bỏ được file PNG.
 */
private val HueStops = listOf(
    Color.Red,
    Color.Yellow,
    Color.Green,
    Color.Cyan,
    Color.Blue,
    Color.Magenta,
    Color.Red,
)

/**
 * Slider chọn hue, dùng chung [state] với [ColorPicker] và [ColorAlphaSlider].
 */
@Composable
fun HueSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    thumb: ThumbStyle = ColorPickerDefaults.thumb(),
    trackThickness: Dp = ColorPickerDefaults.TrackThickness,
    onHueChangeFinished: ((Float) -> Unit)? = null,
) {
    HueSlider(
        hue = state.hue,
        onHueChange = { state.hue = it },
        modifier = modifier,
        colors = colors,
        thumb = thumb,
        trackThickness = trackThickness,
        onHueChangeFinished = onHueChangeFinished,
    )
}

/**
 * Slider chọn hue, bản stateless.
 *
 * @param hue hue hiện tại, 0..360.
 * @param onHueChange gọi liên tục trong lúc kéo.
 * @param onHueChangeFinished gọi một lần khi thả tay.
 */
@Composable
fun HueSlider(
    hue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    thumb: ThumbStyle = ColorPickerDefaults.thumb(),
    trackThickness: Dp = ColorPickerDefaults.TrackThickness,
    onHueChangeFinished: ((Float) -> Unit)? = null,
) {
    val currentHue = hue.coerceIn(0f, 360f)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(ColorPickerDefaults.SliderHeight)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(currentHue, 0f..360f)
            }
            .sliderDrag(
                trackThickness = trackThickness,
                onFraction = { onHueChange(it * 360f) },
                onFinished = { onHueChangeFinished?.invoke(currentHue) },
            )
    ) {
        val thicknessPx = trackThickness.toPx()
        val geometry = trackGeometry(size, thicknessPx, thicknessPx)
        val half = geometry.thickness / 2f

        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = HueStops,
                startX = geometry.start,
                endX = geometry.end,
            ),
            topLeft = Offset(geometry.start - half, geometry.centerY - half),
            size = Size(geometry.end - geometry.start + geometry.thickness, geometry.thickness),
            cornerRadius = CornerRadius(half),
        )

        drawThumb(
            center = Offset(xForFraction(currentHue / 360f, geometry), geometry.centerY),
            radius = geometry.thickness,
            color = Color.hsv(currentHue, 1f, 1f),
            colors = colors,
            thumb = thumb,
        )
    }
}

@Preview(widthDp = 320, backgroundColor = 0xFFFFFFFF, showBackground = true)
@Composable
private fun HueSliderPreview() {
    HueSlider(hue = 200f, onHueChange = {})
}
```

- [ ] **Step 6: Chạy test, xác nhận pass**

Run: `./gradlew :colorpickerview-compose:connectedDebugAndroidTest`
Expected: PASS, 4 test.

- [ ] **Step 7: Xem `@Preview` bằng mắt**

Mở `HueSlider.kt` trong Android Studio, bật khung Preview. Xác nhận thấy một dải cầu vồng bo tròn với thumb tròn ở khoảng 55% chiều rộng. Nếu preview không dựng được, kiểm tra `debugImplementation(libs.androidx.compose.ui.tooling)` đã có trong build file chưa.

- [ ] **Step 8: Commit**

```bash
git add colorpickerview-compose/src/main/java/com/happytech/colorpickerview/compose/HueSlider.kt colorpickerview-compose/src/main/java/com/happytech/colorpickerview/compose/internal colorpickerview-compose/src/androidTest
git commit -m "feat(compose): add HueSlider with thumb and drag helpers"
```

---

### Task 5: `ColorAlphaSlider`

**Files:**
- Create: `colorpickerview-compose/src/main/java/com/happytech/colorpickerview/compose/internal/Checkerboard.kt`
- Create: `colorpickerview-compose/src/main/java/com/happytech/colorpickerview/compose/ColorAlphaSlider.kt`
- Test: `colorpickerview-compose/src/test/java/com/happytech/colorpickerview/compose/internal/CheckerboardTest.kt`
- Test: `colorpickerview-compose/src/androidTest/java/com/happytech/colorpickerview/compose/ColorAlphaSliderTest.kt`

**Interfaces:**
- Consumes: mọi thứ từ Task 1-4.
- Produces:
  - `internal fun checkerCellSizePx(thicknessPx: Float, rows: Int): Int`
  - `internal fun DrawScope.drawCheckerboard(topLeft: Offset, size: Size, cornerRadius: CornerRadius, cellSize: Int, light: Color, dark: Color)`
  - `@Composable fun ColorAlphaSlider(color: Color, alpha: Float, onAlphaChange: (Float) -> Unit, modifier: Modifier, colors: ColorPickerColors, thumb: ThumbStyle, trackThickness: Dp, showChecker: Boolean, checkerRows: Int, onAlphaChangeFinished: ((Float) -> Unit)?)`
  - `@Composable fun ColorAlphaSlider(state: ColorPickerState, modifier: Modifier, colors: ColorPickerColors, thumb: ThumbStyle, trackThickness: Dp, showChecker: Boolean, checkerRows: Int, onAlphaChangeFinished: ((Float) -> Unit)?)`

- [ ] **Step 1: Viết test thất bại cho kích thước ô**

Tạo `colorpickerview-compose/src/test/java/com/happytech/colorpickerview/compose/internal/CheckerboardTest.kt`:

```kotlin
package com.happytech.colorpickerview.compose.internal

import org.junit.Assert.assertEquals
import org.junit.Test

class CheckerboardTest {

    @Test
    fun `cells divide the thickness evenly when it is a clean multiple`() {
        assertEquals(12, checkerCellSizePx(thicknessPx = 36f, rows = 3))
    }

    @Test
    fun `cell size is rounded to a whole pixel`() {
        // 35 / 3 = 11.67 -> 12
        assertEquals(12, checkerCellSizePx(thicknessPx = 35f, rows = 3))
    }

    @Test
    fun `a very thin track still gets a one pixel cell`() {
        assertEquals(1, checkerCellSizePx(thicknessPx = 2f, rows = 8))
    }

    @Test
    fun `a row count below one is treated as one row`() {
        assertEquals(36, checkerCellSizePx(thicknessPx = 36f, rows = 0))
        assertEquals(36, checkerCellSizePx(thicknessPx = 36f, rows = -5))
    }
}
```

- [ ] **Step 2: Chạy test, xác nhận fail**

Run: `./gradlew :colorpickerview-compose:testDebugUnitTest --tests "*CheckerboardTest"`
Expected: FAIL, `Unresolved reference: checkerCellSizePx`.

- [ ] **Step 3: Viết `Checkerboard.kt`**

Tạo `colorpickerview-compose/src/main/java/com/happytech/colorpickerview/compose/internal/Checkerboard.kt`:

```kotlin
package com.happytech.colorpickerview.compose.internal

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Cạnh ô checkerboard, tính bằng pixel nguyên.
 *
 * Làm tròn về số nguyên là thứ giữ cho ô sắc nét — cạnh rơi vào nửa pixel sẽ bị nhoè.
 */
internal fun checkerCellSizePx(thicknessPx: Float, rows: Int): Int =
    (thicknessPx / rows.coerceAtLeast(1)).roundToInt().coerceAtLeast(1)

/**
 * Vẽ nền checkerboard trong một hình chữ nhật bo tròn.
 *
 * Vẽ từng ô trong `clipPath` thay vì dùng shader tile: không phụ thuộc chữ ký
 * `ImageShader` vốn đổi theo phiên bản Compose, và toạ độ ô là pixel nguyên nên sắc nét
 * tuyệt đối.
 */
internal fun DrawScope.drawCheckerboard(
    topLeft: Offset,
    size: Size,
    cornerRadius: CornerRadius,
    cellSize: Int,
    light: Color,
    dark: Color,
) {
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left = topLeft.x,
                top = topLeft.y,
                right = topLeft.x + size.width,
                bottom = topLeft.y + size.height,
                cornerRadius = cornerRadius,
            )
        )
    }

    clipPath(path) {
        drawRect(color = light, topLeft = topLeft, size = size)

        val cell = cellSize.toFloat()
        val columns = ceil(size.width / cell).toInt()
        val rows = ceil(size.height / cell).toInt()

        for (row in 0 until rows) {
            for (column in 0 until columns) {
                if ((row + column) % 2 != 0) continue

                drawRect(
                    color = dark,
                    topLeft = Offset(topLeft.x + column * cell, topLeft.y + row * cell),
                    size = Size(cell, cell),
                )
            }
        }
    }
}
```

- [ ] **Step 4: Chạy test, xác nhận pass**

Run: `./gradlew :colorpickerview-compose:testDebugUnitTest --tests "*CheckerboardTest"`
Expected: PASS, 4 test.

- [ ] **Step 5: Viết test thất bại cho composable**

Tạo `colorpickerview-compose/src/androidTest/java/com/happytech/colorpickerview/compose/ColorAlphaSliderTest.kt`:

```kotlin
package com.happytech.colorpickerview.compose

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
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
```

- [ ] **Step 6: Chạy test, xác nhận fail**

Run: `./gradlew :colorpickerview-compose:connectedDebugAndroidTest --tests "*ColorAlphaSliderTest"`
Expected: FAIL, `Unresolved reference: ColorAlphaSlider`.

- [ ] **Step 7: Viết `ColorAlphaSlider`**

Tạo `colorpickerview-compose/src/main/java/com/happytech/colorpickerview/compose/ColorAlphaSlider.kt`:

```kotlin
package com.happytech.colorpickerview.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.happytech.colorpickerview.compose.internal.checkerCellSizePx
import com.happytech.colorpickerview.compose.internal.drawCheckerboard
import com.happytech.colorpickerview.compose.internal.drawThumb
import com.happytech.colorpickerview.compose.internal.sliderDrag
import com.happytech.colorpickerview.compose.internal.trackGeometry
import com.happytech.colorpickerview.compose.internal.xForFraction

/**
 * Slider chọn alpha, dùng chung [state] với [ColorPicker] và [HueSlider].
 */
@Composable
fun ColorAlphaSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    thumb: ThumbStyle = ColorPickerDefaults.thumb(),
    trackThickness: Dp = ColorPickerDefaults.TrackThickness,
    showChecker: Boolean = true,
    checkerRows: Int = ColorPickerDefaults.CheckerRows,
    onAlphaChangeFinished: ((Float) -> Unit)? = null,
) {
    ColorAlphaSlider(
        color = state.opaqueColor,
        alpha = state.alpha,
        onAlphaChange = { state.alpha = it },
        modifier = modifier,
        colors = colors,
        thumb = thumb,
        trackThickness = trackThickness,
        showChecker = showChecker,
        checkerRows = checkerRows,
        onAlphaChangeFinished = onAlphaChangeFinished,
    )
}

/**
 * Slider chọn alpha, bản stateless.
 *
 * @param color màu đầy alpha, dùng dựng gradient nền.
 * @param alpha alpha hiện tại, 0..1.
 * @param showChecker có vẽ nền ô caro báo hiệu vùng trong suốt hay không.
 * @param checkerRows số hàng ô theo chiều dày thanh; cạnh ô suy ra từ đây nên ô luôn vuông.
 */
@Composable
fun ColorAlphaSlider(
    color: Color,
    alpha: Float,
    onAlphaChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    thumb: ThumbStyle = ColorPickerDefaults.thumb(),
    trackThickness: Dp = ColorPickerDefaults.TrackThickness,
    showChecker: Boolean = true,
    checkerRows: Int = ColorPickerDefaults.CheckerRows,
    onAlphaChangeFinished: ((Float) -> Unit)? = null,
) {
    val currentAlpha = alpha.coerceIn(0f, 1f)
    val opaqueColor = color.copy(alpha = 1f)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(ColorPickerDefaults.SliderHeight)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(currentAlpha, 0f..1f)
            }
            .sliderDrag(
                trackThickness = trackThickness,
                onFraction = onAlphaChange,
                onFinished = { onAlphaChangeFinished?.invoke(currentAlpha) },
            )
    ) {
        val thicknessPx = trackThickness.toPx()
        val geometry = trackGeometry(size, thicknessPx, thicknessPx)
        val half = geometry.thickness / 2f

        val trackTopLeft = Offset(geometry.start - half, geometry.centerY - half)
        val trackSize = Size(
            geometry.end - geometry.start + geometry.thickness,
            geometry.thickness,
        )
        val corner = CornerRadius(half)

        if (showChecker) {
            drawCheckerboard(
                topLeft = trackTopLeft,
                size = trackSize,
                cornerRadius = corner,
                cellSize = checkerCellSizePx(geometry.thickness, checkerRows),
                light = colors.checkerLight,
                dark = colors.checkerDark,
            )
        }

        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(opaqueColor.copy(alpha = 0f), opaqueColor),
                startX = geometry.start,
                endX = geometry.end,
            ),
            topLeft = trackTopLeft,
            size = trackSize,
            cornerRadius = corner,
        )

        drawThumb(
            center = Offset(xForFraction(currentAlpha, geometry), geometry.centerY),
            radius = geometry.thickness,
            color = opaqueColor.copy(alpha = currentAlpha),
            colors = colors,
            thumb = thumb,
        )
    }
}

@Preview(widthDp = 320, backgroundColor = 0xFFFFFFFF, showBackground = true)
@Composable
private fun ColorAlphaSliderPreview() {
    ColorAlphaSlider(color = Color.Red, alpha = 0.6f, onAlphaChange = {})
}
```

- [ ] **Step 8: Chạy test, xác nhận pass**

Run: `./gradlew :colorpickerview-compose:testDebugUnitTest :colorpickerview-compose:connectedDebugAndroidTest`
Expected: PASS, 30 unit test và 9 instrumented test.

- [ ] **Step 9: Xem `@Preview` bằng mắt**

Mở `ColorAlphaSlider.kt` trong Android Studio. Xác nhận: ô caro sắc nét, 3 hàng đều nhau theo chiều dày thanh, ô đầu tiên bắt đầu đúng ở mép trái thanh, gradient đỏ trong suốt → đỏ đặc chạy từ trái sang phải.

- [ ] **Step 10: Commit**

```bash
git add colorpickerview-compose/src/main/java/com/happytech/colorpickerview/compose/ColorAlphaSlider.kt colorpickerview-compose/src/main/java/com/happytech/colorpickerview/compose/internal/Checkerboard.kt colorpickerview-compose/src/test colorpickerview-compose/src/androidTest
git commit -m "feat(compose): add ColorAlphaSlider with crisp checkerboard"
```

---

### Task 6: `ColorPicker`

**Files:**
- Create: `colorpickerview-compose/src/main/java/com/happytech/colorpickerview/compose/ColorPicker.kt`
- Test: `colorpickerview-compose/src/androidTest/java/com/happytech/colorpickerview/compose/ColorPickerTest.kt`

**Interfaces:**
- Consumes: mọi thứ từ Task 1-5.
- Produces:
  - `@Composable fun ColorPicker(hue: Float, saturation: Float, value: Float, onChange: (saturation: Float, value: Float) -> Unit, modifier: Modifier, colors: ColorPickerColors, thumb: ThumbStyle, cornerRadius: Dp, outlineWidth: Dp, onColorChangeFinished: ((Color) -> Unit)?)`
  - `@Composable fun ColorPicker(state: ColorPickerState, modifier: Modifier, colors: ColorPickerColors, thumb: ThumbStyle, cornerRadius: Dp, outlineWidth: Dp, onColorChangeFinished: ((Color) -> Unit)?)`

- [ ] **Step 1: Viết test thất bại**

Tạo `colorpickerview-compose/src/androidTest/java/com/happytech/colorpickerview/compose/ColorPickerTest.kt`:

```kotlin
package com.happytech.colorpickerview.compose

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
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
```

- [ ] **Step 2: Chạy test, xác nhận fail**

Run: `./gradlew :colorpickerview-compose:connectedDebugAndroidTest --tests "*ColorPickerTest"`
Expected: FAIL, `Unresolved reference: ColorPicker`.

- [ ] **Step 3: Viết `ColorPicker`**

Tạo `colorpickerview-compose/src/main/java/com/happytech/colorpickerview/compose/ColorPicker.kt`:

```kotlin
package com.happytech.colorpickerview.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.happytech.colorpickerview.compose.internal.drawThumb
import com.happytech.colorpickerview.compose.internal.planeDrag

/**
 * Mặt phẳng chọn saturation (trái→phải) và value (trên→dưới), dùng chung [state] với
 * [HueSlider] và [ColorAlphaSlider].
 */
@Composable
fun ColorPicker(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    thumb: ThumbStyle = ColorPickerDefaults.thumb(),
    cornerRadius: Dp = ColorPickerDefaults.PickerCornerRadius,
    outlineWidth: Dp = ColorPickerDefaults.PickerOutlineWidth,
    onColorChangeFinished: ((Color) -> Unit)? = null,
) {
    ColorPicker(
        hue = state.hue,
        saturation = state.saturation,
        value = state.value,
        onChange = { saturation, value ->
            state.saturation = saturation
            state.value = value
        },
        modifier = modifier,
        colors = colors,
        thumb = thumb,
        cornerRadius = cornerRadius,
        outlineWidth = outlineWidth,
        onColorChangeFinished = onColorChangeFinished,
    )
}

/**
 * Mặt phẳng chọn saturation/value, bản stateless.
 *
 * Vùng vẽ thụt vào mỗi cạnh đúng bằng `thumb.radius` để thumb không bị cắt ở góc — khớp
 * cách bản XML tính `drawingStart`.
 *
 * @param hue hue nền, 0..360; composable này không đổi hue.
 * @param onChange gọi liên tục trong lúc kéo, với saturation và value đã clamp 0..1.
 */
@Composable
fun ColorPicker(
    hue: Float,
    saturation: Float,
    value: Float,
    onChange: (saturation: Float, value: Float) -> Unit,
    modifier: Modifier = Modifier,
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    thumb: ThumbStyle = ColorPickerDefaults.thumb(),
    cornerRadius: Dp = ColorPickerDefaults.PickerCornerRadius,
    outlineWidth: Dp = ColorPickerDefaults.PickerOutlineWidth,
    onColorChangeFinished: ((Color) -> Unit)? = null,
) {
    val currentHue = hue.coerceIn(0f, 360f)
    val currentSaturation = saturation.coerceIn(0f, 1f)
    val currentValue = value.coerceIn(0f, 1f)

    Canvas(
        modifier
            .fillMaxWidth()
            .planeDrag(
                inset = thumb.radius,
                onFraction = { fractionX, fractionY -> onChange(fractionX, 1f - fractionY) },
                onFinished = {
                    onColorChangeFinished?.invoke(
                        Color.hsv(currentHue, currentSaturation, currentValue)
                    )
                },
            )
    ) {
        val inset = thumb.radius.toPx()
        val planeTopLeft = Offset(inset, inset)
        val planeSize = Size(
            (size.width - inset * 2f).coerceAtLeast(0f),
            (size.height - inset * 2f).coerceAtLeast(0f),
        )
        val corner = CornerRadius(cornerRadius.toPx())

        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.White, Color.hsv(currentHue, 1f, 1f)),
                startX = planeTopLeft.x,
                endX = planeTopLeft.x + planeSize.width,
            ),
            topLeft = planeTopLeft,
            size = planeSize,
            cornerRadius = corner,
        )

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                startY = planeTopLeft.y,
                endY = planeTopLeft.y + planeSize.height,
            ),
            topLeft = planeTopLeft,
            size = planeSize,
            cornerRadius = corner,
        )

        // Bản XML thụt outline vào outlineSize / 2.5 và bo nhỏ hơn 0.5dp — giữ nguyên
        // để hai bản trông giống nhau.
        val outline = outlineWidth.toPx()
        val outlineInset = outline / 2.5f

        drawRoundRect(
            color = colors.pickerOutline,
            topLeft = Offset(planeTopLeft.x + outlineInset, planeTopLeft.y + outlineInset),
            size = Size(
                (planeSize.width - outlineInset * 2f).coerceAtLeast(0f),
                (planeSize.height - outlineInset * 2f).coerceAtLeast(0f),
            ),
            cornerRadius = CornerRadius((cornerRadius - 0.5.dp).toPx().coerceAtLeast(0f)),
            style = Stroke(outline),
        )

        drawThumb(
            center = Offset(
                planeTopLeft.x + planeSize.width * currentSaturation,
                planeTopLeft.y + planeSize.height * (1f - currentValue),
            ),
            radius = inset,
            color = Color.hsv(currentHue, currentSaturation, currentValue),
            colors = colors,
            thumb = thumb,
        )
    }
}

@Preview(widthDp = 320, heightDp = 240, backgroundColor = 0xFFFFFFFF, showBackground = true)
@Composable
private fun ColorPickerPreview() {
    ColorPicker(
        hue = 200f,
        saturation = 0.7f,
        value = 0.8f,
        onChange = { _, _ -> },
    )
}
```

Lưu ý: `ColorPicker` không tự đặt chiều cao — người dùng phải cho qua `modifier`, ví dụ `Modifier.height(240.dp)`. Đây là chủ ý: mặt phẳng có tỉ lệ tuỳ ý, khác với slider có chiều cao mặc định.

- [ ] **Step 4: Chạy toàn bộ test, xác nhận pass**

Run: `./gradlew :colorpickerview-compose:testDebugUnitTest :colorpickerview-compose:connectedDebugAndroidTest`
Expected: PASS, 30 unit test và 13 instrumented test.

- [ ] **Step 5: Xem `@Preview` bằng mắt**

Mở `ColorPicker.kt` trong Android Studio. Xác nhận: mặt phẳng bo góc, trắng ở trái, xanh dương ở phải, tối dần xuống dưới, viền mảnh rất nhạt, thumb tròn ở khoảng 70% ngang và 20% dọc.

- [ ] **Step 6: Commit**

```bash
git add colorpickerview-compose/src/main/java/com/happytech/colorpickerview/compose/ColorPicker.kt colorpickerview-compose/src/androidTest/java/com/happytech/colorpickerview/compose/ColorPickerTest.kt
git commit -m "feat(compose): add ColorPicker saturation/value plane"
```

---

### Task 7: Màn demo Compose trong sample app

**Files:**
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/happytech/colorpickerview/samples/ComposeSampleActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/happytech/colorpickerview/samples/MainActivity.kt`

**Interfaces:**
- Consumes: toàn bộ public API từ Task 1-6.
- Produces: không có API nào cho task sau.

- [ ] **Step 1: Bật Compose cho module app**

Trong `app/build.gradle.kts`, thêm vào block `plugins`:

```kotlin
alias(libs.plugins.kotlin.compose)
```

Trong block `buildFeatures` (đang có `viewBinding` và `buildConfig`), thêm:

```kotlin
compose = true
```

Trong block `dependencies`, thêm ngay trên dòng `implementation(project(":colorpickerview"))`:

```kotlin
implementation(platform(libs.androidx.compose.bom))
implementation(libs.androidx.compose.material3)
implementation(libs.androidx.activity.compose)
implementation(project(":colorpickerview-compose"))
```

- [ ] **Step 2: Kiểm tra build của app trước khi viết màn hình**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Viết màn demo Compose**

Tạo `app/src/main/java/com/happytech/colorpickerview/samples/ComposeSampleActivity.kt`:

```kotlin
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
```

- [ ] **Step 4: Khai báo activity trong manifest**

Trong `app/src/main/AndroidManifest.xml`, thêm ngay trước thẻ đóng `</application>`:

```xml
        <activity
            android:name=".ComposeSampleActivity"
            android:exported="false"
            android:label="@string/compose_sample_title" />
```

- [ ] **Step 5: Thêm chuỗi**

Trong `app/src/main/res/values/strings.xml`, thêm trong `<resources>`:

```xml
    <string name="compose_sample_title">Compose sample</string>
    <string name="open_compose_sample">Open Compose sample</string>
```

- [ ] **Step 6: Thêm nút mở màn Compose**

Trong `app/src/main/res/layout/activity_main.xml`, thêm ngay trước thẻ đóng `</androidx.constraintlayout.widget.ConstraintLayout>`:

```xml
    <Button
        android:id="@+id/openComposeSample"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="@dimen/_16sdp"
        android:text="@string/open_compose_sample"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/colorAlphaSlider" />
```

Trong `MainActivity.onCreate`, thêm sau hai dòng gán slider:

```kotlin
        binding.openComposeSample.setOnClickListener {
            startActivity(Intent(this, ComposeSampleActivity::class.java))
        }
```

Thêm import `android.content.Intent` ở đầu file.

- [ ] **Step 7: Build và chạy thử**

Run: `./gradlew :app:installDebug`
Expected: BUILD SUCCESSFUL.

Mở app trên máy, bấm "Open Compose sample". Kiểm bằng mắt, đối chiếu với màn XML ở màn trước:

- ô caro trên thanh alpha sắc nét, 3 hàng, ô đầu bắt đầu đúng mép trái
- thumb có đủ 4 vòng, cùng kích thước như bản XML
- kéo trên hue slider làm mặt phẳng picker đổi màu ngay
- kéo trên alpha slider làm ô xem trước mờ dần
- xoay màn hình: màu đang chọn giữ nguyên

- [ ] **Step 8: Commit**

```bash
git add app/
git commit -m "feat(sample): add Compose demo screen next to the XML one"
```

---

### Task 8: README

**Files:**
- Modify: `README.md`

**Interfaces:**
- Consumes: public API từ Task 1-6.
- Produces: không có.

- [ ] **Step 1: Xác nhận coordinate thật**

Đẩy branch lên GitHub, tạo tag `1.1.0`, rồi mở https://jitpack.io/#mihphu/ColorPickerView và bấm build tag đó.

Ghi lại chính xác hai chuỗi coordinate mà JitPack in ra trong log. Với dự án multi-module, dạng dự kiến là:

```
com.github.mihphu.ColorPickerView:colorpickerview:1.1.0
com.github.mihphu.ColorPickerView:colorpickerview-compose:1.1.0
```

Nếu log ra khác, dùng chuỗi trong log — đừng dùng chuỗi trên.

Nếu build fail ở `before_install` với lỗi `scripts/prepareJitpackEnvironment.sh: No such file`, đó là lỗi có sẵn nằm ngoài phạm vi plan này. Dừng lại và báo, đừng tự sửa `jitpack.yml`.

- [ ] **Step 2: Sửa mục dependency đang lệch**

Trong `README.md`, thay dòng:

```gradle
implementation ("com.github.phuhm-armob:ColorPickerView:last-version")
```

bằng hai dòng, dùng coordinate lấy được ở Step 1:

```gradle
// XML views — minSdk 18, không kéo theo Compose
implementation("com.github.mihphu.ColorPickerView:colorpickerview:1.1.0")

// Jetpack Compose — minSdk 21
implementation("com.github.mihphu.ColorPickerView:colorpickerview-compose:1.1.0")
```

Sửa badge ở đầu file từ `hominhphu20903` sang `mihphu`:

```markdown
[![](https://jitpack.io/v/mihphu/ColorPickerView.svg)](https://jitpack.io/#mihphu/ColorPickerView)
```

- [ ] **Step 3: Thêm mục Jetpack Compose**

Trong `README.md`, thêm ngay sau mục `📦 Usage` của bản XML:

````markdown
# 🧩 Jetpack Compose

Artifact `colorpickerview-compose` là bản viết lại native bằng Compose Canvas. Nó độc lập
hoàn toàn với artifact XML — thêm một trong hai, hoặc cả hai.

```kotlin
@Composable
fun ColorPickerScreen() {
    val state = rememberColorPickerState(Color.Red)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ColorPicker(state = state, modifier = Modifier.height(240.dp))
        HueSlider(state = state)
        ColorAlphaSlider(state = state)
    }

    // state.color là màu đang chọn, đã gồm alpha
}
```

Ba composable dùng chung một `ColorPickerState` nên tự đồng bộ với nhau, thay cho việc gán
`hueSliderView` / `alphaSliderView` bên bản XML. State sống qua xoay màn hình.

Muốn tự quản state thì dùng overload stateless:

```kotlin
var hue by remember { mutableFloatStateOf(30f) }

HueSlider(hue = hue, onHueChange = { hue = it })
```

## Tuỳ biến

```kotlin
ColorAlphaSlider(
    state = state,
    colors = ColorPickerDefaults.colors(checkerDark = Color.LightGray),
    thumb = ColorPickerDefaults.thumb(strokeSize = 4.dp),
    checkerRows = 4,
    showChecker = true,
)
```

## Khác biệt so với bản XML

- Độ dày thanh khai báo thẳng qua `trackThickness` thay vì suy ra từ chiều cao view.
- Một callback `onXxxChangeFinished` thay cho cặp listener changed/changeEnd.
- Dải hue dựng bằng gradient 7 mốc thay vì bitmap 360 px. Mắt thường không phân biệt được.
- `ColorPicker` không có chiều cao mặc định — cho qua `modifier`.
````

- [ ] **Step 4: Ghi rõ điều kiện của từng artifact**

Trong `README.md`, thêm ngay dưới mục `🚀 Features`:

```markdown
| Artifact | minSdk | Kéo theo Compose |
|---|---|---|
| `colorpickerview` | 18 | không |
| `colorpickerview-compose` | 21 | có |
```

- [ ] **Step 5: Đọc lại README bằng mắt**

Mở `README.md` ở chế độ preview. Xác nhận: không còn chuỗi `phuhm-armob` hay `hominhphu20903` nào, các khối code đóng mở đúng, bảng hiển thị được.

Run: `grep -n "phuhm-armob\|hominhphu20903" README.md`
Expected: không có dòng nào.

- [ ] **Step 6: Commit**

```bash
git add README.md
git commit -m "docs: document the Compose artifact and fix stale coordinates"
```

---

## Self-Review

**Spec coverage:**

| Mục spec | Task |
|---|---|
| §3 cấu trúc module, build, publishing | Task 1 |
| §4.1 state holder + Saver | Task 3 |
| §4.2 sáu composable | Task 4, 5, 6 |
| §4.3 styling defaults | Task 1 |
| §4.4 ba điểm khác XML | Task 4 (semantics, callback), Task 4-6 (trackThickness) |
| §5.1 vẽ ColorPicker | Task 6 |
| §5.2 gradient hue 7 mốc | Task 4 |
| §5.3 checkerboard | Task 5 |
| §5.4 xử lý chạm | Task 4 (`DragGesture.kt`) |
| §6 parity checklist | Kiểm bằng mắt ở Task 7 Step 7 |
| §7 test | Task 1-6 |
| §8 sample app | Task 7 |
| §9 tài liệu | Task 8 |
| §10 rủi ro jitpack | Task 8 Step 1 (dừng và báo, không tự sửa) |

Không có mục spec nào thiếu task.

**Placeholder scan:** không có "TBD", "TODO", "tương tự Task N", hay bước nào chỉ mô tả mà không có code.

**Type consistency:** đã đối chiếu các tên xuyên task — `ColorPickerColors` / `ThumbStyle` / `ColorPickerDefaults` (Task 1) dùng nguyên vẹn ở Task 4-6; `trackGeometry` / `fractionForX` / `xForFraction` / `TrackGeometry` (Task 2) khớp chữ ký lúc gọi trong `DragGesture.kt` và ba composable; `rgbToHsv` (Task 2) khớp cách dùng ở `ColorPickerState`; `drawThumb` / `sliderDrag` / `planeDrag` / `checkerCellSizePx` / `drawCheckerboard` khớp giữa nơi định nghĩa và nơi gọi; `state.opaqueColor` được định nghĩa ở Task 3 và dùng ở Task 5.
