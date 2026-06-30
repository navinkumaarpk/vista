package com.vista.monitor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import com.vista.guardrail.GuardedTriageService;
import com.vista.model.StatsRecord;
import com.vista.stats.StatsDocument;
import com.vista.tools.VistaTools;

import jakarta.annotation.PostConstruct;

@Service
public class MonitorAgent {

    private static final Logger log = LoggerFactory.getLogger(MonitorAgent.class);
    private static final int MAX_FINDINGS = 50;

    private final TaskScheduler scheduler;
    private final VistaTools tools;
    private final GuardedTriageService guarded;
    private final MongoTemplate mongoTemplate;
    private final MonitorProperties properties;

    private final List<MonitorFinding> findings = new CopyOnWriteArrayList<>();

    private volatile Duration interval;
    private volatile ScheduledFuture<?> task;
    private volatile boolean enabled = false;
    private volatile Instant lastRun = null;
    private volatile MonitorSweep lastSweep = null;

    // remembers the last time we raised a finding per service group
    private final Map<String, Instant> lastFindingAt = new ConcurrentHashMap<>();

    public MonitorAgent(TaskScheduler scheduler, VistaTools tools,
                        GuardedTriageService guarded, MongoTemplate mongoTemplate, MonitorProperties properties) {
        this.scheduler = scheduler;
        this.tools = tools;
        this.guarded = guarded;
        this.mongoTemplate = mongoTemplate;
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        this.interval = Duration.ofSeconds(properties.getIntervalSeconds());
        if (properties.isAutoStart()) start();
    }

    public synchronized void start() {
        stop();
        task = scheduler.scheduleWithFixedDelay(this::pollOnce, interval);
        enabled = true;
        log.info("Monitor started: interval={}s model={}", interval.toSeconds(), properties.getModel());
    }

    public synchronized void stop() {
        if (task != null) { task.cancel(false); task = null; }
        enabled = false;
        log.info("Monitor stopped");
    }

    public synchronized void setIntervalSeconds(long seconds) {
        this.interval = Duration.ofSeconds(seconds);
        log.info("Monitor interval set to {}s", seconds);
        if (enabled) start();   // reschedule with the new interval
    }

    public void pollOnce() {
        long start = System.currentTimeMillis();
        lastRun = Instant.now();
        List<String> groups = serviceGroups();
        int healthy = 0, degraded = 0, raised = 0;
    
        log.info("=== Monitor sweep: scanning {} service group(s) ===", groups.size());
        for (String sg : groups) {
            try {
                StatsRecord latest = tools.getLatestStatsForServiceGroup(sg);
                String reason = screeningReason(latest);
                if (reason == null) { healthy++; continue; }
                degraded++;
    
                log.info("Monitor: {} degraded ({}); running proactive triage", sg, reason);
                var result = guarded.run(properties.getModel(), sg);
                String severity = severityOf(reason, result);
    
                Instant last = lastFindingAt.get(sg);
                boolean withinCooldown = last != null &&
                        last.isAfter(Instant.now().minusSeconds(properties.getDedupCooldownSeconds()));
                if (withinCooldown && !"CRITICAL".equals(severity)) {
                    log.debug("Monitor: {} within cooldown; suppressing duplicate", sg);
                    continue;
                }
    
                MonitorFinding finding = new MonitorFinding(
                        sg, Instant.now(), severity, reason,
                        result.interpretation().classification(),
                        result.interpretation().confidence(),
                        result.requiresHumanApproval(),
                        result.decision().recommendedAction(),
                        result.predictionId());
                findings.add(0, finding);
                lastFindingAt.put(sg, Instant.now());
                while (findings.size() > MAX_FINDINGS) findings.remove(findings.size() - 1);
                raised++;
                log.info("Monitor finding [{}]: {} -> {} (approval={})",
                        severity, sg, finding.classification(), finding.requiresApproval());
            } catch (Exception e) {
                log.warn("Monitor: error processing {}: {}", sg, e.getMessage());
            }
        }
    }

    private String severityOf(String screeningReason, GuardedTriageService.TriageResponse result) {
        // CRITICAL if the agent itself flags high-impact approval or escalates;
        // WARNING if grounded but actionable; INFO otherwise.
        if (result.requiresHumanApproval() || result.decision().escalate()) return "CRITICAL";
        if (result.grounded()) return "WARNING";
        return "INFO";
    }

    // screeningReason is pure arithmetic against thresholds, no LLM, so the Monitor can scan every service group on every poll for almost nothing. 
    // Only groups that breach a threshold get the expensive beam-search triage. 
    
    private String screeningReason(StatsRecord latest) {
        if (latest == null || latest.metrics() == null) return null;
        Map<String, Object> m = latest.metrics();
        double rxmer = toDouble(m.get("rxmer"), 99);
        double flap = toDouble(m.get("flapCount"), 0);
        double t3 = toDouble(m.get("t3Timeouts"), 0);
        if (rxmer < properties.getRxmerFloor()) return "RxMER " + rxmer + " below floor " + properties.getRxmerFloor();
        if (flap > properties.getFlapCeiling()) return "flapCount " + flap + " above ceiling " + properties.getFlapCeiling();
        if (t3 > properties.getT3Ceiling()) return "t3Timeouts " + t3 + " above ceiling " + properties.getT3Ceiling();
        return null;
    }

    private double toDouble(Object o, double fallback) {
        if (o instanceof Number n) return n.doubleValue();
        try { return o == null ? fallback : Double.parseDouble(o.toString()); }
        catch (NumberFormatException e) { return fallback; }
    }

    private List<String> serviceGroups() {
        return mongoTemplate.findDistinct(new Query(), "serviceGroup", StatsDocument.class, String.class);
    }

    // status accessors
    public boolean isEnabled() { return enabled; }
    public long getIntervalSeconds() { return interval.toSeconds(); }
    public Instant getLastRun() { return lastRun; }
    public List<MonitorFinding> getFindings() { return List.copyOf(findings); }
    public String getModel() { return properties.getModel(); }
    public MonitorSweep getLastSweep() { return lastSweep; }

}