package com.dotbhai.filex;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.tananaev.adblib.AdbBase64;
import com.tananaev.adblib.AdbConnection;
import com.tananaev.adblib.AdbCrypto;
import com.tananaev.adblib.AdbStream;

import java.net.Socket;
import java.security.PublicKey;
import java.util.Base64;

public class MainActivity extends AppCompatActivity {

    private TextView status;
    private AdbConnection connection = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 96, 48, 48);

        status = new TextView(this);
        status.setText("Status: Belum terhubung ke ADB Lokal.\nPastikan Wireless Debugging udah nyala di Setelan HP lo.");
        status.setTextSize(16);
        status.setPadding(0, 0, 0, 48);

        Button btnConnect = new Button(this);
        btnConnect.setText("1. CONNECT KE ADB LOKAL (127.0.0.1:5555)");
        btnConnect.setOnClickListener(v -> {
            status.setText("Mencoba connect...");
            new Thread(this::connectAdb).start();
        });

        Button btnCopySave = new Button(this);
        btnCopySave.setText("2. COPY SAVE DARI ANDROID/DATA KE DOWNLOAD");
        btnCopySave.setOnClickListener(v -> {
            if (connection == null) {
                Toast.makeText(this, "Connect dulu, boss!", Toast.LENGTH_SHORT).show();
                return;
            }
            status.setText("Mengeksekusi perintah ADB...");
            new Thread(this::runAdbCommand).start();
        });

        layout.addView(status);
        layout.addView(btnConnect);
        layout.addView(btnCopySave);
        setContentView(layout);
    }

    private void connectAdb() {
        try {
            // Generate kunci ADB
            AdbCrypto crypto = AdbCrypto.generateAdbKeyPair(new AdbBase64() {
                @Override
                public String encodeToString(byte[] data) {
                    return Base64.getEncoder().encodeToString(data);
                }
            });

            // Connect ke localhost (Port default wireless debugging biasanya 5555 atau random)
            Socket socket = new Socket("127.0.0.1", 5555);
            connection = AdbConnection.create(socket, crypto);
            connection.connect();

            runOnUiThread(() -> status.setText("Status: TERHUBUNG KE ADB!\nSekarang lo bisa copy file save."));
        } catch (Exception e) {
            runOnUiThread(() -> status.setText("Gagal Connect: " + e.getMessage() + "\n\nCek lagi port Wireless Debugging di HP lo (Setelan > Developer Options > Wireless Debugging)."));
        }
    }

    private void runAdbCommand() {
        try {
            // Perintah buat nge-copy folder dari /Android/data ke /Download
            // GANTI "com.nama.game" sama package name game yang mau lo bobol!
            String packageName = "com.nama.game"; 
            String command = "cp -r /storage/emulated/0/Android/data/" + packageName + " /storage/emulated/0/Download/";
            
            AdbStream stream = connection.open("shell:" + command);
            
            // Baca hasil command (bisa kosong kalau sukses)
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = stream.read(buffer)) > 0) {
                String output = new String(buffer, 0, bytesRead);
                runOnUiThread(() -> status.append("\n" + output));
            }
            stream.close();
            runOnUiThread(() -> status.append("\n\nPROSES SUKSES! Cek folder Download."));
            
        } catch (Exception e) {
            runOnUiThread(() -> status.append("\nERROR: " + e.getMessage()));
        }
    }
                                           }
