package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.dto.morong.SearchModels;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.ViewResolver;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real Thymeleaf contract for the UI disclosure; existing search/business suites remain unchanged. */
class DetaiFilterDisclosureTests extends MorongSearchTestSupport {
    private final MorongUiTests markup = new MorongUiTests();
    @BeforeEach void useRealTemplates() {
        var real = context.getBeansOfType(ViewResolver.class).values().stream()
                .filter(v -> v.getClass().getName().contains("ThymeleafViewResolver")).findFirst().orElseThrow();
        ReflectionTestUtils.setField(mvc.getDispatcherServlet(), "viewResolvers", List.of(real));
    }
    String html(org.springframework.test.web.servlet.MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
    void disclosure(String body, boolean open, int count) {
        var details = Pattern.compile("<details\\b[^>]*id=\"detaiAdvancedFilters\"[^>]*>").matcher(body);
        assertThat(details.find()).isTrue();
        assertThat(Pattern.compile("\\sopen(?:[=\\s>])").matcher(details.group()).find()).isEqualTo(open);
        var summary = Pattern.compile("<summary\\b[^>]*id=\"detaiAdvancedSummary\"[^>]*>").matcher(body);
        assertThat(summary.find()).isTrue();
        assertThat(summary.group()).contains("aria-controls=\"detaiAdvancedPanel\"", "aria-expanded=\"" + open + "\"");
        if (count == 0) assertThat(body).doesNotContain("id=\"detaiAppliedCount\"");
        else assertThat(body).contains(count + " điều kiện đang áp dụng");
    }
    @ParameterizedTest @ValueSource(strings={"none","defaults","page"})
    void defaultsAreCollapsedAndDoNotCountAsAppliedConditions(String scenario) throws Exception {
        var request = get("/detai").with(user("search-chief").roles("TRUONGKHOA"));
        if (scenario.equals("defaults")) request.param("size","25").param("sort","id,desc").param("keyword","");
        if (scenario.equals("page")) request.param("page","1");
        String body = html(mvc.perform(request).andExpect(status().isOk()).andReturn());
        disclosure(body,false,0);
        String form = markup.form(body);
        int details = form.indexOf("<details");
        assertThat(form.indexOf("id=\"keyword\"")).isLessThan(details);
        assertThat(form.indexOf("type=\"submit\"")).isLessThan(details);
        assertThat(form.indexOf("data-export-format=\"xlsx\"")).isLessThan(details);
        assertThat(form.indexOf("data-export-format=\"pdf\"")).isLessThan(details);
        assertThat(Pattern.compile("<summary\\b").matcher(form).results().count()).isEqualTo(1);
        for (String field : List.of("dotdangkyid","bomonid","loaidot","trangthai","giangviendexuatid","giangvienhuongdanid","sort","size")) {
            assertThat(form.indexOf("name=\""+field+"\"")).isGreaterThan(details);
            var control = Pattern.compile("<(?:select|input)\\b(?=[^>]*name=\""+field+"\")[^>]*>").matcher(form);
            assertThat(control.find()).isTrue();assertThat(control.group()).doesNotContain("disabled");
            assertThat(control.find()).isFalse();
        }
    }
    @ParameterizedTest @ValueSource(strings={"q","keyword"})
    void keywordOnlySearchStaysCollapsedAndPreservesItsContract(String field) throws Exception {
        var result = mvc.perform(get("/detai").param(field,"sinh viên").with(user("search-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk()).andReturn();
        String body = html(result);disclosure(body,false,1);
        assertThat(markup.value(markup.form(body),field)).isEqualTo("sinh viên");
        assertThat((List<?>)result.getModelAndView().getModel().get("items")).extracting("id").containsExactly(topicA.getId());
    }
    @ParameterizedTest @CsvSource({"dotdangkyid,999999","bomonid,999999","loaidot,MONHOC","trangthai,CONGBO",
            "giangviendexuatid,999999","giangvienhuongdanid,999999","sort,tendetai asc","size,37"})
    void eachNonDefaultAdvancedConditionOpensAndCountsOnce(String field,String value) throws Exception {
        value = value.replace(' ', ',');
        String body = html(mvc.perform(get("/detai").param(field,value).with(user("search-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk()).andReturn());
        disclosure(body,true,1);
        assertThat(markup.value(markup.form(body),field)).isEqualTo(value);
    }
    @ParameterizedTest @CsvSource({"size,0","size,abc","sort,bad","bomonid,-1"})
    void invalidFilterOpensAndShowsValidationWithoutAnAppliedCountOrExport(String field,String value) throws Exception {
        String body = html(mvc.perform(get("/detai").param(field,value).with(user("search-chief").roles("TRUONGKHOA")))
                .andExpect(status().isBadRequest()).andReturn());
        disclosure(body,true,0);
        assertThat(body).contains("Bộ lọc không hợp lệ", "disabled=\"disabled\"").doesNotContain("data-export-format=\"xlsx\"");
    }
    @Test void oneFormRetainsAllConditionsAndExportsRegardlessOfDisclosureStateAndResetRestoresDefaults() throws Exception {
        var params = new LinkedHashMap<String,String>();
        params.put("keyword","sinh viên");params.put("dotdangkyid",roundA.getId().toString());params.put("bomonid",departmentA.getId().toString());
        params.put("loaidot","MONHOC");params.put("trangthai","CONGBO");params.put("giangviendexuatid",teacherA.getId().toString());
        params.put("giangvienhuongdanid",teacherB.getId().toString());params.put("sort","tendetai,asc");params.put("size","1");
        var request=get("/detai").with(user("search-chief").roles("TRUONGKHOA"));params.forEach(request::param);
        var response=mvc.perform(request).andExpect(status().isOk()).andReturn();String body=html(response);disclosure(body,true,9);
        String form=markup.form(body);
        // Closed details still submit their enabled controls; open/aria-expanded have no form field names.
        assertThat(Pattern.compile("<form\\b").matcher(form).results().count()).isEqualTo(1);
        assertThat(form).doesNotContain("name=\"open\"", "name=\"aria-expanded\"");
        var submit=get("/detai").with(user("search-chief").roles("TRUONGKHOA"));
        params.forEach((key,value)->{assertThat(markup.value(form,key)).isEqualTo(value);submit.param(key,markup.value(form,key));});
        var submitted=mvc.perform(submit).andExpect(status().isOk()).andReturn();
        assertThat((List<?>)submitted.getModelAndView().getModel().get("items")).extracting("id").containsExactly(topicA.getId());
        var exports=(SearchModels.ExportOptions)response.getModelAndView().getModel().get("exportOptions");
        for(String url:List.of(exports.excelUrl(),exports.pdfUrl())) {
            assertThat(url).contains("sort=tendetai%2Casc","giangvienhuongdanid="+teacherB.getId()).doesNotContain("size=","page=");
            byte[] bytes=mvc.perform(get(URI.create(url)).with(user("search-chief").roles("TRUONGKHOA")))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
            assertThat(new DetaiIntegratedSearchTests().text(bytes,url.contains(".pdf?")))
                    .contains(topicA.getTendetai()).doesNotContain(topicB.getTendetai(),topicC.getTendetai());
        }
        var reset=Pattern.compile("<a[^>]*href=\"([^\"]+)\"[^>]*>↺ Đặt lại</a>").matcher(form);assertThat(reset.find()).isTrue();
        assertThat(reset.group(1)).isEqualTo("/detai");
        String resetBody=html(mvc.perform(get(reset.group(1)).with(user("search-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk()).andReturn());disclosure(resetBody,false,0);
        String resetForm=markup.form(resetBody);assertThat(markup.value(resetForm,"keyword")).isEmpty();
        assertThat(markup.value(resetForm,"size")).isEqualTo("25");assertThat(markup.value(resetForm,"sort")).isEqualTo("id,desc");
        assertThat(markup.value(resetForm,"page")).isEqualTo("0");
    }
}
