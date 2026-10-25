package com.example.quanlydetaisinhvien.service.morong.export;

import com.example.quanlydetaisinhvien.dto.morong.ExportModels.*;
import com.example.quanlydetaisinhvien.exception.ExportLimitException;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.*;
import java.math.*;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class PdfReportWriter {
    public byte[] write(Report report) throws IOException {
        ExportLimitException.check(report.rowCount(), Format.PDF.limit);
        try (var document = new PDDocument(); var output = new ByteArrayOutputStream();
                var regular = new ClassPathResource("report-fonts/NotoSans-Regular.ttf").getInputStream();
                var bold = new ClassPathResource("report-fonts/NotoSans-Bold.ttf").getInputStream()) {
            var renderer = new Renderer(document, PDType0Font.load(document, regular), PDType0Font.load(document, bold), report);
            renderer.render(); document.save(output); return output.toByteArray();
        }
    }

    private static class Renderer {
        private static final float MARGIN = 30, SIZE = 8.5f, LINE = 12, PAD = 5, BOTTOM = 40;
        final PDDocument document;
        final PDFont regular, bold;
        final Report report;
        PDPageContentStream stream;
        float y, width;
        PDRectangle paper;
        Renderer(PDDocument document, PDFont regular, PDFont bold, Report report) {
            this.document = document; this.regular = regular; this.bold = bold; this.report = report;
        }
        void render() throws IOException {
            int columns = report.sections().stream().mapToInt(s -> s.columns().size()).max().orElse(2);
            var base = columns > 8 ? PDRectangle.A3 : PDRectangle.A4;
            paper = new PDRectangle(base.getHeight(), base.getWidth()); width = paper.getWidth() - 2 * MARGIN;
            try {
                for (var section : report.sections()) {
                    if (stream == null || y < BOTTOM + 100) newPage(section);
                    else { y -= 20; sectionHeader(section); }
                    if (section.rows().isEmpty()) {
                        text("Không có dữ liệu", MARGIN + PAD, y - LINE, regular, SIZE); y -= LINE * 2;
                    }
                    for (var row : section.rows()) drawRow(section, row);
                }
                if (stream != null) { stream.close(); stream = null; }
                int total = document.getNumberOfPages();
                for (int i = 0; i < total; i++) {
                    try (var footer = new PDPageContentStream(document, document.getPage(i), PDPageContentStream.AppendMode.APPEND, true)) {
                        footer.beginText(); footer.setFont(regular, SIZE); footer.newLineAtOffset(MARGIN, 20);
                        footer.showText("Trang " + (i + 1) + " / " + total); footer.endText();
                    }
                }
            } finally { if (stream != null) stream.close(); }
        }
        void newPage(Section section) throws IOException {
            if (stream != null) { stream.close(); stream = null; }
            if (document.getNumberOfPages() >= 500) throw new ExportLimitException("PDF vượt giới hạn 500 trang. Vui lòng thu hẹp bộ lọc.");
            var page = new PDPage(paper); document.addPage(page); stream = new PDPageContentStream(document, page);
            y = paper.getHeight() - MARGIN;
            for (String line : wrap(report.title(), width, bold, 16)) { text(line, MARGIN, y - 16, bold, 16); y -= 22; }
            text("Thời điểm xuất: " + report.exportedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss XXX")), MARGIN, y - LINE, regular, SIZE); y -= 20;
            for (String filter : report.filters()) for (String line : wrap(filter, width, regular, SIZE)) {
                text(line, MARGIN, y - LINE, regular, SIZE); y -= LINE;
            }
            sectionHeader(section);
            if (y < BOTTOM + LINE + 2 * PAD) throw new ExportLimitException("Bộ lọc quá dài để dàn trang PDF.");
        }
        void sectionHeader(Section section) throws IOException {
            y -= 12; text(section.title(), MARGIN, y - 12, bold, 12); y -= 24;
            var headers = section.columns().stream().map(Column::label).map(s -> (Object) s).toList();
            var lines = lines(section, headers, bold);
            int count = lines.stream().mapToInt(List::size).max().orElse(1);
            segment(section, lines, 0, count, true);
        }
        List<List<String>> lines(Section section, List<Object> row, PDFont font) throws IOException {
            var result = new ArrayList<List<String>>();
            float weights = section.columns().stream().mapToInt(Column::width).sum();
            for (int c = 0; c < row.size(); c++) {
                result.add(wrap(display(row.get(c), section.columns().get(c).kind()),
                        width * section.columns().get(c).width() / weights - 2 * PAD, font, SIZE));
            }
            return result;
        }
        void drawRow(Section section, List<Object> row) throws IOException {
            var lines = lines(section, row, regular);
            int total = lines.stream().mapToInt(List::size).max().orElse(1), offset = 0;
            while (offset < total) {
                int capacity = (int) ((y - BOTTOM - 2 * PAD) / LINE);
                if (capacity < 1) { newPage(section); continue; }
                int count = Math.min(capacity, total - offset);
                segment(section, lines, offset, count, false); offset += count;
                if (offset < total) newPage(section);
            }
        }
        void segment(Section section, List<List<String>> lines, int offset, int count, boolean header) throws IOException {
            float height = count * LINE + 2 * PAD, x = MARGIN;
            float weights = section.columns().stream().mapToInt(Column::width).sum();
            for (int c = 0; c < lines.size(); c++) {
                float cellWidth = width * section.columns().get(c).width() / weights;
                if (header) { stream.setNonStrokingColor(225 / 255f, 234 / 255f, 245 / 255f); stream.addRect(x, y - height, cellWidth, height); stream.fill(); }
                stream.setStrokingColor(0.65f); stream.setLineWidth(0.35f); stream.addRect(x, y - height, cellWidth, height); stream.stroke();
                for (int j = 0; j < count && offset + j < lines.get(c).size(); j++) {
                    text(lines.get(c).get(offset + j), x + PAD, y - PAD - SIZE - j * LINE, header ? bold : regular, SIZE);
                }
                x += cellWidth;
            }
            y -= height;
        }
        String display(Object value, Kind kind) {
            if (value == null) return "—";
            if (value instanceof LocalDateTime date) return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            if (value instanceof Number && kind == Kind.DECIMAL) return new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP).toPlainString();
            return value.toString();
        }
        List<String> wrap(String value, float maxWidth, PDFont font, float size) throws IOException {
            value = safe(value, font);
            var result = new ArrayList<String>();
            for (String paragraph : value.split("\\R", -1)) {
                StringBuilder line = new StringBuilder();
                for (String word : paragraph.split("(?<=\\s)")) {
                    if (font.getStringWidth(line + word) / 1000 * size <= maxWidth) { line.append(word); continue; }
                    if (!line.isEmpty()) { result.add(line.toString().stripTrailing()); line.setLength(0); }
                    for (int code : word.codePoints().toArray()) {
                        String ch = new String(Character.toChars(code));
                        if (!line.isEmpty() && font.getStringWidth(line + ch) / 1000 * size > maxWidth) {
                            result.add(line.toString()); line.setLength(0);
                        }
                        line.append(ch);
                    }
                }
                result.add(line.toString().stripTrailing());
            }
            return result;
        }
        String safe(String text, PDFont font) throws IOException {
            var result = new StringBuilder();
            for (int code : Normalizer.normalize(text, Normalizer.Form.NFC).codePoints().toArray()) {
                if (code == '\n' || code == '\r') { result.append((char) code); continue; }
                if (code == '\t') { result.append("    "); continue; }
                String ch = new String(Character.toChars(code));
                try { font.encode(ch); result.append(ch); }
                catch (IllegalArgumentException ex) { result.append("[U+").append(Integer.toHexString(code).toUpperCase(Locale.ROOT)).append(']'); }
            }
            return result.toString();
        }
        void text(String text, float x, float baseline, PDFont font, float size) throws IOException {
            stream.setNonStrokingColor(0f); stream.beginText(); stream.setFont(font, size);
            stream.newLineAtOffset(x, baseline); stream.showText(safe(text, font)); stream.endText();
        }
    }
}
