package com.auruspay.filewriter;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ExcelWriter {

    private Workbook workbook;
    private Sheet sheet;
    private int rowCount = 0;
    private String filePath;

    public ExcelWriter(String filePath) {
        this.filePath = filePath;
        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet("Data");
    }

    // ================= HEADER =================
    public void createHeader(String[] headers) {
        Row headerRow = sheet.createRow(rowCount++);
        int col = 0;

        for (String header : headers) {
            Cell cell = headerRow.createCell(col++);
            cell.setCellValue(header);
        }
    }

    // ================= ADD ROW =================
    public void addRow(Object[] data) {
        Row row = sheet.createRow(rowCount++);
        int col = 0;

        for (Object field : data) {
            Cell cell = row.createCell(col++);

            if (field == null) {
                cell.setCellValue("");
            } else if (field instanceof String) {
                cell.setCellValue((String) field);
            } else if (field instanceof Integer) {
                cell.setCellValue((Integer) field);
            } else if (field instanceof Double) {
                cell.setCellValue((Double) field);
            } else {
                cell.setCellValue(field.toString());
            }
        }
    }

    // ================= MAP → VERTICAL =================
    public void addMapData(Map<String, Object> map) {

        for (Map.Entry<String, Object> entry : map.entrySet()) {

            Object value = entry.getValue();

            if (value instanceof List) {

                List<?> list = (List<?>) value;

                for (Object item : list) {
                    addRow(new Object[]{entry.getKey(), item});
                }

            } else {
                addRow(new Object[]{entry.getKey(), value});
            }
        }
    }

    // ================= MAP → SINGLE ROW (BEST) =================
    public void addMapAsSingleRow(Map<String, Object> map) {

        // Header Row
        Row headerRow = sheet.createRow(rowCount++);
        Row valueRow = sheet.createRow(rowCount++);

        int col = 0;

        for (Map.Entry<String, Object> entry : map.entrySet()) {

            // Header
            headerRow.createCell(col).setCellValue(entry.getKey());

            // Value
            Object value = entry.getValue();

            if (value instanceof List) {
                value = String.join(", ",
                        ((List<?>) value).stream()
                                .map(Object::toString)
                                .toArray(String[]::new));
            }

            valueRow.createCell(col).setCellValue(
                    value != null ? value.toString() : ""
            );

            col++;
        }
    }

    // ================= AUTO SIZE =================
    private void autoSizeColumns(int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ================= SAVE =================
    public void save() throws IOException {

        // Auto resize columns
        if (sheet.getRow(0) != null) {
            autoSizeColumns(sheet.getRow(0).getPhysicalNumberOfCells());
        }

        FileOutputStream fos = new FileOutputStream(filePath);
        workbook.write(fos);
        workbook.close();
        fos.close();

        System.out.println("✅ Excel file created: " + filePath);
    }
}