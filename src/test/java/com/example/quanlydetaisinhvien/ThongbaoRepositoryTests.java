package com.example.quanlydetaisinhvien;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Sort;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class ThongbaoRepositoryTests extends ThongbaoTestSupport {
    final Sort newest = Sort.by(Sort.Order.desc("ngaydang"), Sort.Order.desc("id"));

    @Test void searchesBothTitleAndBodyIgnoringCaseWithoutDuplicateRows() {
        var title = notice("Đăng ký NCKH", "Thông tin", chief.getId(), oldDate);
        var body = notice("Kế hoạch", "Đăng ký nckh tại khoa", chief.getId(), oldDate);
        var both = notice("NCKH", "NCKH", chief.getId(), oldDate);
        notice("Thông báo khác", "Không liên quan", chief.getId(), oldDate);
        assertThat(repository.findByTieudeContainingIgnoreCaseOrNoidungContainingIgnoreCase("nckh", "nckh", newest))
                .extracting(row -> row.getId()).containsExactly(both.getId(), body.getId(), title.getId());
    }

    @ParameterizedTest @ValueSource(strings = {"%", "_", "' OR '1'='1", "Tiếng Việt", "\\"})
    void searchTreatsWildcardsAndQuotesAsLiteralData(String keyword) {
        var expected = notice("Thông tin " + keyword, "Nội dung", chief.getId(), oldDate);
        notice("Bản ghi thông thường", "Không khớp", chief.getId(), oldDate);
        assertThat(repository.findByTieudeContainingIgnoreCaseOrNoidungContainingIgnoreCase(keyword, keyword, newest))
                .extracting(row -> row.getId()).containsExactly(expected.getId());
    }

    @Test void newestDateThenLargestIdDefinesStableOrder() {
        var oldest = notice("Cũ", "Nội dung", chief.getId(), oldDate.minusDays(1));
        var first = notice("Mới 1", "Nội dung", chief.getId(), oldDate);
        var second = notice("Mới 2", "Nội dung", chief.getId(), oldDate);
        assertThat(repository.findAll(newest)).extracting(row -> row.getId())
                .containsExactly(second.getId(), first.getId(), oldest.getId());
    }

    @Test void textMappingRoundTripsTenThousandCharacters() {
        String body = "Đ".repeat(10000);
        var expected = notice("Nội dung dài", body, chief.getId(), oldDate);
        em.clear();
        assertThat(repository.findById(expected.getId()).orElseThrow().getNoidung()).isEqualTo(body);
    }

    @Test void unmatchedSearchAndEmptyDatabaseReturnEmptyLists() {
        assertThat(repository.findAll()).isEmpty();
        notice("Thông báo", "Nội dung", chief.getId(), oldDate);
        assertThat(repository.findByTieudeContainingIgnoreCaseOrNoidungContainingIgnoreCase("missing", "missing", newest)).isEmpty();
    }
}
