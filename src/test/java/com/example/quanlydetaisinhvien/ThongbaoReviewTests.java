package com.example.quanlydetaisinhvien;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import com.example.quanlydetaisinhvien.dto.ThongbaoFormDTO;
import com.example.quanlydetaisinhvien.service.ThongbaoRevision;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ThongbaoReviewTests extends ThongbaoTestSupport {
    @Test void repeatedCreateRequestInTheSameSessionDoesNotCreateAnotherNotice() throws Exception {
        var session=new MockHttpSession();
        var first=mvc.perform(post("/thongbao").session(session).with(user("notice-chief").roles("TRUONGKHOA")).with(csrf())
                .param("recipientRoles","SINHVIEN","GIANGVIEN","TRUONGBOMON").param("tieude","Một yêu cầu").param("noidung","Nội dung tiếng Việt")).andExpect(status().is3xxRedirection()).andReturn();
        var repeated=mvc.perform(post("/thongbao").session(session).with(user("notice-chief").roles("TRUONGKHOA")).with(csrf())
                .param("recipientRoles","SINHVIEN","GIANGVIEN","TRUONGBOMON").param("tieude","Một yêu cầu").param("noidung","Nội dung tiếng Việt")).andExpect(status().is3xxRedirection()).andReturn();
        assertThat(repeated.getResponse().getRedirectedUrl()).isEqualTo(first.getResponse().getRedirectedUrl());
        assertThat(repository.count()).isEqualTo(1);
    }
    @Test void outOfRangePageReturnsTheLastAvailablePage() throws Exception {
        notice("Còn một thông báo","Nội dung",chief.getId(),oldDate);
        var page=service.searchPage("",org.springframework.data.domain.PageRequest.of(999,20));
        assertThat(page.getNumber()).isZero(); assertThat(page.getContent()).hasSize(1);
    }
    @Test void unicodeWhitespaceCannotCreateAnInvisibleTitle() {
        assertThatThrownBy(()->service.create(form("\u00a0\u2007\u202f","Nội dung")))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        assertThatThrownBy(()->service.create(form("Hợp lệ","\u00a0\u2007\u202f")))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        assertThatThrownBy(()->service.create(form("\u200b\u200d\u0000","Nội dung")))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
    }
    @Test void serverTimezoneDoesNotChangeVietnamNoticeTime() {
        var original=java.util.TimeZone.getDefault();
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
            var before=java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).minusSeconds(1);
            var created=service.create(form("Giờ Việt Nam","Nội dung"));
            assertThat(created.ngaydang()).isBetween(before,java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).plusSeconds(1));
        } finally { java.util.TimeZone.setDefault(original); }
    }
    @Test void twoEditFormsCannotOverwriteTheNewerContent() throws Exception {
        var row=notice("Gốc","Nội dung gốc",chief.getId(),oldDate);
        String revision=ThongbaoRevision.digest(row.getTieude(),row.getNoidung());
        mvc.perform(post("/thongbao/"+row.getId()+"/edit").with(user("notice-chief").roles("TRUONGKHOA")).with(csrf())
                .param("revision",revision).param("recipientRoles","SINHVIEN","GIANGVIEN","TRUONGBOMON").param("tieude","Bản lưu đầu").param("noidung","Nội dung mới"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/thongbao/"+row.getId()+"/edit").with(user("notice-chief").roles("TRUONGKHOA")).with(csrf())
                .param("revision",revision).param("recipientRoles","SINHVIEN","GIANGVIEN","TRUONGBOMON").param("tieude","Bản cũ đến sau").param("noidung","Không được ghi đè"))
                .andExpect(status().isConflict()).andExpect(model().attributeHasErrors("thongbaoForm"));
        assertThat(repository.findById(row.getId()).orElseThrow().getTieude()).isEqualTo("Bản lưu đầu");
    }
    @Test void repeatedIdenticalEditIsSafeButMissingRevisionCannotOverwrite() throws Exception {
        var row=notice("Gốc","Nội dung",chief.getId(),oldDate);
        String revision=ThongbaoRevision.digest(row.getTieude(),row.getNoidung());
        for(int i=0;i<2;i++) mvc.perform(post("/thongbao/"+row.getId()+"/edit").with(user("notice-chief").roles("TRUONGKHOA")).with(csrf())
                .param("revision",revision).param("recipientRoles","SINHVIEN","GIANGVIEN","TRUONGBOMON").param("tieude","Bản mới").param("noidung","Nội dung mới"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/thongbao/"+row.getId()+"/edit").with(user("notice-chief").roles("TRUONGKHOA")).with(csrf())
                .param("recipientRoles","SINHVIEN","GIANGVIEN","TRUONGBOMON").param("tieude","Bỏ qua revision").param("noidung","Nội dung khác"))
                .andExpect(status().isConflict());
        assertThat(repository.findById(row.getId()).orElseThrow().getNgaydang()).isEqualTo(oldDate);
    }
    @Test void freshCreateFormAllowsIntentionalSameContentButReplaysAreDeduplicated() throws Exception {
        var session=new MockHttpSession();
        for(int i=0;i<2;i++) {
            var form=mvc.perform(get("/thongbao/create").session(session).with(user("notice-chief").roles("TRUONGKHOA")))
                    .andExpect(status().isOk()).andReturn();
            String token=(String)form.getModelAndView().getModel().get("submissionToken");
            assertThat(token).isNotBlank();
            String redirect=null;
            for(int replay=0;replay<2;replay++) {
                var result=mvc.perform(post("/thongbao").session(session).with(user("notice-chief").roles("TRUONGKHOA")).with(csrf())
                        .param("submissionToken",token).param("recipientRoles","SINHVIEN","GIANGVIEN","TRUONGBOMON").param("tieude","Cùng nội dung có chủ đích").param("noidung","Nội dung"))
                        .andExpect(status().is3xxRedirection()).andReturn();
                if(redirect!=null) assertThat(result.getResponse().getRedirectedUrl()).isEqualTo(redirect);
                redirect=result.getResponse().getRedirectedUrl();
            }
        }
        assertThat(repository.count()).isEqualTo(2);
    }
    @Test void unknownOrAlteredSubmissionCannotCreateAnotherNotice() throws Exception {
        var session=new MockHttpSession();
        mvc.perform(post("/thongbao").session(session).with(user("notice-chief").roles("TRUONGKHOA")).with(csrf())
                .param("submissionToken","unissued").param("recipientRoles","SINHVIEN","GIANGVIEN","TRUONGBOMON").param("tieude","Không hợp lệ").param("noidung","Nội dung"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/thongbao").session(session).with(user("notice-chief").roles("TRUONGKHOA")).with(csrf())
                .param("submissionToken","legacy:forged").param("recipientRoles","SINHVIEN","GIANGVIEN","TRUONGBOMON").param("tieude","Không hợp lệ").param("noidung","Nội dung"))
                .andExpect(status().isBadRequest());
        assertThat(repository.count()).isZero();
    }
    @Test void pagingRetainsSearchAndStableOrderAcrossBoundaries() throws Exception {
        for(int i=0;i<25;i++) notice("Lịch tiếng Việt "+i,"Đăng ký",chief.getId(),oldDate);
        var first=service.searchPage("  LỊCH  ",org.springframework.data.domain.PageRequest.of(0,20));
        var second=service.searchPage("lịch",org.springframework.data.domain.PageRequest.of(1,20));
        assertThat(first.getTotalElements()).isEqualTo(25); assertThat(first.getContent()).hasSize(20);
        assertThat(second.getContent()).hasSize(5);
        assertThat(first.getContent()).doesNotContainAnyElementsOf(second.getContent());
        assertThat(service.searchPage("missing",org.springframework.data.domain.PageRequest.of(99,20)).getNumber()).isZero();
    }
}
