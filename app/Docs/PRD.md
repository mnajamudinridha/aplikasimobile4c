# PRD — Aplikasi Daftar Tugas (To-Do) Android + Firebase Realtime Database

| | |
|---|---|
| **Nama Produk** | Aplikasi Mobile — Daftar Tugas (To-Do) |
| **Package** | `com.example.aplikasimobile` |
| **Platform** | Android (native) |
| **Bahasa** | Java |
| **Backend** | Firebase Realtime Database (RTDB) |
| **Versi PRD** | 1.0 |
| **Tanggal** | 2026-06-02 |
| **Status** | Draft — siap implementasi |
| **Pemilik** | M. Najamudin Ridha |

---

## 1. Ringkasan (Executive Summary)

Aplikasi **Daftar Tugas** adalah aplikasi Android sederhana untuk mencatat dan mengelola tugas harian. Pengguna dapat **menambah, melihat, mengubah, menandai selesai, dan menghapus** tugas. Seluruh data disimpan dan disinkronkan secara *real-time* melalui **Firebase Realtime Database**, sehingga perubahan langsung tercermin di aplikasi tanpa perlu refresh manual, dan tetap tersedia saat offline (cache lokal).

Tujuan utama proyek ini ada dua: (1) menghasilkan aplikasi To-Do yang fungsional, dan (2) menjadi **referensi pola CRUD Firebase RTDB di Android dengan Java** yang bersih dan dapat diikuti.

---

## 2. Latar Belakang & Tujuan

### 2.1 Latar Belakang
Banyak aplikasi produktivitas membutuhkan operasi data dasar (Create, Read, Update, Delete) yang tersinkron antar perangkat. Firebase RTDB menyediakan database JSON cloud yang menyinkronkan data secara real-time ke semua klien yang terhubung tanpa perlu membangun server backend sendiri.

### 2.2 Tujuan (Goals)
- **G1** — Pengguna dapat melakukan CRUD penuh atas tugas.
- **G2** — Data tersinkronisasi real-time via Firebase RTDB (perubahan tampil otomatis).
- **G3** — Aplikasi tetap dapat membaca & menulis saat offline, lalu sinkron saat online kembali.
- **G4** — Kode menjadi contoh pola CRUD Firebase RTDB + Android Java yang rapi.

### 2.3 Non-Tujuan (Non-Goals)
- Bukan aplikasi kolaborasi tim / berbagi tugas antar pengguna (MVP single-user).
- Tidak ada notifikasi push / reminder berbasis alarm (dipertimbangkan di fase lanjut).
- Tidak ada sinkronisasi dengan kalender eksternal.
- Bukan aplikasi multi-platform (hanya Android pada MVP).

### 2.4 Metrik Keberhasilan
| Metrik | Target |
|---|---|
| Operasi CRUD berhasil tersimpan ke RTDB | 100% pada kondisi online |
| Latensi perubahan tampil di UI setelah write | < 1 detik (online) |
| Crash-free sessions | > 99% |
| Data tetap dapat diakses saat offline | Ya (cache lokal aktif) |

---

## 3. Persona & Pengguna

**Persona utama — "Rina, mahasiswa sibuk"**
Ingin mencatat tugas kuliah dan pribadi dengan cepat, menandai prioritas, dan mencoret yang sudah selesai. Membuka aplikasi 5–10x sehari, kadang di area dengan sinyal buruk (butuh offline).

**Kebutuhan utama:** tambah tugas cepat, lihat daftar jelas (yang belum selesai di atas), tandai selesai dengan sekali ketuk, hapus yang tidak relevan.

---

## 4. Lingkup Fitur (Scope)

### 4.1 Dalam Lingkup (MVP)
1. **Create** — Tambah tugas baru (judul wajib, deskripsi, prioritas, tenggat opsional).
2. **Read** — Daftar semua tugas, terurut & ter-filter; tampil real-time.
3. **Update** — Edit detail tugas; toggle status selesai/belum.
4. **Delete** — Hapus satu tugas (dengan konfirmasi).
5. **Offline persistence** — Baca/tulis offline, sinkron otomatis saat online.
6. **State kosong & loading** — Tampilan saat belum ada tugas / sedang memuat.

### 4.2 Luar Lingkup (Future / Opsional)
- Autentikasi pengguna (lihat **§9 — Keputusan Desain Terbuka**).
- Kategori/label, sub-tugas, lampiran.
- Reminder/notifikasi & widget home screen.
- Pencarian teks & undo delete.

---

## 5. Functional Requirements (Kebutuhan Fungsional)

Notasi: **FR-x** = requirement, **AC** = acceptance criteria.

