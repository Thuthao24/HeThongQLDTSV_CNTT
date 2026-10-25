package com.example.quanlydetaisinhvien.repository.morong;

import com.example.quanlydetaisinhvien.dto.morong.*;
import com.example.quanlydetaisinhvien.dto.morong.SearchModels.*;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.security.MorongPermissions;
import com.example.quanlydetaisinhvien.security.MorongPermissions.Dataset;
import com.example.quanlydetaisinhvien.validation.MorongFilterValidator;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Read-only Criteria queries; authorization is enforced here as well as in the service. */
@Repository
@Transactional(readOnly = true)
public class TimkiemNangcaoRepository {
    private final EntityManager em;
    private final MorongFilterValidator validation;
    public TimkiemNangcaoRepository(EntityManager em, MorongFilterValidator validation) {
        this.em = em; this.validation = validation;
    }

    public Result<DetaiRow> detai(DetaiFilterDTO filter) { return search(Dataset.DETAI, filter, DetaiRow.class); }

    public List<DetaiRow> exportDetai(DetaiFilterDTO filter, int limit) { return export(Dataset.DETAI, filter, DetaiRow.class, limit); }

    private <T> List<T> export(Dataset dataset, SearchFilterDTO filter, Class<T> type, int limit) {
        MorongPermissions.require(dataset);
        var ordering = validation.validate(dataset, filter);
        if (limit < 1 || limit > 10000) throw new IllegalArgumentException("Giới hạn xuất không hợp lệ.");
        var cb = em.getCriteriaBuilder();
        var countQuery = cb.createQuery(Long.class);
        var count = context(countQuery, dataset);
        apply(count, countQuery, dataset, filter);
        countQuery.select(cb.countDistinct(count.columns.get("id"))).where(count.predicates.toArray(Predicate[]::new));
        com.example.quanlydetaisinhvien.exception.ExportLimitException.check(em.createQuery(countQuery).getSingleResult(), limit);
        var query = cb.createQuery(type);
        var ctx = context(query, dataset);
        apply(ctx, query, dataset, filter);
        query.select(cb.construct(type, projection(ctx, dataset))).where(ctx.predicates.toArray(Predicate[]::new));
        var orderColumn = ctx.columns.get(ordering.field());
        query.orderBy(ordering.ascending() ? cb.asc(orderColumn) : cb.desc(orderColumn), cb.desc(ctx.columns.get("id")));
        var rows = em.createQuery(query).setMaxResults(limit + 1).getResultList();
        com.example.quanlydetaisinhvien.exception.ExportLimitException.check(rows.size(), limit);
        return List.copyOf(rows);
    }

    private <T> Result<T> search(Dataset dataset, SearchFilterDTO filter, Class<T> type) {
        MorongPermissions.require(dataset);
        var ordering = validation.validate(dataset, filter);
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(type);
        var ctx = context(query, dataset);
        apply(ctx, query, dataset, filter);
        query.select(cb.construct(type, projection(ctx, dataset)));
        query.where(ctx.predicates.toArray(Predicate[]::new));
        var orderColumn = ctx.columns.get(ordering.field());
        query.orderBy(ordering.ascending() ? cb.asc(orderColumn) : cb.desc(orderColumn), cb.desc(ctx.columns.get("id")));
        var rows = em.createQuery(query).setFirstResult(Math.toIntExact((long) filter.getPage() * filter.getSize()))
                .setMaxResults(filter.getSize()).getResultList();

        var countQuery = cb.createQuery(Long.class);
        var count = context(countQuery, dataset);
        apply(count, countQuery, dataset, filter);
        countQuery.select(cb.countDistinct(count.columns.get("id"))).where(count.predicates.toArray(Predicate[]::new));
        long total = em.createQuery(countQuery).getSingleResult();
        return new Result<>(List.copyOf(rows), PageInfo.of(filter, total));
    }

    private static class Context {
        final Map<String, Expression<?>> columns = new LinkedHashMap<>();
        final List<Predicate> predicates = new ArrayList<>();
        final Map<String, Root<?>> roots = new HashMap<>();
        <T> Root<T> root(AbstractQuery<?> query, Class<T> type, String name, String... fields) {
            Root<T> root = query.from(type);
            roots.put(name, root);
            for (String field : fields) columns.put(field, root.get(field));
            return root;
        }
    }

    private Context context(AbstractQuery<?> query, Dataset dataset) {
        MorongPermissions.require(dataset);
        var cb = em.getCriteriaBuilder();
        var c = new Context();
        var round = c.root(query, Dotdangky.class, "round", "tendot", "loaidot");
        c.columns.put("dotdangkyid", round.get("id"));
        var topic = c.root(query, Detai.class, "topic", "tendetai");
        c.predicates.add(cb.equal(topic.get("dotdangkyid"), round.get("id")));
        for (String name : List.of("id", "mota", "bomonid", "giangviendexuatid", "trangthai")) c.columns.put(name, topic.get(name));
        var department = c.root(query, Bomon.class, "department", "tenbomon");
        var teacher = c.root(query, Giangvien.class, "teacher");
        var account = c.root(query, Taikhoan.class, "account");
        c.columns.put("giangviendexuatHoten", account.get("hoten"));
        c.predicates.add(cb.equal(topic.get("bomonid"), department.get("id")));
        c.predicates.add(cb.equal(topic.get("giangviendexuatid"), teacher.get("id")));
        c.predicates.add(cb.equal(teacher.get("taikhoanid"), account.get("id")));
        return c;
    }

