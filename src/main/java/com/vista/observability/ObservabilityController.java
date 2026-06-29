package com.vista.observability;

import com.vista.guardrail.MetricsService;
import com.vista.guardrail.PredictionLog;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.Search;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/observability")
public class ObservabilityController {

    private final MeterRegistry registry;
    private final MetricsService metrics;
    private final PredictionLog predictions;

    public ObservabilityController(MeterRegistry registry, MetricsService metrics, PredictionLog predictions) {
        this.registry = registry;
        this.metrics = metrics;
        this.predictions = predictions;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        Map<String, Object> out = new LinkedHashMap<>();

        // --- Model usage (from Spring AI gen_ai.* observations) ---
        Timer chat = Search.in(registry).name("gen_ai.client.operation").timer();
        long modelCalls = chat != null ? chat.count() : 0;
        double avgModelMs = chat != null ? chat.mean(java.util.concurrent.TimeUnit.MILLISECONDS) : 0;
        double maxModelMs = chat != null ? chat.max(java.util.concurrent.TimeUnit.MILLISECONDS) : 0;

        // Token totals (gen_ai.* token counters, names vary slightly by version)
        double inputTokens = sumCounter("gen_ai.client.token.usage", "gen_ai.token.type", "input");
        double outputTokens = sumCounter("gen_ai.client.token.usage", "gen_ai.token.type", "output");

        Map<String, Object> model = new LinkedHashMap<>();
        model.put("totalModelCalls", modelCalls);
        model.put("avgResponseMs", round(avgModelMs));
        model.put("slowestResponseMs", round(maxModelMs));
        model.put("inputTokens", (long) inputTokens);
        model.put("outputTokens", (long) outputTokens);
        model.put("totalTokens", (long) (inputTokens + outputTokens));
        out.put("modelUsage", model);

        // --- Agent decisions (from your MetricsService) ---
        out.put("agentDecisions", metrics.snapshot());

        // --- Calibration (deferred ground truth) ---
        out.put("calibration", predictions.calibration());

        return out;
    }

    private double sumCounter(String name, String tagKey, String tagValue) {
        double sum = 0;
        for (var counter : Search.in(registry).name(name).counters()) {
            var tag = counter.getId().getTag(tagKey);
            if (tag == null || tag.equalsIgnoreCase(tagValue)) sum += counter.count();
        }
        return sum;
    }

    private double round(double v) { return Math.round(v * 10.0) / 10.0; }
}