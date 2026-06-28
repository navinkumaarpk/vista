package com.vista.store;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;

import com.vista.model.StatsRecord;
import com.vista.model.Ticket;

@Component
public class DataStore 
{
    private final List<Ticket> tickets = new CopyOnWriteArrayList<>();
    private final List<StatsRecord> stats = new CopyOnWriteArrayList<>();

    public void addTicket(Ticket t) { tickets.add(t); }
    public void addStats(StatsRecord s) { stats.add(s); }

    public List<Ticket> allTickets() { return List.copyOf(tickets); }
    public List<StatsRecord> allStats() { return List.copyOf(stats); }

    public List<StatsRecord> statsForServiceGroup(String sg) 
    {
        return stats.stream().filter(s -> sg.equals(s.serviceGroup())).toList();
    }    
}
