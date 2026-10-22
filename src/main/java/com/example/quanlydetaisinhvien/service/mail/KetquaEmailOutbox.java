package com.example.quanlydetaisinhvien.service.mail;

import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.security.KetquaPermissions;
import com.example.quanlydetaisinhvien.service.KetquaSchema;
import com.example.quanlydetaisinhvien.service.KetquaPublishedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class KetquaEmailOutbox {
    private final EmailketquaRepository emails;
    private final DangkydetaiRepository registrations;
    private final ThanhviennhomRepository members;
    private final SinhvienRepository students;
    private final TaikhoanRepository accounts;
    private final KetquaSchema schema;
    private final ApplicationEventPublisher events;

    public KetquaEmailOutbox(EmailketquaRepository emails, DangkydetaiRepository registrations,
            ThanhviennhomRepository members, SinhvienRepository students,
            TaikhoanRepository accounts, KetquaSchema schema, ApplicationEventPublisher events) {
        this.emails = emails; this.registrations = registrations; this.members = members;
        this.students = students; this.accounts = accounts; this.schema = schema;
        this.events = events;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(Ketqua result) {
        KetquaPermissions.requireTruongKhoa();
        if (!schema.hasTable("Emailketqua")) throw new NghiepvuException("Chưa có bảng Emailketqua; cần migration được duyệt riêng.");
        var registration = registrations.findById(result.getDangkydetaiid()).orElseThrow();
        var groupMembers = members.findByNhomid(registration.getNhomid());
        if (groupMembers.isEmpty()) throw new NghiepvuException("Nhóm chưa có thành viên để nhận kết quả.");
        for (var member : groupMembers) {
            var account = accountOf(member.getSinhvienid());
            String address = MailAddress.validOrNull(account.getEmail());
            var email = new Emailketqua();
            email.setKetquaid(result.getId()); email.setSinhvienid(member.getSinhvienid());
            email.setEmail(address); email.setTieude("Kết quả đề tài sinh viên HCMUTE");
            email.setNoidung("Xin chào " + account.getHoten() + ",\nKết quả đăng ký đề tài "
                    + result.getDangkydetaiid() + " đã được công bố.\nĐiểm cuối: "
                    + result.getDiemcuoi().toPlainString() + "\nNgày công bố: " + result.getNgaycongbo()
                    + "\nVui lòng đăng nhập hệ thống để tra cứu kết quả của nhóm mình.");
            email.setNgaytao(result.getNgaycongbo());
            email.setTrangthai(address == null ? TrangthaiEmailketqua.MISSING_EMAIL : TrangthaiEmailketqua.PENDING);
            if (address == null) email.setLoi("Sinh viên chưa có địa chỉ email hợp lệ.");
            emails.save(email);
        }
        emails.flush();
    }

    @Transactional(readOnly = true)
    public Map<Long, List<Emailketqua>> statuses(List<Long> resultIds) {
        KetquaPermissions.requireTruongKhoa();
        if (resultIds.isEmpty() || !schema.hasTable("Emailketqua")) return Map.of();
        Map<Long, List<Emailketqua>> result = new HashMap<>();
        for (var email : emails.findByKetquaidInOrderByIdAsc(resultIds))
            result.computeIfAbsent(email.getKetquaid(), key -> new ArrayList<>()).add(email);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<Long, String> recipientLabels(Collection<Emailketqua> rows) {
        KetquaPermissions.requireTruongKhoa();
        var studentRows = students.findAllById(rows.stream().map(Emailketqua::getSinhvienid).distinct().toList());
        Map<Long, Taikhoan> accountRows = new HashMap<>();
        accounts.findAllById(studentRows.stream().map(Sinhvien::getTaikhoanid).toList())
                .forEach(account -> accountRows.put(account.getId(), account));
        Map<Long, String> labels = new HashMap<>();
        for (var student : studentRows) {
            var account = accountRows.get(student.getTaikhoanid());
            labels.put(student.getId(), student.getMasinhvien() + (account == null ? "" : " · " + account.getHoten()));
        }
        return labels;
    }

    @Transactional
    public void retry(Long id, boolean confirmedNotAccepted) {
        retry(id, confirmedNotAccepted, null);
    }

    @Transactional
    public void retry(Long id, boolean confirmedNotAccepted, Integer confirmedAttempt) {
        KetquaPermissions.requireTruongKhoa();
        if (!schema.hasTable("Emailketqua")) throw new NghiepvuException("Chưa có bảng Emailketqua.");
        var email = emails.findForUpdate(id).orElseThrow(() -> new NghiepvuException("Không tìm thấy yêu cầu gửi email."));
        var status = email.getTrangthai();
        boolean allowed = status == TrangthaiEmailketqua.FAILED || status == TrangthaiEmailketqua.MISSING_EMAIL
                || (status == TrangthaiEmailketqua.UNCERTAIN && confirmedNotAccepted);
        if (!allowed) throw new NghiepvuException("Không được gửi lại email ở trạng thái " + status
                + ". UNCERTAIN cần xác nhận đã đối soát và thư chưa được tiếp nhận.");
        if (status == TrangthaiEmailketqua.UNCERTAIN
                && (confirmedAttempt == null || confirmedAttempt != email.getSolanthu()))
            throw new NghiepvuException("Xác nhận đối soát đã cũ hoặc thiếu lần thử. Vui lòng tải lại và xác nhận đúng lần gửi.");
        String address = MailAddress.validOrNull(accountOf(email.getSinhvienid()).getEmail());
        if (address == null) throw new NghiepvuException("Sinh viên vẫn chưa có địa chỉ email hợp lệ.");
        email.setEmail(address); email.setTrangthai(TrangthaiEmailketqua.PENDING); email.setLoi(null);
        emails.saveAndFlush(email);
        events.publishEvent(new KetquaPublishedEvent(email.getKetquaid()));
    }

    private Taikhoan accountOf(Long studentId) {
        var student = students.findById(studentId)
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy sinh viên " + studentId + "."));
        return accounts.findById(student.getTaikhoanid())
                .orElseThrow(() -> new NghiepvuException("Sinh viên " + studentId + " chưa có tài khoản hợp lệ."));
    }
}
