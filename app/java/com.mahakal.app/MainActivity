package com.mahakal.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    // Variables declare karo
    private EditText ipInput, portInput;
    private TextView timeDisplay, statusText;
    private SeekBar timeSeekBar;
    private Button startCapture, startAttack, stopAttack, captureFromVPN;
    private int selectedTime = 60;

    private static final int VPN_REQUEST_CODE = 100;
    private static final int OVERLAY_PERMISSION_REQUEST = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Layout ke elements ko connect karo
        ipInput = findViewById(R.id.ipInput);
        portInput = findViewById(R.id.portInput);
        timeDisplay = findViewById(R.id.timeDisplay);
        timeSeekBar = findViewById(R.id.timeSeekBar);
        startCapture = findViewById(R.id.startCapture);
        startAttack = findViewById(R.id.startAttack);
        stopAttack = findViewById(R.id.stopAttack);
        captureFromVPN = findViewById(R.id.captureFromVPN);
        statusText = findViewById(R.id.statusText);

        // Permissions check karo
        checkPermissions();

        // SeekBar setup - Time selector
        timeSeekBar.setMax(300);
        timeSeekBar.setMin(10);
        timeSeekBar.setProgress(60);
        timeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 10) progress = 10;
                selectedTime = progress;
                timeDisplay.setText(progress + "s");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Start VPN Capture button
        startCapture.setOnClickListener(v -> {
            if (!VpnService.prepare(MainActivity.this).equals(null)) {
                Intent intent = VpnService.prepare(MainActivity.this);
                startActivityForResult(intent, VPN_REQUEST_CODE);
            } else {
                startVPNAndFloatingWindow();
            }
        });

        // Capture from VPN button
        captureFromVPN.setOnClickListener(v -> {
            if (CaptureService.isDetected()) {
                String ip = CaptureService.getDetectedIP();
                int port = CaptureService.getDetectedPort();
                ipInput.setText(ip);
                portInput.setText(String.valueOf(port));
                statusText.setText("✅ Captured: " + ip + ":" + port);
                statusText.setTextColor(getColor(android.R.color.holo_green_dark));
                Toast.makeText(this, "✅ IP & Port Captured!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "❌ No target detected!", Toast.LENGTH_SHORT).show();
            }
        });

        // Start Attack button
        startAttack.setOnClickListener(v -> {
            String ip = ipInput.getText().toString().trim();
            String port = portInput.getText().toString().trim();

            if (ip.isEmpty()) {
                Toast.makeText(this, "⚠️ Enter Target IP", Toast.LENGTH_SHORT).show();
                return;
            }

            if (port.isEmpty()) {
                Toast.makeText(this, "⚠️ Enter Target Port", Toast.LENGTH_SHORT).show();
                return;
            }

            // Attack Service start karo
            Intent attackIntent = new Intent(this, AttackService.class);
            attackIntent.putExtra("ip", ip);
            attackIntent.putExtra("port", Integer.parseInt(port));
            attackIntent.putExtra("time", selectedTime);
            startService(attackIntent);

            statusText.setText("🚀 ATTACKING: " + ip + ":" + port);
            statusText.setTextColor(getColor(android.R.color.holo_red_dark));
            Toast.makeText(this, "🚀 Attack Started!", Toast.LENGTH_LONG).show();
        });

        // Stop Attack button
        stopAttack.setOnClickListener(v -> {
            stopService(new Intent(this, AttackService.class));
            statusText.setText("⏹️ Attack Stopped");
            statusText.setTextColor(getColor(android.R.color.darker_gray));
            Toast.makeText(this, "⏹️ Attack Stopped!", Toast.LENGTH_SHORT).show();
        });
    }

    private void startVPNAndFloatingWindow() {
        // VPN Service start
        Intent vpnIntent = new Intent(this, CaptureService.class);
        startService(vpnIntent);

        // Floating Window start
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                startFloatingWindow();
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST);
            }
        } else {
            startFloatingWindow();
        }
    }

    private void startFloatingWindow() {
        Intent floatingIntent = new Intent(this, FloatingWindowService.class);
        startService(floatingIntent);
        Toast.makeText(this, "✅ Floating Window Started!", Toast.LENGTH_SHORT).show();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.INTERNET}, 1);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            startVPNAndFloatingWindow();
        } else if (requestCode == OVERLAY_PERMISSION_REQUEST) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startFloatingWindow();
            } else {
                Toast.makeText(this, "❌ Overlay permission required!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
