package com.dotbhai.filex;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private TextView status;
    private TextView pathText;
    private File currentDir;
    private File selectedSource = null;
    private File selectedDest = null;
    
    private boolean isSelectingSource = false;
    private boolean isSelectingDest = false;
    private LinearLayout fileListLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. MINTA IZIN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        // 2. SETUP LAYOUT UTAMA (DARK MODE)
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#121212")); // Warna gelap
        rootLayout.setPadding(32, 64, 32, 32);

        // Header Text
        TextView title = new TextView(this);
        title.setText("FileX Manager");
        title.setTextColor(Color.parseColor("#4CAF50")); // Warna ijo
        title.setTextSize(24);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 32);
        rootLayout.addView(title);

        // Status Text
        status = new TextView(this);
        status.setText("Siap digunakan.");
        status.setTextColor(Color.LTGRAY);
        status.setTextSize(14);
        status.setPadding(0, 0, 0, 16);
        rootLayout.addView(status);

        // Path Text
        pathText = new TextView(this);
        pathText.setTextColor(Color.YELLOW);
        pathText.setTextSize(12);
        pathText.setPadding(0, 0, 0, 16);
        rootLayout.addView(pathText);

        // 3. TOMBOL-TOMBOL CARDVIEW
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setWeightSum(2f);

        buttonLayout.addView(createCardButton("1. SUMBER", v -> {
            isSelectingSource = true;
            isSelectingDest = false;
            currentDir = new File(Environment.getExternalStorageDirectory(), "Download"); // Mulai dari Download dulu
            showFileList();
        }));

        buttonLayout.addView(createCardButton("2. TUJUAN", v -> {
            isSelectingSource = false;
            isSelectingDest = true;
            currentDir = new File(Environment.getExternalStorageDirectory(), "Download");
            showFileList();
        }));

        rootLayout.addView(buttonLayout);

        // Tombol Copy Full Width
        CardView copyCard = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 150);
        params.setMargins(0, 16, 0, 16);
        copyCard.setLayoutParams(params);
        copyCard.setRadius(16f);
        copyCard.setCardBackgroundColor(Color.parseColor("#4CAF50"));

        Button btnCopy = new Button(this);
        btnCopy.setText("3. MULAI COPY");
        btnCopy.setTextColor(Color.WHITE);
        btnCopy.setTextSize(16);
        btnCopy.setTypeface(null, Typeface.BOLD);
        btnCopy.setBackgroundColor(Color.TRANSPARENT);
        btnCopy.setOnClickListener(v -> {
            if (selectedSource == null || selectedDest == null) {
                Toast.makeText(this, "Pilih Sumber & Tujuan dulu!", Toast.LENGTH_SHORT).show();
                return;
            }
            status.setText("Sedang menyalin...\n");
            status.setTextColor(Color.YELLOW);
            new Thread(() -> doCopy(selectedSource, selectedDest)).start();
        });
        copyCard.addView(btnCopy);
        rootLayout.addView(copyCard);

        // 4. SCROLL VIEW BUAT DAFTAR FILE
        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)); // Biar ngisi sisa layar

        fileListLayout = new LinearLayout(this);
        fileListLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(fileListLayout);

        rootLayout.addView(scrollView);
        setContentView(rootLayout);
    }

    // Fungsi bikin tombol kotak modern
    private CardView createCardButton(String text, View.OnClickListener listener) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, 150, 1f);
        params.setMargins(8, 0, 8, 0);
        card.setLayoutParams(params);
        card.setRadius(16f);
        card.setCardBackgroundColor(Color.parseColor("#333333"));

        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundColor(Color.TRANSPARENT);
        btn.setOnClickListener(listener);
        card.addView(btn);

        return card;
    }

    private void showFileList() {
        fileListLayout.removeAllViews();
        if (currentDir == null) return;
        
        pathText.setText("📁 " + currentDir.getAbsolutePath());

        // Tombol mundur
        Button btnUp = new Button(this);
        btnUp.setText("⬅️ Kembali");
        btnUp.setTextColor(Color.WHITE);
        btnUp.setBackgroundColor(Color.parseColor("#555555"));
        btnUp.setOnClickListener(v -> {
            if (currentDir.getParentFile() != null) {
                currentDir = currentDir.getParentFile();
                showFileList();
            }
        });
        fileListLayout.addView(btnUp);

        File[] files = currentDir.listFiles();
        
        if (files == null || files.length == 0) {
            TextView empty = new TextView(this);
            empty.setText("Folder kosong atau Sistem memblokir.");
            empty.setTextColor(Color.RED);
            empty.setPadding(16, 16, 16, 16);
            fileListLayout.addView(empty);
            return;
        }

        Arrays.sort(files, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory()) return -1;
            if (!f1.isDirectory() && f2.isDirectory()) return 1;
            return f1.getName().compareToIgnoreCase(f2.getName());
        });

        for (File file : files) {
            TextView fileText = new TextView(this);
            String icon = file.isDirectory() ? "📁 " : "📄 ";
            fileText.setText(icon + file.getName());
            fileText.setTextColor(file.isDirectory() ? Color.parseColor("#64B5F6") : Color.WHITE);
            fileText.setTextSize(16);
            fileText.setPadding(16, 24, 16, 24);
            
            // Garis pemisah
            fileText.setBackgroundColor(Color.parseColor("#1E1E1E"));
            
            fileText.setOnClickListener(v -> {
                if (file.isDirectory()) {
                    currentDir = file;
                    showFileList();
                } else {
                    if (isSelectingSource) {
                        selectedSource = file;
                        status.setText("Sumber: " + file.getName());
                    } else if (isSelectingDest) {
                        selectedDest = file;
                        status.setText("Tujuan: " + file.getName());
                    }
                    status.setTextColor(Color.GREEN);
                    fileListLayout.removeAllViews();
                }
            });
            
            // Spasi antar file
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            p.setMargins(0, 0, 0, 4);
            fileText.setLayoutParams(p);

            fileListLayout.addView(fileText);
        }
    }

    private void doCopy(File src, File dst) {
        try {
            copyFileOrDirectory(src, dst);
            runOnUiThread(() -> {
                status.setText("SUKSES COPY! Cek folder tujuan.");
                status.setTextColor(Color.GREEN);
            });
        } catch (Exception e) {
            runOnUiThread(() -> {
                status.setText("GAGAL: " + e.getMessage());
                status.setTextColor(Color.RED);
            });
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
