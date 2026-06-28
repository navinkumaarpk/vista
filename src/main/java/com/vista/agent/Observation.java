package com.vista.agent;

import java.util.List;

import com.vista.model.StatsRecord;
import com.vista.model.Ticket;

public record Observation(
    String serviceGroup,
    int ticketCount,
    int highPriorityTicketCount,
    List<StatsRecord> stats,
    List<Ticket> tickets,
    String summary   // deterministic human-readable digest fed to the LLM
) {}
