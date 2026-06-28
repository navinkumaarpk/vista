package com.vista.agent;

import java.util.List;

import org.springframework.stereotype.Service;

import com.vista.model.StatsRecord;
import com.vista.model.Ticket;
import com.vista.tools.VistaTools;

@Service
public class ObserveStep {

    private final VistaTools tools;

    public ObserveStep(VistaTools tools) {
        this.tools = tools;
    }

    public Observation observe(String serviceGroup) {
        List<StatsRecord> stats = tools.getStatsForServiceGroup(serviceGroup);
        List<Ticket> tickets = tools.getTicketsForServiceGroup(serviceGroup);

        int highPriority = (int) tickets.stream()
                .filter(t -> t.attributes() != null
                        && "high".equalsIgnoreCase(String.valueOf(t.attributes().get("priority"))))
                .count();

        String summary = buildSummary(serviceGroup, stats, tickets, highPriority);
        return new Observation(serviceGroup, tickets.size(), highPriority, stats, tickets, summary);
    }

    private String buildSummary(String sg, List<StatsRecord> stats, List<Ticket> tickets, int high) {
        StringBuilder sb = new StringBuilder();
        sb.append("Service group ").append(sg).append(": ")
          .append(tickets.size()).append(" ticket(s), ")
          .append(high).append(" high priority.\n");
        if (stats.isEmpty()) {
            sb.append("No stats records available.\n");
        } else {
            sb.append("Latest stats records:\n");
            stats.forEach(s -> sb.append("  ").append(s.metrics()).append("\n"));
        }
        return sb.toString();
    }
}
