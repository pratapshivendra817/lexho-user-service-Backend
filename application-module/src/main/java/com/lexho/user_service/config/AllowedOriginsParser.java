package com.lexho.user_service.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.StringTokenizer;

public final class AllowedOriginsParser {

    private AllowedOriginsParser() {
    }

    public static List<String> parse(String rawOrigins) {
        if (rawOrigins == null || rawOrigins.isBlank()) {
            return List.of();
        }

        LinkedHashSet<String> origins = new LinkedHashSet<>();
        StringTokenizer tokenizer = new StringTokenizer(rawOrigins, ",");
        while (tokenizer.hasMoreTokens()) {
            String origin = tokenizer.nextToken().trim();
            if (!origin.isEmpty()) {
                origins.add(origin);
            }
        }
        return new ArrayList<>(origins);
    }
}
