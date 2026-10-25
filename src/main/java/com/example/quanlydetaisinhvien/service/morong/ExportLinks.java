package com.example.quanlydetaisinhvien.service.morong;

import com.example.quanlydetaisinhvien.dto.morong.*;
import com.example.quanlydetaisinhvien.dto.morong.ExportModels.Dataset;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.*;

/** One allowlist for binding, metadata and links. Pagination is validated but never applied to exports. */
public final class ExportLinks {
    private ExportLinks() {}
    public static List<String> fields(Dataset dataset) {
        var common = List.of("keyword", "dotdangkyid", "loaidot", "sort", "page", "size");
        var fields = new ArrayList<>(dataset == Dataset.THONGKE ? List.of("dotdangkyid") : common);
        switch (dataset) {
            case DETAI -> fields.addAll(List.of("bomonid", "trangthai", "giangviendexuatid", "giangvienhuongdanid"));
            case DOTDANGKY -> fields.addAll(List.of("trangthai", "mocThoiGian", "thoiGianTu", "thoiGianDen"));
            case KETQUA -> fields.addAll(List.of("trangthai", "dacongbo", "diemTu", "diemDen", "ngayCongboTu", "ngayCongboDen"));
            case THONGKE -> { }
        }
        return List.copyOf(fields);
    }
    public static Map<String, Object> values(Dataset dataset, Object filter) {
        Map<String, Object> result = new LinkedHashMap<>();
        var bean = new BeanWrapperImpl(filter);
        for (String field : fields(dataset)) {
            if (field.equals("page") || field.equals("size")) continue;
            Object value = bean.getPropertyValue(field);
            if (value != null && !value.toString().isBlank()) result.put(field, value);
        }
        return result;
    }
    public static String url(Dataset dataset, Object filter, String format) {
        String context = RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes.getRequest().getContextPath() : "";
        var builder = UriComponentsBuilder.fromPath(context + "/morong/xuat/" + dataset.slug() + "." + format);
        Map<String, Object> variables = new LinkedHashMap<>();
        values(dataset, filter).forEach((key, value) -> {
            String variable = "p" + variables.size();
            builder.queryParam(key, "{" + variable + "}"); variables.put(variable, value.toString());
        });
        // Encode values as URI variables so literal '+' never becomes a space in query decoding.
        return builder.encode().buildAndExpand(variables).toUriString();
    }
    public static SearchModels.ExportOptions search(Dataset dataset, SearchFilterDTO filter) {
        dataset.requireAccess();
        return new SearchModels.ExportOptions(true, List.of("xlsx", "pdf"), url(dataset, filter, "xlsx"), url(dataset, filter, "pdf"));
    }
    public static StatisticsModels.ExportOptions statistics(ThongkeFilterDTO filter) {
        Dataset.THONGKE.requireAccess();
        return new StatisticsModels.ExportOptions(true, List.of("xlsx", "pdf"),
                url(Dataset.THONGKE, filter, "xlsx"), url(Dataset.THONGKE, filter, "pdf"));
    }
}
