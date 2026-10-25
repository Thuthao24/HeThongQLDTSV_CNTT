package com.example.quanlydetaisinhvien.controller;

import com.example.quanlydetaisinhvien.dto.ThongbaoViewDTO;
import com.example.quanlydetaisinhvien.service.ThongbaoRevision;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.*;
import java.util.function.Supplier;

/** Bounded per-session replay protection; only records a result after the Service transaction returns. */
final class ThongbaoSubmissions {
    private static final String ATTRIBUTE=ThongbaoSubmissions.class.getName();
    private static final long TTL=30*60*1000L;
    private record Entry(String owner,long created,String fingerprint,Long id) implements Serializable {}
    private static class State implements Serializable { final LinkedHashMap<String,Entry> entries=new LinkedHashMap<>(); }
    private static State state(HttpSession session) {
        synchronized(session) {
            if(!(session.getAttribute(ATTRIBUTE) instanceof State)) session.setAttribute(ATTRIBUTE,new State());
            return (State)session.getAttribute(ATTRIBUTE);
        }
    }
    static String issue(HttpSession session,String owner) {
        var state=state(session);
        synchronized(state) {
            prune(state); String token=UUID.randomUUID().toString();
            state.entries.put(token,new Entry(owner,System.currentTimeMillis(),null,null));
            return token;
        }
    }
    static Long create(HttpSession session,String owner,String token,String title,String body,
            java.util.Set<com.example.quanlydetaisinhvien.entity.DoituongThongbao> roles,Supplier<ThongbaoViewDTO> operation) {
        var state=state(session);
        synchronized(state) {
            prune(state);
            String fingerprint=ThongbaoRevision.digest(title.strip(),body,roles);
            // Compatibility with existing form clients without a token; repeated same payload in this session reuses the result.
            boolean legacy=token==null || token.isBlank();
            String key=legacy?"legacy:"+owner+":"+fingerprint:token;
            var entry=state.entries.get(key);
            if(entry==null && !legacy) throw new IllegalArgumentException("Biểu mẫu đã hết hạn; mở lại trang tạo thông báo.");
            if(entry!=null && (!entry.owner().equals(owner) || (entry.fingerprint()!=null && !entry.fingerprint().equals(fingerprint))))
                throw new IllegalArgumentException("Yêu cầu tạo thông báo không khớp biểu mẫu.");
            if(entry!=null && entry.id()!=null) return entry.id();
            Long id=operation.get().id(); // Throw/rollback: keep token unused so validation/database failure can be corrected.
            state.entries.put(key,new Entry(owner,System.currentTimeMillis(),fingerprint,id));
            return id;
        }
    }
    private static void prune(State state) {
        long cutoff=System.currentTimeMillis()-TTL;
        state.entries.values().removeIf(entry->entry.created()<cutoff);
        while(state.entries.size()>=64) state.entries.remove(state.entries.keySet().iterator().next());
    }
}
