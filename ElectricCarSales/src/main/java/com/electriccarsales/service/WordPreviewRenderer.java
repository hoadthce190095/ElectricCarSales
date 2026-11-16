package com.electriccarsales.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.IBody;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText;

/** Replaces fields in trusted DOCX preview templates without rebuilding Word paragraphs. */
public final class WordPreviewRenderer {
    private static final Pattern FIELD = Pattern.compile("\\{\\{([a-zA-Z0-9_.]+)}}");
    public static final String PREVIEW_LABEL = "BẢN XEM TRƯỚC - KHÔNG CÓ GIÁ TRỊ PHÁT HÀNH";

    public byte[] render(InputStream template, Map<String, String> fields, Set<String> required) throws IOException {
        try (var document = new XWPFDocument(template); var output = new ByteArrayOutputStream()) {
            var seen = new HashSet<String>();
            replace(document, fields, seen);
            for (var header : document.getHeaderList()) replace(header, fields, seen);
            for (var footer : document.getFooterList()) replace(footer, fields, seen);

            if (!seen.containsAll(required) || document.getParagraphs().stream()
                    .noneMatch(p -> p.getText().contains(PREVIEW_LABEL))) {
                throw new IOException("Mẫu Word thiếu các trường bắt buộc hoặc nhãn xem trước.");
            }
            document.write(output);
            return output.toByteArray();
        }
    }

    private void replace(IBody body, Map<String, String> fields, Set<String> seen) throws IOException {
        for (var paragraph : body.getParagraphs()) replace(paragraph, fields, seen);
        for (var table : body.getTables()) {
            for (var row : table.getRows()) {
                for (var cell : row.getTableCells()) replace(cell, fields, seen);
            }
        }
    }

    private void replace(XWPFParagraph paragraph, Map<String, String> fields, Set<String> seen) throws IOException {
        var text = new StringBuilder();
        var nodes = new ArrayList<TextNode>();
        for (var run : paragraph.getRuns()) {
            for (CTText node : run.getCTR().getTList()) {
                int begin = text.length();
                text.append(node.getStringValue());
                nodes.add(new TextNode(node, begin, text.length()));
            }
        }

        var replacements = new ArrayList<Replacement>();
        var matcher = FIELD.matcher(text);
        int position = 0;
        while (matcher.find()) {
            validateLiteral(text.substring(position, matcher.start()));
            String key = matcher.group(1);
            String value = fields.get(key);
            if (value == null || value.isBlank()) throw new IOException("Thiếu dữ liệu cho trường Word: " + key);
            replacements.add(new Replacement(matcher.start(), matcher.end(), value));
            seen.add(key);
            position = matcher.end();
        }
        validateLiteral(text.substring(position));

        // Work backwards so earlier offsets still refer to the original source text.
        for (Replacement replacement : replacements.reversed()) {
            for (TextNode segment : nodes) {
                if (segment.end <= replacement.start || segment.start >= replacement.end) continue;
                int start = Math.max(0, replacement.start - segment.start);
                int end = Math.min(segment.end, replacement.end) - segment.start;
                String before = segment.node.getStringValue();
                String value = segment.start <= replacement.start ? replacement.value : "";
                segment.node.setStringValue(before.substring(0, start) + value + before.substring(end));
            }
        }
    }

    private void validateLiteral(String text) throws IOException {
        if (text.contains("{{") || text.contains("}}")) {
            throw new IOException("Mẫu Word có trường sai định dạng.");
        }
    }

    private record TextNode(CTText node, int start, int end) {}
    private record Replacement(int start, int end, String value) {}
}
