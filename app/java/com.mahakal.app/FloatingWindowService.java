package com.mahakal.app;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class FloatingWindowService extends Service {
    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    private Button startBtn, stopBtn;
    private TextView statusText;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Floating view banayein
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_window, null);
        startBtn = floatingView.findViewById(R.id.startBtn);
        stopBtn = floatingView.findViewById(R.id.stopBtn);
        statusText = floatingView.findViewById(R.id.statusText);

        // Window Manager setup
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        int layoutFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 200;

        // View add karo screen par
        windowManager.addView(floatingView, params);

        // Drag karne ke liye - move kar sakte ho
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                }
                return false;
            }
        });

        // Start Button
        startBtn.setOnClickListener(v -> {
            statusText.setText("🔴 ATTACKING");
            statusText.setTextColor(getColor(android.R.color.holo_red_dark));
            startBtn.setVisibility(View.GONE);
            stopBtn.setVisibility(View.VISIBLE);

            // Attack Service start
            Intent attackIntent = new Intent(this, AttackService.class);
            startService(attackIntent);
        });

        // Stop Button
        stopBtn.setOnClickListener(v -> {
            statusText.setText("⏹️ STOPPED");
            statusText.setTextColor(getColor(android.R.color.darker_gray));
            stopBtn.setVisibility(View.GONE);
            startBtn.setVisibility(View.VISIBLE);
            stopService(new Intent(this, AttackService.class));
            CaptureService.stopVPN();
        });

        // Check karo agar attack already chal raha hai toh
        if (AttackService.isRunning()) {
            statusText.setText("🔴 ATTACKING");
            statusText.setTextColor(getColor(android.R.color.holo_red_dark));
            startBtn.setVisibility(View.GONE);
            stopBtn.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) {
            windowManager.removeView(floatingView);
        }
    }
}
