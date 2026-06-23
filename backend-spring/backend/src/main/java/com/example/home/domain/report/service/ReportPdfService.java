package com.example.home.domain.report.service;

import com.example.home.domain.report.dto.ReportDocument;
import com.example.home.global.exception.BusinessException;
import com.example.home.global.exception.docs.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ReportPdfService {

    private static final float MARGIN = 48f;
    private static final float BODY_SIZE = 10.5f;
    private static final float HEADING_SIZE = 16f;
    private static final Pattern YEAR_MONTH = Pattern.compile("(?<!\\d)(\\d{4})(\\d{2})(?!\\d)");

    @Value("${report.pdf.font-path:C:/Windows/Fonts/malgun.ttf}")
    private String fontPath;

    public byte[] render(ReportDocument report) {
        Path fontFile = Path.of(fontPath);
        if (!Files.isRegularFile(fontFile)) {
            throw new BusinessException(ErrorCode.SERVER_ERROR,
                    "PDF 한글 폰트를 찾을 수 없습니다. REPORT_PDF_FONT_PATH를 설정해 주세요.");
        }

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType0Font font;
            try (var fontInput = Files.newInputStream(fontFile)) {
                font = PDType0Font.load(document, fontInput, true);
            }

            PdfLayout layout = new PdfLayout(document, font);
            layout.heading(report.draft().title());
            layout.muted("생성 시각: " + report.createdAt());
            layout.line();

            JsonNode enhancement = report.aiEnhancement();
            if (enhancement == null || enhancement.isNull()) {
                renderDraft(layout, report);
            } else {
                renderEnhancement(layout, enhancement);
            }

            layout.finish();
            document.save(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "PDF 리포트를 생성하지 못했습니다.");
        }
    }

    private void renderEnhancement(PdfLayout layout, JsonNode enhancement) throws IOException {
        layout.section("핵심 요약");
        layout.paragraph(text(enhancement, "executive_summary"));

        for (JsonNode section : enhancement.path("sections")) {
            layout.section(text(section, "title"));
            layout.paragraph(text(section, "content"));
        }

        layout.section("대표 지역 분석");
        for (JsonNode region : enhancement.path("regional_trends")) {
            layout.subheading(text(region, "region_name"));
            layout.labelValue("선정 이유", text(region, "selection_reason"));
            layout.paragraph(text(region, "trend"));
            layout.labelValue("비교 해석", text(region, "comparative_interpretation"));
            for (JsonNode evidence : region.path("evidence")) {
                layout.bullet(evidence.asText());
            }
        }

        layout.section("해석 시 유의사항");
        for (JsonNode caution : enhancement.path("cautions")) {
            layout.bullet(caution.asText());
        }
    }

    private void renderDraft(PdfLayout layout, ReportDocument report) throws IOException {
        layout.section("개요");
        layout.paragraph(report.draft().overview());
        layout.section("주요 관찰");
        for (String finding : report.draft().keyFindings()) {
            layout.bullet(finding);
        }
        layout.section("해석 시 유의사항");
        for (String caution : report.draft().cautions()) {
            layout.bullet(caution);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() ? value.asText() : "";
    }

    private static final class PdfLayout {

        private final PDDocument document;
        private final PDType0Font font;
        private PDPageContentStream content;
        private float y;

        private PdfLayout(PDDocument document, PDType0Font font) throws IOException {
            this.document = document;
            this.font = font;
            nextPage();
        }

        private void heading(String text) throws IOException {
            writeWrapped(text, HEADING_SIZE, 1.45f, false);
            y -= 8f;
        }

        private void section(String text) throws IOException {
            ensureSpace(30f);
            y -= 8f;
            writeWrapped(text, 13f, 1.35f, false);
            line();
        }

        private void subheading(String text) throws IOException {
            ensureSpace(24f);
            writeWrapped(text, 11.5f, 1.35f, false);
        }

        private void paragraph(String text) throws IOException {
            writeWrapped(text, BODY_SIZE, 1.55f, false);
            y -= 6f;
        }

        private void bullet(String text) throws IOException {
            writeWrapped("- " + text, BODY_SIZE, 1.45f, true);
        }

        private void labelValue(String label, String value) throws IOException {
            if (!value.isBlank()) {
                writeWrapped(label + ": " + value, BODY_SIZE, 1.45f, false);
            }
        }

        private void muted(String text) throws IOException {
            writeWrapped(text, 8.5f, 1.3f, false);
            y -= 8f;
        }

        private void line() throws IOException {
            ensureSpace(8f);
            content.moveTo(MARGIN, y);
            content.lineTo(PDRectangle.A4.getWidth() - MARGIN, y);
            content.stroke();
            y -= 12f;
        }

        private void writeWrapped(String text, float size, float lineHeightMultiplier, boolean indent) throws IOException {
            if (text == null || text.isBlank()) {
                return;
            }
            text = YEAR_MONTH.matcher(text).replaceAll("$1년 $2월");
            float width = PDRectangle.A4.getWidth() - MARGIN * 2 - (indent ? 10f : 0f);
            for (String paragraph : text.replace("\r", "").split("\n")) {
                for (String line : wrap(paragraph, size, width)) {
                    ensureSpace(size * lineHeightMultiplier);
                    content.beginText();
                    content.setFont(font, size);
                    content.newLineAtOffset(MARGIN + (indent ? 10f : 0f), y);
                    content.showText(line);
                    content.endText();
                    y -= size * lineHeightMultiplier;
                }
            }
        }

        private List<String> wrap(String text, float size, float width) throws IOException {
            List<String> lines = new ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (String token : text.split("(?<=\\s)|(?=\\s)")) {
                String candidate = line + token;
                if (line.length() > 0 && font.getStringWidth(candidate) / 1000f * size > width) {
                    lines.add(line.toString().stripTrailing());
                    line.setLength(0);
                }
                line.append(token);
            }
            if (line.length() > 0) {
                lines.add(line.toString().stripTrailing());
            }
            return lines.isEmpty() ? List.of("") : lines;
        }

        private void ensureSpace(float required) throws IOException {
            if (y - required < MARGIN) {
                nextPage();
            }
        }

        private void nextPage() throws IOException {
            if (content != null) {
                content.close();
            }
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            y = PDRectangle.A4.getHeight() - MARGIN;
        }

        private void finish() throws IOException {
            if (content != null) {
                content.close();
            }
        }
    }
}
