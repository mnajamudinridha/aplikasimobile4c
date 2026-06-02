# Aplikasi Mobile — Daftar Tugas (To-Do)

Aplikasi Android sederhana (Java) untuk mengelola daftar tugas dengan **CRUD penuh** ke
**Firebase Realtime Database** dan sinkronisasi real-time + dukungan offline.

> PRD lengkap: [`app/Docs/PRD.md`](app/Docs/PRD.md)

## Stack
- Android native, **Java 8**
- `compileSdk 32`, `minSdk 24`, AGP `7.2.1`
- Firebase Realtime Database (BoM `32.7.0`)
- RecyclerView + Material Components

## Arsitektur
```
ui/ (MainActivity, AddEditTaskActivity, TaskAdapter)
        │  observe / aksi
        ▼
data/TaskRepository  ──►  Firebase Realtime Database (node "tasks")
        ▲                         │
        └──── ValueEventListener (real-time) ◄┘
model/Task  — POJO yang dipetakan dari/ke node RTDB
```

## Setup Firebase (WAJIB sebelum menjalankan)

Proyek **tetap bisa di-compile tanpa** konfigurasi Firebase (plugin `google-services`
di-apply kondisional), tetapi **untuk menjalankan app** kamu perlu file konfigurasimu sendiri:

1. Buka [Firebase Console](https://console.firebase.google.com) → buat project.
2. **Add app → Android**, isi package name: `com.example.aplikasimobile`.
3. Unduh **`google-services.json`**, simpan di folder **`app/`**.
   - File ini sudah masuk `.gitignore` (jangan di-commit). Lihat template
     [`app/google-services.json.example`](app/google-services.json.example).
4. Di Console → **Build → Realtime Database → Create Database** (pilih lokasi).
5. Tab **Rules**, tempel isi [`database.rules.json`](database.rules.json) (mode dev, terbuka).
   > ⚠️ Rules dev membuat database terbuka. Hanya untuk belajar/dev — lihat PRD §8.2 untuk produksi.
6. Build & run dari Android Studio.

## Build dari CLI
```bash
./gradlew assembleDebug      # build APK debug (perlu google-services.json untuk run)
./gradlew test               # unit test
```

## Status Milestone (PRD §12)
- [x] **M0** — Setup Firebase, dependensi, offline persistence
- [x] **M1** — Read: daftar tugas real-time
- [ ] **M2** — Create: tambah tugas
- [ ] **M3** — Update & toggle selesai
- [ ] **M4** — Delete
- [ ] **M5** — Polish (sorting, warna, error handling)
