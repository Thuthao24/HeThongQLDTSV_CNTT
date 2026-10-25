package com.example.quanlydetaisinhvien.service.impl;

import com.example.quanlydetaisinhvien.dto.KetquaDoiChieuDto;
import com.example.quanlydetaisinhvien.entity.Dangkydetai;
import com.example.quanlydetaisinhvien.entity.Detai;
import com.example.quanlydetaisinhvien.entity.Diem;
import com.example.quanlydetaisinhvien.entity.HinhthucDanhgia;
import com.example.quanlydetaisinhvien.entity.Ketqua;
import com.example.quanlydetaisinhvien.entity.Nhomsinhvien;
import com.example.quanlydetaisinhvien.entity.TrangthaiDangkydetai;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.DangkydetaiRepository;
import com.example.quanlydetaisinhvien.repository.DetaiRepository;
import com.example.quanlydetaisinhvien.repository.DiemRepository;
import com.example.quanlydetaisinhvien.repository.KetquaRepository;
import com.example.quanlydetaisinhvien.repository.NhomsinhvienRepository;
import com.example.quanlydetaisinhvien.service.KetquaPublicationInfo;
import com.example.quanlydetaisinhvien.service.KetquaPublicationService;
import com.example.quanlydetaisinhvien.service.KetquaService;
import com.example.quanlydetaisinhvien.service.NghiepvuDetaiService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class KetquaServiceImpl implements KetquaService {

    private final KetquaRepository repository;
    private final KetquaPublicationService publicationService;
    private final NghiepvuDetaiService businessService;
    private final DiemRepository diemRepository;
    private final DangkydetaiRepository dangkydetaiRepository;
    private final DetaiRepository detaiRepository;
    private final NhomsinhvienRepository nhomsinhvienRepository;

    public KetquaServiceImpl(
            KetquaRepository repository,
            KetquaPublicationService publicationService,
            NghiepvuDetaiService businessService,
            DiemRepository diemRepository,
            DangkydetaiRepository dangkydetaiRepository,
            DetaiRepository detaiRepository,
            NhomsinhvienRepository nhomsinhvienRepository) {
        this.repository = repository;
        this.publicationService = publicationService;
        this.businessService = businessService;
        this.diemRepository = diemRepository;
        this.dangkydetaiRepository = dangkydetaiRepository;
        this.detaiRepository = detaiRepository;
        this.nhomsinhvienRepository = nhomsinhvienRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, KetquaPublicationInfo> publicationInfo(List<Ketqua> results) {
        requireTruongKhoa();
        return publicationService.info(results);
    }

    @Override
    public void phanCongHoiDong(Long id, HinhthucDanhgia form, Long councilId) {
        requireTruongKhoa();
        publicationService.assign(id, form, councilId);
    }

    @Override
    public Ketqua congboKetqua(Long id) {
        requireTruongKhoa();
        return businessService.congboKetqua(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ketqua> findAll() {
        return findVisibleToCurrentUser();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Ketqua> findVisiblePage(Pageable pageable) {
        Authentication authentication = requireResultReader();

        if (pageable == null || pageable.isUnpaged()) {
            throw new IllegalArgumentException("Danh sách kết quả bắt buộc phân trang.");
        }

        var request = PageRequest.of(
                Math.max(0, pageable.getPageNumber()),
                Math.min(100, Math.max(1, pageable.getPageSize())),
                Sort.by("id").descending()
        );

        if (hasRole(authentication, "ROLE_TRUONGKHOA")) {
            return repository.findAll(request);
        }

        return repository.findPublishedPageForStudent(
                authentication.getName(),
                request
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ketqua> findVisibleToCurrentUser() {
        Authentication authentication = requireResultReader();
        if (hasRole(authentication, "ROLE_TRUONGKHOA")) {
            return repository.findAll();
        }
        return repository.findPublishedForStudent(authentication.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Ketqua> findById(Long id) {
        Authentication authentication = requireResultReader();
        if (hasRole(authentication, "ROLE_TRUONGKHOA")) {
            return repository.findById(id);
        }
        return repository.findPublishedByIdForStudent(id, authentication.getName());
    }

    @Override
    public Ketqua save(Ketqua entity) {
        requireTruongKhoa();
        throw new NghiepvuException("Không ghi kết quả trực tiếp; sử dụng quy trình tính điểm và công bố.");
    }

    @Override
    public void deleteById(Long id) {
        requireTruongKhoa();
        throw new NghiepvuException("Không xóa kết quả trực tiếp sau khi đã thực hiện đánh giá.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<KetquaDoiChieuDto> findAllDoiChieuDto() {
        requireTruongKhoa();
        return findDoiChieuDto(repository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<KetquaDoiChieuDto> findDoiChieuDto(List<Ketqua> ketquas) {
        requireTruongKhoa();
        if (ketquas == null || ketquas.isEmpty()) {
            return List.of();
        }

        Set<Long> dkIds = ketquas.stream()
                .map(Ketqua::getDangkydetaiid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Dangkydetai> dangKyList = dkIds.isEmpty() ? List.of() : dangkydetaiRepository.findAllById(dkIds);
        Map<Long, Dangkydetai> dkMap = dangKyList.stream()
                .collect(Collectors.toMap(Dangkydetai::getId, dk -> dk, (a, b) -> a));

        Set<Long> dtIds = dangKyList.stream()
                .map(Dangkydetai::getDetaiid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Detai> dtMap = dtIds.isEmpty() ? Map.of() : detaiRepository.findAllById(dtIds).stream()
                .collect(Collectors.toMap(Detai::getId, dt -> dt, (a, b) -> a));

        Set<Long> nhomIds = dangKyList.stream()
                .map(Dangkydetai::getNhomid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Nhomsinhvien> nhomMap = nhomIds.isEmpty() ? Map.of() : nhomsinhvienRepository.findAllById(nhomIds).stream()
                .collect(Collectors.toMap(Nhomsinhvien::getId, nh -> nh, (a, b) -> a));

        // Batch query diem theo cac dangkydetaiid voi chunk size toi da 500
        List<Diem> allDiems = new ArrayList<>();
        if (!dkIds.isEmpty()) {
            List<Long> idList = new ArrayList<>(dkIds);
            int batchSize = 500;
            for (int i = 0; i < idList.size(); i += batchSize) {
                List<Long> batch = idList.subList(i, Math.min(i + batchSize, idList.size()));
                allDiems.addAll(diemRepository.findByDangkydetaiidIn(batch));
            }
        }
        Map<Long, List<Diem>> diemByDkMap = allDiems.stream()
                .collect(Collectors.groupingBy(Diem::getDangkydetaiid));

        List<KetquaDoiChieuDto> dtos = new ArrayList<>();

        for (Ketqua kq : ketquas) {
            KetquaDoiChieuDto dto = new KetquaDoiChieuDto();
            dto.setId(kq.getId());
            dto.setDangkydetaiid(kq.getDangkydetaiid());
            dto.setDiemcuoi(kq.getDiemcuoi());
            dto.setDacongbo(kq.getDacongbo());
            dto.setNgaycongbo(kq.getNgaycongbo());

            Dangkydetai dk = dkMap.get(kq.getDangkydetaiid());
            if (dk != null) {
                Detai dt = dtMap.get(dk.getDetaiid());
                dto.setTendetai(dt != null ? dt.getTendetai() : "Đề tài #" + dk.getDetaiid());

                Nhomsinhvien nh = nhomMap.get(dk.getNhomid());
                dto.setTennhom(nh != null ? nh.getTennhom() : "Nhóm #" + dk.getNhomid());
            } else {
                dto.setTendetai("Đăng ký #" + kq.getDangkydetaiid());
                dto.setTennhom("---");
            }

            List<Diem> dsDiem = diemByDkMap.getOrDefault(kq.getDangkydetaiid(), List.of());
            dto.setSodiemthanhphan(dsDiem.size());
            if (!dsDiem.isEmpty()) {
                BigDecimal tong = dsDiem.stream()
                        .map(Diem::getDiemso)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal tb = tong.divide(BigDecimal.valueOf(dsDiem.size()), 2, RoundingMode.HALF_UP);
                dto.setDiemtrungbinhthanhphan(tb);
                dto.setKhop(kq.getDiemcuoi() != null && kq.getDiemcuoi().compareTo(tb) == 0);
            } else {
                dto.setDiemtrungbinhthanhphan(null);
                dto.setKhop(false);
            }

            dtos.add(dto);
        }

        return dtos;
    }

    @Override
    public void tinhLaiKetQua(Long dangkydetaiid) {
        requireTruongKhoa();
        businessService.tinhVaLuuKetqua(dangkydetaiid);
    }

    @Override
    public void tinhLaiTatCaKetQua() {
        requireTruongKhoa();
        List<Dangkydetai> allDk = dangkydetaiRepository.findAll();
        for (Dangkydetai dk : allDk) {
            if (dk.getTrangthai() == TrangthaiDangkydetai.DADUYET) {
                List<Diem> diems = diemRepository.findByDangkydetaiid(dk.getId());
                if (!diems.isEmpty()) {
                    Ketqua kq = repository.findByDangkydetaiid(dk.getId()).orElse(null);
                    if (kq == null || !Boolean.TRUE.equals(kq.getDacongbo())) {
                        businessService.tinhVaLuuKetqua(dk.getId());
                    }
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean doiChieuKetQua(Long dangkydetaiid) {
        requireTruongKhoa();
        return businessService.doiChieuKetQua(dangkydetaiid);
    }

    private Authentication requireResultReader() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(hasRole(authentication, "ROLE_TRUONGKHOA")
                || hasRole(authentication, "ROLE_SINHVIEN"))) {
            throw new AccessDeniedException("Bạn không có quyền tra cứu kết quả.");
        }
        return authentication;
    }

    private void requireTruongKhoa() {
        if (!hasRole(requireResultReader(), "ROLE_TRUONGKHOA")) {
            throw new AccessDeniedException("Chỉ Trưởng khoa được quản lý kết quả.");
        }
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }
}
