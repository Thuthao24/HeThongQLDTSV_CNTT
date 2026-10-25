package com.example.quanlydetaisinhvien.service.morong.export;

import com.example.quanlydetaisinhvien.dto.morong.ExportModels.*;
import com.example.quanlydetaisinhvien.exception.ExportLimitException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class ExcelReportWriter {
    public byte[] write(Report report) throws IOException {
        ExportLimitException.check(report.rowCount(), Format.XLSX.limit);
        try (var workbook = new SXSSFWorkbook(100); var output = new ByteArrayOutputStream()) {
            workbook.setCompressTempFiles(true);
            var normal = workbook.createCellStyle(); normal.setWrapText(true); normal.setVerticalAlignment(VerticalAlignment.TOP);
            var numeric = workbook.createCellStyle(); numeric.cloneStyleFrom(normal); numeric.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
            var decimal = workbook.createCellStyle(); decimal.cloneStyleFrom(normal); decimal.setDataFormat(workbook.createDataFormat().getFormat("0.00"));
            var date = workbook.createCellStyle(); date.cloneStyleFrom(normal); date.setDataFormat(workbook.createDataFormat().getFormat("dd/MM/yyyy HH:mm"));
            var heading = workbook.createCellStyle(); heading.cloneStyleFrom(normal);
            heading.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex()); heading.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            var font = workbook.createFont(); font.setBold(true); font.setColor(IndexedColors.WHITE.getIndex()); heading.setFont(font);
            var titleStyle = workbook.createCellStyle(); var titleFont = workbook.createFont(); titleFont.setFontHeightInPoints((short) 16);
            titleFont.setBold(true); titleStyle.setFont(titleFont); titleStyle.setWrapText(true);
            for (var section : report.sections()) {
                var sheet = workbook.createSheet(section.title());
                for (int c = 0; c < section.columns().size(); c++) sheet.setColumnWidth(c, section.columns().get(c).width() * 256);
                int rowIndex = 0;
                var title = sheet.createRow(rowIndex++); title.setHeightInPoints(28);
                setText(title.createCell(0), report.title()); title.getCell(0).setCellStyle(titleStyle);
                if (section.columns().size() > 1) sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, section.columns().size() - 1));
                setText(sheet.createRow(rowIndex++).createCell(0), "Thời điểm xuất: " + report.exportedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss XXX")));
                for (String filter : report.filters()) {
                    var filterRow = sheet.createRow(rowIndex++); filterRow.setHeightInPoints(30);
                    var cell = filterRow.createCell(0); setText(cell, filter); cell.setCellStyle(normal);
                    if (section.columns().size() > 1) sheet.addMergedRegion(new CellRangeAddress(filterRow.getRowNum(), filterRow.getRowNum(), 0, section.columns().size() - 1));
                }
                setText(sheet.createRow(rowIndex++).createCell(0), section.title());
                int headerIndex = rowIndex;
                var header = sheet.createRow(rowIndex++); header.setHeightInPoints(34);
                for (int c = 0; c < section.columns().size(); c++) {
                    var cell = header.createCell(c); setText(cell, section.columns().get(c).label()); cell.setCellStyle(heading);
                }
                sheet.createFreezePane(0, rowIndex);
                if (section.rows().isEmpty()) setText(sheet.createRow(rowIndex++).createCell(0), "Không có dữ liệu");
                for (var values : section.rows()) {
                    var row = sheet.createRow(rowIndex++);
                    int visibleLines = 1;
                    for (int c = 0; c < values.size(); c++) {
                        if (values.get(c) == null) continue;
                        int characters = Math.max(4, section.columns().get(c).width() - 2);
                        int lines = 0;
                        for (String line : values.get(c).toString().split("\\R", -1)) {
                            lines += Math.max(1, (line.length() + characters - 1) / characters);
                        }
                        visibleLines = Math.max(visibleLines, lines);
                    }
                    row.setHeightInPoints(Math.min(409, Math.max(30, visibleLines * 15 + 8)));
                    for (int c = 0; c < section.columns().size(); c++) {
                        Object value = values.get(c); var cell = row.createCell(c);
                        cell.setCellStyle(normal);
                        if (value == null) continue;
                        if (value instanceof Number number && section.columns().get(c).kind() != Kind.TEXT) {
                            cell.setCellValue(number.doubleValue());
                            cell.setCellStyle(section.columns().get(c).kind() == Kind.DECIMAL ? decimal : numeric);
                        } else if (value instanceof LocalDateTime datetime) {
                            cell.setCellValue(datetime); cell.setCellStyle(date);
                        } else setText(cell, value.toString());
                    }
                }
                sheet.setAutoFilter(new CellRangeAddress(headerIndex, Math.max(headerIndex, rowIndex - 1), 0, section.columns().size() - 1));
                sheet.setRepeatingRows(new CellRangeAddress(headerIndex, headerIndex, -1, -1));
                sheet.getPrintSetup().setLandscape(true); sheet.setFitToPage(true); sheet.getPrintSetup().setFitWidth((short) 1);
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }
    private void setText(Cell cell, String text) {
        if (text.length() > 32767) throw new ExportLimitException("Nội dung một ô vượt giới hạn Excel 32.767 ký tự. Vui lòng thu hẹp dữ liệu hoặc xuất PDF.");
        // setCellValue(String) always creates a STRING cell, including =, +, -, @; never setCellFormula.
        cell.setCellValue(text);
    }
}
