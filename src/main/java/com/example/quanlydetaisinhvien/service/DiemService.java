package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.entity.Diem;

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

    GradeFormOptions availableGradeOptions();

    Long currentLecturerId();
}
