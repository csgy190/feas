package com.xiaoai.feasmanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private TextView tvStatus, tvCpuInfo, tvMode, tvDaemonStatus;
    private RadioGroup rgMode;
    private RadioButton rbPowersave, rbBalance, rbPerformance, rbFast;
    private Button btnRefresh, btnInstallMagisk, btnSceneConfig;
    
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    private static final String[] MODES = {"powersave", "balance", "performance", "fast"};
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        checkRootAndRefresh();
    }
    
    private void initViews() {
        tvStatus = findViewById(R.id.tv_status);
        tvCpuInfo = findViewById(R.id.tv_cpu_info);
        tvMode = findViewById(R.id.tv_current_mode);
        tvDaemonStatus = findViewById(R.id.tv_daemon_status);
        
        rgMode = findViewById(R.id.rg_mode);
        rbPowersave = findViewById(R.id.rb_powersave);
        rbBalance = findViewById(R.id.rb_balance);
        rbPerformance = findViewById(R.id.rb_performance);
        rbFast = findViewById(R.id.rb_fast);
        
        btnRefresh = findViewById(R.id.btn_refresh);
        btnInstallMagisk = findViewById(R.id.btn_install_magisk);
        btnSceneConfig = findViewById(R.id.btn_scene_config);
        
        rgMode.setOnCheckedChangeListener((group, checkedId) -> {
            String mode = getModeFromId(checkedId);
            if (mode != null) {
                setMode(mode);
            }
        });
        
        btnRefresh.setOnClickListener(v -> refreshStatus());
        btnInstallMagisk.setOnClickListener(v -> openMagisk());
        btnSceneConfig.setOnClickListener(v -> openSceneSettings());
    }
    
    private String getModeFromId(int id) {
        if (id == R.id.rb_powersave) return "powersave";
        if (id == R.id.rb_balance) return "balance";
        if (id == R.id.rb_performance) return "performance";
        if (id == R.id.rb_fast) return "fast";
        return null;
    }
    
    private int getIdFromMode(String mode) {
        switch (mode) {
            case "powersave": return R.id.rb_powersave;
            case "balance": return R.id.rb_balance;
            case "performance": return R.id.rb_performance;
            case "fast": return R.id.rb_fast;
            default: return -1;
        }
    }
    
    private void checkRootAndRefresh() {
        executor.execute(() -> {
            boolean hasRoot = ShellUtils.hasRoot();
            mainHandler.post(() -> {
                if (!hasRoot) {
                    showRootDialog();
                } else {
                    refreshStatus();
                    startAutoRefresh();
                }
            });
        });
    }
    
    private void showRootDialog() {
        new AlertDialog.Builder(this)
            .setTitle("需要Root权限")
            .setMessage("本应用需要Root权限来管理CPU调度。请确保您的设备已Root，并授予本应用Root权限。")
            .setPositiveButton("去设置", (d, w) -> {
                try {
                    startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getPackageName())));
                } catch (Exception e) {
                    Toast.makeText(this, "无法打开设置", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("退出", (d, w) -> finish())
            .setCancelable(false)
            .show();
    }
    
    private void refreshStatus() {
        executor.execute(() -> {
            final StringBuilder sb = new StringBuilder();
            
            // 检查模块安装状态
            boolean hasSgameOpt = ShellUtils.fileExists("/data/adb/modules/sgame_optimizer/module.prop");
            boolean hasFeasEmu = ShellUtils.fileExists("/data/adb/modules/feas_emulator/module.prop");
            
            // 检查daemon运行状态
            String sgamePid = ShellUtils.exec("cat /data/sgame-opt/daemon.lock 2>/dev/null");
            String feasPid = ShellUtils.exec("cat /data/feas-emu/daemon.lock 2>/dev/null");
            
            boolean sgameRunning = !sgamePid.isEmpty() && ShellUtils.exec("kill -0 " + sgamePid + " 2>/dev/null && echo ok").equals("ok");
            boolean feasRunning = !feasPid.isEmpty() && ShellUtils.exec("kill -0 " + feasPid + " 2>/dev/null && echo ok").equals("ok");
            
            // 获取当前模式
            String currentMode = ShellUtils.exec("cat /data/feas-emu/current_mode 2>/dev/null");
            if (currentMode.isEmpty()) currentMode = "unknown";
            
            // 获取CPU信息
            String cpuGov = ShellUtils.exec("cat /sys/devices/system/cpu/cpufreq/policy0/scaling_governor 2>/dev/null");
            String cpuMin = ShellUtils.exec("cat /sys/devices/system/cpu/cpufreq/policy0/scaling_min_freq 2>/dev/null");
            String cpuMax = ShellUtils.exec("cat /sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq 2>/dev/null");
            String cpuCur = ShellUtils.exec("cat /sys/devices/system/cpu/cpufreq/policy0/scaling_cur_freq 2>/dev/null");
            
            // 获取uclamp
            String uclampTop = ShellUtils.exec("cat /dev/cpuctl/top-app/cpu.uclamp.min 2>/dev/null");
            
            sb.append("模块状态:\n");
            sb.append("  王者荣耀优化: ").append(hasSgameOpt ? "✓ 已安装" : "✗ 未安装").append("\n");
            sb.append("  FEAS模拟器: ").append(hasFeasEmu ? "✓ 已安装" : "✗ 未安装").append("\n\n");
            
            sb.append("守护进程:\n");
            sb.append("  王者优化: ").append(sgameRunning ? "✓ 运行中 (PID:" + sgamePid + ")" : "✗ 未运行").append("\n");
            sb.append("  FEAS模拟: ").append(feasRunning ? "✓ 运行中 (PID:" + feasPid + ")" : "✗ 未运行").append("\n\n");
            
            sb.append("CPU小核 (Policy0):\n");
            sb.append("  Governor: ").append(cpuGov).append("\n");
            sb.append("  频率: ").append(cpuCur).append(" / ").append(cpuMin).append(" - ").append(cpuMax).append(" Hz\n\n");
            
            sb.append("uclamp (top-app): ").append(uclampTop).append("%");
            
            final String status = sb.toString();
            final String mode = currentMode;
            final boolean sgameRun = sgameRunning;
            final boolean feasRun = feasRunning;
            
            mainHandler.post(() -> {
                tvStatus.setText(status);
                tvMode.setText("当前模式: " + mode.toUpperCase());
                
                String daemonStatus = "";
                if (sgameRun) daemonStatus += "王者优化 ✓ ";
                if (feasRun) daemonStatus += "FEAS模拟 ✓";
                if (!sgameRun && !feasRun) daemonStatus = "无守护运行";
                tvDaemonStatus.setText(daemonStatus);
                
                // 更新RadioButton选中状态
                int modeId = getIdFromMode(mode);
                if (modeId != -1) {
                    rgMode.check(modeId);
                }
            });
        });
    }
    
    private void setMode(String mode) {
        executor.execute(() -> {
            // 优先使用FEAS Emulator
            String result = ShellUtils.exec("su -c 'feas_emu mode " + mode + "'");
            
            // 如果失败，尝试直接写入模式请求文件
            if (result.contains("not found") || result.isEmpty()) {
                ShellUtils.exec("su -c 'echo " + mode + " > /data/feas-emu/mode_request'");
            }
            
            mainHandler.post(() -> {
                Toast.makeText(this, "已切换到: " + mode, Toast.LENGTH_SHORT).show();
                refreshStatus();
            });
        });
    }
    
    private void startAutoRefresh() {
        // 每3秒自动刷新一次
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshStatus();
                mainHandler.postDelayed(this, 3000);
            }
        }, 3000);
    }
    
    private void openMagisk() {
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.topjohnwu.magisk");
            if (intent != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "Magisk未安装", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "无法打开Magisk", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void openSceneSettings() {
        new AlertDialog.Builder(this)
            .setTitle("Scene配置")
            .setMessage("Scene调度配置文件已包含在模块中。\n\n安装后，在Scene的调度设置中选择:\n• taro.json (骁龙7+ Gen 2配置)\n\n即可使用Scene的档位切换功能。")
            .setPositiveButton("打开Scene", (d, w) -> {
                try {
                    Intent intent = getPackageManager().getLaunchIntentForPackage("com.omarea.vtools");
                    if (intent != null) {
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Scene未安装", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "无法打开Scene", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        mainHandler.removeCallbacksAndMessages(null);
    }
}
