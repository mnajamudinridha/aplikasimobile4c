package com.example.aplikasimobile.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.aplikasimobile.R;
import com.example.aplikasimobile.data.TaskRepository;
import com.example.aplikasimobile.model.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Form tambah tugas (CREATE) — PRD FR-1. Mode EDIT ditambahkan pada M3.
 */
public class AddEditTaskActivity extends AppCompatActivity {

    /** Nilai prioritas sesuai urutan item di {@code R.array.priority_labels}. */
    private static final String[] PRIORITY_VALUES = {
            Task.PRIORITY_LOW, Task.PRIORITY_MEDIUM, Task.PRIORITY_HIGH
    };
    private static final int DEFAULT_PRIORITY_INDEX = 1; // medium

    public static final String EXTRA_TASK_ID = "extra_task_id";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_DESCRIPTION = "extra_description";
    public static final String EXTRA_PRIORITY = "extra_priority";
    public static final String EXTRA_COMPLETED = "extra_completed";
    public static final String EXTRA_DUE_DATE = "extra_due_date";
    public static final String EXTRA_CREATED_AT = "extra_created_at";

    private final TaskRepository repository = new TaskRepository();
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID"));

    private TextInputEditText inputTitle;
    private TextInputEditText inputDescription;
    private Spinner spinnerPriority;
    private TextView textDueValue;
    private MaterialButton btnClearDue;
    private MaterialButton btnSave;

    private Long dueDate;          // nullable
    private String editingTaskId;  // null = mode tambah, selain itu = mode edit
    private long editingCreatedAt;
    private boolean editingCompleted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_task);
        setTitle(R.string.title_add_task);

        inputTitle = findViewById(R.id.input_title);
        inputDescription = findViewById(R.id.input_description);
        spinnerPriority = findViewById(R.id.spinner_priority);
        textDueValue = findViewById(R.id.text_due_value);
        btnClearDue = findViewById(R.id.btn_clear_due);
        btnSave = findViewById(R.id.btn_save);

        ArrayAdapter<CharSequence> priorityAdapter = ArrayAdapter.createFromResource(
                this, R.array.priority_labels, android.R.layout.simple_spinner_item);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(priorityAdapter);
        spinnerPriority.setSelection(DEFAULT_PRIORITY_INDEX);

        findViewById(R.id.btn_pick_due).setOnClickListener(v -> showDatePicker());
        btnClearDue.setOnClickListener(v -> {
            dueDate = null;
            renderDueDate();
        });
        btnSave.setOnClickListener(v -> save());

        prefillIfEditing();
        renderDueDate();
    }

    /** Bila dibuka dengan {@code EXTRA_TASK_ID}, isi form dengan data tugas (mode EDIT — FR-3). */
    private void prefillIfEditing() {
        Intent intent = getIntent();
        if (intent == null || !intent.hasExtra(EXTRA_TASK_ID)) {
            return; // mode tambah
        }
        editingTaskId = intent.getStringExtra(EXTRA_TASK_ID);
        editingCreatedAt = intent.getLongExtra(EXTRA_CREATED_AT, System.currentTimeMillis());
        editingCompleted = intent.getBooleanExtra(EXTRA_COMPLETED, false);

        setTitle(R.string.title_edit_task);
        inputTitle.setText(intent.getStringExtra(EXTRA_TITLE));
        inputDescription.setText(intent.getStringExtra(EXTRA_DESCRIPTION));
        spinnerPriority.setSelection(indexForPriority(intent.getStringExtra(EXTRA_PRIORITY)));
        if (intent.hasExtra(EXTRA_DUE_DATE)) {
            dueDate = intent.getLongExtra(EXTRA_DUE_DATE, 0L);
        }
    }

    private int indexForPriority(String priority) {
        for (int i = 0; i < PRIORITY_VALUES.length; i++) {
            if (PRIORITY_VALUES[i].equals(priority)) {
                return i;
            }
        }
        return DEFAULT_PRIORITY_INDEX;
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        if (dueDate != null) {
            calendar.setTimeInMillis(dueDate);
        }
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth, 0, 0, 0);
            selected.set(Calendar.MILLISECOND, 0);
            dueDate = selected.getTimeInMillis();
            renderDueDate();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void renderDueDate() {
        if (dueDate == null) {
            textDueValue.setText(R.string.due_none);
            btnClearDue.setVisibility(View.GONE);
        } else {
            textDueValue.setText(dateFormat.format(new Date(dueDate)));
            btnClearDue.setVisibility(View.VISIBLE);
        }
    }

    private void save() {
        String title = valueOf(inputTitle);
        if (title.isEmpty()) {
            inputTitle.setError(getString(R.string.error_title_required));
            inputTitle.requestFocus();
            return;
        }
        String description = valueOf(inputDescription);
        String priority = PRIORITY_VALUES[spinnerPriority.getSelectedItemPosition()];

        Task task = new Task(title, description, priority, dueDate);

        btnSave.setEnabled(false);
        // Optimistic: tutup form segera. RTDB mengantri tulisan saat offline & listener
        // real-time di MainActivity memperbarui daftar dari cache lokal (FR-6).
        com.google.android.gms.tasks.Task<Void> op;
        if (editingTaskId == null) {
            op = repository.create(task);                 // CREATE (FR-1)
        } else {
            task.id = editingTaskId;                      // UPDATE (FR-3)
            task.createdAt = editingCreatedAt;            // pertahankan waktu dibuat
            task.completed = editingCompleted;            // pertahankan status selesai
            op = repository.update(task);
        }
        op.addOnFailureListener(e ->
                Toast.makeText(getApplicationContext(),
                        getString(R.string.error_save_task, e.getMessage()),
                        Toast.LENGTH_LONG).show());
        finish();
    }

    @NonNull
    private String valueOf(@NonNull TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }
}
