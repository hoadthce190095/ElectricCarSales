package com.electriccarsales.service;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

class WordPreviewRendererTest {
    @Test
    void rendersFieldsAcrossRunsInBodyTableAndFooter() throws IOException {
        byte[] template;
        try (var doc = new XWPFDocument(); var bytes = new ByteArrayOutputStream()) {
            doc.createParagraph().createRun().setText("{{preview.label}}");
            var paragraph = doc.createParagraph();
            var bold = paragraph.createRun();
            bold.setBold(true);
            bold.setText("Khách: {{customer.");
            paragraph.createRun().setText("name}} — {{order.id}}");
            doc.createTable(1, 1).getRow(0).getCell(0).setText("{{payment.amount}}");
            doc.createFooter(HeaderFooterType.DEFAULT).createParagraph().createRun().setText("#{{order.id}}");
            doc.write(bytes);
            template = bytes.toByteArray();
        }
        var fields = Map.of("preview.label", WordPreviewRenderer.PREVIEW_LABEL,
                "customer.name", "Nguyễn Văn A & B", "order.id", "23", "payment.amount", "300000");
        var renderer = new WordPreviewRenderer();
        byte[] result = renderer.render(new ByteArrayInputStream(template), fields, fields.keySet());

        try (var doc = new XWPFDocument(new ByteArrayInputStream(result))) {
            assertEquals("Khách: Nguyễn Văn A & B — 23", doc.getParagraphs().get(1).getText());
            assertTrue(doc.getParagraphs().get(1).getRuns().getFirst().isBold());
            assertEquals("300000", doc.getTables().getFirst().getRow(0).getCell(0).getText());
            assertTrue(doc.getFooterList().getFirst().getText().contains("#23"));
        }
        assertThrows(IOException.class, () -> renderer.render(
                new ByteArrayInputStream(template), Map.of(), Set.of("order.id")));
    }

    @Test
    void requiresPreviewLabelInMainBody() throws IOException {
        try (var doc = new XWPFDocument(); var bytes = new ByteArrayOutputStream()) {
            doc.createParagraph().createRun().setText("{{order.id}}");
            doc.write(bytes);
            assertThrows(IOException.class, () -> new WordPreviewRenderer().render(
                    new ByteArrayInputStream(bytes.toByteArray()), Map.of("order.id", "23"), Set.of("order.id")));
        }
    }
}
