// Nangkep file kalau di-share dari app lain
Intent intent = getIntent();
if (intent != null && Intent.ACTION_SEND.equals(intent.getAction())) {
    Uri sharedUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
    if (sharedUri != null) {
        // Anggap file yang di-share ini sebagai sumber
        sourceTree = sharedUri; 
    }
}
