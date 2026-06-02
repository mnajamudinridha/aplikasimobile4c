package com.example.aplikasimobile.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Model satu tugas, dipetakan langsung dari/ke node {@code tasks/{pushId}} di RTDB.
 *
 * <p>Firebase melakukan deserialisasi via {@code DataSnapshot.getValue(Task.class)}, sehingga
 * constructor kosong WAJIB ada dan field harus {@code public} (atau punya getter/setter).</p>
 */
@IgnoreExtraProperties
public class Task {

    public static final String PRIORITY_LOW = "low";
    public static final String PRIORITY_MEDIUM = "medium";
    public static final String PRIORITY_HIGH = "high";

    public String id;
    public String title;
    public String description;
    public String priority = PRIORITY_MEDIUM; // low | medium | high
    public boolean completed = false;
    public Long dueDate;                       // epoch millis, nullable
    public long createdAt;
    public long updatedAt;

    /** WAJIB untuk deserialisasi Firebase. */
    public Task() {
    }

    public Task(String title, String description, String priority, Long dueDate) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.dueDate = dueDate;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Bobot prioritas untuk pengurutan (high &gt; medium &gt; low). Tidak ditulis ke RTDB. */
    @Exclude
    public int priorityWeight() {
        if (PRIORITY_HIGH.equals(priority)) {
            return 3;
        } else if (PRIORITY_LOW.equals(priority)) {
            return 1;
        }
        return 2; // medium / default
    }

    /** Map field untuk partial update via {@code updateChildren()} (dipakai mulai M3). */
    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("title", title);
        result.put("description", description);
        result.put("priority", priority);
        result.put("completed", completed);
        result.put("dueDate", dueDate);
        result.put("createdAt", createdAt);
        result.put("updatedAt", updatedAt);
        return result;
    }
}