### FR-1 — Tambah Tugas (Create)
- Pengguna menekan tombol **+ (FAB)** → form input muncul.
- Field: **Judul** (wajib, 1–100 karakter), **Deskripsi** (opsional, ≤ 500 karakter), **Prioritas** (`low`/`medium`/`high`, default `medium`), **Tenggat/dueDate** (opsional).
- Saat disimpan, sistem membuat node baru via `push()` (key unik otomatis) dan menulis `setValue()`.
- **AC:**
  - [ ] Judul kosong → tampilkan error, tidak menyimpan.
  - [ ] Setelah simpan sukses, tugas langsung muncul di daftar (real-time).
  - [ ] `createdAt` & `updatedAt` terisi otomatis (epoch millis).

### FR-2 — Lihat Daftar Tugas (Read)
- Daftar ditampilkan dengan `RecyclerView`.
- Data dibaca via `addValueEventListener` (real-time) pada node `tasks`.
- Urutan default: tugas **belum selesai** di atas, lalu berdasarkan **prioritas** (high → low), lalu `createdAt` terbaru.
- **AC:**
  - [ ] Perubahan data di RTDB (dari device lain / konsol Firebase) tampil otomatis tanpa refresh.
  - [ ] Tiap item menampilkan judul, indikator prioritas (warna), status selesai (checkbox/strikethrough), dan tenggat bila ada.
  - [ ] State kosong tampil bila tidak ada tugas.

### FR-3 — Edit Tugas (Update)
- Ketuk item → buka form edit terisi data lama.
- Simpan → `updateChildren()` / `setValue()` pada node tugas tsb; `updatedAt` diperbarui.
- **AC:**
  - [ ] Perubahan tersimpan & tampil real-time.
  - [ ] Validasi judul sama seperti FR-1.

### FR-4 — Tandai Selesai / Belum (Toggle)
- Checkbox pada item membalik nilai `completed`.
- Hanya field `completed` & `updatedAt` yang ditulis (partial update via `updateChildren()`).
- **AC:**
  - [ ] Item selesai diberi gaya *strikethrough* dan/atau pindah ke bagian bawah daftar.
  - [ ] Toggle bekerja offline (tersimpan lokal, sinkron saat online).

### FR-5 — Hapus Tugas (Delete)
- Aksi hapus (swipe atau menu) → dialog konfirmasi → `removeValue()`.
- **AC:**
  - [ ] Konfirmasi muncul sebelum hapus.
  - [ ] Item hilang dari daftar real-time setelah dihapus.

### FR-6 — Offline Support
- `setPersistenceEnabled(true)` dipanggil **satu kali** sebelum operasi RTDB lain (di `Application.onCreate()`).
- **AC:**
  - [ ] Dalam mode pesawat, daftar tugas tetap tampil dari cache.
  - [ ] Operasi tulis offline antri dan tersinkron otomatis saat koneksi kembali.

### FR-7 — Umpan Balik & Error
- Loading indicator saat memuat awal.
- Pesan error pada `onCancelled()` (gagal baca) dan pada `CompletionListener`/`addOnFailureListener` (gagal tulis).
- **AC:**
  - [ ] Gagal tulis menampilkan `Toast`/`Snackbar`, bukan crash.

---

## 6. Model Data & Struktur Firebase RTDB

### 6.1 Prinsip Desain
Mengikuti **best practice Firebase**: *struktur data dibuat se-flat mungkin*. RTDB mengizinkan nesting hingga 32 level, tapi membaca sebuah node ikut menarik **seluruh anak** node tersebut. Maka semua tugas disimpan sebagai daftar datar di bawah satu node `tasks`, bukan dikelompokkan berlapis (mis. per-tanggal/kategori).

### 6.2 Skema JSON

```jsonc
{
  "tasks": {
    "-NpQ1aBcDeFgHiJk": {          // key unik dari push()
      "id": "-NpQ1aBcDeFgHiJk",    // disalin agar mudah saat hapus/edit dari objek
      "title": "Kerjakan laporan PKL",
      "description": "Bab 1–3, kirim ke pembimbing",
      "priority": "high",          // "low" | "medium" | "high"
      "completed": false,
      "dueDate": 1717459200000,    // epoch millis, nullable
      "createdAt": 1717286400000,  // epoch millis
      "updatedAt": 1717286400000   // epoch millis
    }
  }
}
```

### 6.3 POJO Model (Java)

