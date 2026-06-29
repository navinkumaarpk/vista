package com.vista.store;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;

import com.vista.model.Ticket;

@Component
public class DataStore 
{
    private final List<Ticket> tickets = new CopyOnWriteArrayList<>();

    public void addTicket(Ticket t) { tickets.add(t); }

    public List<Ticket> allTickets() { return List.copyOf(tickets); }
         
}
