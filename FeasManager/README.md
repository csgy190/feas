# 调度管理器 (FeasManager)

针对红米Note 12 Turbo (骁龙7+ Gen 2) 的调度管理应用。

## 功能

- ✅ 检测模块安装状态 (王者荣耀优化 / FEAS模拟器)
- ✅ 检测守护进程运行状态
- ✅ 4档性能模式切换 (省电/均衡/性能/极速)
- ✅ 实时显示CPU频率和uclamp
- ✅ 自动刷新 (每3秒)
- ✅ 一键打开Magisk/Scene

## 编译

```bash
cd FeasManager
./gradlew assembleRelease
```

APK输出: `app/build/outputs/apk/release/app-release.apk`

## 使用

1. 安装APK
2. 授予Root权限
3. 先安装Magisk模块 (sgame_optimizer 或 feas_emulator)
4. 打开应用即可看到状态并切换档位

## 支持的模块

| 模块 | 包名 | 状态检测 | 档位切换 |
|------|------|---------|---------|
| 王者荣耀优化 | sgame_optimizer | ✓ | ✓ |
| FEAS模拟器 | feas_emulator | ✓ | ✓ |

## 技术栈

- 纯Java，无第三方库
- 系统API (android.app.Activity)
- Shell命令执行 (su)
- 最小SDK 26 (Android 8.0)
