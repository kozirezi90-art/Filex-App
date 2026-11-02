package com.dotbhai.filex;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private TextView status;
    private File currentDir;
    private File selectedSource = null;
    private File selectedDest = null;
    
    private boolean isSelectingSource = false;
    private boolean isSelectingDest = false;
    private LinearLayout fileListLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // MINTA IZIN DEWA
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(32, 64, 32, 32);

        status = new TextView(this);
        status.setText("Pilih SUMBER dan TUJUAN lewat tombol di bawah.");
        status.setTextSize(14);
        status.setPadding(0, 0, 0, 24);

        fileListLayout = new LinearLayout(this);
        fileListLayout.setOrientation(LinearLayout.VERTICAL);

        Button btnSrc = new Button(this);
        btnSrc.setText("1. PILIH SUMBER (Mulai dari Android/Data)");
        btnSrc.setOnClickListener(v -> {
            isSelectingSource = true;
            isSelectingDest = false;
            // LANGSUNG TEMBAK KE FOLDER ANDROID/DATA
            currentDir = new File(Environment.getExternalStorageDirectory(), "Android/data");
            showFileList();
        });

        Button btnDst = new Button(this);
        btnDst.setText("2. PILIH TUJUAN (Pilih Folder Download)");
        btnDst.setOnClickListener(v -> {
            isSelectingSource = false;
            isSelectingDest = true;
            currentDir = new File(Environment.getExternalStorageDirectory(), "Download");
            showFileList();
        });

        Button btnCopy = new Button(this);
        btnCopy.setText("3. MULAI COPY");
        btnCopy.setOnClickListener(v -> {
            if (selectedSource == null || selectedDest == null) {
                Toast.makeText(this, "Pilih Sumber & Tujuan dulu!", Toast.LENGTH_SHORT).show();
                return;
            }
            status.setText("Copying...\n");
            new Thread(() -> doCopy(selectedSource, selectedDest)).start();
        });

        mainLayout.addView(status);
        mainLayout.addView(btnSrc);
        mainLayout.addView(btnDst);
        mainLayout.addView(btnCopy);
        mainLayout.addView(fileListLayout);
        setContentView(mainLayout);
    }

    private void showFileList() {
        fileListLayout.removeAllViews();
        if (currentDir == null) return;
        
        status.setText("Lokasi: " + currentDir.getAbsolutePath());

        Button btnUp = new Button(this);
        btnUp.setText(".. (Kembali)");
        btnUp.setOnClickListener(v -> {
            if (currentDir.getParentFile() != null) {
                currentDir = currentDir.getParentFile();
                showFileList();
            }
        });
        fileListLayout.addView(btnUp);

        // PAKE FILE LIST TAPI DIPAKSA READ
        File[] files = currentDir.listFiles();
        
        if (files == null || files.length == 0) {
            Button btnEmpty = new Button(this);
            btnEmpty.setText("(Folder kosong / Sistem memblokir)");
            fileListLayout.addView(btnEmpty);
            return;
        }

        Arrays.sort(files, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
        for (File file : files) {
            Button btnFile = new Button(this);
            btnFile.setText(file.isDirectory() ? "[FOLDER] " + file.getName() : "[FILE] " + file.getName());
            btnFile.setOnClickListener(v -> {
                if (file.isDirectory()) {
                    currentDir = file;
                    showFileList(); // Masuk ke folder
                } else {
                    if (isSelectingSource) {
                        selectedSource = file;
                        status.setText("Sumber terpilih: " + file.getName());
                    } else if (isSelectingDest) {
                        selectedDest = file;
                        status.setText("Tujuan terpilih: " + file.getName());
                    }
                    fileListLayout.removeAllViews();
                }
            });
            fileListLayout.addView(btnFile);
        }
    }

    private void doCopy(File src, File dst) {
        try {
            copyFileOrDirectory(src, dst);
            runOnUiThread(() -> status.append("\nSUKSES COPY!"));
        } catch (Exception e) {
            runOnUiThread(() -> status.append("\nGAGAL: " + e.getMessage()));
        }
    }

    private void copyFileOrDirectory(File source, File destination) throws Exception {
        if (source.isDirectory()) {
            if (!destination.exists()) destination.mkdirs();
            String[] children = source.list();
            if (children != null) {
                for (String child : children) {
                    copyFileOrDirectory(new File(source, child), new File(destination, child));
                }
            }
        } else {
            InputStream in = new FileInputStream(source);
            OutputStream out = new FileOutputStream(destination);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in.close();
            out.close();
        }
    }
}
