package com.example.helloworld.service.church.checkin;

import com.example.helloworld.dto.church.checkin.ManualCheckinRow;
import com.example.helloworld.dto.church.checkin.SessionCheckinRow;
import com.example.helloworld.repository.church.checkin.CheckinRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CsvService {

    private final CheckinRepository checkinRepo;

    public CsvService(CheckinRepository checkinRepo) {
        this.checkinRepo = checkinRepo;
    }

    public byte[] exportSessionCheckins(Long sessionId) {
        List<SessionCheckinRow> rows = checkinRepo.findSessionRows(sessionId, false);
        StringBuilder sb = new StringBuilder();
        sb.append("memberNo,memberName,checkedInAt,manual,manualBy,ip\n");
        for (SessionCheckinRow r : rows) {
            sb.append(escape(r.getMemberNo())).append(",")
              .append(escape(r.getMemberName())).append(",")
              .append(r.getCheckedInAt()).append(",")
              .append(r.isManual()).append(",")
              .append(escape(r.getManualBy())).append(",")
              .append(escape(r.getIp())).append("\n");
        }
        // 添加 UTF-8 BOM 以解決 Excel 打開亂碼問題
        byte[] bom = {(byte)0xEF, (byte)0xBB, (byte)0xBF};
        byte[] content = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(content, 0, result, bom.length, content.length);
        return result;
    }

    public byte[] exportManualCheckins(String q, LocalDateTime fromTs, LocalDateTime toTs, boolean includeCanceled) {
        List<ManualCheckinRow> rows = checkinRepo.findManualRows(q, fromTs, toTs, includeCanceled);
        StringBuilder sb = new StringBuilder();
        sb.append("id,sessionTitle,sessionDate,memberNo,memberName,checkedInAt,manualBy,manualNote,ip,canceled,canceledAt,canceledBy,canceledNote\n");
        for (ManualCheckinRow r : rows) {
            sb.append(r.getId()).append(",")
              .append(escape(r.getSessionTitle())).append(",")
              .append(escape(r.getSessionDate())).append(",")
              .append(escape(r.getMemberNo())).append(",")
              .append(escape(r.getMemberName())).append(",")
              .append(r.getCheckedInAt()).append(",")
              .append(escape(r.getManualBy())).append(",")
              .append(escape(r.getManualNote())).append(",")
              .append(escape(r.getIp())).append(",")
              .append(r.isCanceled()).append(",")
              .append(r.getCanceledAt()).append(",")
              .append(escape(r.getCanceledBy())).append(",")
              .append(escape(r.getCanceledNote())).append("\n");
        }
        // 添加 UTF-8 BOM 以解決 Excel 打開亂碼問題
        byte[] bom = {(byte)0xEF, (byte)0xBB, (byte)0xBF};
        byte[] content = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(content, 0, result, bom.length, content.length);
        return result;
    }

    private String escape(String s) {
        if (s == null) return "";
        String t = s.replace("\"", "\"\"");
        if (t.contains(",") || t.contains("\n") || t.contains("\r")) return "\"" + t + "\"";
        return t;
    }
}

