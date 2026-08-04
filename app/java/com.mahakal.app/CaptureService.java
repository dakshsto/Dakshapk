package com.mahakal.app;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;
import java.io.FileInputStream;
import java.util.HashSet;

public class CaptureService extends VpnService {
    private ParcelFileDescriptor vpnInterface;
    private static String detectedIP = "";
    private static int detectedPort = 0;
    private static boolean detected = false;
    private Thread captureThread;
    private static CaptureService instance;

    // Ignore karo in ports ko
    private static final HashSet<Integer> IGNORE_PORTS = new HashSet<Integer>();
    static {
        int[] ports = {8700, 20000, 443, 17500, 9031, 20002, 20001, 8080, 8086, 8011, 9030, 80, 53};
        for (int p : ports) IGNORE_PORTS.add(p);
    }

    // Static methods - doosri files se access karne ke liye
    public static String getDetectedIP() { return detectedIP; }
    public static int getDetectedPort() { return detectedPort; }
    public static boolean isDetected() { return detected; }
    public static void stopVPN() {
        if (instance != null) instance.stopVpn();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startVPN();
        return START_NOT_STICKY;
    }

    private void stopVpn() {
        if (captureThread != null) captureThread.interrupt();
        if (vpnInterface != null) {
            try { vpnInterface.close(); } catch (Exception e) {}
        }
        stopSelf();
    }

    private void startVPN() {
        try {
            Builder builder = new Builder();
            builder.setSession("MahakalCapture")
                   .addAddress("10.0.0.2", 32)
                   .addRoute("0.0.0.0", 0)
                   .setMtu(1500);
            vpnInterface = builder.establish();

            if (vpnInterface == null) {
                stopSelf();
                return;
            }

            Toast.makeText(this, "🔍 Scanning for target...", Toast.LENGTH_SHORT).show();

            captureThread = new Thread(() -> {
                try {
                    FileInputStream in = new FileInputStream(vpnInterface.getFileDescriptor());
                    byte[] packet = new byte[65535];

                    while (!Thread.interrupted() && !detected) {
                        int len = in.read(packet);
                        if (len > 28) {
                            int protocol = packet[9] & 0xFF;
                            if (protocol == 17) { // UDP protocol
                                int srcPort = ((packet[20] & 0xFF) << 8) | (packet[21] & 0xFF);
                                int dstPort = ((packet[22] & 0xFF) << 8) | (packet[23] & 0xFF);

                                int port = 0;
                                String ipStr = "";

                                // Destination port check
                                if (dstPort >= 10000 && dstPort <= 30000 && !IGNORE_PORTS.contains(dstPort)) {
                                    port = dstPort;
                                    int ip = ((packet[16] & 0xFF) << 24) | ((packet[17] & 0xFF) << 16) |
                                             ((packet[18] & 0xFF) << 8) | (packet[19] & 0xFF);
                                    ipStr = ((ip >> 24) & 0xFF) + "." + ((ip >> 16) & 0xFF) +
                                            "." + ((ip >> 8) & 0xFF) + "." + (ip & 0xFF);
                                }
                                // Source port check
                                else if (srcPort >= 10000 && srcPort <= 30000 && !IGNORE_PORTS.contains(srcPort)) {
                                    port = srcPort;
                                    int ip = ((packet[12] & 0xFF) << 24) | ((packet[13] & 0xFF) << 16) |
                                             ((packet[14] & 0xFF) << 8) | (packet[15] & 0xFF);
                                    ipStr = ((ip >> 24) & 0xFF) + "." + ((ip >> 16) & 0xFF) +
                                            "." + ((ip >> 8) & 0xFF) + "." + (ip & 0xFF);
                                }

                                if (port > 0 && !ipStr.isEmpty() && !detected) {
                                    detectedIP = ipStr;
                                    detectedPort = port;
                                    detected = true;
                                    stopVpn();
                                    return;
                                }
                            }
                        }
                    }
                } catch (Exception e) {}
            });
            captureThread.start();

            // 10 second timeout
            new android.os.Handler().postDelayed(() -> {
                if (!detected) {
                    Toast.makeText(CaptureService.this, "❌ No target found!", Toast.LENGTH_SHORT).show();
                    stopSelf();
                }
            }, 10000);

        } catch (Exception e) {
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        if (captureThread != null) captureThread.interrupt();
        if (vpnInterface != null) {
            try { vpnInterface.close(); } catch (Exception e) {}
        }
    }
}
