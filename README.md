# 映刻相机 · AiPoseCamera

原生 Android 相机，当前版本 **0.1.0-beta.4**（versionCode 17）。使用 Kotlin、Jetpack Compose、CameraX 与本地 MediaPipe，目前针对小米 17 / ARM64 开发。正式 0.1.0 尚未发布。

## 功能

- 人像 / 风景模式，默认风景；本地人物与近景人脸检测、姿势参考和构图辅助。
- AI 构图点选主体，本地交互分割；倍率档位、弧形变焦刻度与双指变焦。
- 自然自动调色，以及富士、柯达、理光、徕卡、Agfa、黑白系列的 15 款相机 / 胶片色彩近似。
- Android 13+ 取景、缩略图和全尺寸调色副本共用色彩立方体；保留原片，不插值放大。
- HTTPS APK 二维码更新、主动导出脱敏诊断日志。

## 构建

需要 JDK 17、Android SDK 34；在本地 `local.properties` 设置 `sdk.dir`，或配置 Android SDK 环境变量。

```sh
./gradlew assembleRelease testReleaseUnitTest lintRelease
```

产物：`app/build/outputs/apk/release/app-release.apk`。默认仅打包 `arm64-v8a`，启用压缩；当前 beta 使用本机 debug 签名。不同开发者的 debug 签名不同，不能保证覆盖已有安装。

默认构建不含个人 API Key，人物识别、构图和调色均在本地完成。云端摄影建议需要自行配置服务。个人测试可在忽略的 `local.properties` 设置 `DEEPSEEK_API_KEY` 和 `QWEN_API_KEY`，并显式使用 `-PprivateBetaCredentials=true`；此类包不可公开分发。App 不提供密钥编辑入口。

## 验证与边界

当前 125 项单元测试通过，lint 无错误；尚未进行小米 17 真机验收。测试不代表已验证实际识别、GPU 预览、画质或安装效果。

风格为自编近似，并非原厂算法或授权滤镜；尚未进行同场景原厂样片校准。来源和实现见 [风格研究](docs/camera-style-references.md)。最高分辨率取决于设备向 CameraX 开放的能力，不承诺特定像素数。

日志导出入口：相机辅助面板或扫一扫右上角更多菜单。日志不含照片、密钥、二维码内容或模型回复，不会自动上传。

## 仓库内容

包含源码、测试、构建脚本和运行资源。安装包、临时下载地址、密钥、日志、个人诊断资料、设计参考截图与本地工具二进制不纳入版本控制。

第三方模型及依赖保留各自许可；相机品牌名仅用于说明色彩参考，不表示官方关联。未为项目自有代码额外选择开源许可。
