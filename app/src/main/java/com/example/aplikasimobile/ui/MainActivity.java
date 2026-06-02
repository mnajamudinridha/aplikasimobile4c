package com.example.aplikasimobile.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aplikasimobile.R;
import com.example.aplikasimobile.data.TaskRepository;
import com.example.aplikasimobile.model.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Layar utama: menampilkan daftar tugas secara real-time dari Firebase RTDB (PRD FR-2).
 *
 * <p>Listener dipasang di {@link #onStart()} dan dilepas di {@link #onStop()} agar tidak
 * terjadi memory leak / update saat layar tidak terlihat (NFR-7).</p>
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Urutan tampilan (PRD FR-2): tugas <b>belum selesai</b> di atas, lalu <b>prioritas</b>
     * (high → low), lalu <b>terbaru</b> (createdAt desc).
     */
    private static final Comparator<Task> TASK_ORDER = (a, b) -> {
        if (a.completed != b.completed) {
            return a.completed ? 1 : -1;                       // belum selesai dulu
        }
        int byPriority = Integer.compare(b.priorityWeight(), a.priorityWeight());
        if (byPriority != 0) {
            return byPriority;                                 // high → low
        }
        return Long.compare(b.createdAt, a.createdAt);         // terbaru dulu
    };

    private TaskRepository repository;
    private TaskAdapter adapter;
    private ValueEventListener tasksListener;

    private RecyclerView recyclerTasks;
    private TextView emptyView;
    private ProgressBar progressBar;
    private FloatingActionButton fabAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = new TaskRepository();

        recyclerTasks = findViewById(R.id.recycler_tasks);
        emptyView = findViewById(R.id.empty_view);
        progressBar = findViewById(R.id.progress);

        adapter = new TaskAdapter(new TaskAdapter.OnTaskInteractionListener() {
            @Override
            public void onToggleCompleted(@NonNull Task task, boolean completed) {
                repository.setCompleted(task.id, completed).addOnFailureListener(e ->
                        showError(getString(R.string.error_save_task, e.getMessage())));
            }

            @Override
            public void onEditTask(@NonNull Task task) {
                openEdit(task);
            }
        });
        recyclerTasks.setLayoutManager(new LinearLayoutManager(this));
        recyclerTasks.setAdapter(adapter);
        attachSwipeToDelete();

        fabAdd = findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(v ->
                startActivity(new Intent(this, AddEditTaskActivity.class)));
    }

    /** Pasang gesture geser-untuk-hapus pada daftar (FR-5). */
    private void attachSwipeToDelete() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) {
                    return;
                }
                confirmDelete(adapter.getCurrentList().get(position), position);
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(recyclerTasks);
    }

    /** Dialog konfirmasi sebelum menghapus (AC FR-5). Batal → kembalikan item yang ter-swipe. */
    private void confirmDelete(@NonNull Task task, int position) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete_title)
                .setMessage(getString(R.string.confirm_delete_message, task.title))
                .setPositiveButton(R.string.action_delete, (dialog, which) ->
                        repository.delete(task.id).addOnFailureListener(e -> {
                            adapter.notifyItemChanged(position);
                            showError(getString(R.string.error_delete_task, e.getMessage()));
                        }))
                .setNegativeButton(R.string.action_cancel,
                        (dialog, which) -> adapter.notifyItemChanged(position))
                .setOnCancelListener(dialog -> adapter.notifyItemChanged(position))
                .show();
    }

    /** Buka form dalam mode EDIT dengan data tugas terisi (FR-3). */
    private void openEdit(@NonNull Task task) {
        Intent intent = new Intent(this, AddEditTaskActivity.class);
        intent.putExtra(AddEditTaskActivity.EXTRA_TASK_ID, task.id);
        intent.putExtra(AddEditTaskActivity.EXTRA_TITLE, task.title);
        intent.putExtra(AddEditTaskActivity.EXTRA_DESCRIPTION, task.description);
        intent.putExtra(AddEditTaskActivity.EXTRA_PRIORITY, task.priority);
        intent.putExtra(AddEditTaskActivity.EXTRA_COMPLETED, task.completed);
        intent.putExtra(AddEditTaskActivity.EXTRA_CREATED_AT, task.createdAt);
        if (task.dueDate != null) {
            intent.putExtra(AddEditTaskActivity.EXTRA_DUE_DATE, (long) task.dueDate);
        }
        startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        showLoading(true);
        tasksListener = repository.observeTasks(new TaskRepository.TasksListener() {
            @Override
            public void onTasksChanged(@NonNull List<Task> tasks) {
                showLoading(false);
                List<Task> sorted = new ArrayList<>(tasks);
                Collections.sort(sorted, TASK_ORDER);
                adapter.submitList(sorted);
                showEmpty(sorted.isEmpty());
            }

            @Override
            public void onError(@NonNull DatabaseError error) {
                showLoading(false);
                showError(getString(R.string.error_load_tasks, error.getMessage()));
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (tasksListener != null) {
            repository.removeListener(tasksListener);
            tasksListener = null;
        }
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showEmpty(boolean empty) {
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerTasks.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    /** Tampilkan pesan error sebagai Snackbar, di-anchor di atas FAB (FR-7). */
    private void showError(@NonNull String message) {
        Snackbar snackbar = Snackbar.make(recyclerTasks, message, Snackbar.LENGTH_LONG);
        if (fabAdd != null) {
            snackbar.setAnchorView(fabAdd);
        }
        snackbar.show();
    }
}
