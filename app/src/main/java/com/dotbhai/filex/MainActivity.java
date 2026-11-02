package com.dotbhai.filex;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private TextView status;
    private Uri sourceTree = null;
    private Uri destTree = null;

    private ActivityResultLauncher<Intent> pickSource;
    private ActivityResultLauncher<Intent> pickDest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // MINTA IZIN DEWA PAS APP DIBUKA
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }

        pickSource = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    try {
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception e) {}
                    sourceTree = uri;
                    updateStatus();
                }
            });

        pickDest = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    try {
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    } catch (Exception e) {}
                    destTree = uri;
                    updateStatus();
                }
            });

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 96, 48, 48);

        status = new TextView(this);
        status.setTextSize(14);
        status.setPadding(0, 0, 0, 48);
        updateStatus();

        Intent intent = getIntent();
        if (intent != null && Intent.ACTION_SEND.equals(intent.getAction())) {
            Uri sharedUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (sharedUri != null) sourceTree = sharedUri;
        }

        Button btnSrc = new Button(this);
        btnSrc.setText("1. Pilih SUMBER");
        btnSrc.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            pickSource.launch(i);
        });

        Button btnDst = new Button(this);
        btnDst.setText("2. Pilih TUJUAN");
        btnDst.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            pickDest.launch(i);
        });

        // TOMBOL AJAIB BUAT NEMBUS FOLDER PRIVATE
        Button btnForceCopy = new Button(this);
        btnForceCopy.setText("3. FORCE COPY (Android/Data)");
        btnForceCopy.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                Toast.makeText(this, "Izin All Files ditolak! Buka Setelan > All Files Access", Toast.LENGTH_LONG).show();
                return;
            }
            status.setText("Force Copying...\n");
            new Thread(this::doForceCopy).start();
        });

        layout.addView(status);
        layout.addView(btnSrc);
        layout.addView(btnDst);
        layout.addView(btnForceCopy);
        setContentView(layout);
    }

    private void updateStatus() {
        status.setText("Source: " + (sourceTree == null ? "belum" : sourceTree.getLastPathSegment()) +
                "\nDest: " + (destTree == null ? "belum" : destTree.getLastPathSegment()));
    }

    private void doForceCopy() {
        // Kita hardcode path-nya buat ngetes (Ganti sesuai kebutuhan)
        // Contoh: Ngebaca folder game, terus save ke Download
        File sourceFolder = new File(Environment.getExternalStorageDirectory(), "Android/data");
        File destFolder = new File(Environment.getExternalStorageDirectory(), "Download/FileX_Backup");

        if (!destFolder.exists()) destFolder.mkdirs();

        try {
            copyDirectory(sourceFolder, destFolder);
            runOnUiThread(() -> status.append("\nForce Copy SUKSES! Cek folder Download."));
        } catch (Exception e) {
            runOnUiThread(() -> status.append("\nGAGAL: " + e.getMessage()));
        }
    }

    private void copyDirectory(File source, File destination) throws Exception {
        if (source.isDirectory()) {
            if (!destination.exists()) destination.mkdirs();
            String[] children = source.list();
            if (children != null) {
                for (String child : children) {
                    copyDirectory(new File(source, child), new File(destination, child));
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
