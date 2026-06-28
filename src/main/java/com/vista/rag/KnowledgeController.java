package com.vista.rag;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kb")
public class KnowledgeController {

    private final KnowledgeBaseService kb;

    public KnowledgeController(KnowledgeBaseService kb) {
        this.kb = kb;
    }

    @PostMapping("/cases")
    public Map<String, String> addCase(@RequestBody IncidentCase c) {
        kb.addCase(c);
        return Map.of("status", "embedded", "caseId", c.caseId());
    }
}