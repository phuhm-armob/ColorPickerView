# Hỗ trợ Jetpack Compose cho ColorPickerView

Ngày: 2026-09-05
Repo: https://github.com/mihphu/ColorPickerView

## 1. Mục tiêu

Người dùng thư viện dựng UI bằng XML hay bằng Jetpack Compose đều dùng được, mỗi bên
theo cách tự nhiên của mình.

Phi mục tiêu:

- Không đổi bất kỳ hành vi nào của 3 View hiện có.
- Không hỗ trợ Compose Multiplatform ở lần này (chỉ Android).
- Không dựng hạ tầng screenshot test.

## 2. Các quyết định đã chốt

| Quyết định | Chọn | Lý do |
|---|---|---|
| Packaging | Module riêng `colorpickerview-compose` | `colorpickerview` giữ nguyên `minSdk 18`, consumer XML không bị kéo Compose runtime |
| Cách dựng | Viết lại native bằng Compose `Canvas` | Chạy được `@Preview`, không có interop overhead |
| Phụ thuộc giữa 2 module | Không có | Bản native không dùng chung dòng code vẽ nào; ép phụ thuộc sẽ buộc nâng helper `internal`/`protected` lên public, làm bẩn API bản XML |
| Quản lý state | `rememberColorPickerState()` + overload stateless | Thay cơ chế gán chéo `alphaSliderView`/`hueSliderView` bên XML |
| Styling | Gom vào `ColorPickerDefaults` | Giống `SliderDefaults.colors()` của Material3; thêm tuỳ chọn sau không phá signature |
| Maven coordinate | Để JitPack tự sinh | Bỏ hardcode, tránh lệch giữa README và build script về sau |

Cái giá đã chấp nhận của quyết định "viết lại native": thư viện có **hai bản code vẽ song
song**. Mọi thay đổi hành vi phải làm ở cả hai nơi. Mục 6 là công cụ chống lệch.

## 3. Cấu trúc module và build

```
settings.gradle.kts        + include(":colorpickerview-compose")
build.gradle.kts (root)    + alias(libs.plugins.kotlin.compose) apply false
colorpickerview-compose/
  build.gradle.kts
  src/main/java/com/happytech/colorpickerview/compose/
      ColorPickerState.kt
      ColorPickerDefaults.kt
      ColorPicker.kt
      HueSlider.kt
      ColorAlphaSlider.kt
      internal/Checkerboard.kt
      internal/Thumb.kt
      internal/DragGesture.kt
  src/test/java/...
  src/androidTest/java/...
```

`colorpickerview-compose/build.gradle.kts`:

- plugin: `com.android.library`, `org.jetbrains.kotlin.android`, `org.jetbrains.kotlin.plugin.compose`, `maven-publish`
- `namespace = "com.happytech.colorpickerview.compose"`
- `compileSdk = 35`, `minSdk = 21`
- `sourceCompatibility`/`targetCompatibility` = 11, `jvmTarget = "11"`
- `buildFeatures { compose = true }`
- `publishing { singleVariant("release") { withSourcesJar(); withJavadocJar() } }`

Dependency — phải là `api` chứ không `implementation`, vì chữ ký public phơi ra
`Modifier`, `Color`, `Dp`:

```kotlin
api(platform(libs.androidx.compose.bom))
api(libs.androidx.compose.ui)
api(libs.androidx.compose.foundation)
api(libs.androidx.compose.ui.graphics)
api(libs.androidx.compose.ui.tooling.preview)
debugImplementation(libs.androidx.compose.ui.tooling)
testImplementation(libs.junit)
androidTestImplementation(libs.androidx.compose.ui.test.junit4)
androidTestImplementation(libs.androidx.junit)
debugImplementation(libs.androidx.compose.ui.test.manifest)
```

Thêm vào `gradle/libs.versions.toml`: version `composeBom`; plugin `kotlin-compose`
(version.ref = `kotlin`, đang là 2.0.21); các library ở trên.

Publishing: cả hai module bỏ `groupId`/`artifactId`/`version` hardcode trong block
`publishing`, để publication kế thừa `project.group` / `project.name` /
`project.version` mà JitPack inject lúc build. Từ đây version của release **là git tag**,
không còn là một chuỗi trong build script; `1.1.0` nhắc tới trong tài liệu này chính là
tag dự kiến.

## 4. API surface

Package `com.happytech.colorpickerview.compose`.

### 4.1 State holder

