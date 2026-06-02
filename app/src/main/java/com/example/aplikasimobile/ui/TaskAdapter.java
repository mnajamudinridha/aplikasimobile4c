package com.example.aplikasimobile.ui;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aplikasimobile.R;
import com.example.aplikasimobile.model.Task;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/**
 * Adapter daftar tugas berbasis {@link ListAdapter} + {@link DiffUtil} agar pembaruan
 * efisien (hanya item yang berubah yang di-render ulang — PRD NFR-1).
 */
public class TaskAdapter extends ListAdapter<Task, TaskAdapter.TaskViewHolder> {

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID"));

    /** Aksi item yang diteruskan ke {@code MainActivity}. */
    public interface OnTaskInteractionListener {
        void onToggleCompleted(@NonNull Task task, boolean completed);

        void onEditTask(@NonNull Task task);
    }

    private final OnTaskInteractionListener listener;

    public TaskAdapter(@NonNull OnTaskInteractionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Task> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Task>() {
                @Override
                public boolean areItemsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
                    return oldItem.id != null && oldItem.id.equals(newItem.id);
                }

                @Override
                public boolean areContentsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
                    return oldItem.completed == newItem.completed
                            && oldItem.updatedAt == newItem.updatedAt
                            && Objects.equals(oldItem.title, newItem.title)
                            && Objects.equals(oldItem.description, newItem.description)
                            && Objects.equals(oldItem.priority, newItem.priority)
                            && Objects.equals(oldItem.dueDate, newItem.dueDate);
                }
            };

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final View priorityIndicator;
        private final CheckBox checkCompleted;
        private final TextView textTitle;
        private final TextView textDescription;
        private final TextView textDueDate;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            priorityIndicator = itemView.findViewById(R.id.priority_indicator);
            checkCompleted = itemView.findViewById(R.id.check_completed);
            textTitle = itemView.findViewById(R.id.text_title);
            textDescription = itemView.findViewById(R.id.text_description);
            textDueDate = itemView.findViewById(R.id.text_due_date);
        }

        void bind(@NonNull Task task, @NonNull OnTaskInteractionListener listener) {
            textTitle.setText(task.title);

            // Deskripsi: sembunyikan baris bila kosong.
            if (task.description != null && !task.description.trim().isEmpty()) {
                textDescription.setVisibility(View.VISIBLE);
                textDescription.setText(task.description);
            } else {
                textDescription.setVisibility(View.GONE);
            }

            // Tenggat: tampilkan bila ada.
            if (task.dueDate != null) {
                textDueDate.setVisibility(View.VISIBLE);
                textDueDate.setText(itemView.getContext()
                        .getString(R.string.due_prefix, DATE_FORMAT.format(new Date(task.dueDate))));
            } else {
                textDueDate.setVisibility(View.GONE);
            }

            // Status selesai. Lepas listener dulu sebelum setChecked agar tidak terpicu
            // oleh view daur-ulang (recycle), lalu pasang kembali.
            checkCompleted.setOnCheckedChangeListener(null);
            checkCompleted.setChecked(task.completed);
            applyStrikethrough(task.completed);
            checkCompleted.setOnCheckedChangeListener(
                    (buttonView, isChecked) -> listener.onToggleCompleted(task, isChecked));

            // Ketuk item → buka form edit (FR-3).
            itemView.setOnClickListener(v -> listener.onEditTask(task));

            // Indikator warna prioritas.
            priorityIndicator.setBackgroundColor(
                    ContextCompat.getColor(itemView.getContext(), colorForPriority(task.priority)));
        }

        private void applyStrikethrough(boolean completed) {
            int flags = textTitle.getPaintFlags();
            if (completed) {
                textTitle.setPaintFlags(flags | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                textTitle.setPaintFlags(flags & ~Paint.STRIKE_THRU_TEXT_FLAG);
            }
        }

        private int colorForPriority(String priority) {
            if (Task.PRIORITY_HIGH.equals(priority)) {
                return R.color.priority_high;
            } else if (Task.PRIORITY_LOW.equals(priority)) {
                return R.color.priority_low;
            }
            return R.color.priority_medium;
        }
    }
}
