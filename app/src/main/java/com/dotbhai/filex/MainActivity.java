package com.dotbhai.filex;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

public class MainActivity extends AppCompatActivity {

    private TextView status;
    private Uri sourceTree = null;
    private Uri destTree = null;

    private ActivityResultLauncher<Intent> pickSource;
    private ActivityResultLauncher<Intent> pickDest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pickSource = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    try {
                        getContentResolver().takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
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
                        getContentResolver().takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION |
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
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

        // Nangkep file kalau di-share dari app lain
        Intent intent = getIntent();
        if (intent != null && Intent.ACTION_SEND.equals(intent.getAction())) {
            Uri sharedUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (sharedUri != null) {
                sourceTree = sharedUri; 
            }
        }

        Button btnSrc = new Button(this);
        btnSrc.setText("1. Pilih SUMBER");
        btnSrc.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            // INI DIA TRIK CHOOSER NYA
            Intent chooser = Intent.createChooser(i, "Pilih File Manager");
            pickSource.launch(chooser);
        });

        Button btnDst = new Button(this);
        btnDst.setText("2. Pilih TUJUAN");
        btnDst.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            // INI DIA TRIK CHOOSER NYA
            Intent chooser = Intent.createChooser(i, "Pilih File Manager");
            pickDest.launch(chooser);
        });

        Button btnCopy = new Button(this);
        btnCopy.setText("3. COPY");
        btnCopy.setOnClickListener(v -> {
            if (sourceTree == null || destTree == null) {
                Toast.makeText(this, "Pilih source & dest dulu", Toast.LENGTH_SHORT).show();
                return;
            }
            status.setText("Copying...\n");
            new Thread(() -> doCopy(sourceTree, destTree)).start();
        });

        layout.addView(status);
        layout.addView(btnSrc);
        layout.addView(btnDst);
        layout.addView(btnCopy);
        setContentView(layout);
    }

    private void updateStatus() {
        status.setText("Source: " + (sourceTree == null ? "belum" : sourceTree.getLastPathSegment()) +
                "\nDest: " + (destTree == null ? "belum" : destTree.getLastPathSegment()));
    }

    private void doCopy(Uri src, Uri dst) {
        DocumentFile srcDoc = DocumentFile.fromTreeUri(this, src);
        DocumentFile dstDoc = DocumentFile.fromTreeUri(this, dst);
        
        // Fallback kalau yang di-share itu file satuan
        if (srcDoc == null && dstDoc != null) {
            try {
                DocumentFile out = dstDoc.createFile("application/octet-stream", "shared_file.dat");
                java.io.InputStream in = getContentResolver().openInputStream(src);
                java.io.OutputStream os = getContentResolver().openOutputStream(out.getUri());
                if (in != null && os != null) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) > 0) os.write(buf, 0, len);
                    in.close();
                    os.close();
                }
                runOnUiThread(() -> status.append("\nFile satuan berhasil di-copy."));
            } catch (Exception e) {
                runOnUiThread(() -> status.append("\nERROR: " + e.getMessage()));
            }
            return;
        }

        if (srcDoc == null || dstDoc == null) return;

        try {
            int n = copyRecursive(srcDoc, dstDoc);
            final int count = n;
            runOnUiThread(() -> status.append("\nSelesai. " + count + " file ke-copy."));
        } catch (Exception e) {
            runOnUiThread(() -> status.append("\nERROR: " + e.getMessage()));
        }
    }

    private int copyRecursive(DocumentFile src, DocumentFile dst) throws Exception {
        int n = 0;
        for (DocumentFile child : src.listFiles()) {
            if (child.isDirectory()) {
                DocumentFile newDir = dst.findFile(child.getName());
                if (newDir == null) newDir = dst.createDirectory(child.getName());
                if (newDir != null) n += copyRecursive(child, newDir);
            } else if (child.isFile()) {
                String name = child.getName();
                DocumentFile out = dst.findFile(name);
                if (out == null) out = dst.createFile(child.getType(), name);
                if (out != null) {
                    java.io.InputStream in = getContentResolver().openInputStream(child.getUri());
                    java.io.OutputStream os = getContentResolver().openOutputStream(out.getUri());
                    if (in != null && os != null) {
                        byte[] buf = new byte[8192];
                        int len;
                        while ((len = in.read(buf)) > 0) os.write(buf, 0, len);
                        in.close();
                        os.close();
                    }
                    n++;
                }
            }
        }
        return n;
    }
}
