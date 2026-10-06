# beta.9 自主回放验证

2026-10-06。目标是减少新手误操作和自动处理失误，不能据此认定成片达到摄影比赛获奖水平。

## 修改

- 自动曝光增加亮度直方图的 10%、50%、95% 分位：明亮天空与暗前景共存时不盲目增加曝光；大面积过曝仍降低曝光；暗景不强制变成中灰。
- 暗部和高光曲线保持黑白端点，亮度增益统一作用于 RGB，限制曲线新增通道过曝。风格矩阵本身仍可能改变颜色，不宣称完全无色偏。
- 风景建议只在横向边缘有充分支持、上方平滑明亮且下方有纹理、稳定一秒时显示。它是保守的图像边缘启发式，不能识别语义天空或评估艺术价值；无可靠证据时保留一般建议。
- 识别不可用不再作为“没有人脸”；未知人像类型不显示全身姿势卡。
- 发布包保留 Protobuf Lite 反射字段和 Flogger 调用栈相关代码，修复实际回放复现的模型初始化失败。仅保留 JNI 方法不足以解决该故障。
- 照片预览标题/返回/分享图标明确使用浅色，修复黑色背景下不可读；诊断记录先保存帧尺寸，避免访问 MediaPipe 已释放的 Bitmap。

## 样片与方法

1. Google MediaPipe 的 [portrait.jpg](https://storage.googleapis.com/mediapipe-assets/portrait.jpg) 与 [pose.jpg](https://storage.googleapis.com/mediapipe-assets/pose.jpg) 测试素材，仅作为本地测试输入，不随 APK 或仓库分发。
2. [Matterhorn 照片](https://commons.wikimedia.org/wiki/File:CH.VS.Zermatt_2021-10-17_Matterhorn_8726.jpg)，Roy Egloff，CC BY-SA 4.0。输入为 Commons 的 1280px 缩略图；本地调色输出属于修改版本，未公开分发。
3. 工具 `python3 tools/prepare-photo-replay.py` 下载到忽略的 `.local-validation/` 并转换 PPM。可选 JUnit 测试直接调用生产 SceneOptimizer / StyleLut，生成普通、暗景、局部压暗逆光共六组前后图和 JSON 指标；未准备素材时该测试跳过。
4. Android 官方模拟器 imagefile 相机输入走 CameraX、MediaPipe 与真实 APK 的拍摄流程。使用 API 35 AOSP ARM64、1080×1920、SwiftShader，测试压缩 release 包；模拟输入的视场裁切不能等同真机取景。

## 结果与范围

六组回放的新增通道截断比例均为零；平均每通道变化约 0–4.73/255。两个暗景保留 ORIGINAL；这验证处理克制，不验证美学评分。未模拟传感器改变 EV 后的重新曝光，JVM 调色计时也不代表手机性能。

压缩包运行中两种模型均记录 MODEL_READY；近景人像 FACES=1、RAW_PEOPLE=1、RELIABLE_PEOPLE=0，全身素材 RAW_PEOPLE=1、RELIABLE_PEOPLE=1。近景提示保留头顶空间，全身裁切提示后退，模型结果未用手工注入替代。

快门生成原片和调色副本，最近缩略图打开预览，可切换原片并继续拍摄；预览显示实际输出 1090×960（模拟器相机），不宣称实机分辨率。无 Xiaomi 17 实拍、真实弱光/运动/镜头光学验证；不支持闭眼筛选、计算 HDR 或多帧择优。

9 个新增确定性测试覆盖黑白端点、通道比例/透明度、饱和颜色保护、逆光高光、暗景与构图提示稳定/失效。完整 release 构建、173 项测试、lint 与模型包检查为本次验证依据。下载校验另见 DELIVERY-beta.9.md。
