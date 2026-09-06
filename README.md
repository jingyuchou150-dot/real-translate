# 实时翻译（离线字幕版）— 本地视频「日 → 中」字幕 App

一款安卓端的离线字幕生成工具：导入本地视频 → 用 **Vosk** 离线语音识别（ASR）在手机本地提取日语音频原文 → 翻译为中文 → 播放时叠加字幕，并支持导出 **SRT**。

> 与"在线字幕 App"的最大区别：**语音识别全程在手机本地完成，不依赖云端、不上传音视频、无需任何 API Key**。翻译为可选模块（默认开启，失败时自动降级为日文原文，不会中断字幕生成）。

---

## 〇、先说结论：这里为什么没有 `.apk`

本工程是在 **云端工作区** 里编写的，那里 **没有 JDK / Android SDK / Gradle**，且外网被限速（约 17KB/s）。因此我**无法在这里直接编译出 `实时翻译.apk` 二进制**。

我已经把工程做到「**拿到就能构建、装好即用**」的程度：

- 应用名固定为「**实时翻译**」，构建产物自动命名为 `实时翻译.apk`；
- 首次打开 App 会**自动下载日语识别模型**（约 48MB），你不需要手动放模型；
- 默认「翻译模式」开启，自动把日识别成中文字幕；
- 仓库已内置 **GitHub Actions 工作流**，你只需点一下就能在云端拿到 APK。

你只需从下面「**路径 A（Android Studio）**」或「**路径 B（GitHub Actions，零安装）**」任选其一，**构建一次**即可得到 `实时翻译.apk`。

---

## 一、功能特性

- **本地视频导入**：系统文件选择器选任意视频，无需存储权限。
- **离线语音识别**：集成 Vosk，日语音频在端侧识别，隐私友好（核心能力，**离线可用**）。
- **日 → 中 翻译**：默认开启，内置多个公共翻译实例兜底，失败时降级为日文原文。
- **带时间轴字幕**：识别结果按词级时间戳聚合成句，播放时精确同步。
- **SRT 导出**：一键导出标准字幕文件，可在其它剪辑软件复用。
- **多格式播放**：基于 Media3 ExoPlayer，兼容主流音视频封装。

## 二、整体架构

```
实时翻译(离线字幕版)
|-- MainActivity           选视频 / 翻译开关 -> 启动播放
|-- PlayerActivity         首次自动下载模型 + 后台识别 + 播放 + 字幕叠加 + 导出
|     |
|     |-- ModelDownloader       首次启动自动下载并解压日语模型(ja-0.22)
|     |-- AudioExtractor        MediaExtractor+MediaCodec 提取音轨 -> 16k 单声道 PCM
|     |       |-- Resampler     声道混合 + 线性插值重采样
|     |
|     |-- VoskRecognizerEngine   Vosk 流式识别 -> SubtitleCue 列表
|     |
|     |-- Translator(接口)
|     |       |-- LibreTranslateClient  在线翻译(内置多实例兜底, 失败降级原文)
|     |
|     |-- SrtWriter            生成/导出 SRT
|     |
|     +-- ExoPlayer            播放视频, 按 currentPosition 叠加当前字幕
```

数据流：`视频 Uri → AudioExtractor(解码/重采样) → Vosk(离线ASR, 日) → [翻译, 日→中] → SubtitleCue[] → 播放叠加 / SRT 导出`

## 三、环境要求

- **Android Studio** Hedgehog / Iguana 及以上（含 Android SDK 34、构建工具）。
- **JDK 17**（AGP 8.5 要求）。
- 一台 Android 7.0（API 24）及以上设备用于运行。
- 日语识别模型：**首次启动自动下载**，无需手动准备。

## 四、拿到 APK（两条路径，任选其一）

### 路径 A：Android Studio 本地构建（最直观）

1. 把本 `XSubtitle/` 目录下载到电脑，用 **Android Studio** 打开（首次会自动下载 Gradle 并生成 `gradle-wrapper.jar`）。
2. 配置 SDK 路径（File → Settings → Android SDK，确认已装 SDK Platform 34 + Build-Tools）。
3. 连上手机（开启 USB 调试）或启动模拟器。
4. 菜单 **Build → Build Bundle(s)/APK(s) → Build APK(s)**。
5. 完成后右下角弹出路径，产物即为 **`实时翻译.apk`**（位于 `app/build/outputs/apk/debug/实时翻译.apk`）。
6. 拷到手机安装即可（若提示“未知来源”，在系统设置里允许本次安装）。

> 想用命令行：用 Android Studio 打开一次工程会自动生成 `gradle-wrapper.jar`，之后即可 `./gradlew assembleDebug`；若本机已装 Gradle 8.9，也可直接 `gradle assembleDebug`。产物同名。