```kotlin
@Stable
class ColorPickerState internal constructor(initialColor: Color) {
    var hue: Float          // 0f..360f
    var saturation: Float   // 0f..1f
    var value: Float        // 0f..1f
    var alpha: Float        // 0f..1f
    var color: Color        // get: dựng từ HSV + alpha; set: tách ngược ra 4 trường trên
}

@Composable
fun rememberColorPickerState(initialColor: Color = Color.Red): ColorPickerState
```

Bốn trường dùng `mutableFloatStateOf`. `rememberColorPickerState` dùng
`rememberSaveable` với một `Saver` lưu `floatArrayOf(hue, saturation, value, alpha)`,
để state sống qua xoay màn hình — giữ đúng tính năng `onSaveInstanceState` của bản XML.

Setter clamp giá trị vào miền hợp lệ thay vì ném exception. (Bản XML ném
`IllegalStateException` khi `hue` ngoài 0..360; trong Compose setter bị gọi lúc
recomposition nên ném exception là sai — chốt clamp.)

### 4.2 Composable

Bản state-holder:

```kotlin
@Composable fun ColorPicker(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    thumb: ThumbStyle = ColorPickerDefaults.thumb(),
    cornerRadius: Dp = ColorPickerDefaults.PickerCornerRadius,
    outlineWidth: Dp = ColorPickerDefaults.PickerOutlineWidth,
    onColorChangeFinished: ((Color) -> Unit)? = null,
)

@Composable fun HueSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    thumb: ThumbStyle = ColorPickerDefaults.thumb(),
    trackThickness: Dp = ColorPickerDefaults.TrackThickness,
    onHueChangeFinished: ((Float) -> Unit)? = null,
)

@Composable fun ColorAlphaSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    thumb: ThumbStyle = ColorPickerDefaults.thumb(),
    trackThickness: Dp = ColorPickerDefaults.TrackThickness,
    showChecker: Boolean = true,
    checkerRows: Int = ColorPickerDefaults.CheckerRows,
    onAlphaChangeFinished: ((Float) -> Unit)? = null,
)
```

Bản stateless (cùng tên, khác tham số đầu):

```kotlin
@Composable fun ColorPicker(
    hue: Float, saturation: Float, value: Float,
    onChange: (saturation: Float, value: Float) -> Unit, ...)

@Composable fun HueSlider(hue: Float, onHueChange: (Float) -> Unit, ...)

@Composable fun ColorAlphaSlider(
    color: Color, alpha: Float, onAlphaChange: (Float) -> Unit, ...)
```

Bản state-holder là lớp mỏng gọi thẳng xuống bản stateless, không chứa logic riêng —
nên hai bản không thể lệch nhau.

`ColorAlphaSlider` bản stateless nhận `color` là màu đầy alpha để dựng gradient nền;
`alpha` là vị trí thumb.

### 4.3 Styling

```kotlin
@Immutable class ColorPickerColors internal constructor(
    val thumbStroke: Color,
    val thumbOutline: Color,
    val pickerOutline: Color,
    val checkerLight: Color,
    val checkerDark: Color,
)

@Immutable class ThumbStyle internal constructor(
    val radius: Dp,       // chỉ dùng cho ColorPicker; slider lấy radius = trackThickness
    val strokeSize: Dp,
    val outlineSize: Dp,
)

object ColorPickerDefaults {
    @Composable fun colors(
        thumbStroke: Color = Color.White,
        thumbOutline: Color = Color.White,
        pickerOutline: Color = Color(0x0D000000),
        checkerLight: Color = Color.White,
        checkerDark: Color = Color(0xFFD7D7E1),
    ): ColorPickerColors

    @Composable fun thumb(
        radius: Dp = 12.dp,
        strokeSize: Dp = 2.dp,
        outlineSize: Dp = 1.dp,
    ): ThumbStyle

    val PickerCornerRadius: Dp = 8.dp
    val PickerOutlineWidth: Dp = 1.dp
    val TrackThickness: Dp = 12.dp
    val SliderHeight: Dp = 48.dp
    const val CheckerRows: Int = 3
}
```

### 4.4 Ba điểm cố ý khác bản XML

1. `trackThickness` là tham số tường minh. Bản XML suy độ dày thanh = ¼ chiều cao view,
   ngầm và khó đoán. Bản Compose khai báo thẳng rồi canh giữa theo chiều cao.
