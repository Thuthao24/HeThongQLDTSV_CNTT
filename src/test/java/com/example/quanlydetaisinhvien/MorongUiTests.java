package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.entity.Vaitro;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.util.HtmlUtils;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Reuses isolated H2 fixtures and the real Security chain, but explicitly renders with Thymeleaf. */
class MorongUiTests extends MorongSearchTestSupport {
    String url(String dataset) { return dataset.equals("detai") ? "/detai" : "/morong/" + (dataset.equals("thongke") ? dataset : "timkiem/" + dataset); }
    @BeforeEach void useRealTemplates() {
        account("search-head", Vaitro.TRUONGBOMON); em.flush();
        var real = context.getBeansOfType(ViewResolver.class).values().stream()
                .filter(v -> v.getClass().getName().contains("ThymeleafViewResolver")).findFirst().orElseThrow();
        // The inherited backend-only resolver is intentionally bypassed for these rendering tests.
        ReflectionTestUtils.setField(mvc.getDispatcherServlet(), "viewResolvers", List.of(real));
    }
    String username(String role) {
        return switch(role) { case "SINHVIEN" -> "search-student-a"; case "GIANGVIEN" -> "search-teacher-a";
            case "TRUONGBOMON" -> "search-head"; default -> "search-chief"; };
    }
    String html(org.springframework.test.web.servlet.MvcResult result) throws Exception {
        String html=result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("<!DOCTYPE html>").doesNotContain("th:replace=", "TemplateInputException");
        return html;
    }
    String form(String html) {
        var m=Pattern.compile("(?s)<form\\b(?=[^>]*id=\"filterForm\")[^>]*>.*?</form>").matcher(html);
        assertThat(m.find()).isTrue(); return m.group();
    }
    String value(String form,String name) {
        var input=Pattern.compile("<input\\b(?=[^>]*name=\""+name+"\")[^>]*>").matcher(form);
        if(input.find()) { var v=Pattern.compile("value=\"([^\"]*)\"").matcher(input.group());assertThat(v.find()).isTrue();return HtmlUtils.htmlUnescape(v.group(1)); }
        var select=Pattern.compile("(?s)<select\\b(?=[^>]*name=\""+name+"\")[^>]*>(.*?)</select>").matcher(form);
        assertThat(select.find()).as(name).isTrue();
        var option=Pattern.compile("<option\\b(?=[^>]*selected)[^>]*>").matcher(select.group(1));
        assertThat(option.find()).as(name+" selected").isTrue();var v=Pattern.compile("value=\"([^\"]*)\"").matcher(option.group());
        assertThat(v.find()).isTrue();return HtmlUtils.htmlUnescape(v.group(1));
    }
    @ParameterizedTest @ValueSource(strings={"TRUONGKHOA","GIANGVIEN","TRUONGBOMON","SINHVIEN"})
    void actualTemplatesRespectRolesAndExposeOneExportMenu(String role) throws Exception {
        for(String dataset:List.of("detai","thongke")) {
            boolean allowed=role.equals("TRUONGKHOA") || dataset.equals("detai") && !role.equals("SINHVIEN")
                   ;
            var result=mvc.perform(get(url(dataset))
                    .with(user(username(role)).roles(role))).andExpect(status().is(allowed?200:403)).andReturn();
            if(allowed) {
                String body=html(result);
                assertThat(Pattern.compile("<button\\b[^>]*class=\"morong-export-btn\"").matcher(body).results().count()).isEqualTo(1);
                assertThat(body).contains("<span>Xuất</span>", "data-export-format=\"xlsx\"", "data-export-format=\"pdf\"")
                        .doesNotContain("<span>Xuất báo cáo</span>");
                if(!dataset.equals("thongke")) assertThat(body).contains("/js/morong-search.js").doesNotContain("filterForm').submit()");
            }
        }
    }
    @ParameterizedTest @ValueSource(strings={"detai"})
    void urlFiltersAreRenderedOnceAndSurviveFormRoundTrip(String dataset) throws Exception {
        var params=new LinkedHashMap<String,String>();
        params.put("keyword","Tiếng Việt + % _ !");params.put("size","37");params.put("page","1");params.put("sort","id,asc");
        params.put("dotdangkyid",roundA.getId().toString());params.put("loaidot","MONHOC");
        if(dataset.equals("detai")) {params.put("bomonid",departmentA.getId().toString());params.put("trangthai","CONGBO");
            params.put("giangviendexuatid",teacherA.getId().toString());params.put("giangvienhuongdanid",teacherB.getId().toString());}
        var request=get(url(dataset)).with(user("search-chief").roles("TRUONGKHOA"));params.forEach(request::param);
        String original=form(html(mvc.perform(request).andExpect(status().isOk()).andReturn()));
        var next=get(url(dataset)).with(user("search-chief").roles("TRUONGKHOA"));
        params.forEach((key,expected)->{assertThat(value(original,key)).isEqualTo(expected);
            assertThat(Pattern.compile("name=\""+key+"\"").matcher(original).results().count()).isEqualTo(1);
            next.param(key,key.equals("page")?"2":value(original,key));});
        String subsequent=form(html(mvc.perform(next).andExpect(status().isOk()).andReturn()));
        params.forEach((key,expected)->assertThat(value(subsequent,key)).isEqualTo(key.equals("page")?"2":expected));
        var all=mvc.perform(request).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(all).contains("/morong/xuat/"+dataset+".xlsx?", "size\"", "loaidot=MONHOC", "dotdangkyid="+roundA.getId());
    }
    @Test void actualValidationPagesShowErrorsAndDisableExport() throws Exception {
        for(String[] input:List.of(new String[]{"detai","size","101"},new String[]{"detai","bomonid","0"})){
            String body=html(mvc.perform(get(url(input[0])).with(user("search-chief").roles("TRUONGKHOA"))
                    .param(input[1],input[2])).andExpect(status().isBadRequest()).andReturn());
            assertThat(body).contains("Bộ lọc không hợp lệ", "disabled=\"disabled\"").doesNotContain("data-export-format=\"xlsx\"");
        }
    }
    @ParameterizedTest @ValueSource(strings={"detai"})
    void emptyResultsRenderNormally(String dataset) throws Exception {
        String body=html(mvc.perform(get(url(dataset)).with(user("search-chief").roles("TRUONGKHOA"))
                .param("keyword","Không có dữ liệu thử này")).andExpect(status().isOk()).andReturn());
        assertThat(body).contains("Chưa có đề tài", "data-export-format=\"pdf\"");
    }
}
