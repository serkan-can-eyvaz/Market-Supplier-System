package com.example.marketsupplier.agent.nlp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class IntentConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(IntentConfigLoader.class);

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @Value("${agent.intents.config:intents.yaml}")
    private String configPath;

    private final AtomicReference<IntentConfig> cache = new AtomicReference<>();

    public IntentConfigLoader() {
    }

    public synchronized IntentConfig load() {
        try (InputStream is = new ClassPathResource(configPath).getInputStream()) {
            IntentConfig cfg = yamlMapper.readValue(is, IntentConfig.class);
            if (cfg == null) throw new IllegalStateException("intents.yaml parse failed");
            cache.set(cfg);
            log.info("Intents config loaded. version={}, intents={} ", cfg.version, cfg.intents != null ? cfg.intents.size() : 0);
            return cfg;
        } catch (IOException e) {
            log.error("Failed to load intents config from {}", configPath, e);
            IntentConfig fallback = cache.get();
            if (fallback != null) return fallback;
            // minimal fallback
            IntentConfig minimal = new IntentConfig();
            minimal.version = 1;
            minimal.default_intent = "chat";
            minimal.confidence_threshold = 0.75;
            minimal.intents = Collections.emptyList();
            cache.set(minimal);
            return minimal;
        }
    }

    public IntentConfig getCached() {
        IntentConfig cfg = cache.get();
        if (cfg == null) return load();
        return cfg;
    }

    public synchronized void reload() {
        load();
    }

    public Optional<IntentDef> match(String text) {
        if (text == null) return Optional.empty();
        String t = text.toLowerCase(java.util.Locale.ROOT);
        IntentConfig cfg = getCached();
        if (cfg == null || cfg.intents == null) return Optional.empty();
        for (IntentDef def : cfg.intents) {
            if (def.keywords == null) continue;
            for (String kw : def.keywords) {
                if (kw == null) continue;
                String k = kw.trim().toLowerCase(java.util.Locale.ROOT);
                if (!k.isEmpty() && t.contains(k)) {
                    return Optional.of(def);
                }
            }
        }
        return Optional.empty();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IntentConfig {
        public int version;
        public String default_intent;
        public double confidence_threshold;
        public List<IntentDef> intents;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IntentDef {
        public String name;
        public List<String> keywords;
        public String action;
        public String response;
    }
}


