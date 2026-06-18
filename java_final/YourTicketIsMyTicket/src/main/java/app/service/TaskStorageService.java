package app.service;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import app.util.AppDirs;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class TaskStorageService {
    private static final Type TASK_LIST_TYPE = new TypeToken<List<StoredTask>>() {}.getType();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path storagePath;

    public TaskStorageService() {
        this(AppDirs.tasksJson());
    }

    public TaskStorageService(Path storagePath) {
        this.storagePath = storagePath;
    }

    public Path getStoragePath() {
        return storagePath;
    }

    public Optional<StoredTask> loadLatestTask() throws IOException {
        return loadTasks().stream()
                .max(Comparator.comparing(task -> safeTimestamp(task.updatedAt)));
    }

    public void upsertTask(StoredTask task) throws IOException {
        List<StoredTask> tasks = loadTasks();
        String now = Instant.now().toString();

        if (task.createdAt == null || task.createdAt.isBlank()) {
            task.createdAt = now;
        }
        task.updatedAt = now;

        int existingIndex = findTaskIndex(tasks, task.url);
        if (existingIndex >= 0) {
            StoredTask existing = tasks.get(existingIndex);
            task.createdAt = existing.createdAt == null || existing.createdAt.isBlank()
                    ? task.createdAt
                    : existing.createdAt;
            tasks.set(existingIndex, task);
        } else {
            tasks.add(task);
        }

        writeTasks(tasks);
    }

    private List<StoredTask> loadTasks() throws IOException {
        if (!Files.exists(storagePath)) {
            return new ArrayList<>();
        }

        String json = Files.readString(storagePath, StandardCharsets.UTF_8).trim();
        if (json.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            List<StoredTask> tasks = gson.fromJson(json, TASK_LIST_TYPE);
            return tasks == null ? new ArrayList<>() : new ArrayList<>(tasks);
        } catch (JsonSyntaxException e) {
            throw new IOException("tasks.json 格式錯誤：" + e.getMessage(), e);
        }
    }

    private void writeTasks(List<StoredTask> tasks) throws IOException {
        Path parent = storagePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(storagePath, gson.toJson(tasks, TASK_LIST_TYPE), StandardCharsets.UTF_8);
    }

    private int findTaskIndex(List<StoredTask> tasks, String url) {
        for (int i = 0; i < tasks.size(); i++) {
            StoredTask task = tasks.get(i);
            if (url != null && url.equals(task.url)) {
                return i;
            }
        }
        return -1;
    }

    private String safeTimestamp(String value) {
        return value == null ? "" : value;
    }

    public static class StoredTask {
        public String url;
        public String area;
        public String price;
        public String keyword;
        public String createdAt;
        public String updatedAt;
        public String lastStatus;
        /** 自訂音效路徑；null 或空字串代表使用預設音效 */
        public String soundPath;

        public static StoredTask fromInputs(String url,String area, String price, String lastStatus, String soundPath) {
            StoredTask task = new StoredTask();
            task.url = clean(url);
            task.area = clean(area);
            task.price = clean(price);
            task.keyword = String.format("%s %s", task.area, task.price).trim();
            task.lastStatus = clean(lastStatus);
            task.soundPath = clean(soundPath);
            return task;
        }

        private static String clean(String value) {
            return value == null ? "" : value.trim();
        }
    }
}
