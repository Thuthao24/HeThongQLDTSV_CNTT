package com.example.quanlydetaisinhvien.service.morong.export;

import com.example.quanlydetaisinhvien.dto.morong.ExportModels.*;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
public class ReportExportService {
    private final ReportDataService data;
    private final ExcelReportWriter excel;
    private final PdfReportWriter pdf;
    public ReportExportService(ReportDataService data, ExcelReportWriter excel, PdfReportWriter pdf) {
        this.data = data; this.excel = excel; this.pdf = pdf;
    }
    public Download export(Dataset dataset, Object filter, Format format) throws IOException {
        dataset.requireAccess();
        var report = data.load(dataset, filter, format);
        byte[] bytes = format == Format.XLSX ? excel.write(report) : pdf.write(report);
        String name = dataset.slug() + "_" + report.exportedAt().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + "." + format.name().toLowerCase(java.util.Locale.ROOT);
        return new Download(bytes, name, format.mime);
    }
}
