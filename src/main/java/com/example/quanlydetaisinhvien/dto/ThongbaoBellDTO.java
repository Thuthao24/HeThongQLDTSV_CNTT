package com.example.quanlydetaisinhvien.dto;

import java.time.LocalDateTime;
import java.util.*;

public record ThongbaoBellDTO(long unreadCount,List<Item> items) {
    public ThongbaoBellDTO { items=List.copyOf(items); }
    public record Item(Long id,String title,String author,LocalDateTime postedAt,boolean unread) {}
    public record State(long unreadCount,Set<Long> unreadIds) {
        public State { unreadIds=Set.copyOf(unreadIds); }
    }
}
