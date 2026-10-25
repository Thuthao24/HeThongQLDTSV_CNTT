package com.example.quanlydetaisinhvien.service.impl;

import com.example.quanlydetaisinhvien.entity.Diem;
import com.example.quanlydetaisinhvien.repository.DiemRepository;
import com.example.quanlydetaisinhvien.service.DiemService;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import java.math.BigDecimal;
import java.util.Set;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.service.KetquaPublicationValidator;

import java.util.List;
import java.util.Optional;

@Service
public class DiemServiceImpl implements DiemService {

    private final DiemRepository repository;
    private final DangkydetaiRepository registrations;
    private final GiangvienRepository lecturers;
    private final KetquaRepository results;
    private final GiangvienhuongdanRepository supervisors;
    private final TaikhoanRepository accounts;
    private final PhanconghoidongRepository assignments;
    private final KetquaPublicationValidator validator;

    public DiemServiceImpl(DiemRepository repository, DangkydetaiRepository registrations,
            GiangvienRepository lecturers, KetquaRepository results,
            GiangvienhuongdanRepository supervisors, TaikhoanRepository accounts,
            PhanconghoidongRepository assignments, KetquaPublicationValidator validator) {
        this.repository = repository;
        this.registrations = registrations;
        this.lecturers = lecturers;
        this.results = results;
        this.supervisors = supervisors;
        this.accounts = accounts;
        this.assignments = assignments;
        this.validator = validator;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Diem> findPage(org.springframework.data.domain.Pageable pageable) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new AccessDeniedException("Chưa đăng nhập.");
        if (auth.getAuthorities().stream().anyMatch(a -> "ROLE_TRUONGKHOA".equals(a.getAuthority())))
            return repository.findAll(pageable);
        return repository.findByGiangvienid(currentLecturer().getId(), pageable);
    }

