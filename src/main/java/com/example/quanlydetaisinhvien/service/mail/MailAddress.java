package com.example.quanlydetaisinhvien.service.mail;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.AddressException;

public final class MailAddress {
    private MailAddress() {}
    public static String validOrNull(String value) {
        if (value == null || value.isBlank() || value.length() > 150
                || value.contains("\r") || value.contains("\n")) return null;
        String address = value.trim();
        try {
            var parsed = new InternetAddress(address, true);
            parsed.validate();
            return address.equals(parsed.getAddress()) && parsed.getPersonal() == null ? address : null;
        } catch (AddressException ex) {
            return null;
        }
    }
}
