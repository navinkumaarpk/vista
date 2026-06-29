package com.vista.api;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vista.model.StatsRecord;
import com.vista.model.Ticket;
import com.vista.store.DataStore;

@RestController
@RequestMapping("/api")
public class IngestionController 
{
    private final DataStore store;

    public IngestionController(DataStore store) {
        this.store = store;
    }

    @PostMapping("/tickets")
    public ResponseEntity<Map<String, Object>> ingestTicket(@RequestBody Ticket ticket) {
        Ticket stamped = new Ticket(
                ticket.ticketId(),
                ticket.serviceGroup(),
                ticket.cmMac(),
                ticket.description(),
                ticket.receivedAt() != null ? ticket.receivedAt() : Instant.now(),
                ticket.attributes()
        );
        store.addTicket(stamped);
        return ResponseEntity.ok(Map.of("status", "accepted", "ticketId", stamped.ticketId()));
    }

    @GetMapping("/tickets")
    public List<Ticket> tickets() { return store.allTickets(); }
}
