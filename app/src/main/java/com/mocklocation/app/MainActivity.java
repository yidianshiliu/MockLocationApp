package com.mocklocation.app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private LocationManager locationManager;
    private EditText etLatitude, etLongitude;
    private static final String MOCK_PROVIDER = "github_mock_gps";
    private static final int PERMISSION_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createDynamicUI();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        checkPermissions();
    }

    private void createDynamicUI() {
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(100, 100, 100, 100);

        // 标题
        TextView title = new TextView(this);
        title.setText("GitHub模拟定位工具 v1.0");
        title.setTextSize(24);
        title.setPadding(0, 0, 0, 50);
        mainLayout.addView(title);

        // 纬度输入
        TextView latLabel = new TextView(this);
        latLabel.setText("📍 纬度 (Latitude):");
        latLabel.setTextSize(18);
        latLabel.setPadding(0, 10, 0, 10);
        mainLayout.addView(latLabel);

        etLatitude = new EditText(this);
        etLatitude.setText("39.9042"); // 北京
        etLatitude.setTextSize(16);
        etLatitude.setPadding(30, 30, 30, 30);
        mainLayout.addView(etLatitude);

        // 经度输入
        TextView lngLabel = new TextView(this);
        lngLabel.setText("🌐 经度 (Longitude):");
        lngLabel.setTextSize(18);
        lngLabel.setPadding(0, 20, 0, 10);
        mainLayout.addView(lngLabel);

        etLongitude = new EditText(this);
        etLongitude.setText("116.4074"); // 北京
        etLongitude.setTextSize(16);
        etLongitude.setPadding(30, 30, 30, 30);
        mainLayout.addView(etLongitude);

        // 按钮容器
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setPadding(0, 40, 0, 0);

        Button startBtn = new Button(this);
        startBtn.setText("🚀 开始模拟");
        startBtn.setPadding(40, 30, 40, 30);
        startBtn.setOnClickListener(v -> startMockLocation());
        
        Button stopBtn = new Button(this);
        stopBtn.setText("🛑 停止模拟");
        stopBtn.setPadding(40, 30, 40, 30);
        stopBtn.setOnClickListener(v -> stopMockLocation());

        buttonLayout.addView(startBtn);
        buttonLayout.addView(stopBtn);
        mainLayout.addView(buttonLayout);

        // 状态显示
        TextView status = new TextView(this);
        status.setText("📱 状态: 等待操作...");
        status.setTextSize(14);
        status.setPadding(0, 40, 0, 0);
        mainLayout.addView(status);

        // 使用说明
        TextView instructions = new TextView(this);
        instructions.setText("使用说明：\n1. 先在手机开发者选项中开启'允许模拟位置'\n2. 输入经纬度坐标\n3. 点击开始模拟\n4. 在其他地图应用中测试");
        instructions.setTextSize(12);
        instructions.setPadding(0, 30, 0, 0);
        mainLayout.addView(instructions);

        setContentView(mainLayout);
    }

    private void startMockLocation() {
        // 检查模拟位置是否开启
        if (!isMockLocationEnabled()) {
            showMockLocationDialog();
            return;
        }

        // 检查位置权限
        if (!hasLocationPermission()) {
            requestLocationPermission();
            return;
        }

        try {
            double lat = Double.parseDouble(etLatitude.getText().toString());
            double lng = Double.parseDouble(etLongitude.getText().toString());
            
            // 设置模拟位置
            setupMockProvider();
            setMockLocation(lat, lng);
            
            showToast("✅ 模拟位置已设置: " + lat + ", " + lng);
        } catch (Exception e) {
            showToast("❌ 请输入有效的经纬度坐标");
        }
    }

    private void stopMockLocation() {
        try {
            locationManager.removeTestProvider(MOCK_PROVIDER);
            showToast("🛑 模拟定位已停止");
        } catch (Exception e) {
            // Provider可能不存在，忽略错误
        }
    }

    private void setupMockProvider() {
        try {
            // 清理旧的提供器
            try {
                locationManager.removeTestProvider(MOCK_PROVIDER);
            } catch (Exception e) {
                // 忽略错误
            }
            
            // 创建新的模拟位置提供器
            locationManager.addTestProvider(
                MOCK_PROVIDER,
                false, // requiresNetwork
                false, // requiresSatellite
                false, // requiresCell
                false, // hasMonetaryCost
                false, // supportsAltitude
                true,  // supportsSpeed
                false, // supportsBearing
                android.location.Criteria.POWER_LOW,
                android.location.Criteria.ACCURACY_FINE
            );
            
            locationManager.setTestProviderEnabled(MOCK_PROVIDER, true);
        } catch (SecurityException e) {
            showToast("❌ 权限不足，无法设置模拟位置");
        }
    }

    private void setMockLocation(double lat, double lng) {
        try {
            Location mockLocation = new Location(MOCK_PROVIDER);
            mockLocation.setLatitude(lat);
            mockLocation.setLongitude(lng);
            mockLocation.setAltitude(0);
            mockLocation.setTime(System.currentTimeMillis());
            mockLocation.setAccuracy(5.0f); // 5米精度
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            }
            
            locationManager.setTestProviderLocation(MOCK_PROVIDER, mockLocation);
        } catch (Exception e) {
            showToast("❌ 设置位置失败");
        }
    }

    private boolean isMockLocationEnabled() {
        return Settings.Secure.getInt(getContentResolver(), 
                Settings.Secure.ALLOW_MOCK_LOCATION, 0) != 0;
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, 
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_CODE);
    }

    private void checkPermissions() {
        if (!hasLocationPermission()) {
            requestLocationPermission();
        }
    }

    private void showMockLocationDialog() {
        new AlertDialog.Builder(this)
            .setTitle("🔧 需要开启模拟位置")
            .setMessage("请按以下步骤操作：\n\n1. 进入手机'设置' → '关于手机'\n2. 连续点击'版本号'7次开启开发者选项\n3. 返回设置 → 找到'开发者选项'\n4. 开启'允许模拟位置'功能\n5. 将本应用设为模拟位置应用")
            .setPositiveButton("去设置", (dialog, which) -> {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
            })
            .setNegativeButton("稍后", null)
            .show();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast("✅ 位置权限已获取");
            } else {
                showToast("❌ 需要位置权限才能使用模拟定位");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopMockLocation();
    }
}
