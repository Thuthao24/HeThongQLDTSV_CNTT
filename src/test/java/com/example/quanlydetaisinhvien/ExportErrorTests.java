package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.controller.morong.ExportController;
import com.example.quanlydetaisinhvien.dto.morong.DetaiFilterDTO;
import com.example.quanlydetaisinhvien.dto.morong.ExportModels.*;
import com.example.quanlydetaisinhvien.service.morong.export.*;
import com.example.quanlydetaisinhvien.exception.ExportLimitException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.io.IOException;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ExportErrorTests {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    void chief() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "chief", "unused", List.of(new SimpleGrantedAuthority("ROLE_TRUONGKHOA"))));
    }

    @ParameterizedTest @ValueSource(strings = {"db", "writer", "limit"})
    void errorsReturnGenericJsonAndNeverAnAttachmentOrPartialFile(String cause) throws Exception {
        chief(); var service = mock(ReportExportService.class);
        Exception error = switch (cause) {
            case "db" -> new DataAccessResourceFailureException("PRIVATE_DATABASE_ERROR");
            case "writer" -> new IOException("PRIVATE_FILE_ERROR");
            default -> new ExportLimitException("Vượt giới hạn xuất.");
        };
        when(service.export(any(), any(), any())).thenThrow(error);
        var mvc = MockMvcBuilders.standaloneSetup(new ExportController(service)).build();
        var response = mvc.perform(get("/morong/xuat/detai.xlsx"))
                .andExpect(status().is(cause.equals("limit") ? 422 : 503))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(content().contentTypeCompatibleWith("application/json")).andReturn().getResponse();
        assertThat(response.getContentAsString()).doesNotContain("PRIVATE_DATABASE_ERROR", "PRIVATE_FILE_ERROR");
    }

    @Test void directAnonymousAndUnauthenticatedCallsAreDeniedBeforeAnyDataRead() {
        var data = mock(ReportDataService.class);
        var service = new ReportExportService(data, mock(ExcelReportWriter.class), mock(PdfReportWriter.class));
        var anonymous = new AnonymousAuthenticationToken("key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        for (var authentication : Arrays.asList(null, anonymous, UsernamePasswordAuthenticationToken.unauthenticated("chief", "unused"))) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
            for (Dataset dataset : Dataset.values()) assertThatThrownBy(() -> service.export(dataset, new DetaiFilterDTO(), Format.XLSX))
                    .isInstanceOf(AccessDeniedException.class);
        }
        verifyNoInteractions(data);
    }
}
