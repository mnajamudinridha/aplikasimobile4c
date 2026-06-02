package com.example.aplikasimobile;

import android.app.Application;
import android.util.Log;

import com.google.firebase.database.FirebaseDatabase;

/**
 * Application class kustom.
 *
 * <p>Bertugas mengaktifkan <b>disk persistence</b> Firebase Realtime Database satu kali saat
 * proses aplikasi dibuat (sebelum operasi RTDB apa pun). Dengan persistence aktif, data
 * tugas tetap dapat dibaca/ditulis saat offline dan tersinkron otomatis ketika koneksi
 * kembali (PRD FR-6).</p>
 */
public class App extends Application {

    private static final String TAG = "App";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            // Wajib dipanggil SEKALI, sebelum getReference()/operasi RTDB lain.
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Exception e) {
            // Bisa terjadi bila google-services.json belum ditambahkan (FirebaseApp belum terinisialisasi)
            // atau database sudah terlanjur dipakai. Lihat README.md → Setup Firebase.
            Log.w(TAG, "Gagal mengaktifkan persistence RTDB. Sudah menambahkan google-services.json?", e);
        }
    }
}
