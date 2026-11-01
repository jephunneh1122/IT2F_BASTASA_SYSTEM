package com.yourdomain.ui;

import com.yourdomain.model.ReportSection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Exports a report to CSV, Excel (XLSX) or PDF using only the JDK, so no extra
 * libraries are needed. Files are written to the "reports" folder.
 */
public class ReportExporter {

    private static final Path OUTPUT_DIR = Paths.get("reports");

    public Path exportCsv(String baseName, String title, List<ReportSection> sections) throws IOException {
        Path file = prepare(baseName + ".csv");
        StringBuilder sb = new StringBuilder("﻿"); // BOM so Excel reads UTF-8 correctly
        sb.append(csvLine(new String[]{title})).append("\r\n\r\n");
        for (ReportSection s : sections) {
            sb.append(csvLine(new String[]{s.title()})).append("\r\n");
            sb.append(csvLine(s.headers())).append("\r\n");
            for (String[] row : s.rows()) {
                sb.append(csvLine(row)).append("\r\n");
            }
            sb.append("\r\n");
        }
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
        return file;
    }

    public Path exportXlsx(String baseName, String title, List<ReportSection> sections) throws IOException {
        Path file = prepare(baseName + ".xlsx");

        List<String[]> allRows = new ArrayList<>();
        allRows.add(new String[]{title});
        allRows.add(new String[]{});
        for (ReportSection s : sections) {
            allRows.add(new String[]{s.title()});
            allRows.add(s.headers());
            allRows.addAll(s.rows());
            allRows.add(new String[]{});
        }

        StringBuilder sheet = new StringBuilder();
        sheet.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sheet.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        for (int r = 0; r < allRows.size(); r++) {
            String[] row = allRows.get(r);
            if (row.length == 0) {
                continue;
            }
            sheet.append("<row r=\"").append(r + 1).append("\">");
            for (int c = 0; c < row.length; c++) {
                String ref = columnLetter(c) + (r + 1);
                String value = row[c] == null ? "" : row[c];
                if (value.matches("-?\\d+(\\.\\d+)?") && !(value.length() > 1 && value.startsWith("0") && !value.startsWith("0."))) {
                    sheet.append("<c r=\"").append(ref).append("\"><v>").append(value).append("</v></c>");
                } else {
                    sheet.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                            .append(xmlEscape(value)).append("</t></is></c>");
                }
            }
            sheet.append("</row>");
        }
        sheet.append("</sheetData></worksheet>");

        String contentTypes = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "</Types>";
        String rootRels = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "</Relationships>";
        String workbook = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<sheets><sheet name=\"Daily Report\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>";
        String workbookRels = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                + "</Relationships>";

        try (OutputStream os = Files.newOutputStream(file);
             ZipOutputStream zip = new ZipOutputStream(os, StandardCharsets.UTF_8)) {
            addEntry(zip, "[Content_Types].xml", contentTypes);
            addEntry(zip, "_rels/.rels", rootRels);
            addEntry(zip, "xl/workbook.xml", workbook);
            addEntry(zip, "xl/_rels/workbook.xml.rels", workbookRels);
            addEntry(zip, "xl/worksheets/sheet1.xml", sheet.toString());
        }
        return file;
    }

