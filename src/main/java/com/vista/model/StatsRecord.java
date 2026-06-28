package com.vista.model;

import java.time.Instant;
import java.util.Map;

public record StatsRecord(
    String serviceGroup,
    String cmMac,
    Instant timestamp,
    Map<String, Object> metrics
) 
{
    
}
