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

    @Test void validButUnknownOrConflictingCriteriaReturnEmptyWithoutExistenceErrors() {
        var f = new DetaiFilterDTO(); f.setDotdangkyid(Long.MAX_VALUE);
        assertThat(service.detai(f).items()).isEmpty();
        f.setDotdangkyid(roundA.getId()); f.setLoaidot(Loaidot.KLTN);
        assertThat(service.detai(f).pageInfo().totalElements()).isZero();
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
    }

    @Test void blankKeywordMatchesAllAndOptionsAreBasedOnRealRows() {
        var f = new DetaiFilterDTO(); f.setKeyword("  \t ");
        assertThat(service.detai(f).items()).hasSize(4);
        var opts = service.options(Dataset.DETAI);
        assertThat(opts.dotdangky()).hasSize(4).extracting(Option::value).doesNotContain(emptyRound.getId().toString());
        assertThat(opts.bomon()).hasSize(2);
        assertThat(opts.giangviendexuat()).hasSize(2);
        assertThat(opts.giangvienhuongdan()).hasSize(2);
    }

}
