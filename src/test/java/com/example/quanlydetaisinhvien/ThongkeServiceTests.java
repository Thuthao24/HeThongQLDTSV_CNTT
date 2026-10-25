package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.controller.morong.ThongkeController;
import com.example.quanlydetaisinhvien.dto.morong.ThongkeFilterDTO;
import com.example.quanlydetaisinhvien.dto.morong.StatisticsModels.*;
import com.example.quanlydetaisinhvien.entity.Loaidot;
import com.example.quanlydetaisinhvien.repository.morong.ThongkeRepository;
import com.example.quanlydetaisinhvien.repository.morong.ThongkeRepository.Aggregates;
import com.example.quanlydetaisinhvien.service.morong.ThongkeService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.beans.Introspector;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ThongkeServiceTests {
    @Test void legacyNullStatusesRemainVisibleAndNullPublicationIsNotPublished() {
        var repository = mock(ThongkeRepository.class);
        when(repository.aggregate(null)).thenReturn(new Aggregates(List.of(new RoundOption(1L, "Đợt", Loaidot.NCKH)),
                List.<Object[]>of(new Object[] {1L, null, 2L}), List.<Object[]>of(new Object[] {1L, 1L, 0L}),
                List.<Object[]>of(new Object[] {1L, null, 1L, 0L}), List.<Object[]>of(new Object[] {1L, null, 1L, 1L})));
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "chief", "unused", List.of(new SimpleGrantedAuthority("ROLE_TRUONGKHOA"))));
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            var report = new ThongkeService(repository, validators.getValidator()).statistics(new ThongkeFilterDTO());
            assertThat(report.detaiByStatus()).hasSize(5);
            assertThat(report.detaiByStatus().getLast().code()).isEqualTo("KHONG_XAC_DINH");
            assertThat(report.detaiByStatus().getLast().count()).isEqualTo(2);
            assertThat(report.dangkyByStatus()).hasSize(4);
            assertThat(report.dangkyByStatus().getLast().count()).isEqualTo(1);
            assertThat(report.summary().chuaCongbo()).isEqualTo(1);
            assertThat(report.summary().ketquaThieuDiemCuoi()).isEqualTo(1);
        } finally { SecurityContextHolder.clearContext(); }
    }

    @Test void databaseFailureReturns503WithoutInternalDetails() throws Exception {
        var service = mock(ThongkeService.class);
        when(service.statistics(any())).thenThrow(new DataAccessResourceFailureException("PRIVATE_DATABASE_ERROR"));
        var mvc = MockMvcBuilders.standaloneSetup(new ThongkeController(service))
                .setViewResolvers(new MorongSearchTestSupport.BackendViews()).build();
        mvc.perform(get("/morong/thongke")).andExpect(status().isServiceUnavailable())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("message", "Chưa thể truy cập dữ liệu thống kê. Vui lòng thử lại."));
    }

    @Test void allFrontendDtoPropertiesHaveWorkingJavaBeanGetters() throws Exception {
        var summary = new Summary(1, 2, 3, 4, 1, 3, 0, 1, 0);
        var row = new RoundRow(1L, "Đợt", Loaidot.MONHOC, 1, 2, 3, 4, 1, 0, 1, 0);
        var status = new StatusCount("CHODUYET", "Chờ duyệt", 1, java.math.BigDecimal.TEN);
        var option = new RoundOption(1L, "Đợt", Loaidot.MONHOC);
        var options = new FilterOptions(List.of(option));
        var report = new Report(summary, List.of(row), List.of(status), List.of(status), List.of(status), options);
        for (Object dto : List.of(summary, row, status, option, options, ExportOptions.unavailable(), report, new ThongkeFilterDTO())) {
            var getters = Arrays.stream(Introspector.getBeanInfo(dto.getClass()).getPropertyDescriptors())
                    .filter(p -> !p.getName().equals("class")).collect(Collectors.toMap(p -> p.getName(), p -> p.getReadMethod()));
            assertThat(getters).isNotEmpty();
            for (var getter : getters.values()) {
                assertThat(getter).isNotNull(); getter.invoke(dto);
            }
            if (dto.getClass().isRecord()) {
                for (var component : dto.getClass().getRecordComponents()) assertThat(getters).containsKey(component.getName());
            }
        }
        assertThat(option.getValue()).isEqualTo("1");
        assertThat(option.getLabel()).isEqualTo("Đợt");
        assertThat(ExportOptions.unavailable().isEnabled()).isFalse();
    }
}
