package com.vista.model;

import java.time.Instant;
import java.util.Map;

public record Ticket(
    String ticketId,
    String serviceGroup,
    String cmMac,
    String description,
    Instant receivedAt,
    Map<String, Object> attributes) 
{
    
}
