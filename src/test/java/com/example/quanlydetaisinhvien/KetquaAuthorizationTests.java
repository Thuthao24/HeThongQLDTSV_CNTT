package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.service.KetquaService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:ketqua-security;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@Transactional
class KetquaAuthorizationTests {
    @Autowired private KetquaService service;
    @Autowired private EntityManager em;
    @Autowired private WebApplicationContext context;

    private MockMvc mvc;
    private Ketqua ownPublished;
    private Ketqua ownDraft;
    private Ketqua otherPublished;
    private Sinhvien studentA;
    private Nhomsinhvien ownGroup;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        account("chief", Vaitro.TRUONGKHOA);
        studentA = student("student-a");
        Sinhvien member = student("member-a");
        Sinhvien studentB = student("student-b");
        student("no-group");
        Sinhvien noResult = student("no-result");
        account("no-profile", Vaitro.SINHVIEN);
        account("lecturer", Vaitro.GIANGVIEN);
        account("department-head", Vaitro.TRUONGBOMON);

        ownGroup = group(studentA);
        membership(ownGroup, studentA);
        membership(ownGroup, member);
        ownPublished = result(ownGroup, true, "8.25");
        Nhomsinhvien draftGroup = group(studentA);
        membership(draftGroup, studentA);
        ownDraft = result(draftGroup, false, "6.75");
        Nhomsinhvien otherGroup = group(studentB);
        membership(otherGroup, studentB);
        otherPublished = result(otherGroup, true, "9.50");
        Nhomsinhvien emptyGroup = group(noResult);
        membership(emptyGroup, noResult);
        em.flush();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @WithMockUser(username = "student-a", roles = "SINHVIEN")
    void studentOnlySeesPublishedResultsOfTheirGroups() {
        assertThat(service.findVisibleToCurrentUser()).containsExactly(ownPublished);
        assertThat(service.findAll()).containsExactly(ownPublished);
        assertThat(studentA.getId()).isNotEqualTo(studentA.getTaikhoanid());
    }

    @Test
    @WithMockUser(username = "member-a", roles = "SINHVIEN")
    void ordinaryMemberCanReadTheSameGroupResult() {
        assertThat(service.findAll()).containsExactly(ownPublished);
        assertThat(service.findById(ownPublished.getId())).contains(ownPublished);
    }

    @Test
    @WithMockUser(username = "student-b", roles = "SINHVIEN")
    void differentStudentOnlySeesTheirOwnGroup() {
        assertThat(service.findAll()).containsExactly(otherPublished);
        assertThat(service.findById(ownPublished.getId())).isEmpty();
    }

