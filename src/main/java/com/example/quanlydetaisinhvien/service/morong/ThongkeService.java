package com.example.quanlydetaisinhvien.service.morong;

import com.example.quanlydetaisinhvien.dto.morong.ThongkeFilterDTO;
import com.example.quanlydetaisinhvien.dto.morong.StatisticsModels.*;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.repository.morong.ThongkeRepository;
import com.example.quanlydetaisinhvien.security.ThongkePermissions;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class ThongkeService {
    private final ThongkeRepository repository;
    private final Validator validator;
    public ThongkeService(ThongkeRepository repository, Validator validator) {
        this.repository = repository; this.validator = validator;
    }

    public Report statistics(ThongkeFilterDTO filter) {
        ThongkePermissions.requireChief();
        if (filter == null) throw new IllegalArgumentException("Thiếu bộ lọc.");
        var violations = validator.validate(filter);
        if (!violations.isEmpty()) throw new ConstraintViolationException(violations);
        Long selected = filter.getDotdangkyid();
        var data = repository.aggregate(selected);
        Map<Long, Counts> counts = new HashMap<>();
        Map<String, Long> topics = new HashMap<>(), registrations = new HashMap<>();
        for (Object[] row : data.topics()) {
            long count = number(row[2]);
            counts.computeIfAbsent((Long) row[0], k -> new Counts()).topics += count;
            topics.merge(code(row[1]), count, Long::sum);
        }
        for (Object[] row : data.groups()) {
            var c = counts.computeIfAbsent((Long) row[0], k -> new Counts());
            c.groups += number(row[1]); c.unregistered += number(row[2]);
        }
        for (Object[] row : data.registrations()) {
            long count = number(row[2]);
            var c = counts.computeIfAbsent((Long) row[0], k -> new Counts());
            c.registrations += count; c.mismatches += number(row[3]);
            registrations.merge(code(row[1]), count, Long::sum);
        }
        for (Object[] row : data.results()) {
            var c = counts.computeIfAbsent((Long) row[0], k -> new Counts());
            c.results += number(row[2]); c.missingScore += number(row[3]);
            if (Boolean.TRUE.equals(row[1])) c.published += number(row[2]);
        }
        var rounds = data.rounds().stream().filter(r -> selected == null || selected.equals(r.id()))
                .map(r -> {
                    var c = counts.getOrDefault(r.id(), new Counts());
                    return new RoundRow(r.id(), r.tendot(), r.loaidot(), c.topics, c.groups, c.registrations,
                            c.results, c.published, c.unregistered, c.missingScore, c.mismatches);
                }).toList();
        var all = new Counts();
        counts.values().forEach(c -> {
            all.topics += c.topics; all.groups += c.groups; all.registrations += c.registrations;
            all.results += c.results; all.published += c.published; all.unregistered += c.unregistered;
            all.missingScore += c.missingScore; all.mismatches += c.mismatches;
        });
        var summary = new Summary(all.topics, all.groups, all.registrations, all.results, all.published,
                all.results - all.published, all.unregistered, all.missingScore, all.mismatches);
        return new Report(summary, rounds,
                statuses(Arrays.stream(TrangthaiDetai.values()).map(Enum::name).toList(), topics, all.topics),
                statuses(Arrays.stream(TrangthaiDangkydetai.values()).map(Enum::name).toList(), registrations, all.registrations),
                List.of(status("DACONGBO", "Đã công bố", all.published, all.results),
                        status("CHUACONGBO", "Chưa công bố", all.results - all.published, all.results)),
                new FilterOptions(data.rounds()));
    }

    private static class Counts {
        long topics, groups, registrations, results, published, unregistered, missingScore, mismatches;
    }
    private long number(Object value) { return ((Number) value).longValue(); }
    private String code(Object value) { return value == null ? "KHONG_XAC_DINH" : value.toString(); }
    private List<StatusCount> statuses(List<String> codes, Map<String, Long> counts, long total) {
        var rows = new ArrayList<StatusCount>();
        for (String code : codes) {
            String label = switch (code) {
                case "CHODUYET" -> "Chờ duyệt";
                case "DADUYET" -> "Đã duyệt";
                case "CONGBO" -> "Công bố";
                case "TUCHOI" -> "Từ chối";
                default -> throw new IllegalArgumentException("Trạng thái không được hỗ trợ.");
            };
            rows.add(status(code, label, counts.getOrDefault(code, 0L), total));
        }
        // The delivered SQL allows NULL statuses although the entity marks them non-null.
        if (counts.containsKey("KHONG_XAC_DINH")) {
            rows.add(status("KHONG_XAC_DINH", "Không xác định", counts.get("KHONG_XAC_DINH"), total));
        }
        return List.copyOf(rows);
    }
    private StatusCount status(String code, String label, long count, long total) {
        var percentage = total == 0 ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(count).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        return new StatusCount(code, label, count, percentage);
    }
}