    public Path exportPdf(String baseName, String title, List<ReportSection> sections) throws IOException {
        Path file = prepare(baseName + ".pdf");

        // Turn the report into plain text lines first (monospaced font keeps columns aligned).
        List<PdfLine> lines = new ArrayList<>();
        lines.add(new PdfLine(title, true));
        for (ReportSection s : sections) {
            lines.add(new PdfLine("", false));
            lines.add(new PdfLine(s.title(), true));

            int cols = s.headers().length;
            int[] widths = new int[cols];
            for (int c = 0; c < cols; c++) {
                widths[c] = s.headers()[c].length();
                for (String[] row : s.rows()) {
                    if (c < row.length && row[c] != null) {
                        widths[c] = Math.max(widths[c], row[c].length());
                    }
                }
                widths[c] = Math.min(widths[c], 28);
            }
            lines.add(new PdfLine(pdfRow(s.headers(), widths), true));
            lines.add(new PdfLine("-".repeat(Math.max(1, pdfRow(s.headers(), widths).length())), false));
            if (s.rows().isEmpty()) {
                lines.add(new PdfLine("(no records)", false));
            }
            for (String[] row : s.rows()) {
                lines.add(new PdfLine(pdfRow(row, widths), false));
            }
        }

        final int linesPerPage = 62;
        List<List<PdfLine>> pages = new ArrayList<>();
        for (int i = 0; i < lines.size(); i += linesPerPage) {
            pages.add(lines.subList(i, Math.min(lines.size(), i + linesPerPage)));
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        int pageCount = pages.size();

        pdfWrite(out, "%PDF-1.4\n");
        offsets.add(out.size());
        pdfWrite(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pageCount; i++) {
            kids.append(5 + 2 * i).append(" 0 R ");
        }
        offsets.add(out.size());
        pdfWrite(out, "2 0 obj\n<< /Type /Pages /Kids [" + kids + "] /Count " + pageCount + " >>\nendobj\n");
        offsets.add(out.size());
        pdfWrite(out, "3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>\nendobj\n");
        offsets.add(out.size());
        pdfWrite(out, "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Courier-Bold >>\nendobj\n");

        for (int i = 0; i < pageCount; i++) {
            int pageObj = 5 + 2 * i;
            int contentObj = pageObj + 1;

            StringBuilder content = new StringBuilder();
            List<PdfLine> pageLines = pages.get(i);
            for (int j = 0; j < pageLines.size(); j++) {
                PdfLine line = pageLines.get(j);
                int y = 752 - j * 11;
                content.append("BT /").append(line.bold() ? "F2" : "F1").append(" 8 Tf 1 0 0 1 40 ")
                        .append(y).append(" Tm (").append(pdfEscape(line.text())).append(") Tj ET\n");
            }
            byte[] contentBytes = content.toString().getBytes(StandardCharsets.ISO_8859_1);

            offsets.add(out.size());
            pdfWrite(out, pageObj + " 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
                    + "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + contentObj + " 0 R >>\nendobj\n");
            offsets.add(out.size());
            pdfWrite(out, contentObj + " 0 obj\n<< /Length " + contentBytes.length + " >>\nstream\n");
            out.write(contentBytes);
            pdfWrite(out, "\nendstream\nendobj\n");
        }

        int xrefPos = out.size();
        int objectCount = offsets.size() + 1;
        StringBuilder xref = new StringBuilder("xref\n0 " + objectCount + "\n0000000000 65535 f \n");
        for (int offset : offsets) {
            xref.append(String.format("%010d 00000 n \n", offset));
        }
        xref.append("trailer\n<< /Size ").append(objectCount).append(" /Root 1 0 R >>\nstartxref\n")
                .append(xrefPos).append("\n%%EOF\n");
        pdfWrite(out, xref.toString());

        Files.write(file, out.toByteArray());
        return file;
    }

    // ----------------------------------------------------------------- helpers

    private Path prepare(String fileName) throws IOException {
        Files.createDirectories(OUTPUT_DIR);
        return OUTPUT_DIR.resolve(fileName);
    }

    private String csvLine(String[] cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            String value = cells[i] == null ? "" : cells[i];
            if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                value = "\"" + value.replace("\"", "\"\"") + "\"";
            }
            sb.append(value);
        }
        return sb.toString();
    }

    private String columnLetter(int index) {
        StringBuilder sb = new StringBuilder();
        int n = index;
        while (n >= 0) {
            sb.insert(0, (char) ('A' + n % 26));
            n = n / 26 - 1;
        }
        return sb.toString();
    }

    private String xmlEscape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private void addEntry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String pdfRow(String[] cells, int[] widths) {
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < widths.length; c++) {
            String value = c < cells.length && cells[c] != null ? cells[c] : "";
            value = ConsoleTable.fit(value, widths[c]);
            sb.append(String.format("%-" + widths[c] + "s", value));
            if (c < widths.length - 1) {
                sb.append("  ");
            }
        }
        return sb.toString();
    }

    private String pdfEscape(String text) {
        StringBuilder sb = new StringBuilder();
        for (char ch : text.toCharArray()) {
            if (ch == '\\' || ch == '(' || ch == ')') {
                sb.append('\\').append(ch);
            } else if (ch > 255 || ch < 32) {
                sb.append('?');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private void pdfWrite(ByteArrayOutputStream out, String text) throws IOException {
        out.write(text.getBytes(StandardCharsets.ISO_8859_1));
    }

    private record PdfLine(String text, boolean bold) {
    }
}
