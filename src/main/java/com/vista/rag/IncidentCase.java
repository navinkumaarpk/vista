package com.vista.rag;

public record IncidentCase(
    String caseId,
    String symptom,
    String classification,   // PLANT, CPE, PROVISIONING
    String resolution
) {}