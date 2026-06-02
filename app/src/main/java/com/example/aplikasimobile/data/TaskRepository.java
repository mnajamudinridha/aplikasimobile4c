package com.example.aplikasimobile.data;

import androidx.annotation.NonNull;

import com.example.aplikasimobile.model.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Satu-satunya pintu akses ke Firebase Realtime Database untuk data tugas.
 *
 * <p>Membungkus {@link DatabaseReference} ke node {@code "tasks"} sehingga lapisan UI tidak
 * bergantung langsung pada Firebase (PRD §7.1, NFR-6). Operasi tulis (create/update/delete)
 * ditambahkan pada milestone M2–M4.</p>
 */
public class TaskRepository {

    private final DatabaseReference tasksRef =
            FirebaseDatabase.getInstance().getReference("tasks");

    /** Callback hasil pembacaan real-time node {@code tasks}. */
    public interface TasksListener {
        void onTasksChanged(@NonNull List<Task> tasks);

        void onError(@NonNull DatabaseError error);
    }

    /**
     * Mulai mengamati node {@code tasks} secara real-time.
     *
     * <p>{@code onDataChange} dipanggil sekali dengan data awal, lalu setiap kali ada perubahan.
     * Simpan {@link ValueEventListener} yang dikembalikan untuk dilepas via
     * {@link #removeListener(ValueEventListener)} saat layar berhenti (cegah memory leak — NFR-7).</p>
     */
    public ValueEventListener observeTasks(@NonNull TasksListener listener) {
        ValueEventListener veListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Task> tasks = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Task task = child.getValue(Task.class);
                    if (task != null) {
                        // Pastikan id terisi walau node lama tidak menyimpannya.
                        if (task.id == null) {
                            task.id = child.getKey();
                        }
                        tasks.add(task);
                    }
                }
                listener.onTasksChanged(tasks);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error);
            }
        };
        tasksRef.addValueEventListener(veListener);
        return veListener;
    }

    public void removeListener(@NonNull ValueEventListener listener) {
        tasksRef.removeEventListener(listener);
    }

    /**
     * Membuat tugas baru di {@code tasks/{pushId}} (PRD FR-1).
     *
     * <p>{@code push()} menghasilkan key unik berbasis timestamp sehingga urutan penambahan
     * terjaga tanpa perlu mengelola id manual. Mengembalikan {@link com.google.android.gms.tasks.Task}
     * agar pemanggil dapat memasang {@code addOnFailureListener}.</p>
     */
    public com.google.android.gms.tasks.Task<Void> create(@NonNull Task task) {
        DatabaseReference newRef = tasksRef.push();
        task.id = newRef.getKey();
        return newRef.setValue(task);
    }

    /**
     * Memperbarui seluruh field tugas (PRD FR-3). {@code updatedAt} disetel ke waktu sekarang.
     */
    public com.google.android.gms.tasks.Task<Void> update(@NonNull Task task) {
        task.updatedAt = System.currentTimeMillis();
        return tasksRef.child(task.id).setValue(task);
    }

    /**
     * Partial update status selesai (PRD FR-4) — hanya menulis {@code completed} + {@code updatedAt}
     * via {@code updateChildren()}, sehingga field lain tidak tertimpa.
     */
    public com.google.android.gms.tasks.Task<Void> setCompleted(@NonNull String id, boolean completed) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("completed", completed);
        updates.put("updatedAt", System.currentTimeMillis());
        return tasksRef.child(id).updateChildren(updates);
    }

    /** Menghapus tugas dari {@code tasks/{id}} (PRD FR-5). */
    public com.google.android.gms.tasks.Task<Void> delete(@NonNull String id) {
        return tasksRef.child(id).removeValue();
    }
}