```java
package com.example.aplikasimobile.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Task {
    public String id;
    public String title;
    public String description;
    public String priority = "medium"; // low | medium | high
    public boolean completed = false;
    public Long dueDate;               // nullable
    public long createdAt;
    public long updatedAt;

    // WAJIB: constructor kosong untuk deserialisasi Firebase
    public Task() {}

    public Task(String title, String description, String priority, Long dueDate) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.dueDate = dueDate;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // Dipakai untuk partial update via updateChildren()
    @Exclude
    public java.util.Map<String, Object> toMap() {
        java.util.HashMap<String, Object> result = new java.util.HashMap<>();
        result.put("title", title);
        result.put("description", description);
        result.put("priority", priority);
        result.put("completed", completed);
        result.put("dueDate", dueDate);
        result.put("updatedAt", updatedAt);
        return result;
    }
}
```

> **Catatan:** `getValue(Task.class)` pada `DataSnapshot` melakukan deserialisasi otomatis ke POJO. Karena itu constructor kosong wajib ada, dan field harus `public` (atau punya getter/setter).

---

## 7. Arsitektur & Detail Teknis

### 7.1 Gambaran Arsitektur
Pola sederhana **MVVM-ringan / Repository**:

```
UI (Activity/Fragment + RecyclerView)
        │  observe
        ▼
TaskRepository  ──►  FirebaseDatabase (DatabaseReference "tasks")
        ▲                     │
        └──── ValueEventListener (real-time stream) ◄──┘
```

- **`MainActivity`** — menampilkan daftar (`RecyclerView` + `TaskAdapter`), FAB tambah.
- **`AddEditTaskActivity`** — form create/update.
- **`TaskRepository`** — membungkus semua akses `DatabaseReference` (create/read/update/delete). Satu sumber kebenaran untuk operasi RTDB.
- **`TaskAdapter`** — `RecyclerView.Adapter` dengan `DiffUtil` untuk update efisien.

> **Insight arsitektur:** membungkus akses Firebase dalam `TaskRepository` (bukan memanggil `DatabaseReference` langsung dari Activity) membuat UI tidak bergantung pada Firebase → mudah diuji & ditukar sumber datanya nanti.

### 7.2 Inisialisasi (Application class)

```java
package com.example.aplikasimobile;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Harus dipanggil SEKALI, sebelum operasi RTDB apa pun.
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
```
Daftarkan di `AndroidManifest.xml`: `<application android:name=".App" ...>`.

### 7.3 Operasi CRUD (referensi implementasi)

**Referensi node:**
```java
DatabaseReference tasksRef = FirebaseDatabase.getInstance().getReference("tasks");
```

**CREATE — push key unik lalu setValue:**
```java
String key = tasksRef.push().getKey();
Task task = new Task(title, description, priority, dueDate);
task.id = key;
tasksRef.child(key).setValue(task)
        .addOnSuccessListener(unused -> { /* sukses */ })
        .addOnFailureListener(e -> { /* tampilkan error */ });
```

**READ — real-time via ValueEventListener:**
```java
tasksRef.addValueEventListener(new ValueEventListener() {
    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
        List<Task> list = new ArrayList<>();
        for (DataSnapshot child : snapshot.getChildren()) {
            Task t = child.getValue(Task.class);
            if (t != null) list.add(t);
        }
        // sort: belum selesai dulu, lalu prioritas, lalu createdAt desc
        adapter.submitList(list);
    }
    @Override public void onCancelled(@NonNull DatabaseError error) {
        Log.w(TAG, "Gagal membaca tasks", error.toException());
    }
});
```

**UPDATE penuh / partial:**
```java
// Partial update (mis. toggle completed) — hemat & atomik
Map<String, Object> updates = new HashMap<>();
updates.put("completed", newValue);
updates.put("updatedAt", System.currentTimeMillis());
tasksRef.child(taskId).updateChildren(updates);
```

**DELETE:**
```java
tasksRef.child(taskId).removeValue()
        .addOnFailureListener(e -> { /* error */ });
```

**Sorting/Filter dengan Query (opsional di sisi server):**
```java
// Contoh: urutkan berdasarkan prioritas di sisi RTDB
Query byPriority = tasksRef.orderByChild("priority");
```
> Karena dataset MVP kecil, **sorting dapat dilakukan di sisi klien** setelah membaca. `orderByChild` server-side berguna saat data besar dan butuh indeks (`.indexOn`).

### 7.4 Dependensi & Setup Build

**Project-level `build.gradle`** — tambahkan plugin Google Services:
```groovy
plugins {
    id 'com.android.application' version '7.2.1' apply false
    id 'com.android.library' version '7.2.1' apply false
    id 'com.google.gms.google-services' version '4.3.15' apply false
}
```

