package com.example.postgresql.api;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OtpStore {

    private static final int CODE_TTL_SECONDS = 600; 

    private record Entry(String code, Instant expiresAt) {}

    private static final Map<String, Entry> store = new ConcurrentHashMap<>();

    
    public static void save(String email, String code) {
        store.put(email.toLowerCase(),
                new Entry(code, Instant.now().plusSeconds(CODE_TTL_SECONDS)));
    }

    
    public static boolean verify(String email, String code) {
        Entry entry = store.get(email.toLowerCase());
        if (entry == null) return false;
        if (Instant.now().isAfter(entry.expiresAt())) {
            store.remove(email.toLowerCase());
            return false;
        }
        if (!entry.code().equals(code.trim())) return false;
        store.remove(email.toLowerCase()); 
        return true;
    }

    
    public static void remove(String email) {
        store.remove(email.toLowerCase());
    }
}
