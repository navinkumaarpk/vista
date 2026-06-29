package com.vista.tools;

import java.util.List;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.vista.model.StatsRecord;
import com.vista.model.Ticket;
import com.vista.stats.StatsRepository;
import com.vista.store.DataStore;

@Component
public class VistaTools {

    private final DataStore store;
    private final StatsRepository statsRepo;

    public VistaTools(DataStore store, StatsRepository statsRepo) {
        this.store = store;
        this.statsRepo = statsRepo;
    }

    @Tool(description = "List all subscriber trouble tickets currently in the system.")
    @McpTool(description = "List all subscriber trouble tickets currently in the system.")
    public List<Ticket> listTickets() {
        return store.allTickets();
    }

    // we won't be doing this at scale
    @Tool(description = "List all stats records (RxMER, flap counts, T3/T4 timeouts) for all service groups.")
    @McpTool(description = "List all stats records (RxMER, flap counts, T3/T4 timeouts) for all service groups.")
    public List<StatsRecord> listStats() {
        return statsRepo.findAll().stream()
            .map(d -> new StatsRecord(d.serviceGroup(), d.cmMac(), d.timestamp(), d.metrics()))
            .toList();
    }

    @Tool(description = "Get the most recent stats records (RxMER, flap counts, T3/T4 timeouts) for a given service group.")
    @McpTool(description = "Get the most recent stats records (RxMER, flap counts, T3/T4 timeouts) for a given service group.")
    public List<StatsRecord> getStatsForServiceGroup(
            @ToolParam(description = "Service group identifier, e.g. SG-7")
            @McpToolParam(description = "Service group identifier, e.g. SG-7", required = true)
            String serviceGroup) {

            //return store.statsForServiceGroup(serviceGroup);   <-- used to return from internal knowledgeBase (cache)
            
            return statsRepo.findByServiceGroupOrderByTimestampDesc(serviceGroup).stream()
            .map(d -> new StatsRecord(d.serviceGroup(), d.cmMac(), d.timestamp(), d.metrics()))
            .toList();
    }

    @Tool(description = "Get only the single latest stats reading for a given service group.")
    @McpTool(description = "Get only the single latest stats reading for a given service group.")
    public StatsRecord getLatestStatsForServiceGroup(
        @ToolParam(description = "Service group identifier, e.g. SG-7")
        @McpToolParam(description = "Service group identifier, e.g. SG-7", required = true)
        String serviceGroup) {
        return statsRepo.findFirstByServiceGroupOrderByTimestampDesc(serviceGroup)
            .map(d -> new StatsRecord(d.serviceGroup(), d.cmMac(), d.timestamp(), d.metrics()))
            .orElse(null);
}

    @Tool(description = "Get trouble tickets associated with a given service group.")
    @McpTool(description = "Get trouble tickets associated with a given service group.")
    public List<Ticket> getTicketsForServiceGroup(
            @ToolParam(description = "Service group identifier, e.g. SG-7")
            @McpToolParam(description = "Service group identifier, e.g. SG-7", required = true)
            String serviceGroup) {
        return store.allTickets().stream()
                .filter(t -> serviceGroup.equals(t.serviceGroup()))
                .toList();
    }
}
