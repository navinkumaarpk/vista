package com.vista.prompt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class PromptLibrary {

    private final Map<String, String> prompts = new HashMap<>();

    public PromptLibrary() throws IOException {
        var resolver = new PathMatchingResourcePatternResolver();
        for (Resource r : resolver.getResources("classpath*:prompts/*.md")) {
            String name = r.getFilename().replace(".md", "");
            prompts.put(name, new String(r.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    public String get(String name) {
        String p = prompts.get(name);
        if (p == null) throw new IllegalArgumentException("No prompt named: " + name);
        return p;
    }

    public String render(String name, Map<String, String> vars) {
        String p = get(name);
        for (var e : vars.entrySet()) {
            p = p.replace("{" + e.getKey() + "}", e.getValue());
        }
        return p;
    }
}