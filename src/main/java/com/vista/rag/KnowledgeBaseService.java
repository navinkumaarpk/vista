package com.vista.rag;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);

    private final VectorStore vectorStore;

    public KnowledgeBaseService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void addCase(IncidentCase c) {
        Document doc = new Document(
                c.symptom(),
                Map.of(
                        "classification", c.classification(),
                        "resolution", c.resolution(),
                        "caseId", c.caseId()
                ));
        vectorStore.add(List.of(doc));
        log.info("Embedded incident case {} (class={})", c.caseId(), c.classification());
    }

    public List<Document> findSimilar(String observationSummary, int topK, double threshold) {
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(observationSummary)
                        .topK(topK)
                        .similarityThreshold(threshold)
                        .build());

        log.info("Retrieval for query [{}]: {} result(s) above threshold {}",
                observationSummary.replaceAll("\\s+", " ").trim(), results.size(), threshold);
        results.forEach(d -> log.debug("  match caseId={} class={} score~={}",
                d.getMetadata().get("caseId"),
                d.getMetadata().get("classification"),
                d.getMetadata().get("distance")));

        return results;
    }
}