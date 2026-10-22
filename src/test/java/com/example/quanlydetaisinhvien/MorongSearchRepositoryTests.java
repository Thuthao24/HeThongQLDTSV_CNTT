package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.dto.morong.*;
import com.example.quanlydetaisinhvien.dto.morong.SearchModels.*;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.security.MorongPermissions.Dataset;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class MorongSearchRepositoryTests extends MorongSearchTestSupport {
    @ParameterizedTest @ValueSource(strings = {"keyword", "description", "dotdangkyid", "bomonid", "loaidot", "trangthai", "giangviendexuatid", "giangvienhuongdanid", "combined"})
    void topicCriteriaAreAppliedInDatabase(String field) {
        var f = new DetaiFilterDTO();
        switch (field) {
            case "keyword" -> f.setKeyword("  HỆ THỐNG  ");
            case "description" -> f.setKeyword("TIẾNG VIỆT");
            case "dotdangkyid" -> f.setDotdangkyid(roundA.getId());
            case "bomonid" -> { f.setBomonid(departmentA.getId()); f.setKeyword("sinh viên"); }
            case "loaidot" -> f.setLoaidot(Loaidot.MONHOC);
            case "trangthai" -> f.setTrangthai(TrangthaiDetai.CONGBO);
            case "giangviendexuatid" -> { f.setGiangviendexuatid(teacherA.getId()); f.setKeyword("sinh viên"); }
            case "giangvienhuongdanid" -> f.setGiangvienhuongdanid(teacherA.getId());
            case "combined" -> {
                f.setKeyword("Tiếng Việt"); f.setDotdangkyid(roundA.getId()); f.setBomonid(departmentA.getId());
                f.setLoaidot(Loaidot.MONHOC); f.setTrangthai(TrangthaiDetai.CONGBO);
                f.setGiangviendexuatid(teacherA.getId()); f.setGiangvienhuongdanid(teacherB.getId());
            }
        }
        var page = repository.detai(f);
        assertThat(page.items()).extracting(DetaiRow::id).containsExactly(topicA.getId());
        assertThat(page.pageInfo().totalElements()).isEqualTo(1);
        assertThat(page.items().getFirst().tenbomon()).isEqualTo(departmentA.getTenbomon());
        assertThat(page.items().getFirst().giangviendexuatHoten()).isEqualTo("Kiểm thử search-teacher-a");
    }

    @ParameterizedTest @ValueSource(strings = {"%", "_", "!", "' OR '1'='1", "\\", "Tiếng Việt"})
    void topicKeywordEscapesWildcardsAndTreatsQuotesAsData(String keyword) {
        var f = new DetaiFilterDTO(); f.setKeyword(keyword);
        assertThat(service.detai(f).items()).extracting(DetaiRow::id).containsExactly(topicA.getId());
    }

    @ParameterizedTest @ValueSource(strings = {"keyword", "dotdangkyid", "loaidot", "trangthai", "combined"})
    void roundCriteriaAreCombinedWithoutInventedTopicType(String field) {
        var f = new DotdangkyFilterDTO();
        switch (field) {
            case "keyword" -> f.setKeyword("KHÓA LUẬN");
            case "dotdangkyid" -> f.setDotdangkyid(roundB.getId());
            case "loaidot" -> f.setLoaidot(Loaidot.KLTN);
            case "trangthai" -> f.setTrangthai(TrangthaiDotdangky.GIAIDOAN2);
            case "combined" -> {
                f.setKeyword("Khóa luận"); f.setDotdangkyid(roundB.getId());
                f.setLoaidot(Loaidot.KLTN); f.setTrangthai(TrangthaiDotdangky.GIAIDOAN2);
                f.setThoiGianTu(LocalDate.of(2026, 9, 5)); f.setThoiGianDen(LocalDate.of(2026, 9, 5));
            }
        }
        assertThat(service.dotdangky(f).items()).extracting(DotdangkyRow::id).containsExactly(roundB.getId());
    }

    @ParameterizedTest @EnumSource(DotdangkyFilterDTO.MocThoiGian.class)
    void everyActualRoundDateSupportsInclusiveDayBounds(DotdangkyFilterDTO.MocThoiGian field) {
        var date = switch (field) {
            case BATDAUGIANGVIEN -> roundA.getBatdaugiangvien();
            case KETTHUCGIANGVIEN -> roundA.getKetthucgiangvien();
            case BATDAUSINHVIEN -> roundA.getBatdausinhvien();
            case KETTHUCSINHVIEN -> roundA.getKetthucsinhvien();
            case HANCHAMPHANBIEN -> roundA.getHanchamphanbien();
            case NGAYBAOCAOHOIDONG -> roundA.getNgaybaocaohoidong();
        };
        var f = new DotdangkyFilterDTO(); f.setMocThoiGian(field);
        f.setThoiGianTu(date.toLocalDate()); f.setThoiGianDen(date.toLocalDate());
        assertThat(service.dotdangky(f).items()).extracting(DotdangkyRow::id).containsExactly(roundA.getId());
    }

    @ParameterizedTest @ValueSource(strings = {"keyword", "group", "dotdangkyid", "loaidot", "trangthai", "diemTu", "diemDen", "date", "combined"})
    void resultCriteriaFilterActualRegistrationStatusScoresAndPublicationDates(String field) {
        var f = new KetquaFilterDTO();
        switch (field) {
            case "keyword" -> f.setKeyword("sinh viên");
            case "group" -> f.setKeyword("NHÓM MỘT");
            case "dotdangkyid" -> f.setDotdangkyid(roundA.getId());
            case "loaidot" -> f.setLoaidot(Loaidot.MONHOC);
            case "trangthai" -> { f.setTrangthai(TrangthaiDangkydetai.DADUYET); f.setDacongbo(true); }
            case "diemTu" -> { f.setDiemTu(new BigDecimal("8.25")); f.setDiemDen(new BigDecimal("8.25")); }
            case "diemDen" -> { f.setDiemDen(new BigDecimal("8.25")); f.setDiemTu(new BigDecimal("8")); }
            case "date" -> { f.setNgayCongboTu(LocalDate.of(2026, 10, 15)); f.setNgayCongboDen(LocalDate.of(2026, 10, 15)); }
            case "combined" -> {
                f.setKeyword("Một"); f.setDotdangkyid(roundA.getId()); f.setLoaidot(Loaidot.MONHOC);
                f.setTrangthai(TrangthaiDangkydetai.DADUYET); f.setDacongbo(true);
                f.setDiemTu(new BigDecimal("8.25")); f.setDiemDen(new BigDecimal("8.25"));
                f.setNgayCongboTu(LocalDate.of(2026, 10, 15)); f.setNgayCongboDen(LocalDate.of(2026, 10, 15));
            }
        }
        var page = service.ketqua(f);
        assertThat(page.items()).extracting(KetquaRow::id).containsExactly(resultA.getId());
        assertThat(page.pageInfo().totalElements()).isEqualTo(1);
    }

    @Test void publishedFlagAndNullScoresHaveDistinctMeaningAndDayEndIsIncluded() {
        var f = new KetquaFilterDTO(); f.setDacongbo(false);
        assertThat(service.ketqua(f).items()).extracting(KetquaRow::id).containsExactly(draft.getId());
        f.setDiemTu(BigDecimal.ZERO);
        assertThat(service.ketqua(f).items()).isEmpty();
        f = new KetquaFilterDTO(); f.setNgayCongboTu(LocalDate.of(2026, 10, 16)); f.setNgayCongboDen(LocalDate.of(2026, 10, 16));
        assertThat(service.ketqua(f).items()).extracting(KetquaRow::id).containsExactly(resultB.getId());
    }

    @Test void validButUnknownOrConflictingCriteriaReturnEmptyWithoutExistenceErrors() {
        var f = new DetaiFilterDTO(); f.setDotdangkyid(Long.MAX_VALUE);
        assertThat(service.detai(f).items()).isEmpty();
        f.setDotdangkyid(roundA.getId()); f.setLoaidot(Loaidot.KLTN);
        assertThat(service.detai(f).pageInfo().totalElements()).isZero();
        var k = new KetquaFilterDTO(); k.setKeyword("không tồn tại");
        assertThat(service.ketqua(k).items()).isEmpty();
        var d = new DotdangkyFilterDTO(); d.setKeyword("không tồn tại");
        assertThat(service.dotdangky(d).items()).isEmpty();
    }

    @Test void databasePaginationCountAndStableSortDoNotDuplicateGuidedTopics() {
        var f = new DetaiFilterDTO(); f.setGiangvienhuongdanid(teacherB.getId()); f.setSize(1); f.setSort("id,asc");
        var first = service.detai(f);
        assertThat(first.items()).extracting(DetaiRow::id).containsExactly(topicA.getId());
        assertThat(first.pageInfo()).isEqualTo(new PageInfo(0, 1, 2, 2, false, true));
        f.setPage(1);
        assertThat(service.detai(f).items()).extracting(DetaiRow::id).containsExactly(topicB.getId());
        assertThat(service.detai(f).pageInfo().hasNext()).isFalse();
        f.setPage(99);
        assertThat(service.detai(f).items()).isEmpty();
        assertThat(service.detai(f).pageInfo().totalElements()).isEqualTo(2);
        var k = new KetquaFilterDTO(); k.setSize(1); k.setSort("diemcuoi,desc");
        assertThat(service.ketqua(k).items()).extracting(KetquaRow::id).containsExactly(resultB.getId());
        var d = new DotdangkyFilterDTO(); d.setSize(1); d.setSort("batdausinhvien,asc");
        assertThat(service.dotdangky(d).items()).extracting(DotdangkyRow::id).containsExactly(roundA.getId());
    }

    @Test void blankKeywordMatchesAllAndOptionsAreBasedOnRealRows() {
        var f = new DetaiFilterDTO(); f.setKeyword("  \t ");
        assertThat(service.detai(f).items()).hasSize(4);
        var opts = service.options(Dataset.DETAI);
        assertThat(opts.dotdangky()).hasSize(4).extracting(Option::value).doesNotContain(emptyRound.getId().toString());
        assertThat(opts.bomon()).hasSize(2);
        assertThat(opts.giangviendexuat()).hasSize(2);
        assertThat(opts.giangvienhuongdan()).hasSize(2);
        assertThat(service.options(Dataset.DOTDANGKY).dotdangky()).hasSize(5);
    }

    @Test void optionsAndCountsChangeWithDataRatherThanHardcodedValues() {
        var before = service.ketqua(new KetquaFilterDTO()).pageInfo().totalElements();
        em.remove(draft); em.flush();
        assertThat(service.ketqua(new KetquaFilterDTO()).pageInfo().totalElements()).isEqualTo(before - 1);
        assertThat(service.options(Dataset.KETQUA).dacongbo()).extracting(Option::value).containsExactly("true");
    }
}
