package com.example.quanlydetaisinhvien.service.impl;

import com.example.quanlydetaisinhvien.dto.DangkydetaiOptionDto;
import com.example.quanlydetaisinhvien.dto.GiangvienOptionDto;
import com.example.quanlydetaisinhvien.dto.PhanbienFormDto;
import com.example.quanlydetaisinhvien.dto.PhanbienViewDto;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.GiangvienphanbienService;
import com.example.quanlydetaisinhvien.service.NghiepvuDetaiService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class GiangvienphanbienServiceImpl implements GiangvienphanbienService {

    private final GiangvienphanbienRepository repository;
    private final DangkydetaiRepository dangkydetaiRepository;
    private final DetaiRepository detaiRepository;
    private final NhomsinhvienRepository nhomsinhvienRepository;
    private final DotdangkyRepository dotdangkyRepository;
    private final GiangvienRepository giangvienRepository;
    private final TaikhoanRepository taikhoanRepository;
    private final GiangvienhuongdanRepository giangvienhuongdanRepository;
    private final NghiepvuDetaiService nghiepvuDetaiService;
    private final DiemRepository diemRepository;
    private final KetquaRepository ketquaRepository;

    public GiangvienphanbienServiceImpl(
            GiangvienphanbienRepository repository,
            DangkydetaiRepository dangkydetaiRepository,
            DetaiRepository detaiRepository,
            NhomsinhvienRepository nhomsinhvienRepository,
            DotdangkyRepository dotdangkyRepository,
            GiangvienRepository giangvienRepository,
            TaikhoanRepository taikhoanRepository,
            GiangvienhuongdanRepository giangvienhuongdanRepository,
            NghiepvuDetaiService nghiepvuDetaiService,
            DiemRepository diemRepository,
            KetquaRepository ketquaRepository) {
        this.repository = repository;
        this.dangkydetaiRepository = dangkydetaiRepository;
        this.detaiRepository = detaiRepository;
        this.nhomsinhvienRepository = nhomsinhvienRepository;
        this.dotdangkyRepository = dotdangkyRepository;
        this.giangvienRepository = giangvienRepository;
        this.taikhoanRepository = taikhoanRepository;
        this.giangvienhuongdanRepository = giangvienhuongdanRepository;
        this.nghiepvuDetaiService = nghiepvuDetaiService;
        this.diemRepository = diemRepository;
        this.ketquaRepository = ketquaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Giangvienphanbien> findAll() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Giangvienphanbien> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Giangvienphanbien save(Giangvienphanbien entity) {
        if (entity == null || entity.getDangkydetaiid() == null || entity.getGiangvienid() == null) {
            throw new NghiepvuException("Thiếu thông tin phân công phản biện");
        }
        return nghiepvuDetaiService.phanGiangvienPhanbien(entity.getDangkydetaiid(), entity.getGiangvienid());
    }

    @Override
    public void deleteById(Long id) {
        Giangvienphanbien pb = repository.findById(id)
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy phân công phản biện"));

        if (ketquaRepository.existsByDangkydetaiidAndDacongboTrue(pb.getDangkydetaiid())) {
            throw new NghiepvuException("Kết quả đã công bố, không thể thay đổi phân công.");
        }

        if (diemRepository.existsByDangkydetaiidAndGiangvienid(pb.getDangkydetaiid(), pb.getGiangvienid())) {
            throw new NghiepvuException("Không thể xóa phân công phản biện vì giảng viên đã nhập điểm.");
        }

        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PhanbienViewDto> findAllViewDto() {
        List<Giangvienphanbien> listPb = repository.findAll();

        Map<Long, Dangkydetai> dkMap = dangkydetaiRepository.findAll().stream()
                .collect(Collectors.toMap(Dangkydetai::getId, dk -> dk, (a, b) -> a));
        Map<Long, Detai> detaiMap = detaiRepository.findAll().stream()
                .collect(Collectors.toMap(Detai::getId, d -> d, (a, b) -> a));
        Map<Long, Nhomsinhvien> nhomMap = nhomsinhvienRepository.findAll().stream()
                .collect(Collectors.toMap(Nhomsinhvien::getId, n -> n, (a, b) -> a));
        Map<Long, Dotdangky> dotMap = dotdangkyRepository.findAll().stream()
                .collect(Collectors.toMap(Dotdangky::getId, dot -> dot, (a, b) -> a));
        Map<Long, Giangvien> gvMap = giangvienRepository.findAll().stream()
                .collect(Collectors.toMap(Giangvien::getId, g -> g, (a, b) -> a));
        Map<Long, Taikhoan> tkMap = taikhoanRepository.findAll().stream()
                .collect(Collectors.toMap(Taikhoan::getId, t -> t, (a, b) -> a));
        List<Giangvienhuongdan> allGvhd = giangvienhuongdanRepository.findAll();

        List<PhanbienViewDto> result = new ArrayList<>();
        for (Giangvienphanbien pb : listPb) {
            PhanbienViewDto dto = new PhanbienViewDto();
            dto.setId(pb.getId());
            dto.setDangkydetaiid(pb.getDangkydetaiid());
            dto.setGiangvienid(pb.getGiangvienid());

            Giangvien gv = gvMap.get(pb.getGiangvienid());
            if (gv != null) {
                dto.setMagiangvien(gv.getMagiangvien());
                Taikhoan tk = tkMap.get(gv.getTaikhoanid());
                if (tk != null) {
                    dto.setTengiangvien(tk.getHoten());
                    dto.setEmail(tk.getEmail());
                }
            }

            Dangkydetai dk = dkMap.get(pb.getDangkydetaiid());
            if (dk != null) {
                Detai detai = detaiMap.get(dk.getDetaiid());
                if (detai != null) {
                    dto.setTendetai(detai.getTendetai());
                    Dotdangky dot = dotMap.get(detai.getDotdangkyid());
                    if (dot != null) {
                        dto.setTendot(dot.getTendot());
                    }

                    List<String> gvhdNames = allGvhd.stream()
                            .filter(h -> h.getDetaiid().equals(detai.getId()))
                            .map(h -> {
                                Giangvien gvhd = gvMap.get(h.getGiangvienid());
                                if (gvhd != null) {
                                    Taikhoan tk = tkMap.get(gvhd.getTaikhoanid());
                                    return tk != null ? tk.getHoten() + " (" + gvhd.getMagiangvien() + ")" : gvhd.getMagiangvien();
                                }
                                return "GV #" + h.getGiangvienid();
                            })
                            .collect(Collectors.toList());
                    dto.setDanhsachGvhd(String.join(", ", gvhdNames));
                }

                Nhomsinhvien nhom = nhomMap.get(dk.getNhomid());
                if (nhom != null) {
                    dto.setTennhom(nhom.getTennhom());
                }
            }

            dto.setDaCongBo(ketquaRepository.existsByDangkydetaiidAndDacongboTrue(pb.getDangkydetaiid()));
            dto.setDaCoDiem(diemRepository.existsByDangkydetaiidAndGiangvienid(pb.getDangkydetaiid(), pb.getGiangvienid()));

            result.add(dto);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DangkydetaiOptionDto> getDangkydetaiOptions() {
        Map<Long, Detai> detaiMap = detaiRepository.findAll().stream()
                .collect(Collectors.toMap(Detai::getId, d -> d, (a, b) -> a));
        Map<Long, Nhomsinhvien> nhomMap = nhomsinhvienRepository.findAll().stream()
                .collect(Collectors.toMap(Nhomsinhvien::getId, n -> n, (a, b) -> a));
        Map<Long, Giangvien> gvMap = giangvienRepository.findAll().stream()
                .collect(Collectors.toMap(Giangvien::getId, g -> g, (a, b) -> a));
        Map<Long, Taikhoan> tkMap = taikhoanRepository.findAll().stream()
                .collect(Collectors.toMap(Taikhoan::getId, t -> t, (a, b) -> a));
        List<Giangvienhuongdan> allGvhd = giangvienhuongdanRepository.findAll();

        List<DangkydetaiOptionDto> options = new ArrayList<>();
        for (Dangkydetai dk : dangkydetaiRepository.findAll()) {
            Detai detai = detaiMap.get(dk.getDetaiid());
            Nhomsinhvien nhom = nhomMap.get(dk.getNhomid());
            String tenDeTai = detai != null ? detai.getTendetai() : ("Đề tài #" + dk.getDetaiid());
            String tenNhom = nhom != null ? nhom.getTennhom() : ("Nhóm #" + dk.getNhomid());

            List<String> gvhdNames = Collections.emptyList();
            if (detai != null) {
                gvhdNames = allGvhd.stream()
                        .filter(h -> h.getDetaiid().equals(detai.getId()))
                        .map(h -> {
                            Giangvien gvhd = gvMap.get(h.getGiangvienid());
                            if (gvhd != null) {
                                Taikhoan tk = tkMap.get(gvhd.getTaikhoanid());
                                return tk != null ? tk.getHoten() + " (" + gvhd.getMagiangvien() + ")" : gvhd.getMagiangvien();
                            }
                            return "GV #" + h.getGiangvienid();
                        })
                        .collect(Collectors.toList());
            }

            String gvhdStr = gvhdNames.isEmpty() ? "Chưa có GVHD" : String.join(", ", gvhdNames);
            String hienthi = String.format("[ĐK #%d] %s (Nhóm: %s - GVHD: %s)", dk.getId(), tenDeTai, tenNhom, gvhdStr);

            options.add(new DangkydetaiOptionDto(dk.getId(), hienthi, gvhdStr));
        }
        return options;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GiangvienOptionDto> getGiangvienOptions() {
        Map<Long, Taikhoan> tkMap = taikhoanRepository.findAll().stream()
                .collect(Collectors.toMap(Taikhoan::getId, t -> t, (a, b) -> a));

        return giangvienRepository.findAll().stream()
                .map(gv -> {
                    Taikhoan tk = tkMap.get(gv.getTaikhoanid());
                    String hoten = tk != null ? tk.getHoten() : "";
                    return new GiangvienOptionDto(gv.getId(), gv.getMagiangvien(), hoten);
                })
                .sorted(Comparator.comparing(GiangvienOptionDto::getHienthi))
                .collect(Collectors.toList());
    }

    @Override
    public Giangvienphanbien phanCong(PhanbienFormDto form) {
        if (form == null || form.getDangkydetaiid() == null) {
            throw new NghiepvuException("Vui lòng chọn đề tài đăng ký");
        }
        if (form.getGiangvienid() == null) {
            throw new NghiepvuException("Vui lòng chọn giảng viên phản biện");
        }
        return nghiepvuDetaiService.phanGiangvienPhanbien(form.getDangkydetaiid(), form.getGiangvienid());
    }
}