    @Test
    @WithMockUser(username = "student-a", roles = "SINHVIEN")
    void guessedIdsCannotExposeOtherGroupsOrDrafts() {
        assertThat(service.findById(ownPublished.getId())).contains(ownPublished);
        assertThat(service.findById(ownDraft.getId())).isEmpty();
        assertThat(service.findById(otherPublished.getId())).isEmpty();
        assertThat(service.findById(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    void studentsWithoutGroupResultOrProfileGetAnEmptyList() {
        for (String username : List.of("no-group", "no-result", "no-profile", "missing-account")) {
            authenticate(username, "SINHVIEN");
            assertThat(service.findAll()).as(username).isEmpty();
            assertThat(service.findById(ownPublished.getId())).as(username).isEmpty();
        }
    }

    @Test
    @WithMockUser(username = "student-a", roles = "SINHVIEN")
    void membershipRemovalImmediatelyRemovesReadAccess() {
        em.createQuery("delete from Thanhviennhom tv where tv.nhomid = :group and tv.sinhvienid = :student")
                .setParameter("group", ownGroup.getId()).setParameter("student", studentA.getId())
                .executeUpdate();
        assertThat(service.findAll()).isEmpty();
        assertThat(service.findById(ownPublished.getId())).isEmpty();
    }

    @Test
    @WithMockUser(username = "student-a", roles = "SINHVIEN")
    void withdrawingPublicationImmediatelyRemovesReadAccess() {
        ownPublished.setDacongbo(false);
        em.flush();
        assertThat(service.findAll()).isEmpty();
        assertThat(service.findById(ownPublished.getId())).isEmpty();
    }

    @Test
    @WithMockUser(username = "student-a", roles = "SINHVIEN")
    void studentSeesPublishedResultsForEveryGroupTheyBelongTo() {
        ownDraft.setDacongbo(true);
        em.flush();
        assertThat(service.findAll()).containsExactlyInAnyOrder(ownPublished, ownDraft);
    }

    @Test
    @WithMockUser(username = "chief", roles = "TRUONGKHOA")
    void chiefRetainsFullReadAndManagementAccess() {
        assertThat(service.findAll()).containsExactlyInAnyOrder(ownPublished, ownDraft, otherPublished);
        assertThat(service.findById(ownDraft.getId())).contains(ownDraft);
        // Truong khoa duoc doc, nhung khong ghi/xoa truc tiep ket qua.
        // Phai dung quy trinh tinh diem va cong bo.
        assertThatThrownBy(() -> service.save(ownDraft))
                .isInstanceOf(com.example.quanlydetaisinhvien.exception.NghiepvuException.class);
        assertThatThrownBy(() -> service.deleteById(otherPublished.getId()))
                .isInstanceOf(com.example.quanlydetaisinhvien.exception.NghiepvuException.class);
        assertThat(service.findById(otherPublished.getId()))
                .contains(otherPublished);
    }

    @Test
    @WithMockUser(username = "student-a", roles = "SINHVIEN")
    void studentCannotChangeOrDeleteResultsThroughCrudService() {
        assertThatThrownBy(() -> service.save(ownPublished)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.deleteById(ownPublished.getId())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void serviceDeniesAnonymousUnauthenticatedAndUnsupportedRoles() {
        SecurityContextHolder.clearContext();
        assertReadsDenied();
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "test-key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        assertReadsDenied();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.unauthenticated("student-a", "unused"));
        assertReadsDenied();
        for (String role : List.of("GIANGVIEN", "TRUONGBOMON")) {
            authenticate("unsupported", role);
            assertReadsDenied();
        }
    }

    @Test
    void forgedQueryParametersCannotChangeTheAuthenticatedStudent() throws Exception {
        MvcResult result = mvc.perform(get("/ketqua").with(user("student-a").roles("SINHVIEN"))
                        .param("username", "chief").param("sinhvienid", "999")
                        .param("nhomid", "999").param("id", otherPublished.getId().toString())
                        .param("dacongbo", "false").param("vaitro", "TRUONGKHOA"))
                .andExpect(status().isOk()).andExpect(view().name("ketqua-list")).andReturn();
        assertThat(result.getModelAndView().getModel().get("items")).isEqualTo(List.of(ownPublished));
        assertThat(result.getResponse().getContentAsString()).contains("8.25").doesNotContain("6.75", "9.50");
    }

    @Test
    void chiefPageStillShowsUnpublishedAndOtherGroupResults() throws Exception {
        MvcResult result = mvc.perform(get("/ketqua").with(user("chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk()).andExpect(view().name("ketqua-list")).andReturn();
        assertThat(result.getResponse().getContentAsString()).contains("8.25", "6.75", "9.50", "Chưa công bố");
    }

    @Test
    void emptyPageExplainsWhyNoResultIsAvailable() throws Exception {
        mvc.perform(get("/ketqua").with(user("no-group").roles("SINHVIEN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa có kết quả để hiển thị")));
    }

    @Test
    void webSecurityRejectsAnonymousUnsupportedRolesAndStudentWrites() throws Exception {
        mvc.perform(get("/ketqua")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        for (String role : List.of("GIANGVIEN", "TRUONGBOMON")) {
            mvc.perform(get("/ketqua").with(user("unsupported").roles(role)))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/ketqua/" + otherPublished.getId()).with(user("unsupported").roles(role)))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/ketqua").with(user("student-a").roles("SINHVIEN")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/ketqua/" + ownPublished.getId())
                        .with(user("student-a").roles("SINHVIEN")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    private void assertReadsDenied() {
        assertThatThrownBy(service::findAll).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(service::findVisibleToCurrentUser).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.findById(ownPublished.getId())).isInstanceOf(AccessDeniedException.class);
    }

    private void authenticate(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                username, "unused", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private Taikhoan account(String username, Vaitro role) {
        Taikhoan account = new Taikhoan();
        account.setTendangnhap(username);
        account.setMatkhau("unused-test-password");
        account.setHoten(username);
        account.setVaitro(role);
        account.setKichhoat(true);
        em.persist(account);
        return account;
    }

    private Sinhvien student(String username) {
        Sinhvien student = new Sinhvien();
        student.setTaikhoanid(account(username, Vaitro.SINHVIEN).getId());
        student.setMasinhvien("MSSV-" + username);
        student.setBomonid(1L);
        em.persist(student);
        return student;
    }

    private Nhomsinhvien group(Sinhvien leader) {
        Nhomsinhvien group = new Nhomsinhvien();
        group.setDotdangkyid(1L);
        group.setTennhom("Test group");
        group.setNhomtruongid(leader.getId());
        em.persist(group);
        return group;
    }

    private void membership(Nhomsinhvien group, Sinhvien student) {
        Thanhviennhom member = new Thanhviennhom();
        member.setNhomid(group.getId());
        member.setSinhvienid(student.getId());
        em.persist(member);
    }

    private Ketqua result(Nhomsinhvien group, boolean published, String score) {
        Dangkydetai registration = new Dangkydetai();
        registration.setNhomid(group.getId());
        registration.setDetaiid(1L);
        registration.setTrangthai(TrangthaiDangkydetai.DADUYET);
        registration.setNgaydangky(LocalDateTime.now());
        em.persist(registration);
        Ketqua result = new Ketqua();
        result.setDangkydetaiid(registration.getId());
        result.setDiemcuoi(new BigDecimal(score));
        result.setDacongbo(published);
        if (published) result.setNgaycongbo(LocalDateTime.now());
        em.persist(result);
        return result;
    }
}