2. Một callback `onXxxChangeFinished` thay cho cặp `setOnXxxChangedListener` /
   `setOnXxxChangeEndListener`. Giá trị đang kéo đã phản ánh qua state, nên chỉ cần
   callback lúc thả tay — đúng kiểu `Slider` của Material3.
3. Hai slider có `Modifier.semantics { progressBarRangeInfo = ... }` để TalkBack đọc
   được. Bản XML không có; đây là mức sàn chất lượng, không tính là tính năng thêm.

Composable `ColorPicker` trùng tên với class `ColorPicker` bên XML. Khác package nên
không xung đột, trừ khi import cả hai vào cùng một file thì phải dùng `import ... as`.
Giữ trùng tên vì tính nhất quán đáng giá hơn.

## 5. Chi tiết vẽ

### 5.1 ColorPicker

Mặt phẳng SV thụt vào đúng bằng `thumb.radius` ở cả 4 cạnh (khớp
`drawingStart = paddingStart + circleIndicatorRadius` bên XML) để thumb không bị cắt.

| Bản XML | Bản Compose |
|---|---|
| `LinearGradient(WHITE → hueColor)` ngang | `Brush.horizontalGradient(Color.White, Color.hsv(hue, 1f, 1f))` |
| `LinearGradient(TRANSPARENT → BLACK)` dọc | `Brush.verticalGradient(Color.Transparent, Color.Black)` |
| `drawRoundRect` ×2 + outline stroke | `drawRoundRect` ×2 + `drawRoundRect(style = Stroke(outlineWidth))` |
| `ColorUtils.blendARGB(c, BLACK, 0.1f)` | `lerp(c, Color.Black, 0.1f)` |

Outline bo bán kính `cornerRadius - 0.5.dp`, khớp bản XML.

`cornerRadius` là `Dp` chứ không phải `Shape`: bản XML nhận `cpv_pickerBorderRadius`
dạng dimension, và outline cần trừ đi 0.5.dp từ chính bán kính đó — không rút được
con số ấy ra từ một `Shape` bất kỳ.

### 5.2 HueSlider

Bản XML decode `full_hue_bitmap.png` (360 px, mỗi pixel một độ hue) rồi kéo giãn.

Bản Compose dùng `Brush.horizontalGradient` với 7 mốc:
`Red → Yellow → Green → Cyan → Blue → Magenta → Red`.

Đây là khác biệt thật: nội suy tuyến tính sRGB giữa 7 mốc, không phải sweep HSV từng độ.
Mắt thường gần như không phân biệt được, đổi lại module compose không mang theo file PNG.
Chấp nhận có ý thức.

### 5.3 ColorAlphaSlider

```kotlin
// tile ImageBitmap 2×2 ô; cạnh ô làm tròn về pixel nguyên
val brush = ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
drawRoundRect(brush, cornerRadius = CornerRadius(thickness / 2f),
              filterQuality = FilterQuality.None)
drawRoundRect(Brush.horizontalGradient(color.copy(alpha = 0f), color),
              cornerRadius = CornerRadius(thickness / 2f))
```

Bản View phải dùng mẹo vẽ đè `drawLine` với `strokeCap` để bám hình viên thuốc; Compose
có `drawRoundRect` trực tiếp nên bỏ được mẹo đó. Hai yếu tố quyết định độ nét giữ nguyên:
cạnh ô làm tròn về **pixel nguyên** và `FilterQuality.None`.

Tile được `remember` theo `(cellSize, checkerLight, checkerDark)`, không dựng lại mỗi
lần recomposition.

### 5.4 Xử lý chạm

```kotlin
Modifier.pointerInput(trackGeometry) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        emit(down.position); down.consume()
        drag(down.id) { it.consume(); emit(it.position) }
        onChangeFinished(currentValue)
    }
}
```

Chạm phát là nhảy tới vị trí đó luôn, giống `ACTION_DOWN` bên XML. Giá trị được clamp
vào 0..1 trước khi phát ra.

## 6. Parity checklist

Rà lại danh sách này mỗi lần sửa hành vi ở một trong hai bản:

1. Track hình viên thuốc, độ dày = `trackThickness`.
2. Thumb = 4 vòng tròn đồng tâm; bán kính `r`, `r - outlineSize`,
   `r - outlineSize - strokeSize`, `r - outlineSize - strokeSize - 1.dp`;
   màu lần lượt `thumbOutline`, `thumbStroke`, `lerp(color, Black, 0.1f)`, `color`.
   Với slider, `r = trackThickness`; với picker, `r = thumb.radius`.
