package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class KetquaPublicationValidator {
    private final DangkydetaiRepository registrations;
    private final DetaiRepository topics;
    private final NhomsinhvienRepository groups;
    private final DotdangkyRepository rounds;
    private final HoidongRepository councils;
    private final ThanhvienhoidongRepository councilMembers;
    private final GiangvienphanbienRepository reviewers;
    private final GiangvienhuongdanRepository supervisors;
    private final GiangvienRepository lecturers;
    private final TaikhoanRepository accounts;
    private final DiemRepository scores;

    public KetquaPublicationValidator(DangkydetaiRepository registrations, DetaiRepository topics,
            NhomsinhvienRepository groups, DotdangkyRepository rounds, HoidongRepository councils,
            ThanhvienhoidongRepository councilMembers, GiangvienphanbienRepository reviewers,
            GiangvienhuongdanRepository supervisors, GiangvienRepository lecturers,
            TaikhoanRepository accounts, DiemRepository scores) {
        this.registrations = registrations; this.topics = topics; this.groups = groups;
        this.rounds = rounds; this.councils = councils; this.councilMembers = councilMembers;
        this.reviewers = reviewers; this.supervisors = supervisors; this.lecturers = lecturers;
        this.accounts = accounts; this.scores = scores;
    }

    public record Context(Dangkydetai registration, Detai topic, Nhomsinhvien group, Dotdangky round) {}

    public Context context(Ketqua result) {
        if (result.getDangkydetaiid() == null) fail("Kết quả chưa có đăng ký đề tài.");
        var registration = registrations.findById(result.getDangkydetaiid())
                .orElseThrow(() -> error("Không tìm thấy đăng ký đề tài."));
        if (registration.getTrangthai() != TrangthaiDangkydetai.DADUYET)
            fail("Đăng ký đề tài chưa được duyệt.");
        var topic = topics.findById(registration.getDetaiid())
                .orElseThrow(() -> error("Không tìm thấy đề tài."));
        var group = groups.findById(registration.getNhomid())
                .orElseThrow(() -> error("Không tìm thấy nhóm sinh viên."));
        if (!Objects.equals(topic.getDotdangkyid(), group.getDotdangkyid()))
            fail("Nhóm và đề tài không thuộc cùng đợt đăng ký.");
        var round = rounds.findById(topic.getDotdangkyid())
                .orElseThrow(() -> error("Không tìm thấy đợt đăng ký."));
        if (round.getLoaidot() == null) fail("Chưa xác định loại đợt đăng ký.");
        return new Context(registration, topic, group, round);
    }

    /** Checks assignment integrity without demanding that scores already exist. */
    public Set<Long> assignedGraders(Context context, Phanconghoidong assignment) {
        if (assignment == null) fail("Chưa xác nhận hình thức đánh giá hoặc phân công hội đồng.");
        if (!Objects.equals(context.registration().getId(), assignment.getDangkydetaiid()))
            fail("Phân công không thuộc đăng ký đề tài này.");
        Set<Long> supervisorIds = supervisors.findByDetaiid(context.topic().getId()).stream()
                .map(Giangvienhuongdan::getGiangvienid).collect(Collectors.toSet());
        if (supervisorIds.isEmpty()) fail("Thiếu dữ liệu giảng viên hướng dẫn để đối soát quyền chấm.");
        Set<Long> required = reviewers.findByDangkydetaiid(context.registration().getId()).stream()
                .map(Giangvienphanbien::getGiangvienid).collect(Collectors.toCollection(TreeSet::new));
        if ((context.round().getLoaidot() == Loaidot.TLCN || context.round().getLoaidot() == Loaidot.KLTN)
                && required.isEmpty()) fail("Chưa phân công giảng viên phản biện cho " + context.round().getLoaidot() + ".");

        if (assignment.getHinhthuc() == HinhthucDanhgia.COHOIDONG) {
            if (assignment.getHoidongid() == null) fail("Cần chọn hội đồng cụ thể.");
            var council = councils.findById(assignment.getHoidongid())
                    .orElseThrow(() -> error("Hội đồng không tồn tại."));
            if (!Objects.equals(council.getDotdangkyid(), context.round().getId()))
                fail("Hội đồng không thuộc đợt đăng ký của đề tài.");
            var members = councilMembers.findByHoidongid(council.getId());
            Set<Long> memberIds = members.stream().map(Thanhvienhoidong::getGiangvienid)
                    .collect(Collectors.toSet());
            if (members.size() != memberIds.size() || memberIds.size() < 3 || memberIds.size() > 5)
                fail("Hội đồng phải có 3–5 giảng viên khác nhau.");
            if (members.stream().anyMatch(member -> member.getVaitro() == null))
                fail("Thành viên hội đồng chưa có vai trò hợp lệ.");
            if (members.stream().filter(member -> member.getVaitro() == VaitroHoidong.CHUTICH).count() != 1
                    || members.stream().filter(member -> member.getVaitro() == VaitroHoidong.THUKY).count() != 1)
                fail("Hội đồng phải có đúng một chủ tịch và một thư ký.");
            required.addAll(memberIds);
        } else if (assignment.getHinhthuc() == HinhthucDanhgia.KHONGHOIDONG) {
            if (assignment.getHoidongid() != null) fail("KHONGHOIDONG không được kèm ID hội đồng.");
            if (context.round().getLoaidot() == Loaidot.KLTN)
                fail("KLTN bắt buộc được phân công và đánh giá bởi hội đồng.");
        } else {
            fail("Hình thức đánh giá không hợp lệ.");
        }
        if (required.isEmpty()) fail("Chưa có danh sách giảng viên được phân công chấm.");
        for (Long id : required) {
            if (supervisorIds.contains(id))
                fail("Giảng viên " + id + " đang hướng dẫn đề tài, không được phân công chấm đề tài này.");
            validLecturer(id);
        }
        return required;
    }

    public void validate(Ketqua result, Phanconghoidong assignment) {
        validate(result, assignment, false);
    }

    public void validate(Ketqua result, Phanconghoidong assignment, boolean lockScores) {
        if (Boolean.TRUE.equals(result.getDacongbo())) fail("Kết quả đã được công bố; giữ nguyên ngày công bố.");
        if (result.getNgaycongbo() != null) fail("Kết quả chưa công bố nhưng đã có ngày công bố; cần đối soát dữ liệu.");
        if (!validScore(result.getDiemcuoi())) fail("Điểm cuối phải có giá trị trong khoảng 0–10.");
        var context = context(result);
        Set<Long> required = assignedGraders(context, assignment);
        var components = lockScores ? scores.findForPublication(context.registration().getId())
                : scores.findByDangkydetaiid(context.registration().getId());
        Set<Long> received = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Diem component : components) {
            if (!required.contains(component.getGiangvienid()))
                fail("Điểm của giảng viên " + component.getGiangvienid() + " không thuộc phân công hợp lệ.");
            if (!received.add(component.getGiangvienid()))
                fail("Có điểm trùng của giảng viên " + component.getGiangvienid() + ".");
            if (!validScore(component.getDiemso()))
                fail("Điểm của giảng viên " + component.getGiangvienid() + " phải trong khoảng 0–10.");
            total = total.add(component.getDiemso());
        }
        Set<Long> missing = new TreeSet<>(required);
        missing.removeAll(received);
        if (!missing.isEmpty()) fail("Thiếu điểm của giảng viên: " + missing.stream()
                .map(String::valueOf).collect(Collectors.joining(", ")) + ".");
        BigDecimal average = total.divide(BigDecimal.valueOf(received.size()), 2, RoundingMode.HALF_UP);
        if (result.getDiemcuoi().compareTo(average) != 0)
            fail("Điểm cuối không khớp trung bình HALF_UP: cần " + average.toPlainString() + ".");
    }

    private void validLecturer(Long id) {
        if (id == null) fail("Phân công thiếu ID giảng viên.");
        var lecturer = lecturers.findById(id).orElseThrow(() -> error("Giảng viên " + id + " không tồn tại."));
        var account = accounts.findById(lecturer.getTaikhoanid())
                .orElseThrow(() -> error("Giảng viên " + id + " chưa có tài khoản hợp lệ."));
        if (!Boolean.TRUE.equals(account.getKichhoat()) || account.getVaitro() == null || !Set.of(Vaitro.GIANGVIEN,
                Vaitro.TRUONGBOMON, Vaitro.TRUONGKHOA).contains(account.getVaitro()))
            fail("Giảng viên " + id + " không có quyền chấm hoặc tài khoản đã bị khóa.");
    }

    private boolean validScore(BigDecimal score) {
        return score != null && score.compareTo(BigDecimal.ZERO) >= 0 && score.compareTo(BigDecimal.TEN) <= 0;
    }
    private static NghiepvuException error(String message) { return new NghiepvuException(message); }
    private static void fail(String message) { throw error(message); }
}
