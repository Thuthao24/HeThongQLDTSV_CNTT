package com.example.quanlydetaisinhvien.service.impl;

import com.example.quanlydetaisinhvien.dto.DiemFormDto;
import com.example.quanlydetaisinhvien.dto.DiemViewDto;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.DiemService;
import com.example.quanlydetaisinhvien.service.KetquaPublicationValidator;
import com.example.quanlydetaisinhvien.service.NghiepvuDetaiService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DiemServiceImpl implements DiemService {

    private final DiemRepository repository;
    private final DangkydetaiRepository registrations;
    private final GiangvienRepository lecturers;
    private final KetquaRepository results;
    private final GiangvienhuongdanRepository supervisors;
    private final TaikhoanRepository accounts;
    private final PhanconghoidongRepository assignments;
    private final KetquaPublicationValidator validator;
    private final NghiepvuDetaiService nghiepvuDetaiService;
    private final DetaiRepository detaiRepository;
    private final NhomsinhvienRepository nhomsinhvienRepository;
    private final GiangvienphanbienRepository reviewers;
    private final ThanhvienhoidongRepository councilMembers;

    public DiemServiceImpl(
            DiemRepository repository,
            DangkydetaiRepository registrations,
            GiangvienRepository lecturers,
            KetquaRepository results,
            GiangvienhuongdanRepository supervisors,
            TaikhoanRepository accounts,
            PhanconghoidongRepository assignments,
            KetquaPublicationValidator validator,
            NghiepvuDetaiService nghiepvuDetaiService,
            DetaiRepository detaiRepository,
            NhomsinhvienRepository nhomsinhvienRepository,
            GiangvienphanbienRepository reviewers,
            ThanhvienhoidongRepository councilMembers) {
        this.repository = repository;
        this.registrations = registrations;
        this.lecturers = lecturers;
        this.results = results;
        this.supervisors = supervisors;
        this.accounts = accounts;
        this.assignments = assignments;
        this.validator = validator;
        this.nghiepvuDetaiService = nghiepvuDetaiService;
        this.detaiRepository = detaiRepository;
        this.nhomsinhvienRepository = nhomsinhvienRepository;
        this.reviewers = reviewers;
        this.councilMembers = councilMembers;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Diem> findPage(Pageable pageable) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new AccessDeniedException("Chưa đăng nhập.");
        if (auth.getAuthorities().stream().anyMatch(a -> "ROLE_TRUONGKHOA".equals(a.getAuthority())))
            return repository.findAll(pageable);
        return repository.findByGiangvienid(currentLecturer().getId(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Diem> findAll() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new AccessDeniedException("Chưa đăng nhập.");
        if (auth.getAuthorities().stream().anyMatch(a -> "ROLE_TRUONGKHOA".equals(a.getAuthority())))
            return repository.findAll();
        var teacher = currentLecturer();
        return repository.findByGiangvienid(teacher.getId());
    }

    @Override
    @Transactional(readOnly = true)
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
    public Diem save(Diem entity) {
        if (entity.getDangkydetaiid() == null || entity.getGiangvienid() == null)
            throw new NghiepvuException("Thiếu đăng ký hoặc giảng viên chấm.");
        var registration = registrations.findById(entity.getDangkydetaiid())
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy đăng ký đề tài."));
        if (registration.getTrangthai() != null && registration.getTrangthai() != TrangthaiDangkydetai.DADUYET) {
            throw new NghiepvuException("Đăng ký đề tài chưa được duyệt, không thể nhập điểm.");
        }
        var teacher = currentLecturer();
        if (!teacher.getId().equals(entity.getGiangvienid()))
            throw new AccessDeniedException("Giảng viên chỉ được nhập điểm cho chính mình.");

        // P0-01: Nếu đăng ký đã duyệt mà chưa có bản ghi kết quả trung gian, tự động tạo kết quả
        var result = results.findByDangkydetaiid(entity.getDangkydetaiid())
                .orElseGet(() -> {
                    if (registration.getTrangthai() == TrangthaiDangkydetai.DADUYET) {
                        Ketqua kq = new Ketqua();
                        kq.setDangkydetaiid(entity.getDangkydetaiid());
                        kq.setDacongbo(false);
                        kq.setDiemcuoi(null);
                        return results.save(kq);
                    }
                    throw new NghiepvuException("Chưa lập kết quả cho đăng ký đề tài.");
                });

        var assignment = assignments.findById(entity.getDangkydetaiid())
                .orElse(null);
        if (assignment != null) {
            Set<Long> permitted = validator.assignedGraders(validator.context(result), assignment);
            if (!permitted.contains(teacher.getId()))
                throw new AccessDeniedException("Giảng viên chưa được phân công chấm đề tài này.");
        } else {
            boolean isReviewer = reviewers.existsByDangkydetaiidAndGiangvienid(entity.getDangkydetaiid(), teacher.getId());
            if (!isReviewer) {
                throw new AccessDeniedException("Giảng viên chưa được phân công chấm đề tài này.");
            }
        }
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

        Diem saved = repository.save(entity);
        try {
            nghiepvuDetaiService.tinhVaLuuKetqua(entity.getDangkydetaiid());
        } catch (Exception ignored) {
        }
        return saved;
    }

    @Override
    public void deleteById(Long id) {
        xoaDiem(id);
    }

    @Override
    @Transactional(readOnly = true)
    public GradeFormOptions availableGradeOptions() {
        Long lecturerId = currentLecturer().getId();

        var rows = registrations.findAvailableGradesForLecturer(
                lecturerId,
                TrangthaiDangkydetai.DADUYET
        );

        var roundsMap = new LinkedHashMap<Long, GradeRoundOption>();
        var topics = new ArrayList<GradeTopicOption>();

        for (Object[] row : rows) {
            Long registrationId = ((Number) row[0]).longValue();
            Long roundId = ((Number) row[1]).longValue();
            String roundName = (String) row[2];
            String topicName = (String) row[3];
            String groupName = (String) row[4];

            roundsMap.putIfAbsent(
                    roundId,
                    new GradeRoundOption(roundId, roundName));

            topics.add(new GradeTopicOption(
                    registrationId, roundId, topicName, groupName));
        }

        return new GradeFormOptions(
                List.copyOf(roundsMap.values()),
                List.copyOf(topics));
    }

    @Override
    public Long currentLecturerId() {
        return currentLecturer().getId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiemViewDto> findAllViewDto() {
        List<Diem> diems = findAll();

        Map<Long, Dangkydetai> dkMap = registrations.findAll().stream()
                .collect(Collectors.toMap(Dangkydetai::getId, dk -> dk, (a, b) -> a));
        Map<Long, Detai> dtMap = detaiRepository.findAll().stream()
                .collect(Collectors.toMap(Detai::getId, dt -> dt, (a, b) -> a));
        Map<Long, Nhomsinhvien> nhomMap = nhomsinhvienRepository.findAll().stream()
                .collect(Collectors.toMap(Nhomsinhvien::getId, nh -> nh, (a, b) -> a));

        Map<Long, String> gvNameMap = layMapTenGiangvien();

        List<DiemViewDto> views = new ArrayList<>();
        for (Diem d : diems) {
            DiemViewDto dto = new DiemViewDto();
            dto.setId(d.getId());
            dto.setDangkydetaiid(d.getDangkydetaiid());
            dto.setGiangvienid(d.getGiangvienid());
            dto.setDiemso(d.getDiemso());
            dto.setNhanxet(d.getNhanxet());

            Dangkydetai dk = dkMap.get(d.getDangkydetaiid());
            if (dk != null) {
                Detai dt = dtMap.get(dk.getDetaiid());
                dto.setTendetai(dt != null ? dt.getTendetai() : "Đề tài #" + dk.getDetaiid());

                Nhomsinhvien nh = nhomMap.get(dk.getNhomid());
                dto.setTennhom(nh != null ? nh.getTennhom() : "Nhóm #" + dk.getNhomid());
            } else {
                dto.setTendetai("Đăng ký #" + d.getDangkydetaiid());
                dto.setTennhom("---");
            }

            dto.setTengiangvien(gvNameMap.getOrDefault(d.getGiangvienid(), "GV #" + d.getGiangvienid()));
            views.add(dto);
        }
        return views;
    }

    @Override
    @Transactional(readOnly = true)
    public DiemFormDto getFormDtoById(Long id) {
        Diem d = findById(id)
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy điểm"));
        DiemFormDto dto = new DiemFormDto();
        dto.setId(d.getId());
        dto.setDangkydetaiid(d.getDangkydetaiid());
        dto.setGiangvienid(d.getGiangvienid());
        dto.setDiemso(d.getDiemso());
        dto.setNhanxet(d.getNhanxet());
        return dto;
    }

    @Override
    public Diem nhapDiem(DiemFormDto form) {
        if (form == null || form.getDangkydetaiid() == null) {
            throw new NghiepvuException("Vui lòng chọn đề tài đăng ký");
        }
        var registration = registrations.findById(form.getDangkydetaiid())
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy đăng ký đề tài."));
        if (registration.getTrangthai() != null && registration.getTrangthai() != TrangthaiDangkydetai.DADUYET) {
            throw new NghiepvuException("Đăng ký đề tài chưa được duyệt, không thể nhập điểm.");
        }
        if (results.existsByDangkydetaiidAndDacongboTrue(form.getDangkydetaiid())) {
            throw new NghiepvuException("Kết quả đã công bố, không được sửa điểm.");
        }
        if (form.getDiemso() == null || form.getDiemso().compareTo(BigDecimal.ZERO) < 0
                || form.getDiemso().compareTo(BigDecimal.TEN) > 0) {
            throw new NghiepvuException("Điểm phải nằm trong thang điểm từ 0 đến 10");
        }

        // P0: Kiểm tra xác thực và chống chấm thay (Anti-impersonation)
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Chưa đăng nhập.");
        }
        var teacher = currentLecturer();
        if (form.getGiangvienid() != null && !form.getGiangvienid().equals(teacher.getId())) {
            throw new AccessDeniedException("Giảng viên chỉ được nhập điểm cho chính mình, không được chấm thay giảng viên khác.");
        }
        Long gradingLecturerId = teacher.getId();
        form.setGiangvienid(gradingLecturerId);

        if (form.getId() != null) {
            var existing = repository.findById(form.getId())
                    .orElseThrow(() -> new NghiepvuException("Không tìm thấy điểm cần cập nhật."));
            if (!gradingLecturerId.equals(existing.getGiangvienid())) {
                throw new AccessDeniedException("Không được sửa điểm của giảng viên khác.");
            }
            if (!form.getDangkydetaiid().equals(existing.getDangkydetaiid())) {
                throw new NghiepvuException("Không được đổi đăng ký của điểm đã có.");
            }
        }

        if (supervisors.existsByDetaiidAndGiangvienid(registration.getDetaiid(), gradingLecturerId)) {
            throw new NghiepvuException("Giảng viên hướng dẫn không được tự chấm đề tài.");
        }

        // Kiểm tra phân công hợp lệ (Reviewer hoặc Council member)
        boolean isReviewer = reviewers.existsByDangkydetaiidAndGiangvienid(form.getDangkydetaiid(), gradingLecturerId);
        boolean isCouncilMember = assignments.findById(form.getDangkydetaiid())
                .filter(a -> a.getHoidongid() != null)
                .map(a -> councilMembers.existsByHoidongidAndGiangvienid(a.getHoidongid(), gradingLecturerId))
                .orElse(false);

        if (!isReviewer && !isCouncilMember) {
            throw new AccessDeniedException("Giảng viên chưa được phân công chấm đề tài này.");
        }

        // P0-01: Tự động khởi tạo kết quả trung gian nếu chưa có
        results.findByDangkydetaiid(form.getDangkydetaiid())
                .orElseGet(() -> {
                    Ketqua kq = new Ketqua();
                    kq.setDangkydetaiid(form.getDangkydetaiid());
                    kq.setDacongbo(false);
                    kq.setDiemcuoi(null);
                    return results.save(kq);
                });

        Diem saved = nghiepvuDetaiService.chamDiem(
                form.getDangkydetaiid(),
                gradingLecturerId,
                form.getDiemso(),
                form.getNhanxet()
        );
        try {
            nghiepvuDetaiService.tinhVaLuuKetqua(form.getDangkydetaiid());
        } catch (Exception ignored) {
        }
        return saved;
    }

    @Override
    public void xoaDiem(Long id) {
        Diem d = repository.findById(id)
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy điểm"));
        if (results.existsByDangkydetaiidAndDacongboTrue(d.getDangkydetaiid())) {
            throw new NghiepvuException("Kết quả đã công bố, không thể xóa điểm.");
        }
        repository.deleteById(id);
        try {
            nghiepvuDetaiService.tinhVaLuuKetqua(d.getDangkydetaiid());
        } catch (Exception ignored) {
        }
    }

    @Override
    public Ketqua tinhKetQuaTrungGian(Long dangkydetaiid) {
        return nghiepvuDetaiService.tinhVaLuuKetqua(dangkydetaiid);
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

    private Map<Long, String> layMapTenGiangvien() {
        Map<Long, String> gvNameMap = new HashMap<>();
        List<Giangvien> gvs = lecturers.findAll();
        Map<Long, Taikhoan> tkMap = accounts.findAll().stream()
                .collect(Collectors.toMap(Taikhoan::getId, tk -> tk, (a, b) -> a));

        for (Giangvien gv : gvs) {
            Taikhoan tk = tkMap.get(gv.getTaikhoanid());
            String hoten = tk != null ? tk.getHoten() : gv.getMagiangvien();
            gvNameMap.put(gv.getId(), gv.getMagiangvien() + " - " + hoten);
        }
        return gvNameMap;
    }
}
