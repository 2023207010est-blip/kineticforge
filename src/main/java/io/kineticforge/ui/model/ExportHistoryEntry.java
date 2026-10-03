package io.kineticforge.ui.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Una entrada del historial de exportaciones.
 */
public record ExportHistoryEntry(
    String fileName,
    String fullPath,
    String format,
    int frameCount,
    int fps,
    long timestampMillis
) {

    private static final DateTimeFormatter TIME_FMT =
        DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault());

    public String formattedTime() {
        return TIME_FMT.format(Instant.ofEpochMilli(timestampMillis));
    }

    @Override
    public String toString() {
        return String.format("%s — %d frames @ %d fps (%s)",
            fileName, frameCount, fps, formattedTime());
    }
}
