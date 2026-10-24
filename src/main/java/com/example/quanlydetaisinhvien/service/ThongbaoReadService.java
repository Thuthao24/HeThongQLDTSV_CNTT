package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.dto.*;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.ThongbaoNotFoundException;
import com.example.quanlydetaisinhvien.repository.*;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;

@Service
@Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
public class ThongbaoReadService {
    private final ThongbaoReadRepository reads;
    private final ThongbaoReadActivationRepository activation;
    private final ThongbaoRepository notices;
    private final TaikhoanRepository accounts;
    private final ThongbaoService noticeService;
    public ThongbaoReadService(ThongbaoReadRepository reads,ThongbaoReadActivationRepository activation,
            ThongbaoRepository notices,TaikhoanRepository accounts,ThongbaoService noticeService) {
        this.reads=reads;this.activation=activation;this.notices=notices;this.accounts=accounts;this.noticeService=noticeService;
    }
    public ThongbaoBellDTO bell() {
        var account=reader(false);
        if(account.getVaitro()==Vaitro.TRUONGKHOA) return new ThongbaoBellDTO(0,List.of());
        var latest=noticeService.findLatestForHeader();var state=state(account,latest);
        return new ThongbaoBellDTO(state.unreadCount(),latest.stream().map(t->new ThongbaoBellDTO.Item(
                t.id(),t.tieude(),t.nguoidangHoten(),t.ngaydang(),state.unreadIds().contains(t.id()))).toList());
    }
    public ThongbaoBellDTO.State headerState(List<ThongbaoViewDTO> latest) { return state(reader(false),latest); }
    private ThongbaoBellDTO.State state(Taikhoan account,List<ThongbaoViewDTO> latest) {
        if(account.getVaitro()==Vaitro.TRUONGKHOA) return new ThongbaoBellDTO.State(0,Set.of());
        long boundary=boundary();var role=DoituongThongbao.valueOf(account.getVaitro().name());
        var ids=latest.stream().map(ThongbaoViewDTO::id).toList();
        var unread=ids.isEmpty()?Set.<Long>of():reads.unreadAmong(account.getId(),role,boundary,ids);
        return new ThongbaoBellDTO.State(reads.countUnread(account.getId(),role,boundary),unread);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public ThongbaoBellDTO.State markRead(Long noticeId) {
        if(noticeId==null || noticeId<=0) throw new IllegalArgumentException("ID không hợp lệ.");
        var account=reader(true);
        if(account.getVaitro()==Vaitro.TRUONGKHOA) throw denied();
        var notice=notices.findForUpdate(noticeId).orElseThrow(ThongbaoNotFoundException::new);
        var role=DoituongThongbao.valueOf(account.getVaitro().name());
        if(!notice.getRecipientRoles().isEmpty() && !notice.getRecipientRoles().contains(role)) throw new ThongbaoNotFoundException();
        long boundary=boundary();
        if(noticeId>boundary && !reads.existsById(new ThongbaoReadId(account.getId(),noticeId))) {
            reads.saveAndFlush(new ThongbaoRead(account.getId(),noticeId,LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).withNano(0)));
        }
        return new ThongbaoBellDTO.State(reads.countUnread(account.getId(),role,boundary),Set.of());
    }
    private long boundary() {
        return activation.findById(1).map(ThongbaoReadActivation::getNoticeBoundary)
                .orElseThrow(()->new DataAccessResourceFailureException("Chưa có mốc đọc thông báo; cần triển khai V005."));
    }
    private Taikhoan reader(boolean lock) {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) throw denied();
        var account=(lock?reads.lockReader(auth.getName()):accounts.findByTendangnhap(auth.getName())).orElseThrow(this::denied);
        if(!Boolean.TRUE.equals(account.getKichhoat()) || account.getVaitro()==null
                || auth.getAuthorities().stream().noneMatch(a->a.getAuthority().equals("ROLE_"+account.getVaitro().name()))) throw denied();
        if(account.getVaitro()!=Vaitro.TRUONGKHOA) {
            try {DoituongThongbao.valueOf(account.getVaitro().name());}catch(IllegalArgumentException ex){throw denied();}
        }
        return account;
    }
    private AccessDeniedException denied() { return new AccessDeniedException("Bạn không có quyền nhận thông báo."); }
}
