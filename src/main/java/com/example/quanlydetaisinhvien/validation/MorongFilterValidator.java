package com.example.quanlydetaisinhvien.validation;

import com.example.quanlydetaisinhvien.dto.morong.*;
import com.example.quanlydetaisinhvien.dto.morong.SearchModels.Option;
import com.example.quanlydetaisinhvien.security.MorongPermissions.Dataset;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class MorongFilterValidator {
    private final Validator validator;
    public MorongFilterValidator(Validator validator) { this.validator = validator; }
    public record Ordering(String field, boolean ascending) {}

    public Ordering validate(Dataset dataset, SearchFilterDTO filter) {
        if (filter == null) throw new IllegalArgumentException("Thiếu bộ lọc.");
        if (!(switch (dataset) {
            case DETAI -> filter instanceof DetaiFilterDTO;
        })) throw new IllegalArgumentException("Sai loại bộ lọc.");
        var violations = validator.validate(filter);
        if (!violations.isEmpty()) throw new ConstraintViolationException(violations);
        return ordering(dataset, filter.getSort());
    }

    public Ordering ordering(Dataset dataset, String sort) {
        if (sort == null) throw new IllegalArgumentException("Thiếu cách sắp xếp.");
        String[] parts = sort.split(",", -1);
        if (parts.length != 2 || !fields(dataset).contains(parts[0])
                || !(parts[1].equals("asc") || parts[1].equals("desc")))
            throw new IllegalArgumentException("Trường hoặc chiều sắp xếp không hợp lệ.");
        return new Ordering(parts[0], parts[1].equals("asc"));
    }

    public List<Option> sortOptions(Dataset dataset) {
        List<Option> result = new ArrayList<>();
        for (String field : fields(dataset)) {
            result.add(new Option(field + ",asc", field + " ↑"));
            result.add(new Option(field + ",desc", field + " ↓"));
        }
        return List.copyOf(result);
    }

    private List<String> fields(Dataset dataset) {
        return switch (dataset) {
            case DETAI -> List.of("id", "tendetai", "tendot", "trangthai");
        };
    }
}
