package com.example.quanlydetaisinhvien.repository.morong;

import com.example.quanlydetaisinhvien.dto.morong.StatisticsModels.RoundOption;
import com.example.quanlydetaisinhvien.exception.ThongkeRoundNotFoundException;
import com.example.quanlydetaisinhvien.security.ThongkePermissions;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** Five aggregate/projection queries, independent of the number of rounds; never loads entity lists. */
@Repository
@Transactional(readOnly = true)
public class ThongkeRepository {
    private final EntityManager em;
    public ThongkeRepository(EntityManager em) { this.em = em; }

    public record Aggregates(List<RoundOption> rounds, List<Object[]> topics, List<Object[]> groups,
            List<Object[]> registrations, List<Object[]> results) {}

    public Aggregates aggregate(Long roundId) {
        ThongkePermissions.requireChief();
        if (roundId != null && roundId <= 0) throw new IllegalArgumentException("ID đợt phải dương.");
        var rounds = em.createQuery("select new com.example.quanlydetaisinhvien.dto.morong.StatisticsModels$RoundOption"
                + "(d.id, d.tendot, d.loaidot) from Dotdangky d order by d.id desc", RoundOption.class).getResultList();
        if (roundId != null && rounds.stream().noneMatch(r -> r.id().equals(roundId))) {
            throw new ThongkeRoundNotFoundException();
        }
        var topics = query("select t.dotdangkyid, t.trangthai, count(distinct t.id) from Detai t",
                "t.dotdangkyid", "t.dotdangkyid, t.trangthai", roundId);
        var groups = query("select n.dotdangkyid, count(distinct n.id), "
                + "count(distinct case when r.id is null then n.id else null end) "
                + "from Nhomsinhvien n left join Dangkydetai r on r.nhomid=n.id",
                "n.dotdangkyid", "n.dotdangkyid", roundId);
        // Round ownership comes from the topic. The group join detects mismatches without excluding them.
        var registrations = query("select t.dotdangkyid, r.trangthai, count(distinct r.id), "
                + "count(distinct case when n.dotdangkyid<>t.dotdangkyid then r.id else null end) "
                + "from Dangkydetai r join Detai t on t.id=r.detaiid "
                + "left join Nhomsinhvien n on n.id=r.nhomid",
                "t.dotdangkyid", "t.dotdangkyid, r.trangthai", roundId);
        var results = query("select t.dotdangkyid, k.dacongbo, count(distinct k.id), "
                + "count(distinct case when k.diemcuoi is null then k.id else null end) "
                + "from Ketqua k join Dangkydetai r on r.id=k.dangkydetaiid join Detai t on t.id=r.detaiid",
                "t.dotdangkyid", "t.dotdangkyid, k.dacongbo", roundId);
        return new Aggregates(List.copyOf(rounds), topics, groups, registrations, results);
    }

    private List<Object[]> query(String select, String roundColumn, String groupBy, Long roundId) {
        var query = em.createQuery(select + (roundId == null ? "" : " where " + roundColumn + "=:roundId")
                + " group by " + groupBy, Object[].class);
        if (roundId != null) query.setParameter("roundId", roundId);
        return query.getResultList();
    }
}
