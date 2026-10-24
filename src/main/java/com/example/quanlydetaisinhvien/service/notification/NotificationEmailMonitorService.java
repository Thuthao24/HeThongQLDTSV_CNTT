package com.example.quanlydetaisinhvien.service.notification;

import com.example.quanlydetaisinhvien.config.NotificationEmailSettings;
import com.example.quanlydetaisinhvien.dto.NotificationEmailFilterDTO;
import com.example.quanlydetaisinhvien.dto.NotificationEmailModels.*;
import com.example.quanlydetaisinhvien.entity.TrangthaiEmailthongbao;
import com.example.quanlydetaisinhvien.repository.NotificationEmailMonitorRepository;
import com.example.quanlydetaisinhvien.security.NotificationEmailPermissions;
import jakarta.validation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Transactional(readOnly=true)
public class NotificationEmailMonitorService {
    private final NotificationEmailMonitorRepository repository;
    private final NotificationEmailPermissions permissions;
    private final NotificationEmailSchema schema;
    private final NotificationEmailSettings settings;
    private final NotificationEmailSafety safety;
    private final Validator validator;
    public NotificationEmailMonitorService(NotificationEmailMonitorRepository repository, NotificationEmailPermissions permissions,
            NotificationEmailSchema schema, NotificationEmailSettings settings, NotificationEmailSafety safety, Validator validator) {
        this.repository=repository; this.permissions=permissions; this.schema=schema;
        this.settings=settings; this.safety=safety; this.validator=validator;
    }
    public Monitor read(NotificationEmailFilterDTO filter) {
        permissions.requireChief();
        if (filter == null) throw new IllegalArgumentException("Thiếu bộ lọc.");
        var violations=validator.validate(filter);
        if (!violations.isEmpty()) throw new ConstraintViolationException(violations);
        if (!schema.available()) return new Monitor(false,false,false,
                new Counts(0,0,0,0,0,0,0),List.of(),new Page(filter.getPage(),filter.getSize(),0,0));
        var counts=new EnumMap<TrangthaiEmailthongbao,Long>(TrangthaiEmailthongbao.class);
        for (var status : TrangthaiEmailthongbao.values()) counts.put(status,0L);
        for (Object[] row:repository.counts(filter)) counts.put((TrangthaiEmailthongbao)row[0],((Number)row[1]).longValue());
        long total=counts.values().stream().mapToLong(Long::longValue).sum();
        return new Monitor(true,false,safety.deliveryAllowed(),new Counts(total,
                counts.get(TrangthaiEmailthongbao.PENDING),counts.get(TrangthaiEmailthongbao.SENDING),counts.get(TrangthaiEmailthongbao.SENT),
                counts.get(TrangthaiEmailthongbao.FAILED),counts.get(TrangthaiEmailthongbao.MISSING_EMAIL),counts.get(TrangthaiEmailthongbao.UNCERTAIN)),
                repository.items(filter),new Page(filter.getPage(),filter.getSize(),total,(total+filter.getSize()-1)/filter.getSize()));
    }
}
