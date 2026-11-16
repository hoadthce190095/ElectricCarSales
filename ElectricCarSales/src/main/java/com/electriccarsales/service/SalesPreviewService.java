package com.electriccarsales.service;

import com.electriccarsales.dao.SalesPreviewDAO;
import com.electriccarsales.model.SalesPreviewKind;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

public final class SalesPreviewService {
    private static final long MAX_TEMPLATE_BYTES = 5 * 1024 * 1024;
    private final SalesPreviewDAO dao = new SalesPreviewDAO();
    private final WordPreviewRenderer renderer = new WordPreviewRenderer();

    public byte[] render(SalesPreviewKind kind, int orderId, int userId) throws IOException, SQLException {
        var fields = dao.find(kind, orderId, userId);
        if (fields.isEmpty()) return null;
        fields.put("preview.label", WordPreviewRenderer.PREVIEW_LABEL);
        Path path = template(kind);
        if (path == null || !Files.isRegularFile(path) || Files.size(path) > MAX_TEMPLATE_BYTES) {
            throw new IOException("Mẫu Word xem trước chưa được thiết lập.");
        }
        try (var input = Files.newInputStream(path)) {
            return renderer.render(input, fields, kind.requiredFields());
        }
    }

    public static boolean isAvailable(SalesPreviewKind kind) {
        Path path = template(kind);
        return path != null && Files.isRegularFile(path);
    }

    private static Path template(SalesPreviewKind kind) {
        String directory = System.getProperty("ECS_WORD_PREVIEW_DIR", System.getenv("ECS_WORD_PREVIEW_DIR"));
        return directory == null || directory.isBlank() ? null : Path.of(directory).resolve(kind.filename());
    }
}
