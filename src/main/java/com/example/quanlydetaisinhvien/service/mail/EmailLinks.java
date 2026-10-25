package com.example.quanlydetaisinhvien.service.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;

/** Trusted configuration, independent of request Host headers. Only new snapshots use this URL. */
@Component
public class EmailLinks {
    private final String base;
    public EmailLinks(@Value("${app.email.base-url:https://hethongqldtsv-cntt.onrender.com}") String base) {
        URI uri=URI.create(base);
        if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())) || uri.getHost()==null
                || uri.getUserInfo()!=null || uri.getQuery()!=null || uri.getFragment()!=null)
            throw new IllegalArgumentException("URL website trong email không hợp lệ.");
        this.base=base.replaceAll("/+$", "");
    }
    public String results() { return base+"/ketqua"; }
}
