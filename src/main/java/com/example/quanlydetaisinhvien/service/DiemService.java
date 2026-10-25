package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.dto.DiemFormDto;
import com.example.quanlydetaisinhvien.dto.DiemViewDto;
import com.example.quanlydetaisinhvien.entity.Diem;
import com.example.quanlydetaisinhvien.entity.Ketqua;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DiemService extends CrudService<Diem, Long> {
    record GradeRoundOption(Long id, String name) {}

    record GradeTopicOption(
            Long registrationId,
            Long roundId,
            String topicName,
            String groupName) {}

    record GradeFormOptions(
            java.util.List<GradeRoundOption> rounds,
            java.util.List<GradeTopicOption> topics) {}

    Page<Diem> findPage(Pageable pageable);
    Long currentLecturerId();
    GradeFormOptions availableGradeOptions();

    List<DiemViewDto> findAllViewDto();
    DiemFormDto getFormDtoById(Long id);
    Diem nhapDiem(DiemFormDto form);
    void xoaDiem(Long id);
    Ketqua tinhKetQuaTrungGian(Long dangkydetaiid);
}
