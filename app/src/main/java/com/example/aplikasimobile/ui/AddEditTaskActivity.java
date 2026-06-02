package com.example.aplikasimobile.ui;

import android.app.DatePickerDialog;
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

    private final TaskRepository repository = new TaskRepository();
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID"));

    private TextInputEditText inputTitle;
    private TextInputEditText inputDescription;
    private Spinner spinnerPriority;
    private TextView textDueValue;
    private MaterialButton btnClearDue;
    private MaterialButton btnSave;

    private Long dueDate; // nullable

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

        renderDueDate();
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
        repository.create(task).addOnFailureListener(e ->
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
