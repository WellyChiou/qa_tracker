package com.example.helloworld.service.church.checkin;

import com.example.helloworld.dto.church.checkin.ManualCheckinRow;
import com.example.helloworld.dto.church.checkin.SessionCheckinRow;
import com.example.helloworld.repository.church.checkin.CheckinRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelService {

    private final CheckinRepository checkinRepo;

    public ExcelService(CheckinRepository checkinRepo) {
        this.checkinRepo = checkinRepo;
    }

    public byte[] exportSessionCheckins(Long sessionId) throws IOException {
        List<SessionCheckinRow> rows = checkinRepo.findSessionRows(sessionId, false);
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("簽到記錄");
            
            // 創建標題樣式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            
            // 創建標題行
            Row headerRow = sheet.createRow(0);
            String[] headers = {"會員編號", "姓名", "簽到時間", "來源", "操作人", "裝置", "IP"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // 創建資料行
            int rowNum = 1;
            for (SessionCheckinRow r : rows) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(r.getMemberNo() != null ? r.getMemberNo() : "");
                row.createCell(1).setCellValue(r.getMemberName() != null ? r.getMemberName() : "");
                row.createCell(2).setCellValue(r.getCheckedInAt() != null ? 
                    r.getCheckedInAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
                row.createCell(3).setCellValue(r.isManual() ? "補登" : "自助");
                row.createCell(4).setCellValue(r.getManualBy() != null ? r.getManualBy() : "");
                row.createCell(5).setCellValue(getDeviceType(r.getUserAgent()));
                row.createCell(6).setCellValue(r.getIp() != null ? r.getIp() : "");
            }
            
            // 自動調整欄位寬度
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 1000, 15000));
            }
            
            // 寫入到 ByteArrayOutputStream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    public byte[] exportManualCheckins(String q, LocalDateTime fromTs, LocalDateTime toTs, boolean includeCanceled) throws IOException {
        List<ManualCheckinRow> rows = checkinRepo.findManualRows(q, fromTs, toTs, includeCanceled);
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("補登稽核");
            
            // 創建標題樣式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            
            // 創建標題行
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "場次標題", "場次日期", "會員編號", "姓名", "補登時間", "操作人", "備註", "裝置", "IP", "狀態", "取消時間", "取消人", "取消原因"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // 創建資料行
            int rowNum = 1;
            for (ManualCheckinRow r : rows) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(r.getId() != null ? r.getId() : 0);
                row.createCell(1).setCellValue(r.getSessionTitle() != null ? r.getSessionTitle() : "");
                row.createCell(2).setCellValue(r.getSessionDate() != null ? r.getSessionDate() : "");
                row.createCell(3).setCellValue(r.getMemberNo() != null ? r.getMemberNo() : "");
                row.createCell(4).setCellValue(r.getMemberName() != null ? r.getMemberName() : "");
                row.createCell(5).setCellValue(r.getCheckedInAt() != null ? 
                    r.getCheckedInAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
                row.createCell(6).setCellValue(r.getManualBy() != null ? r.getManualBy() : "");
                row.createCell(7).setCellValue(r.getManualNote() != null ? r.getManualNote() : "");
                row.createCell(8).setCellValue(getDeviceType(r.getUserAgent()));
                row.createCell(9).setCellValue(r.getIp() != null ? r.getIp() : "");
                row.createCell(10).setCellValue(r.isCanceled() ? "已取消" : "有效");
                row.createCell(11).setCellValue(r.getCanceledAt() != null ? 
                    r.getCanceledAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
                row.createCell(12).setCellValue(r.getCanceledBy() != null ? r.getCanceledBy() : "");
                row.createCell(13).setCellValue(r.getCanceledNote() != null ? r.getCanceledNote() : "");
            }
            
            // 自動調整欄位寬度
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 1000, 15000));
            }
            
            // 寫入到 ByteArrayOutputStream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private String getDeviceType(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "-";
        }
        
        String ua = userAgent.toLowerCase();
        
        // 手機
        if (ua.matches(".*(mobile|android|iphone|ipod|blackberry|iemobile|opera mini).*")) {
            if (ua.contains("iphone") || ua.contains("ipod")) {
                return "📱 iPhone";
            }
            if (ua.contains("android")) {
                return "📱 Android";
            }
            if (ua.contains("ipad")) {
                return "📱 iPad";
            }
            return "📱 手機";
        }
        
        // 平板
        if (ua.matches(".*(tablet|ipad|playbook|silk).*")) {
            if (ua.contains("ipad")) {
                return "📱 iPad";
            }
            return "📱 平板";
        }
        
        // 電腦 - 識別作業系統
        if (ua.contains("windows")) {
            return "💻 電腦(Windows)";
        }
        
        if (ua.matches(".*(macintosh|mac os x|mac_powerpc).*")) {
            return "💻 電腦(Mac)";
        }
        
        if (ua.contains("linux")) {
            return "💻 電腦(Linux)";
        }
        
        return "❓ 未知";
    }
}