    private Selection<?>[] projection(Context c, Dataset dataset) {
        List<String> names = switch (dataset) {
            case DETAI -> List.of("id", "tendetai", "mota", "dotdangkyid", "tendot", "loaidot", "bomonid",
                    "tenbomon", "giangviendexuatid", "giangviendexuatHoten", "trangthai");
        };
        return names.stream().map(c.columns::get).toArray(Selection<?>[]::new);
    }

    private void apply(Context c, AbstractQuery<?> query, Dataset dataset, SearchFilterDTO f) {
        var cb = em.getCriteriaBuilder();
        equal(c, "dotdangkyid", f.getDotdangkyid()); equal(c, "loaidot", f.getLoaidot());
        if (f instanceof DetaiFilterDTO legacy && !legacy.getQ().isEmpty()) {
            // Match the original /detai JPQL, including null coalescing and LIKE wildcard behavior.
            String pattern = "%" + legacy.getQ().toLowerCase(Locale.ROOT) + "%";
            c.predicates.add(cb.or(
                    cb.like(cb.lower(cb.coalesce(c.columns.get("tendetai").as(String.class), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(c.columns.get("mota").as(String.class), "")), pattern)));
        } else if (!f.getKeyword().isBlank()) {
            String pattern = "%" + f.getKeyword().strip().toLowerCase(Locale.ROOT)
                    .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            List<String> textFields = switch (dataset) {
                case DETAI -> List.of("tendetai", "mota");
            };
            c.predicates.add(cb.or(textFields.stream().map(name -> cb.like(cb.lower(c.columns.get(name).as(String.class)), pattern, '!'))
                    .toArray(Predicate[]::new)));
        }
        if (f instanceof DetaiFilterDTO d) {
            equal(c, "bomonid", d.getBomonid()); equal(c, "trangthai", d.getTrangthai());
            equal(c, "giangviendexuatid", d.getGiangviendexuatid());
            if (d.getGiangvienhuongdanid() != null) {
                var exists = query.subquery(Long.class);
                var guide = exists.from(Giangvienhuongdan.class);
                exists.select(guide.get("detaiid")).where(cb.equal(guide.get("detaiid"), c.columns.get("id")),
                        cb.equal(guide.get("giangvienid"), d.getGiangvienhuongdanid()));
                c.predicates.add(cb.exists(exists));
            }
        }
    }

    private void equal(Context c, String column, Object value) {
        if (value != null) c.predicates.add(em.getCriteriaBuilder().equal(c.columns.get(column), value));
    }

    /** Unfiltered choices are derived only from the authorized dataset, never a global lookup. */
    public FilterOptions options(Dataset dataset) {
        MorongPermissions.require(dataset);
        var cb = em.getCriteriaBuilder();
        var query = cb.createTupleQuery();
        var ctx = context(query, dataset);
        List<String> fields = new ArrayList<>(List.of("dotdangkyid", "tendot", "loaidot", "trangthai"));
        if (dataset == Dataset.DETAI) fields.addAll(List.of("bomonid", "tenbomon", "giangviendexuatid", "giangviendexuatHoten"));
        query.select(cb.tuple(fields.stream().map(ctx.columns::get).toArray(Selection<?>[]::new))).distinct(true)
                .where(ctx.predicates.toArray(Predicate[]::new));
        Map<String, SortedMap<String, String>> choices = new HashMap<>();
        for (String key : List.of("dotdangky", "loaidot", "trangthai", "bomon", "giangviendexuat", "giangvienhuongdan")) choices.put(key, new TreeMap<>());
        for (var row : em.createQuery(query).getResultList()) {
            Map<String, Object> values = new HashMap<>();
            for (int i = 0; i < fields.size(); i++) values.put(fields.get(i), row.get(i));
            choice(choices, "dotdangky", values.get("dotdangkyid"), values.get("tendot"));
            choice(choices, "loaidot", values.get("loaidot"), values.get("loaidot"));
            choice(choices, "trangthai", values.get("trangthai"), values.get("trangthai"));
            choice(choices, "bomon", values.get("bomonid"), values.get("tenbomon"));
            choice(choices, "giangviendexuat", values.get("giangviendexuatid"), values.get("giangviendexuatHoten"));
        }
        if (dataset == Dataset.DETAI) guideOptions(choices);
        return new FilterOptions(options(choices, "dotdangky"), options(choices, "bomon"), options(choices, "loaidot"),
                options(choices, "trangthai"), options(choices, "giangviendexuat"), options(choices, "giangvienhuongdan"),
                validation.sortOptions(dataset));
    }

    private void guideOptions(Map<String, SortedMap<String, String>> choices) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createTupleQuery();
        var c = context(query, Dataset.DETAI);
        var guide = query.from(Giangvienhuongdan.class);
        var teacher = query.from(Giangvien.class);
        var account = query.from(Taikhoan.class);
        c.predicates.add(cb.equal(guide.get("detaiid"), c.columns.get("id")));
        c.predicates.add(cb.equal(guide.get("giangvienid"), teacher.get("id")));
        c.predicates.add(cb.equal(teacher.get("taikhoanid"), account.get("id")));
        query.select(cb.tuple(teacher.get("id"), account.get("hoten"))).distinct(true).where(c.predicates.toArray(Predicate[]::new));
        for (var row : em.createQuery(query).getResultList()) choice(choices, "giangvienhuongdan", row.get(0), row.get(1));
    }

    private void choice(Map<String, SortedMap<String, String>> choices, String key, Object value, Object label) {
        if (value != null) choices.get(key).put(value.toString(), label == null ? value.toString() : label.toString());
    }
    private List<Option> options(Map<String, SortedMap<String, String>> choices, String key) {
        return choices.get(key).entrySet().stream().map(e -> new Option(e.getKey(), e.getValue())).toList();
    }
}