**App-level `app/build.gradle`:**
```groovy
plugins {
    id 'com.android.application'
    id 'com.google.gms.google-services'
}

dependencies {
    // Firebase BoM mengelola versi antar-library secara konsisten
    implementation platform('com.google.firebase:firebase-bom:32.7.0')
    implementation 'com.google.firebase:firebase-database'

    // RecyclerView untuk daftar
    implementation 'androidx.recyclerview:recyclerview:1.2.1'

    implementation 'androidx.appcompat:appcompat:1.3.0'
    implementation 'com.google.android.material:material:1.4.0'
}
```

> **Catatan kompatibilitas:** proyek saat ini `compileSdk 32`, `minSdk 24`, AGP `7.2.1`. Firebase BoM `32.7.0` & google-services `4.3.15` kompatibel dengan konfigurasi ini. Sesuaikan ke versi stabil terbaru yang kompatibel saat implementasi.

**Langkah konsol Firebase (manual, satu kali):**
1. Buat project di [Firebase Console](https://console.firebase.google.com).
2. Tambah app Android dengan package `com.example.aplikasimobile`.
3. Unduh **`google-services.json`** → letakkan di folder `app/`.
4. Buat **Realtime Database** → pilih lokasi → mulai dengan **rules** (lihat §8).
5. Pastikan `google-services.json` masuk `.gitignore` (jangan commit kredensial).

---

## 8. Keamanan (Security Rules)

### 8.1 MVP (Mode Tanpa Login) — HANYA untuk pengembangan
```json
{
  "rules": {
    "tasks": {
      ".read": true,
      ".write": true
    }
  }
}
```
> ⚠️ **Peringatan:** rule ini membuat database terbuka untuk siapa pun. **Hanya** untuk fase belajar/dev. Jangan publikasikan aplikasi dengan rule ini.

### 8.2 Rekomendasi Produksi (dengan Auth — fase lanjut)
Jika autentikasi diaktifkan, data dipisah per pengguna `tasks/{uid}` dan diamankan:
```json
{
  "rules": {
    "tasks": {
      "$uid": {
        ".read": "$uid === auth.uid",
        ".write": "$uid === auth.uid"
      }
    }
  }
}
```

### 8.3 Validasi Data (opsional, server-side)
```json
".validate": "newData.hasChildren(['title','createdAt'])"
```

---

## 9. Keputusan Desain Terbuka (Butuh Input)

Beberapa keputusan memengaruhi arsitektur namun tidak wajib untuk MVP. **Default MVP** dipilih agar bisa langsung jalan; ubah bila Anda punya preferensi:

| # | Keputusan | Default MVP | Implikasi bila diubah |
|---|---|---|---|
| D1 | **Autentikasi pengguna** | Tanpa login (data publik `tasks/`) | Bila pakai Firebase Auth → struktur jadi `tasks/{uid}`, rules ketat, butuh layar login |
| D2 | **Lokasi sorting/filter** | Sisi klien (data kecil) | Bila server-side → perlu `orderByChild` + `.indexOn` di rules |
| D3 | **Hapus** | Hard delete (`removeValue`) | Bila soft delete → tambah field `deletedAt`, filter di query |
| D4 | **Undo delete** | Tidak ada | Bila ada → simpan sementara objek terhapus untuk restore |

> Bila tidak ada arahan, implementasi akan mengikuti kolom **Default MVP**.

---

## 10. Kebutuhan Non-Fungsional (NFR)

| Kode | Kebutuhan |
|---|---|
| NFR-1 | **Performa** — daftar ≤ 500 item ter-render mulus (gunakan `DiffUtil`). |
| NFR-2 | **Offline-first** — disk persistence aktif; UI tidak blank saat offline. |
| NFR-3 | **Reliabilitas** — semua write punya error handler; tidak ada crash pada kegagalan jaringan. |
| NFR-4 | **Keamanan** — `google-services.json` tidak di-commit; rules produksi tidak `true/true`. |
| NFR-5 | **Kompatibilitas** — berjalan di Android 7.0 (API 24) ke atas. |
| NFR-6 | **Keterbacaan kode** — akses Firebase terisolasi di `TaskRepository`. |
| NFR-7 | **Lifecycle** — listener di-`removeEventListener` saat `onStop()`/`onDestroy()` untuk cegah memory leak. |

---

## 11. UI / Layar (UX)

| Layar | Komponen Utama | Aksi |
|---|---|---|
| **Daftar Tugas** (`MainActivity`) | `RecyclerView`, item card (judul, prioritas, checkbox, tenggat), `FloatingActionButton` (+), empty-state | Toggle selesai, ketuk→edit, swipe→hapus |
| **Tambah/Edit** (`AddEditTaskActivity`) | `TextInputLayout` judul & deskripsi, dropdown prioritas, date picker tenggat, tombol Simpan | Validasi + simpan |
| **Dialog Konfirmasi Hapus** | `AlertDialog` | Hapus / Batal |

**Indikator prioritas (warna):** `high` = merah, `medium` = oranye, `low` = abu/hijau.
**Status selesai:** checkbox tercentang + teks *strikethrough*, item turun ke bagian bawah.

---

## 12. Rencana Implementasi (Milestones)

| Fase | Cakupan | Output |
|---|---|---|
| **M0 — Setup** | Buat project Firebase, tambah `google-services.json`, dependensi, `setPersistenceEnabled`. | App build & terhubung RTDB. |
| **M1 — Read** | `Task` POJO, `TaskRepository.observeTasks()`, `RecyclerView` + adapter, empty/loading state. | Daftar real-time tampil. |
| **M2 — Create** | Form tambah, validasi, `push().setValue()`. | Bisa menambah tugas. |
| **M3 — Update & Toggle** | Form edit, `updateChildren()`, toggle `completed`. | Bisa mengubah & menandai selesai. |
| **M4 — Delete** | Swipe + konfirmasi + `removeValue()`. | Bisa menghapus. |
| **M5 — Polish** | Sorting, warna prioritas, error handling, uji offline, cleanup listener. | MVP siap. |

---

## 13. Kriteria Penerimaan Global (Definition of Done)

- [ ] Semua **FR-1…FR-7** terpenuhi beserta AC-nya.
- [ ] CRUD tervalidasi melalui Firebase Console (data benar-benar tersimpan/terubah/terhapus).
- [ ] Aplikasi diuji pada kondisi **online & offline** (mode pesawat).
- [ ] Tidak ada crash pada skenario gagal jaringan.
- [ ] `google-services.json` di `.gitignore`; rules dev tidak ikut ke produksi.
- [ ] Listener dilepas di lifecycle yang tepat (tidak ada leak).

---

## 14. Risiko & Mitigasi

| Risiko | Dampak | Mitigasi |
|---|---|---|
| Rules `true/true` bocor ke publik | Data bisa diubah siapa pun | Checklist sebelum rilis; aktifkan Auth + rules per-uid |
| `google-services.json` ter-commit | Kebocoran konfigurasi | Tambah ke `.gitignore` sejak awal |
| Listener tidak dilepas | Memory leak / dobel update | Lepas di `onStop`/`onDestroy` |
| Nesting data terlalu dalam | Unduh data berlebih | Struktur flat `tasks/{id}` (sesuai best practice) |
| Versi Firebase tak cocok AGP 7.2.1 | Build gagal | Gunakan Firebase BoM; kunci versi yang teruji |

---

## 15. Lampiran — Referensi API yang Dipakai

Bersumber dari dokumentasi resmi (via Context7 — Firebase Android SDK & Firebase Docs):

| API | Kegunaan dalam app |
|---|---|
| `FirebaseDatabase.getInstance()` | Instance database default |
| `getReference("tasks")` | Referensi ke node `tasks` |
| `DatabaseReference.push()` | Membuat key unik untuk tugas baru |
| `setValue(Object)` | Menulis objek `Task` (Create/Update penuh) |
| `updateChildren(Map)` | Partial update (toggle `completed`, dll.) |
| `removeValue()` | Menghapus tugas (Delete) |
| `addValueEventListener(ValueEventListener)` | Stream real-time (Read) |
| `onDataChange(DataSnapshot)` / `onCancelled(DatabaseError)` | Callback sukses / gagal baca |
| `DataSnapshot.getValue(Task.class)` | Deserialisasi ke POJO |
| `DataSnapshot.getChildren()` | Iterasi daftar tugas |
| `Query.orderByChild(...)` / `limitToFirst/Last` | Sorting & paging server-side (opsional) |
| `setPersistenceEnabled(true)` | Cache offline disk |
| `onDisconnect()` | (Opsional) operasi saat klien terputus |

**Referensi dokumentasi:**
- Firebase RTDB — Read & Write (Android): https://firebase.google.com/docs/database/android/start
- Offline Capabilities: https://firebase.google.com/docs/database/android/offline-capabilities
- Structure Your Database: https://firebase.google.com/docs/database/android/structure-data
- Security Rules: https://firebase.google.com/docs/database/security

---

*Dokumen ini adalah PRD MVP. Bagian §9 menandai keputusan yang masih terbuka — beri arahan bila ingin berbeda dari Default MVP sebelum implementasi dimulai.*
