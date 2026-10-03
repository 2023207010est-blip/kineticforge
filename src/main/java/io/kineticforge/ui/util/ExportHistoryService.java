package io.kineticforge.ui.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.kineticforge.ui.model.ExportHistoryEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Persiste el historial de exportaciones en ~/kineticforge/historial.json.
 */
public final class ExportHistoryService {

    private static final Logger log = LoggerFactory.getLogger(ExportHistoryService.class);

    private static final Path HISTORY_DIR =
        Path.of(System.getProperty("user.home"), "kineticforge");
    private static final Path HISTORY_FILE = HISTORY_DIR.resolve("historial.json");
    private static final int MAX_ENTRIES = 50;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ExportHistoryService() {}

    public static List<ExportHistoryEntry> load() {
        try {
            if (!Files.exists(HISTORY_FILE)) return new ArrayList<>();
            String json = Files.readString(HISTORY_FILE);
            List<ExportHistoryEntry> list = GSON.fromJson(json,
                new TypeToken<List<ExportHistoryEntry>>(){}.getType());
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            log.warn("No se pudo leer el historial", e);
            return new ArrayList<>();
        }
    }

    public static void add(ExportHistoryEntry entry) {
        try {
            List<ExportHistoryEntry> list = load();
            list.add(0, entry); // más reciente arriba
            if (list.size() > MAX_ENTRIES) {
                list = list.subList(0, MAX_ENTRIES);
            }
            Files.createDirectories(HISTORY_DIR);
            Files.writeString(HISTORY_FILE, GSON.toJson(list));
        } catch (IOException e) {
            log.warn("No se pudo guardar el historial", e);
        }
    }

    public static void clear() {
        try {
            Files.deleteIfExists(HISTORY_FILE);
        } catch (IOException e) {
            log.warn("No se pudo limpiar el historial", e);
        }
    }
}
