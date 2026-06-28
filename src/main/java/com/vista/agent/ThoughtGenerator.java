package com.vista.agent;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class ThoughtGenerator {

    private static final List<String> CATEGORIES = List.of("PLANT", "CPE", "PROVISIONING");

    public List<Hypothesis> generate() {
        return CATEGORIES.stream().map(Hypothesis::new).toList();
    }
}