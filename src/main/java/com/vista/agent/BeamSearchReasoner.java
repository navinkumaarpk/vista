package com.vista.agent;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import com.vista.rag.KnowledgeBaseService;

@Service
public class BeamSearchReasoner {

    private static final Logger log = LoggerFactory.getLogger(BeamSearchReasoner.class);

    private static final int BEAM_WIDTH = 3;
    private static final double PRUNE_THRESHOLD = 0.40;
    private static final double ACCEPT_THRESHOLD = 0.65;
    private static final double TIE_EPSILON = 0.05;
    private static final long BUDGET_MS = 15_000;

    private final ThoughtGenerator generator;
    private final CriticAgent critic;
    private final KnowledgeBaseService kb;

    public BeamSearchReasoner(ThoughtGenerator generator, CriticAgent critic, KnowledgeBaseService kb) {
        this.generator = generator;
        this.critic = critic;
        this.kb = kb;
    }

    public Interpretation reason(String modelKey, Observation obs) {
    List<Document> precedent = kb.findSimilar(obs.summary(), 3, 0.5);
    List<Hypothesis> hypotheses = generator.generate();

    // Score all branches concurrently on virtual threads.
    // Against a single local Ollama instance on your one GPU, the three calls may partially serialize because the GPU is the real bottleneck, so the local speedup is partial. 
    // Against Claude (a remote API) the three genuinely run in parallel and we'll see the full benefit. 
    List<ScoredBranch> scored;
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        List<Future<ScoredBranch>> futures = hypotheses.stream()
                .map(h -> executor.submit(() -> critic.score(modelKey, h, obs, precedent)))
                .toList();
                
        long deadline = System.currentTimeMillis() + BUDGET_MS;
        scored = futures.stream().map(f -> {
            try {
                long remaining = Math.max(0, deadline - System.currentTimeMillis());
                return f.get(remaining, TimeUnit.MILLISECONDS);
            } catch (TimeoutException timeoutEx) {
                f.cancel(true);
                log.warn("  branch dropped: exceeded {}ms latency budget", BUDGET_MS);
                return null;
            } catch (InterruptedException interruptedEx) {
                Thread.currentThread().interrupt();   // restore the interrupt status
                log.warn("  branch interrupted");
                return null;
            } catch (ExecutionException execEx) {
                Throwable cause = execEx.getCause();
                String detail = (cause != null) ? cause.getMessage() : execEx.getMessage();
                log.warn("  branch failed: {}", detail);
                return null;
            }
        }).filter(Objects::nonNull).toList();

        if (scored.isEmpty()) {
            log.warn("All branches timed out or failed; returning UNKNOWN escalation.");
            return new Interpretation("UNKNOWN", 0.30, "No branch completed within the latency budget.");
        }
        
    }  // try-with-resources: close() waits for all virtual threads to finish

    scored.forEach(b -> log.info("  branch {}: composite={} (signal={}, retrieval={}, scope={}, resolution={})",
            b.category(), round(b.composite()), round(b.signalAlignment()),
            round(b.retrievalSupport()), round(b.scopeConsistency()), round(b.resolutionFeasibility())));

    List<ScoredBranch> beam = scored.stream()
            .sorted(Comparator.comparingDouble((ScoredBranch b) -> b.composite()).reversed())
            .limit(BEAM_WIDTH)
            .filter(b -> b.composite() >= PRUNE_THRESHOLD)
            .toList();

        log.info("Beam after pruning ({} survivors): {}", beam.size(),
                beam.stream().map(b -> b.category() + "=" + round(b.composite())).toList());

        if (beam.isEmpty()) {
            log.info("All branches pruned; returning UNKNOWN low-confidence.");
            return new Interpretation("UNKNOWN", 0.30,
                    "All root-cause hypotheses scored below the pruning threshold.");
        }

        // Select best; break ties (within epsilon) by retrieval support
        ScoredBranch best = beam.get(0);
        for (ScoredBranch b : beam) {
            if (Math.abs(b.composite() - best.composite()) <= TIE_EPSILON
                    && b.retrievalSupport() > best.retrievalSupport()) {
                best = b;
            }
        }

        boolean grounded = best.composite() >= ACCEPT_THRESHOLD;
        log.info("Selected {} (composite={}, accepted={})", best.category(), round(best.composite()), grounded);

        return new Interpretation(best.category(), round(best.composite()), best.rationale());
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}