3. Mặt phẳng picker thụt vào bằng bán kính thumb; 2 gradient chồng; outline bo
   `cornerRadius - 0.5.dp`.
4. Checker: 3 hàng mặc định, cạnh ô pixel nguyên, `#FFFFFF` + `#D7D7E1`.
5. Hue 0→360 ánh xạ trái→phải.
6. State sống qua config change.

## 7. Test

**Unit test JVM** (`src/test`) cho `ColorPickerState`:

- round-trip `Color → HSV → Color` giữ nguyên màu trong sai số 1/255
- clamp biên: hue 0 và 360, saturation/value/alpha ở 0 và 1
- gán `color` rồi đọc lại 4 trường HSV cho ra đúng giá trị
- `Saver` save rồi restore ra state tương đương

`androidx.compose.ui.graphics.Color` là pure Kotlin nên chạy được trên JVM thuần. Nếu
lúc chạy dính stub `android.graphics`, thêm Robolectric — ghi vào plan, không giấu.

**Instrumented test** (`src/androidTest`) với `createComposeRule()`:

- kéo trên `HueSlider` tới giữa chiều rộng → `state.hue` ≈ 180 (sai số 5)
- `onHueChangeFinished` bắn đúng một lần khi thả tay, không bắn lúc đang kéo
- `ColorAlphaSlider` với `showChecker = false` không vẽ checker (kiểm qua state, không
  qua pixel)

**`@Preview`** cho cả 3 composable, dùng để nhìn bằng mắt trong Android Studio.

**Giới hạn đã biết:** parity từng pixel giữa hai bản không được test tự động. Kiểm bằng
mắt, đặt hai bản cạnh nhau trong sample app. Tự động hoá sẽ cần Paparazzi hoặc
Roborazzi — nằm ngoài phạm vi lần này.

## 8. Sample app

Thêm Compose vào module `app` (plugin `kotlin-compose`, `compose-bom`,
`activity-compose`), tạo `ComposeSampleActivity` hiển thị `ColorPicker` + `HueSlider` +
`ColorAlphaSlider` cùng một ô xem trước màu kết quả. `MainActivity` hiện tại thêm một nút
mở activity mới.

Không tách module `sample-compose` riêng: đặt hai bản cạnh nhau trong cùng một app là
cách duy nhất kiểm parity bằng mắt cho tử tế. Module `app` không publish nên phình ra
không ảnh hưởng người dùng thư viện.

## 9. Tài liệu

`README.md` thêm mục "Jetpack Compose": dòng dependency, snippet dùng
`rememberColorPickerState`, và ghi rõ artifact XML **không đổi gì** — vẫn `minSdk 18`,
không kéo Compose runtime.

Với JitPack multi-module và coordinate tự sinh, hai artifact sẽ có dạng:

```gradle
implementation("com.github.mihphu.ColorPickerView:colorpickerview:1.1.0")
implementation("com.github.mihphu.ColorPickerView:colorpickerview-compose:1.1.0")
```

Chú ý dấu chấm trước tên repo — đó là quy ước multi-module của JitPack, khác với dạng
một-module `com.github.mihphu:ColorPickerView`. Chuỗi chính xác vẫn phải đối chiếu với
build log của JitPack ở tag đầu tiên trước khi chốt vào README.

README hiện tại đang lệch ở hai chỗ và sẽ sửa cùng bước này:

- dòng dependency ghi `com.github.phuhm-armob:ColorPickerView`
- badge JitPack trỏ `hominhphu20903`

Owner hiện tại là `mihphu`.

## 10. Rủi ro và việc còn treo

| Việc | Trạng thái |
|---|---|
| `jitpack.yml` gọi `./scripts/prepareJitpackEnvironment.sh`, thư mục `scripts/` không tồn tại và chưa từng được commit → JitPack fail ở `before_install` | Lỗi có sẵn, ngoài phạm vi. **Chặn việc publish artifact thứ hai.** Phải xử lý trước khi release `1.1.0` |
| Coordinate thật do JitPack sinh ra chưa xác nhận bằng build log | Đối chiếu ở tag đầu, rồi mới chốt README |
| Hai bản code vẽ phải đồng bộ tay | Giảm thiểu bằng checklist mục 6 |
| Gradient hue 7 mốc khác bitmap 360 px | Chấp nhận có ý thức, mục 5.2 |
