package com.example.quanlydetaisinhvien.service.morong;

import com.example.quanlydetaisinhvien.dto.morong.*;
import com.example.quanlydetaisinhvien.dto.morong.SearchModels.*;
import com.example.quanlydetaisinhvien.repository.morong.TimkiemNangcaoRepository;
import com.example.quanlydetaisinhvien.security.MorongPermissions;
import com.example.quanlydetaisinhvien.security.MorongPermissions.Dataset;
import com.example.quanlydetaisinhvien.validation.MorongFilterValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TimkiemNangcaoService {
    private final TimkiemNangcaoRepository repository;
    private final MorongFilterValidator validation;
    public TimkiemNangcaoService(TimkiemNangcaoRepository repository, MorongFilterValidator validation) {
        this.repository = repository; this.validation = validation;
    }
    public Result<DetaiRow> detai(DetaiFilterDTO filter) {
        check(Dataset.DETAI, filter); return repository.detai(filter);
    }
    public Result<DotdangkyRow> dotdangky(DotdangkyFilterDTO filter) {
        check(Dataset.DOTDANGKY, filter); return repository.dotdangky(filter);
    }
    public Result<KetquaRow> ketqua(KetquaFilterDTO filter) {
        check(Dataset.KETQUA, filter); return repository.ketqua(filter);
    }
    public FilterOptions options(Dataset dataset) {
        MorongPermissions.require(dataset); return repository.options(dataset);
    }
    public java.util.List<DetaiRow> exportDetai(DetaiFilterDTO filter, int limit) {
        check(Dataset.DETAI, filter); return repository.exportDetai(filter, limit);
    }
    public java.util.List<DotdangkyRow> exportDotdangky(DotdangkyFilterDTO filter, int limit) {
        check(Dataset.DOTDANGKY, filter); return repository.exportDotdangky(filter, limit);
    }
    public java.util.List<KetquaRow> exportKetqua(KetquaFilterDTO filter, int limit) {
        check(Dataset.KETQUA, filter); return repository.exportKetqua(filter, limit);
    }
    private void check(Dataset dataset, SearchFilterDTO filter) {
        MorongPermissions.require(dataset); validation.validate(dataset, filter);
    }
}
