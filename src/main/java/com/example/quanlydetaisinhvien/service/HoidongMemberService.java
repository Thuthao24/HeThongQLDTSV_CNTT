package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class HoidongMemberService {
    private final ThanhvienhoidongRepository members;
    private final HoidongRepository councils;
    private final GiangvienRepository teachers;
    private final PhanconghoidongRepository assignments;
    private final KetquaRepository results;

    public HoidongMemberService(ThanhvienhoidongRepository members, HoidongRepository councils,
             GiangvienRepository teachers, PhanconghoidongRepository assignments, KetquaRepository results) {
        this.members=members; this.councils=councils; this.teachers=teachers;
        this.assignments=assignments; this.results=results;
    }

    @Transactional(readOnly=true)
    public List<Thanhvienhoidong> forCouncil(Long id) { return members.findByHoidongid(id); }

    /** Tải thành viên của các hội đồng hiện trên trang bằng MỘT query có WHERE IN. */
    @Transactional(readOnly=true)
    public java.util.Map<Long, List<Thanhvienhoidong>> forCouncils(java.util.Collection<Long> ids) {
        var roster = new java.util.HashMap<Long, List<Thanhvienhoidong>>();
        if (ids == null || ids.isEmpty()) return roster;
        for (var member : members.findByHoidongidIn(ids)) {
            roster.computeIfAbsent(member.getHoidongid(), key -> new java.util.ArrayList<>()).add(member);
        }
        return roster;
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('TRUONGKHOA')")
    @Transactional
    public void add(Long councilId, Long lecturerId, VaitroHoidong role) {
        if (role == null || councilId == null || lecturerId == null) throw new NghiepvuException("Thiếu thông tin thành viên.");
        if (!councils.existsById(councilId) || !teachers.existsById(lecturerId))
            throw new NghiepvuException("Hội đồng hoặc giảng viên không tồn tại.");
        assertUnpublished(councilId);
        var existing = members.findByHoidongid(councilId);
        if (existing.stream().anyMatch(m -> m.getGiangvienid().equals(lecturerId)))
            throw new NghiepvuException("Giảng viên đã thuộc hội đồng.");
        if (existing.size() >= 5) throw new NghiepvuException("Hội đồng tối đa 5 giảng viên.");
        if (role != VaitroHoidong.THANHVIEN && existing.stream().anyMatch(m -> m.getVaitro() == role))
            throw new NghiepvuException("Hội đồng chỉ có một chủ tịch và một thư ký.");
        var member = new Thanhvienhoidong(); member.setHoidongid(councilId);
        member.setGiangvienid(lecturerId); member.setVaitro(role);
        members.save(member);
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('TRUONGKHOA')")
    @Transactional
    public void remove(Long councilId, Long lecturerId) {
        assertUnpublished(councilId);
        members.deleteById(new ThanhvienhoidongId(councilId, lecturerId));
    }

    private void assertUnpublished(Long councilId) {
        // Reject edits if any published result references this council.
        for (var assignment : assignments.findByHoidongid(councilId)) {
            if (results.existsByDangkydetaiidAndDacongboTrue(assignment.getDangkydetaiid()))
                throw new NghiepvuException("Hội đồng đã dùng cho kết quả công bố, không thể thay thành viên.");
        }
    }
}