### 路径 B：GitHub Actions 云端构建（零安装，推荐给没有 Android Studio 的人）

1. 把本仓库 **推送到你自己的 GitHub 仓库**（含 `.github/workflows/build.yml`）。
2. 进仓库 **Actions** 标签页 → 选工作流 **Build 实时翻译 APK** → 点 **Run workflow**。
3. 等待约 3~6 分钟，构建完成后在 **Artifacts** 里下载 **`实时翻译.apk`**。
4. 下载后传到手机安装即可。

> 该工作流会自行安装 JDK 17 + Android SDK 34、缓存依赖，并产出名为 `实时翻译.apk` 的产物，全程无需你本机装任何东西。

## 五、傻瓜式使用流程（装好之后）

1. 在手机上安装 `实时翻译.apk` 并打开。
2. **首次打开**会弹进度「正在下载日语识别模型…」（约 48MB，需联网一次）；下载完自动进入主界面。之后离线也能识别。
3. 点「**选择视频**」挑一段本地日文视频。
4. 默认「**翻译模式（日→中，需联网）**」已开启 —— 保持即可。
5. 点「**生成字幕**」：后台解码音频 → 离线识别日文 → 翻译为中文 → 自动播放并叠加中文字幕。
6. 点右下角按钮「**导出 SRT**」可保存字幕文件。

```
装好 App → (首次)自动下模型 ~48MB → 选视频 → 生成字幕 → 看中文字幕 / 导出SRT
```

## 六、离线 vs 在线 说明（务必看清）

| 模块 | 是否离线 | 说明 |
|------|----------|------|
| 语音识别（日语音频 → 日文文本） | ✅ 完全离线 | Vosk 本地推理，识别模型首次下载后不再联网 |
| 翻译（日文文本 → 中文文本） | ❌ 需联网 | 走公共 LibreTranslate 实例；**失败时降级为日文原文**，字幕不中断 |

> 也就是说：**识别一定可用（离线）**；**中文翻译需要联网且依赖公共实例的稳定性**。如果你想要稳定、长期可用的「日→中」，强烈建议自建一个翻译实例（见第八节）。

## 七、已知限制 / 可优化点

- **识别为"预处理"式**：导入后整段识别完成再播放（非边播边识别），长视频需等待数十秒~数分钟；可改为边解码边显示 partial 结果以更"实时"。
- **重采样为简易线性插值**：极端采样率/声道下音质略损，可替换为 `libsamplerate`/`SSRC`。
- **翻译依赖公共实例**：公共实例可能限流/偶发不可用，导致仅显示日文原文；自建实例可根治（见下）。
- **断句策略简单**：以标点/词数断句，长静音段可能不自然，可结合能量/VAD 优化。

## 八、让「日→中」长期稳定（可选，自建翻译服务）

公共实例仅供开箱兜底。若要稳定翻译，用 Docker 在自己电脑/服务器跑一个 LibreTranslate：

```bash
docker run -d -p 5000:5000 libretranslate/libretranslate
```

然后在 `PlayerActivity.kt` 把翻译实现改成指向你自己的地址（需联网访问该地址）：

```kotlin
// 原：val tr: Translator = LibreTranslateClient()
// 改：
val tr: Translator = LibreTranslateClient(baseUrl = "http://你的IP:5000")
```

若实例启用了 API Key，传入 `apiKey = "xxxx"` 即可。

## 九、目录结构

```
XSubtitle/
|-- app/src/main/
|   |-- AndroidManifest.xml
|   |-- java/com/example/xsubtitle/
|   |   |-- MainActivity.kt
|   |   |-- PlayerActivity.kt
|   |   |-- ModelDownloader.kt          首次自动下载日语模型
|   |   |-- model/SubtitleCue.kt
|   |   |-- audio/AudioExtractor.kt, Resampler.kt
|   |   |-- asr/VoskRecognizerEngine.kt
|   |   |-- translate/Translator.kt, LibreTranslateClient.kt, NoTranslate.kt
|   |   +-- subtitle/SrtWriter.kt
|   +-- res/layout/activity_main.xml, activity_player.xml
+-- build.gradle.kts, settings.gradle.kts, gradle.properties
+-- gradle/wrapper/gradle-wrapper.properties
+-- .github/workflows/build.yml        GitHub Actions 构建 APK
```

## 十、依赖

- `com.alphacephei:vosk-android:0.3.47`（离线 ASR）
- `androidx.media3:media3-exoplayer` + `media3-ui`（播放）
- `com.squareup.okhttp3:okhttp`（在线翻译）
- `androidx.lifecycle:lifecycle-runtime-ktx`、`kotlinx-coroutines-android`

> 仓库已使用阿里云镜像（`settings.gradle.kts`）以规避 `maven.google.com` 不可达问题；若你在可直连 Google 的网络，可改回官方仓库。
