package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.dto.*;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.*;
import com.example.quanlydetaisinhvien.service.ThongbaoRevision;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpSession;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ThongbaoAudienceTests extends ThongbaoTestSupport {
    Set<DoituongThongbao> roles(int mask) {
        var result=EnumSet.noneOf(DoituongThongbao.class);
        for(var role:DoituongThongbao.values()) if((mask&(1<<role.ordinal()))!=0) result.add(role);
        return result;
    }
    ThongbaoFormDTO targeted(int mask,String title) { var f=form(title,"Nội dung tiếng Việt"); f.setRecipientRoles(roles(mask)); return f; }
    @ParameterizedTest @ValueSource(ints={1,2,3,4,5,6,7})
    void everyNonemptyAudienceIsAppliedToListSearchCountHeaderAndDetail(int mask) throws Exception {
        var saved=service.create(targeted(mask,"Chỉ dành đúng đối tượng"));
        assertThat(service.findAll()).hasSize(1); assertThat(service.findLatestForHeader()).isEmpty();
        assertThat(saved.recipientRoles()).isEqualTo(roles(mask));
        assertThat(jdbc.queryForObject("select count(*) from thongbaodoituong where Thongbaoid=?",Long.class,saved.id())).isEqualTo(roles(mask).size());
        for(var role:DoituongThongbao.values()) {
            authenticate("reader",role.name()); boolean allowed=roles(mask).contains(role);
            assertThat(service.search("ĐÚNG")).hasSize(allowed?1:0);
            assertThat(service.searchPage("đúng",PageRequest.of(0,1)).getTotalElements()).isEqualTo(allowed?1:0);
            assertThat(service.findLatestForHeader()).hasSize(allowed?1:0);
            mvc.perform(get("/thongbao/"+saved.id()).with(user("reader").roles(role.name())))
                    .andExpect(allowed?status().isOk():status().isNotFound());
        }
        authenticate("notice-chief","TRUONGKHOA"); assertThat(service.findById(saved.id())).isEqualTo(saved);
    }
    @ParameterizedTest @ValueSource(strings={"SINHVIEN","GIANGVIEN","TRUONGBOMON"})
    void limitAndPageCountAreAppliedAfterFilteringAndKeywordIsLiteral(String roleName) {
        var role=DoituongThongbao.valueOf(roleName);
        for(int i=0;i<8;i++) {var f=targeted(1<<role.ordinal(),"Phù hợp %_! "+i);service.create(f);}
        var other=DoituongThongbao.values()[(role.ordinal()+1)%3];
        for(int i=0;i<9;i++) service.create(targeted(1<<other.ordinal(),"Ẩn mới hơn %_! "+i));
        em.flush(); em.clear(); authenticate("reader",roleName);
        assertThat(service.findLatestForHeader()).hasSize(5).allMatch(t->t.tieude().startsWith("Phù hợp"));
        var page=service.searchPage("%_!",PageRequest.of(1,3));
        assertThat(page.getTotalElements()).isEqualTo(8);assertThat(page.getTotalPages()).isEqualTo(3);assertThat(page.getContent()).hasSize(3);
        assertThat(service.search("Ẩn")).isEmpty();
    }
    @Test void changingOnlyRolesChangesRevisionAndImmediatelyChangesVisibility() {
        var saved=service.create(targeted(1,"Không đổi nội dung"));
        var update=targeted(2,saved.tieude());update.setRevision(ThongbaoRevision.of(saved));
        var edited=service.update(saved.id(),update);
        assertThat(ThongbaoRevision.of(edited)).isNotEqualTo(ThongbaoRevision.of(saved));
        assertThat(edited.ngaydang()).isEqualTo(saved.ngaydang());assertThat(edited.nguoidangHoten()).isEqualTo(saved.nguoidangHoten());
        var stale=targeted(4,saved.tieude());stale.setRevision(ThongbaoRevision.of(saved));
        assertThatThrownBy(()->service.update(saved.id(),stale)).isInstanceOf(ThongbaoConflictException.class);
        authenticate("reader","SINHVIEN");assertThat(service.findAll()).isEmpty();assertThat(service.findLatestForHeader()).isEmpty();
        authenticate("reader","GIANGVIEN");assertThat(service.findById(saved.id()).id()).isEqualTo(saved.id());
        authenticate("notice-chief","TRUONGKHOA");service.deleteById(saved.id());
        assertThat(jdbc.queryForObject("select count(*) from thongbaodoituong",Long.class)).isZero();
    }
    @Test void historicalRowsRemainVisibleWithoutBackfillAndEditFormShowsAllThree() throws Exception {
        var old=notice("Lịch sử giữ nguyên","Nội dung",chief.getId(),oldDate);
        var before=noticeSnapshot();
        var response=mvc.perform(get("/thongbao/"+old.getId()+"/edit").with(user("notice-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk()).andReturn();
        assertThat(((ThongbaoFormDTO)response.getModelAndView().getModel().get("thongbaoForm")).getRecipientRoles()).isEqualTo(DoituongThongbao.all());
        for(var role:DoituongThongbao.values()) {authenticate("reader",role.name());assertThat(service.findById(old.getId()).recipientRoles()).hasSize(3);assertThat(service.findLatestForHeader()).hasSize(1);}
        assertThat(noticeSnapshot()).isEqualTo(before);assertThat(jdbc.queryForObject("select count(*) from thongbaodoituong",Long.class)).isZero();
    }
    @ParameterizedTest @ValueSource(strings={"","TRUONGKHOA","ADMIN","SINHVIEN,TRUONGKHOA"})
    void missingOrForgedRolesCannotBeSubmitted(String value) throws Exception {
        var request=post("/thongbao").with(user("notice-chief").roles("TRUONGKHOA")).with(csrf())
                .param("tieude","Giả mạo").param("noidung","Không lưu");
        if(!value.isEmpty())request.param("recipientRoles",value.split(","));
        mvc.perform(request).andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("thongbaoForm","recipientRoles"));
        assertThat(repository.count()).isZero();
    }
    @Test void serviceRejectsEmptyOrNullRecipientSetBeforeWriting() {
        var f=targeted(0,"Không có đối tượng");
        assertThatThrownBy(()->service.create(f)).isInstanceOf(ConstraintViolationException.class);
        f.setRecipientRoles(null);assertThatThrownBy(()->service.create(f)).isInstanceOf(ConstraintViolationException.class);
        assertThat(repository.count()).isZero();
    }
    @Test void replayTokenIncludesAudienceAndDoesNotReuseNoticeForDifferentRoles() throws Exception {
        var session=new MockHttpSession();
        var get=mvc.perform(get("/thongbao/create").session(session).with(user("notice-chief").roles("TRUONGKHOA"))).andReturn();
        var token=(String)get.getModelAndView().getModel().get("submissionToken");
        for(int i=0;i<2;i++)mvc.perform(post("/thongbao").session(session).with(user("notice-chief").roles("TRUONGKHOA")).with(csrf())
                .param("submissionToken",token).param("tieude","Một form").param("noidung","Nội dung").param("recipientRoles","SINHVIEN"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/thongbao").session(session).with(user("notice-chief").roles("TRUONGKHOA")).with(csrf())
                .param("submissionToken",token).param("tieude","Một form").param("noidung","Nội dung").param("recipientRoles","GIANGVIEN"))
                .andExpect(status().isBadRequest());
        assertThat(repository.count()).isEqualTo(1);
    }
}