    @Override
    public List<Diem> findAll() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new AccessDeniedException("Chưa đăng nhập.");
        if (auth.getAuthorities().stream().anyMatch(a -> "ROLE_TRUONGKHOA".equals(a.getAuthority())))
            return repository.findAll();
        var teacher = currentLecturer();
        return repository.findByGiangvienid(teacher.getId());
    }

    @Override
    public Optional<Diem> findById(Long id) {
        var score = repository.findById(id);
        if (score.isEmpty()) return score;
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new AccessDeniedException("Chưa đăng nhập.");
        if (auth.getAuthorities().stream().anyMatch(a -> "ROLE_TRUONGKHOA".equals(a.getAuthority()))) return score;
        if (!currentLecturer().getId().equals(score.get().getGiangvienid()))
            throw new AccessDeniedException("Không được xem điểm của giảng viên khác.");
        return score;
    }

    @Override
    @Transactional
    public Diem save(Diem entity) {
        if (entity.getDangkydetaiid() == null || entity.getGiangvienid() == null)
            throw new NghiepvuException("Thiếu đăng ký hoặc giảng viên chấm.");
        var registration = registrations.findById(entity.getDangkydetaiid())
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy đăng ký đề tài."));
        var teacher = currentLecturer();
        if (!teacher.getId().equals(entity.getGiangvienid()))
            throw new AccessDeniedException("Giảng viên chỉ được nhập điểm cho chính mình.");
        var result = results.findByDangkydetaiid(entity.getDangkydetaiid())
                .orElseGet(() -> {
                    if (registration.getTrangthai() != TrangthaiDangkydetai.DADUYET) {
                        throw new NghiepvuException("Đăng ký đề tài chưa được duyệt chính thức.");
                    }
                    Ketqua emptyKq = new Ketqua();
                    emptyKq.setDangkydetaiid(entity.getDangkydetaiid());
                    emptyKq.setDiemcuoi(null);
                    emptyKq.setDacongbo(false);
                    return results.save(emptyKq);
                });
        var assignment = assignments.findById(entity.getDangkydetaiid())
                .orElseThrow(() -> new NghiepvuException("Chưa phân công hình thức đánh giá."));
        Set<Long> permitted = validator.assignedGraders(validator.context(result), assignment);
        if (!permitted.contains(teacher.getId()))
            throw new AccessDeniedException("Giảng viên chưa được phân công chấm đề tài này.");
        if (!lecturers.existsById(entity.getGiangvienid()))
            throw new NghiepvuException("Giảng viên không tồn tại.");
        if (entity.getDiemso() == null || entity.getDiemso().compareTo(BigDecimal.ZERO) < 0
                || entity.getDiemso().compareTo(BigDecimal.TEN) > 0)
            throw new NghiepvuException("Điểm phải trong khoảng 0–10.");
        if (supervisors.existsByDetaiidAndGiangvienid(registration.getDetaiid(), entity.getGiangvienid()))
            throw new NghiepvuException("Giảng viên hướng dẫn không được tự chấm đề tài.");
        if (entity.getId() != null) {
            var old = repository.findById(entity.getId())
                    .orElseThrow(() -> new NghiepvuException("Không tìm thấy điểm cần cập nhật."));
            if (!old.getGiangvienid().equals(entity.getGiangvienid())
                    || !old.getDangkydetaiid().equals(entity.getDangkydetaiid()))
                throw new NghiepvuException("Không được đổi giảng viên hoặc đăng ký của điểm đã có.");
        }
        if (entity.getId() == null && repository.existsByDangkydetaiidAndGiangvienid(
                entity.getDangkydetaiid(), entity.getGiangvienid()))
            throw new NghiepvuException("Giảng viên này đã nhập điểm cho đăng ký.");
        if (results.existsByDangkydetaiidAndDacongboTrue(entity.getDangkydetaiid()))
            throw new NghiepvuException("Kết quả đã công bố, không được sửa điểm.");
        Diem saved;
        try {
            saved = repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException ex) {
            throw new NghiepvuException("Giảng viên này đã nhập điểm cho đăng ký.");
        }
        var allScores = repository.findByDangkydetaiid(entity.getDangkydetaiid());
        if (!allScores.isEmpty()) {
            BigDecimal total = allScores.stream().map(Diem::getDiemso).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avg = total.divide(BigDecimal.valueOf(allScores.size()), 2, java.math.RoundingMode.HALF_UP);
            result.setDiemcuoi(avg);
            results.save(result);
        }
        return saved;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new AccessDeniedException("Chưa đăng nhập.");
        boolean isTruongKhoa = auth.getAuthorities().stream().anyMatch(a -> "ROLE_TRUONGKHOA".equals(a.getAuthority()));
        var score = repository.findById(id)
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy điểm cần xóa."));
        if (!isTruongKhoa && !currentLecturer().getId().equals(score.getGiangvienid())) {
            throw new AccessDeniedException("Bạn không có quyền xóa điểm này.");
        }
        if (results.existsByDangkydetaiidAndDacongboTrue(score.getDangkydetaiid())) {
            throw new NghiepvuException("Kết quả đã công bố, không được xóa điểm.");
        }
        Long dkId = score.getDangkydetaiid();
        repository.deleteById(id);

        results.findByDangkydetaiid(dkId).ifPresent(kq -> {
            var remaining = repository.findByDangkydetaiid(dkId);
            if (remaining.isEmpty()) {
                kq.setDiemcuoi(null);
            } else {
                BigDecimal total = remaining.stream().map(Diem::getDiemso).reduce(BigDecimal.ZERO, BigDecimal::add);
                kq.setDiemcuoi(total.divide(BigDecimal.valueOf(remaining.size()), 2, java.math.RoundingMode.HALF_UP));
            }
            results.save(kq);
        });
    }


    @Override
    @Transactional(readOnly = true)
    public GradeFormOptions availableGradeOptions() {
        Long lecturerId = currentLecturer().getId();

        var rows = registrations.findAvailableGradesForLecturer(
                lecturerId,
                TrangthaiDangkydetai.DADUYET
        );

        var rounds = new java.util.LinkedHashMap<Long, GradeRoundOption>();
        var topics = new java.util.ArrayList<GradeTopicOption>();

        for (Object[] row : rows) {
            Long registrationId = ((Number) row[0]).longValue();
            Long roundId = ((Number) row[1]).longValue();
            String roundName = (String) row[2];
            String topicName = (String) row[3];
            String groupName = (String) row[4];

            rounds.putIfAbsent(
                    roundId,
                    new GradeRoundOption(roundId, roundName));

            topics.add(new GradeTopicOption(
                    registrationId, roundId, topicName, groupName));
        }

        return new GradeFormOptions(
                java.util.List.copyOf(rounds.values()),
                java.util.List.copyOf(topics));
    }
    @Override
    public Long currentLecturerId() {
        return currentLecturer().getId();
    }

    private Giangvien currentLecturer() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new AccessDeniedException("Chưa đăng nhập.");
        var account = accounts.findByTendangnhap(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("Tài khoản không tồn tại."));
        if (!Boolean.TRUE.equals(account.getKichhoat())) throw new AccessDeniedException("Tài khoản đã khóa.");
        return lecturers.findByTaikhoanid(account.getId())
                .orElseThrow(() -> new AccessDeniedException("Tài khoản chưa liên kết giảng viên."));
    }
}
