package com.vista.stats;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("stats")
public record StatsDocument(
        @Id String id,
        String serviceGroup,
        String cmMac,
        Instant timestamp,
        Map<String, Object> metrics
) {}